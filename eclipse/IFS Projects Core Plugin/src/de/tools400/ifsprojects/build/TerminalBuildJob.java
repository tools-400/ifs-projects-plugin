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
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.rse.core.model.IHost;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.CommandCall;
import com.ibm.as400.access.IFSFile;

import de.tools400.ifsprojects.IfsProjectsPlugin;
import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.errorlist.ErrorList;
import de.tools400.ifsprojects.ibmi.PaseCommands;
import de.tools400.ifsprojects.local.SourceMapping;
import de.tools400.ifsprojects.project.IProjValues;
import de.tools400.ifsprojects.remote.RemoteJobs;
import de.tools400.ifsprojects.remote.RemoteSession;
import de.tools400.ifsprojects.terminal.ITerminalCommandLine;

/**
 * Waits for the end of a build or compile command, that runs in the SSH
 * terminal of the project, and shows the events files of the compiled objects
 * in the 'Error List' view like {@link RseBuildJob}, see {@link BuildEvents}.
 * <p>
 * The command writes marker files, see {@link TerminalBuildCommands}. The job
 * polls them with a copy of the RSE system object:
 * <ul>
 * <li>Until the start file exists, the shell has not started the command. If
 * the terminal session is closed until then, the command is never executed
 * and the job ends. The job also ends, if the shell has not started the
 * command 60 seconds after it has been written to the terminal, e.g. because
 * the project directory does not exist, the prompt line contained input or
 * the command line has been discarded with Ctrl+C.</li>
 * <li>The exit code file shows, that the command has ended.</li>
 * <li>A killed subshell does not write the exit code file. Therefore the job
 * checks every 30 seconds, whether the subshell still exists.</li>
 * </ul>
 * Until the job ends, the terminal session does not receive the command lines
 * of other builds, see {@link ITerminalCommandLine#setRunning(boolean)}.
 * Canceling the job only stops waiting. The command is interrupted with
 * Ctrl+C in the terminal.
 */
public class TerminalBuildJob extends Job {

    private static final long POLL_INTERVAL_MILLIS = 1000;
    private static final long CHECK_INTERVAL_MILLIS = 30000;

    /**
     * Time after the command line has been written to the terminal, until the
     * job stops waiting for the start file.
     */
    private static final long START_TIMEOUT_MILLIS = 60000;

    /**
     * Number of polls after the terminal session has been closed, until the
     * job stops waiting for the start file.
     */
    private static final int CLOSED_SESSION_POLLS = 2;

    /** Start time, if the shell has not started the command. */
    private static final long NOT_STARTED = -1;

    private final IHost host;
    private final AS400 rseSystem;
    private final String directory;
    private final String objectLibrary;
    private final SourceMapping sourceMapping;
    private final String temporaryDirectory;
    private final String id;

    private ITerminalCommandLine commandLine;

    private TerminalBuildJob(String aName, IHost aHost, AS400 aRseSystem, String aDirectory, String anObjectLibrary,
        SourceMapping aSourceMapping, String aTemporaryDirectory, String anId) {
        super(aName);

        this.host = aHost;
        this.rseSystem = aRseSystem;
        this.directory = aDirectory;
        this.objectLibrary = anObjectLibrary;
        this.sourceMapping = aSourceMapping;
        this.temporaryDirectory = aTemporaryDirectory;
        this.id = anId;
    }

    /**
     * Prepares a build: installs the script, that writes the marker files,
     * and creates the ID of the marker files.
     * <p>
     * Must not be called on the UI thread.
     *
     * @param aName - name of the job, e.g. "Building DSPSRC ..."
     * @param aHost - RSE connection
     * @param aRseSystem - connected system object of RSE, which is copied
     * @param aDirectory - project directory
     * @param anObjectLibrary - library of the compiled objects or
     *        <code>null</code>, see {@link IProjValues#getObjectLibrary}
     * @param aSourceMapping - mapping of the project directory to a local
     *        project or <code>null</code>, see {@link ErrorList#showEvents}
     * @return job, that is started with {@link #start(ITerminalCommandLine)}
     * @throws Exception if the script could not be installed
     */
    public static TerminalBuildJob prepare(String aName, IHost aHost, AS400 aRseSystem, String aDirectory, String anObjectLibrary,
        SourceMapping aSourceMapping) throws Exception {

        String temporaryDirectory = RemoteJobs.getTemporaryDirectory(aRseSystem);
        installScript(aRseSystem, temporaryDirectory);

        return new TerminalBuildJob(aName, aHost, aRseSystem, aDirectory, anObjectLibrary, aSourceMapping, temporaryDirectory,
            RemoteJobs.createTemporaryFileId());
    }

    /**
     * Writes the script, that writes the marker files, if it does not exist
     * or differs. An existing script is deleted before, so that a shell, that
     * is reading it, keeps the old content.
     *
     * @param aSystem - system object
     * @param aTemporaryDirectory - directory of temporary files, see
     *        {@link RemoteJobs#getTemporaryDirectory(AS400)}
     * @throws Exception if the script could not be written
     */
    public static void installScript(AS400 aSystem, String aTemporaryDirectory) throws Exception {

        String script = TerminalBuildCommands.getScript(RemoteJobs.getTemporaryFilePrefix(aTemporaryDirectory));

        IFSFile file = new IFSFile(aSystem, TerminalBuildCommands.getScriptFile(aTemporaryDirectory));
        if (file.exists()) {
            if (file.getCCSID() == RemoteJobs.CCSID_UTF8 && script.equals(RemoteJobs.readText(file))) {
                return;
            }
            file.delete();
        }

        RemoteJobs.writeText(file, script, RemoteJobs.CCSID_UTF8);
    }

    /**
     * Returns the command wrapped in a subshell, that writes the marker files
     * of this job, see {@link TerminalBuildCommands#wrap}.
     *
     * @param aCommand - command, e.g. <code>makei build</code>
     * @return wrapped command or <code>null</code>, if the command cannot be
     *         wrapped
     */
    public String wrap(String aCommand) {
        return TerminalBuildCommands.wrap(aCommand, id, TerminalBuildCommands.getScriptFile(temporaryDirectory));
    }

    /**
     * Starts the job, after the wrapped command line has been passed to the
     * terminal. Until the job ends, the command is marked as running.
     *
     * @param aCommandLine - command line passed to the terminal
     */
    public void start(final ITerminalCommandLine aCommandLine) {
        this.commandLine = aCommandLine;

        // done() is also called, if the job is canceled before it runs.
        aCommandLine.setRunning(true);
        addJobChangeListener(new JobChangeAdapter() {
            @Override
            public void done(IJobChangeEvent anEvent) {
                aCommandLine.setRunning(false);
            }
        });

        schedule();
    }

    @Override
    protected IStatus run(IProgressMonitor aMonitor) {

        // The 'Progress' view shows the name of the job and below the task
        // name and the subtask, shortened to the width of the view. Without
        // task name, it only shows the subtask.
        aMonitor.beginTask("", IProgressMonitor.UNKNOWN); //$NON-NLS-1$
        aMonitor.subTask(Messages.Task_Waiting_for_command_to_start);

        try (RemoteSession session = new RemoteSession(rseSystem)) {

            IFSFile startFile = session.newTemporaryFile(id, TerminalBuildCommands.START_FILE_SUFFIX);
            IFSFile exitCodeFile = session.newTemporaryFile(id, TerminalBuildCommands.EXIT_CODE_FILE_SUFFIX);
            IFSFile goneFile = session.newTemporaryFile(id, TerminalBuildCommands.GONE_FILE_SUFFIX);

            long startTime = waitForStart(startFile, exitCodeFile, aMonitor);
            if (startTime == NOT_STARTED) {
                return Status.OK_STATUS;
            }

            if (startTime == 0) {
                // Short command: the exit code file has been found first.
                startTime = startFile.exists() ? startFile.lastModified() : exitCodeFile.lastModified();
            } else if (waitForEnd(session.getCommandCall(), startFile, exitCodeFile, goneFile, aMonitor)) {
                IfsProjectsPlugin.logWarning("The command in the terminal has ended without exit code, e.g. because it has been killed: " + directory); //$NON-NLS-1$
            }

            String events = BuildEvents.collect(session, directory, objectLibrary, startTime);
            if (events != null) {
                ErrorList.showEvents(getName(), host, directory, events, session.getJobCcsid(), sourceMapping);
            }

        } catch (InterruptedException e) {
            return Status.CANCEL_STATUS;
        } catch (Exception e) {
            IfsProjectsPlugin.logError("Could not wait for the end of the command in the terminal: " + directory, e); //$NON-NLS-1$
        } finally {
            aMonitor.done();
        }

        return Status.OK_STATUS;
    }

    /**
     * Waits, until the shell has started the command: the start file exists.
     *
     * @return modification time of the start file, 0, if the command has
     *         already ended, or {@link #NOT_STARTED}, if the shell has not
     *         started the command: the terminal session has been closed or the
     *         command has not started in time
     * @throws InterruptedException if the job has been canceled
     */
    private long waitForStart(IFSFile aStartFile, IFSFile anExitCodeFile, IProgressMonitor aMonitor) throws Exception {

        long sentTime = 0;
        int closedSessionPolls = 0;

        while (!anExitCodeFile.exists()) {

            checkCanceled(aMonitor);

            long startTime = aStartFile.exists() ? aStartFile.lastModified() : 0;
            if (startTime != 0) {
                return startTime;
            }

            if (commandLine != null && commandLine.isSessionClosed() && ++closedSessionPolls > CLOSED_SESSION_POLLS) {
                // The shell has not read the command line.
                return NOT_STARTED;
            }
            if (sentTime == 0 && commandLine != null && commandLine.isSent()) {
                sentTime = System.currentTimeMillis();
            }
            if (sentTime != 0 && System.currentTimeMillis() - sentTime >= START_TIMEOUT_MILLIS) {
                IfsProjectsPlugin.logWarning("The command in the terminal has not started in time, e.g. because the directory does not exist: " + directory); //$NON-NLS-1$
                return NOT_STARTED;
            }

            Thread.sleep(POLL_INTERVAL_MILLIS);
        }

        return 0;
    }

    /**
     * Waits, until the command has ended: the exit code file exists. Checks
     * periodically, whether the subshell of the command still exists.
     *
     * @return <code>true</code>, if the command has ended without exit code,
     *         e.g. because it has been killed
     * @throws InterruptedException if the job has been canceled
     */
    private boolean waitForEnd(CommandCall aCommandCall, IFSFile aStartFile, IFSFile anExitCodeFile, IFSFile aGoneFile, IProgressMonitor aMonitor)
        throws Exception {

        aMonitor.subTask(Messages.Task_Waiting_for_command_to_end);

        Integer processId = null;
        long checkTime = System.currentTimeMillis();

        while (!anExitCodeFile.exists()) {

            checkCanceled(aMonitor);

            if (processId == null) {
                // The file may be empty, until the shell has written the
                // process ID.
                processId = TerminalBuildCommands.parseProcessId(RemoteJobs.readText(aStartFile));
            }
            if (processId != null && System.currentTimeMillis() - checkTime >= CHECK_INTERVAL_MILLIS) {
                checkTime = System.currentTimeMillis();
                if (isGone(aCommandCall, processId.intValue(), anExitCodeFile, aGoneFile)) {
                    return true;
                }
            }

            Thread.sleep(POLL_INTERVAL_MILLIS);
        }

        return false;
    }

    private void checkCanceled(IProgressMonitor aMonitor) throws InterruptedException {
        if (aMonitor.isCanceled()) {
            throw new InterruptedException();
        }
    }

    /**
     * Returns <code>true</code>, if the subshell of the command has ended
     * without writing the exit code file.
     */
    private boolean isGone(CommandCall aCommandCall, int aProcessId, IFSFile anExitCodeFile, IFSFile aGoneFile) throws Exception {

        String script = TerminalBuildCommands.getCheckScript(aProcessId, aGoneFile.getAbsolutePath());
        aCommandCall.run(PaseCommands.getCallCommand(PaseCommands.SH, script));

        // The subshell writes the exit code file, before it ends.
        return !anExitCodeFile.exists() && aGoneFile.exists();
    }
}
