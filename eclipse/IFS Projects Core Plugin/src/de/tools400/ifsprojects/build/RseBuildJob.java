/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.build;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.swt.widgets.Display;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.CommandCall;
import com.ibm.as400.access.IFSFile;

import de.tools400.ifsprojects.IfsProjectsPlugin;
import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.ibmi.PaseCommands;
import de.tools400.ifsprojects.local.SourceMapping;
import de.tools400.ifsprojects.remote.RemoteJobs;
import de.tools400.ifsprojects.utils.PlatformUtils;
import de.tools400.ifsprojects.utils.StringUtils;

/**
 * Runs the build or compile command of an IFS project in a remote command
 * server job of the RSE connection instead of the SSH terminal.
 * <p>
 * The output is shown line by line in the 'Commands Log' view. Afterwards the
 * events files of the compiled objects are shown in the 'Error List' view, see
 * {@link BuildEvents}.
 * <p>
 * Builds of the same project run one after the other. A canceled build is
 * stopped by terminating its processes, see
 * {@link RseBuildCommands#getStopScript(String)}.
 */
public class RseBuildJob extends Job {

    private static final long POLL_INTERVAL_MILLIS = 500;
    private static final String LOG_FILE_SUFFIX = ".log"; //$NON-NLS-1$
    private static final String EXIT_CODE_FILE_SUFFIX = ".rc"; //$NON-NLS-1$
    private static final String PROCESS_ID_FILE_SUFFIX = ".pid"; //$NON-NLS-1$
    private static final long STOP_TIMEOUT_MILLIS = 30000;
    private static final String KEY_SEPARATOR = ":"; //$NON-NLS-1$

    private final IHost host;
    private final AS400 rseSystem;
    private final String directory;
    private final String objectLibrary;
    private final String displayCommand;
    private final String command;
    private final SourceMapping sourceMapping;

    /**
     * Creates the job.
     *
     * @param aName - name of the job, e.g. "Building DSPSRC ..."
     * @param aHost - RSE connection
     * @param aRseSystem - connected system object of RSE, which is copied
     * @param aDirectory - project directory
     * @param anObjectLibrary - library of the compiled objects or
     *        <code>null</code>, see {@link RseBuildCommands#getObjectLibrary}
     * @param aDisplayCommand - command shown in the 'Commands Log' view, e.g.
     *        <code>makei build</code>
     * @param aCommand - command including the exports of the environment
     *        variables
     * @param aSourceMapping - mapping of the project directory to a local
     *        project or <code>null</code>, see {@link BuildErrors#show}
     */
    public RseBuildJob(String aName, IHost aHost, AS400 aRseSystem, String aDirectory, String anObjectLibrary, String aDisplayCommand,
        String aCommand, SourceMapping aSourceMapping) {
        super(aName);

        this.host = aHost;
        this.rseSystem = aRseSystem;
        this.directory = aDirectory;
        this.objectLibrary = anObjectLibrary;
        this.displayCommand = aDisplayCommand;
        this.command = aCommand;
        this.sourceMapping = aSourceMapping;

        setRule(new ProjectRule(aHost.getAliasName() + KEY_SEPARATOR + aDirectory));
    }

    @Override
    protected IStatus run(IProgressMonitor aMonitor) {

        aMonitor.beginTask(getName(), IProgressMonitor.UNKNOWN);

        CommandsLog.show(host);
        CommandsLog.log(host, Messages.bind(Messages.Log_Running_A_in_B, displayCommand, directory));

        AS400 system = RemoteJobs.copySystem(rseSystem);
        IFSFile logFile = null;
        IFSFile exitCodeFile = null;
        IFSFile processIdFile = null;
        try {

            CommandCall commandCall = RemoteJobs.createCommandCall(system);
            String temporaryDirectory = RemoteJobs.getTemporaryDirectory(system);
            RemoteJobs.deleteObsoleteTemporaryFiles(system, temporaryDirectory);
            String home = RemoteJobs.getHomeDirectory(system);
            String shell = new IFSFile(system, PaseCommands.BASH).exists() ? PaseCommands.BASH : PaseCommands.SH;

            logFile = new IFSFile(system, RemoteJobs.getTemporaryFile(temporaryDirectory, LOG_FILE_SUFFIX));
            logFile.createNewFile();
            logFile.setCCSID(RemoteJobs.CCSID_UTF8);
            long startTime = logFile.lastModified();

            exitCodeFile = new IFSFile(system, RemoteJobs.getTemporaryFile(temporaryDirectory, EXIT_CODE_FILE_SUFFIX));
            processIdFile = new IFSFile(system, RemoteJobs.getTemporaryFile(temporaryDirectory, PROCESS_ID_FILE_SUFFIX));

            String script = RseBuildCommands.getScript(shell, home, logFile.getAbsolutePath(), exitCodeFile.getAbsolutePath(),
                processIdFile.getAbsolutePath(), directory, command);
            CommandThread commandThread = new CommandThread(commandCall, PaseCommands.getCallCommand(shell, script));
            commandThread.start();

            LogLines logLines = new LogLines();
            long offset = 0;
            while (commandThread.isAlive()) {
                if (aMonitor.isCanceled()) {
                    boolean isStopped = stop(processIdFile.getAbsolutePath(), commandThread);
                    logNewLines(logFile, offset, logLines);
                    if (isStopped) {
                        CommandsLog.log(host, Messages.Log_Canceled);
                    } else {
                        // Ends the remote command server job. Jobs started by
                        // the command may still be running.
                        system.disconnectAllServices();
                        CommandsLog.log(host, Messages.Log_Canceled_command_may_still_be_running);
                    }
                    return Status.CANCEL_STATUS;
                }
                commandThread.join(POLL_INTERVAL_MILLIS);
                offset = logNewLines(logFile, offset, logLines);
            }
            logNewLines(logFile, offset, logLines);
            String lastLine = logLines.flush();
            if (lastLine != null) {
                CommandsLog.log(host, lastLine);
            }

            if (commandThread.getException() != null) {
                throw commandThread.getException();
            }

            if (!commandThread.isSuccessful()) {
                CommandsLog.log(host, RemoteJobs.getMessages(commandCall));
            }

            Integer exitCode = exitCodeFile.exists() ? RseBuildCommands.parseExitCode(RemoteJobs.readText(exitCodeFile)) : null;
            if (exitCode == null) {
                CommandsLog.log(host, Messages.Log_Finished);
            } else {
                CommandsLog.log(host, Messages.bind(Messages.Log_Finished_with_exit_code_A, exitCode));
            }

            String events = BuildEvents.collect(system, commandCall, directory, objectLibrary, startTime, temporaryDirectory);
            if (events == null) {
                CommandsLog.log(host, Messages.Log_No_events_files_found);
            } else {
                BuildErrors.show(getName(), host, directory, events, RemoteJobs.getJobCcsid(commandCall, system), sourceMapping);
            }

        } catch (InterruptedException e) {
            return Status.CANCEL_STATUS;
        } catch (Exception e) {
            IfsProjectsPlugin.logError("Could not run command: " + displayCommand, e); //$NON-NLS-1$
            String message = Messages.bind(Messages.Error_Could_not_run_A_B, displayCommand, e.getLocalizedMessage());
            CommandsLog.log(host, message);
            showError(message);
        } finally {
            if (logFile != null) {
                RemoteJobs.delete(logFile);
            }
            if (exitCodeFile != null) {
                RemoteJobs.delete(exitCodeFile);
            }
            if (processIdFile != null) {
                RemoteJobs.delete(processIdFile);
            }
            system.disconnectAllServices();
            aMonitor.done();
        }

        return Status.OK_STATUS;
    }

    /**
     * Terminates the shell of the build and all its descendants in a job of
     * its own and waits, until the build command has ended.
     *
     * @return <code>true</code>, if the build command has ended
     */
    private boolean stop(String aProcessIdFile, Thread aCommandThread) throws InterruptedException {

        AS400 stopSystem = RemoteJobs.copySystem(rseSystem);
        try {
            CommandCall commandCall = RemoteJobs.createCommandCall(stopSystem);
            commandCall.run(PaseCommands.getCallCommand(PaseCommands.SH, RseBuildCommands.getStopScript(aProcessIdFile)));
        } catch (Exception e) {
            IfsProjectsPlugin.logError("Could not stop command: " + displayCommand, e); //$NON-NLS-1$
        } finally {
            stopSystem.disconnectAllServices();
        }

        aCommandThread.join(STOP_TIMEOUT_MILLIS);

        return !aCommandThread.isAlive();
    }

    /**
     * Logs the lines, that have been appended to the log file.
     *
     * @return new offset
     */
    private long logNewLines(IFSFile aLogFile, long anOffset, LogLines aLogLines) throws Exception {

        byte[] bytes = RemoteJobs.readBytes(aLogFile, anOffset);
        for (String line : aLogLines.add(bytes)) {
            // Empty lines would be logged as empty comments.
            if (!StringUtils.isBlank(line)) {
                CommandsLog.log(host, line);
            }
        }

        return anOffset + bytes.length;
    }

    private void showError(final String aMessage) {

        Display.getDefault().asyncExec(new Runnable() {
            public void run() {
                MessageDialog.openError(PlatformUtils.getWorkbenchShell(), Messages.Title_IFS_Projects, aMessage);
            }
        });
    }

    /**
     * Runs the CL command, which runs the shell, while the job reads the log
     * file.
     */
    private static class CommandThread extends Thread {

        private final CommandCall commandCall;
        private final String clCommand;
        private volatile boolean isSuccessful;
        private volatile Exception exception;

        public CommandThread(CommandCall aCommandCall, String aClCommand) {
            super("IFS Projects build"); //$NON-NLS-1$
            this.commandCall = aCommandCall;
            this.clCommand = aClCommand;
            setDaemon(true);
        }

        @Override
        public void run() {
            try {
                isSuccessful = commandCall.run(clCommand);
            } catch (Exception e) {
                exception = e;
            }
        }

        public boolean isSuccessful() {
            return isSuccessful;
        }

        public Exception getException() {
            return exception;
        }
    }

    /**
     * Lets the builds of the same project run one after the other.
     */
    private static class ProjectRule implements ISchedulingRule {

        private final String key;

        public ProjectRule(String aKey) {
            this.key = aKey;
        }

        public boolean contains(ISchedulingRule aRule) {
            return aRule == this;
        }

        public boolean isConflicting(ISchedulingRule aRule) {
            return aRule instanceof ProjectRule && ((ProjectRule)aRule).key.equals(key);
        }
    }
}
