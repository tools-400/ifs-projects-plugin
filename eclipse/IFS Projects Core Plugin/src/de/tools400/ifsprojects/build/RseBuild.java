/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.build;

import java.io.IOException;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.rse.core.model.IHost;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.CommandCall;
import com.ibm.as400.access.IFSFile;

import de.tools400.ifsprojects.IfsProjectsPlugin;
import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.ibmi.PaseCommands;
import de.tools400.ifsprojects.project.IProjConfig;
import de.tools400.ifsprojects.project.IProjValues;
import de.tools400.ifsprojects.remote.RemoteJobs;
import de.tools400.ifsprojects.remote.RemoteSession;
import de.tools400.ifsprojects.utils.ExceptionUtils;
import de.tools400.ifsprojects.utils.StringUtils;

/**
 * Runs the build or compile command of an IFS project in a remote command
 * server job of the RSE connection, without SSH, and reads the events files of
 * the compiled objects afterwards, see {@link BuildEvents}.
 * <p>
 * The output of the command and the messages of the build are passed to an
 * {@link IOutput} line by line. A canceled build is stopped by terminating its
 * processes, see {@link RseBuildCommands#getStopScript(String)}. The class
 * opens no view and no dialog; {@link RseBuildJob} shows the build in the views
 * of RDi.
 * <p>
 * The methods access the remote system and must not be called on the UI thread.
 */
public final class RseBuild {

    private static final long POLL_INTERVAL_MILLIS = 500;
    private static final String LOG_FILE_SUFFIX = ".log"; //$NON-NLS-1$
    private static final String EXIT_CODE_FILE_SUFFIX = ".rc"; //$NON-NLS-1$
    private static final String PROCESS_ID_FILE_SUFFIX = ".pid"; //$NON-NLS-1$
    private static final long STOP_TIMEOUT_MILLIS = 30000;
    private static final String KEY_SEPARATOR = ":"; //$NON-NLS-1$

    private final AS400 rseSystem;
    private final String directory;
    private final String objectLibrary;
    private final String displayCommand;
    private final String command;

    private RseBuild(AS400 aRseSystem, String aDirectory, String anObjectLibrary, String aDisplayCommand, String aCommand) {
        this.rseSystem = aRseSystem;
        this.directory = aDirectory;
        this.objectLibrary = anObjectLibrary;
        this.displayCommand = aDisplayCommand;
        this.command = aCommand;
    }

    /**
     * Creates the build of a command of a project: the command runs with the
     * environment variables of the project, and the events files are searched
     * in its object library, see {@link IProjValues#getObjectLibrary}.
     *
     * @param aRseSystem - connected system object of RSE, which is copied
     * @param aDirectory - project directory
     * @param aConfig - content of <code>iproj.json</code>
     * @param aVariables - environment variables of the project
     * @param aCommand - build or compile command with replaced placeholders
     * @return build
     */
    public static RseBuild create(AS400 aRseSystem, String aDirectory, IProjConfig aConfig, Map<String, String> aVariables, String aCommand) {
        return new RseBuild(aRseSystem, aDirectory, IProjValues.getObjectLibrary(aConfig, aVariables), aCommand.trim(),
            PaseCommands.addExports(aVariables, aCommand));
    }

    /**
     * Returns the scheduling rule, that lets the builds of the same project run
     * one after the other.
     *
     * @param aHost - RSE connection
     * @param aDirectory - project directory
     * @return rule
     */
    public static ISchedulingRule getRule(IHost aHost, String aDirectory) {
        return new ProjectRule(aHost.getAliasName() + KEY_SEPARATOR + aDirectory);
    }

    /**
     * Returns the project directory, in which the command runs.
     *
     * @return directory
     */
    public String getDirectory() {
        return directory;
    }

    /**
     * Returns the command, that is named in the output.
     *
     * @return command, e.g. <code>makei build</code>
     */
    public String getDisplayCommand() {
        return displayCommand;
    }

    /**
     * Runs the command and reads the events files of the compiled objects.
     *
     * @param anOutput - receives the output of the command and the messages of
     *        the build
     * @param aMonitor - progress monitor; it shows the step of the build, and
     *        its cancellation stops the build
     * @return result
     * @throws OperationCanceledException if the build has been canceled
     * @throws InterruptedException if the thread has been interrupted
     * @throws Exception if the command could not be run
     */
    public Result run(IOutput anOutput, IProgressMonitor aMonitor) throws Exception {

        try (RemoteSession session = new RemoteSession(rseSystem)) {

            AS400 system = session.getSystem();
            String shell = new IFSFile(system, PaseCommands.BASH).exists() ? PaseCommands.BASH : PaseCommands.SH;

            IFSFile logFile = session.newTemporaryFile(LOG_FILE_SUFFIX);
            logFile.createNewFile();
            logFile.setCCSID(RemoteJobs.CCSID_UTF8);
            long startTime = logFile.lastModified();

            IFSFile exitCodeFile = session.newTemporaryFile(EXIT_CODE_FILE_SUFFIX);
            IFSFile processIdFile = session.newTemporaryFile(PROCESS_ID_FILE_SUFFIX);

            String script = RseBuildCommands.getScript(shell, RemoteJobs.getHomeDirectory(system), logFile.getAbsolutePath(),
                exitCodeFile.getAbsolutePath(), processIdFile.getAbsolutePath(), directory, command);
            CommandThread commandThread = new CommandThread(session.getCommandCall(), PaseCommands.getCallCommand(shell, script));
            commandThread.start();

            if (!logOutput(system, commandThread, logFile, processIdFile, anOutput, aMonitor)) {
                throw new OperationCanceledException();
            }

            Integer exitCode = logResult(commandThread, session.getCommandCall(), exitCodeFile, anOutput);

            String events = readEvents(session, startTime, anOutput, aMonitor);

            return new Result(exitCode, events, session.getJobCcsid());
        }
    }

    /**
     * Reads the events files of the compiled objects, see {@link BuildEvents}.
     * Errors are logged and passed to the output.
     *
     * @return events or <code>null</code>, if there are none or they could not
     *         be read
     */
    private String readEvents(RemoteSession aSession, long aStartTime, IOutput anOutput, IProgressMonitor aMonitor) {

        String events;
        try {
            events = BuildEvents.collect(aSession, directory, objectLibrary, aStartTime, aMonitor);
        } catch (IOException e) {
            IfsProjectsPlugin.logError("Could not read the events files of: " + directory, e); //$NON-NLS-1$
            anOutput.line(ExceptionUtils.getLocalizedMessage(e));
            return null;
        }

        if (events == null) {
            anOutput.line(Messages.Log_No_events_files_found);
        }

        return events;
    }

    /**
     * Passes the output of the command line by line to the output, until the
     * command has ended or the build has been canceled. The command of a
     * canceled build is stopped, see {@link #stop(String, Thread)}.
     *
     * @param aSystem - system object of the job, which runs the command
     * @return <code>false</code>, if the build has been canceled
     */
    private boolean logOutput(AS400 aSystem, CommandThread aCommandThread, IFSFile aLogFile, IFSFile aProcessIdFile, IOutput anOutput,
        IProgressMonitor aMonitor) throws Exception {

        LogLines logLines = new LogLines();
        BuildProgress progress = new BuildProgress();
        long offset = 0;
        while (aCommandThread.isAlive()) {
            if (aMonitor.isCanceled()) {
                boolean isStopped = stop(aProcessIdFile.getAbsolutePath(), aCommandThread);
                logNewLines(aLogFile, offset, logLines, progress, anOutput, aMonitor);
                if (isStopped) {
                    anOutput.line(Messages.Log_Canceled);
                } else {
                    // Ends the remote command server job. Jobs started by the
                    // command may still be running.
                    aSystem.disconnectAllServices();
                    anOutput.line(Messages.Log_Canceled_command_may_still_be_running);
                }
                return false;
            }
            aCommandThread.join(POLL_INTERVAL_MILLIS);
            offset = logNewLines(aLogFile, offset, logLines, progress, anOutput, aMonitor);
        }

        logNewLines(aLogFile, offset, logLines, progress, anOutput, aMonitor);
        String lastLine = logLines.flush();
        if (lastLine != null) {
            anOutput.line(lastLine);
        }

        return true;
    }

    /**
     * Passes the end of the command to the output: the messages of a failed CL
     * command and the exit code of the shell.
     *
     * @return exit code of the shell or <code>null</code>, if it is not known
     * @throws Exception if the CL command could not be run
     */
    private Integer logResult(CommandThread aCommandThread, CommandCall aCommandCall, IFSFile anExitCodeFile, IOutput anOutput) throws Exception {

        if (aCommandThread.getException() != null) {
            throw aCommandThread.getException();
        }

        if (!aCommandThread.isSuccessful()) {
            anOutput.line(RemoteJobs.getMessages(aCommandCall));
        }

        Integer exitCode = anExitCodeFile.exists() ? RseBuildCommands.parseExitCode(RemoteJobs.readText(anExitCodeFile)) : null;
        if (exitCode == null) {
            anOutput.line(Messages.Log_Finished);
        } else {
            anOutput.line(Messages.bind(Messages.Log_Finished_with_exit_code_A, exitCode));
        }

        return exitCode;
    }

    /**
     * Terminates the shell of the build and all its descendants in a job of its
     * own and waits, until the build command has ended.
     *
     * @return <code>true</code>, if the build command has ended
     */
    private boolean stop(String aProcessIdFile, Thread aCommandThread) throws InterruptedException {

        try (RemoteSession stopSession = new RemoteSession(rseSystem)) {
            stopSession.getCommandCall().run(PaseCommands.getCallCommand(PaseCommands.SH, RseBuildCommands.getStopScript(aProcessIdFile)));
        } catch (Exception e) {
            IfsProjectsPlugin.logError("Could not stop command: " + displayCommand, e); //$NON-NLS-1$
        }

        aCommandThread.join(STOP_TIMEOUT_MILLIS);

        return !aCommandThread.isAlive();
    }

    /**
     * Passes the lines, that have been appended to the log file, to the output
     * and shows the step of the build and the number of created and failed
     * objects in the progress monitor, see {@link BuildProgress}.
     *
     * @return new offset
     */
    private long logNewLines(IFSFile aLogFile, long anOffset, LogLines aLogLines, BuildProgress aProgress, IOutput anOutput,
        IProgressMonitor aMonitor) throws Exception {

        byte[] bytes = RemoteJobs.readBytes(aLogFile, anOffset);
        boolean isProgressChanged = false;
        for (String line : aLogLines.add(bytes)) {
            // Empty lines would be logged as empty comments.
            if (!StringUtils.isBlank(line)) {
                anOutput.line(line);
                if (aProgress.add(line)) {
                    isProgressChanged = true;
                }
            }
        }

        if (isProgressChanged && aProgress.getStep() != null) {
            aMonitor.subTask(Messages.bind(Messages.Task_Build_progress_A_B_C,
                new Object[] { Integer.toString(aProgress.getCreatedCount()), Integer.toString(aProgress.getFailedCount()), aProgress.getStep() }));
        }

        return anOffset + bytes.length;
    }

    /**
     * Receives the output of the command and the messages of the build.
     */
    public interface IOutput {

        /**
         * Receives a line. Called on the thread of the build.
         *
         * @param aLine - line, not empty
         */
        void line(String aLine);
    }

    /**
     * Result of a build, that has ended.
     */
    public static final class Result {

        private final Integer exitCode;
        private final String events;
        private final int ccsid;

        private Result(Integer anExitCode, String anEvents, int aCcsid) {
            this.exitCode = anExitCode;
            this.events = anEvents;
            this.ccsid = aCcsid;
        }

        /**
         * Returns the exit code of the shell.
         *
         * @return exit code or <code>null</code>, if it is not known, e.g.
         *         because the CL command failed
         */
        public Integer getExitCode() {
            return exitCode;
        }

        /**
         * Returns the events of the compiled objects, one events file after the
         * other.
         *
         * @return events or <code>null</code>, if no events files have been
         *         found or they could not be read
         */
        public String getEvents() {
            return events;
        }

        /**
         * Returns the CCSID of the job, in which the events files were read.
         *
         * @return CCSID
         */
        public int getCcsid() {
            return ccsid;
        }
    }

    /**
     * Runs the CL command, which runs the shell, while the build reads the log
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
