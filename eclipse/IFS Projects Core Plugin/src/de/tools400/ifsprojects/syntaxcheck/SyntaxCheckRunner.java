/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.syntaxcheck;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.CommandCall;
import com.ibm.as400.access.IFSFile;

import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.errorlist.EventsFiles;
import de.tools400.ifsprojects.ibmi.ClCommands;
import de.tools400.ifsprojects.remote.RemoteJobs;
import de.tools400.ifsprojects.remote.RemoteSession;
import de.tools400.ifsprojects.utils.ExceptionUtils;

/**
 * Executes the commands of a syntax check with IBM Toolbox for Java.
 * <p>
 * The commands run in a remote command server job of their own, because they
 * change the current directory and the library list and use
 * <code>QTEMP</code>, see {@link RemoteSession}. The events file is copied to
 * a temporary stream file in the same job and read with the file server.
 * <p>
 * The methods of this class access the remote system and must not be called
 * on the UI thread.
 */
public final class SyntaxCheckRunner {

    /** Library already exists in library list. */
    private static final String CPF2103 = "CPF2103"; //$NON-NLS-1$

    private SyntaxCheckRunner() {
        // Utility class
    }

    /**
     * Reads the content of a stream file, e.g. the source that is checked.
     *
     * @param aSystem - system object of RSE
     * @param aPath - absolute path of the stream file
     * @return content
     * @throws SyntaxCheckException if the file could not be read
     */
    public static String readStreamFile(AS400 aSystem, String aPath) throws SyntaxCheckException {

        try {
            return RemoteJobs.readText(new IFSFile(aSystem, aPath));
        } catch (Exception e) {
            throw new SyntaxCheckException(Messages.bind(Messages.Error_Could_not_read_A_B, aPath, ExceptionUtils.getLocalizedMessage(e)), e);
        }
    }

    /**
     * Executes the commands of a syntax check.
     *
     * @param aRseSystem - system object of RSE, which is copied
     * @param aSetupCommands - commands that set up the job, see
     *        {@link SyntaxCheckCommands#getSetupCommands}
     * @param anObjectName - name of the object and of the member of the events
     *        file
     * @param aCompileCommand - compile command, see
     *        {@link SyntaxCheckCommands#getCompileCommand}
     * @param aMonitor - progress monitor
     * @return result of the syntax check
     * @throws SyntaxCheckException if the syntax check could not be performed
     * @throws InterruptedException if the user canceled the syntax check
     */
    public static SyntaxCheckResult run(AS400 aRseSystem, List<String> aSetupCommands, String anObjectName, String aCompileCommand,
        IProgressMonitor aMonitor) throws SyntaxCheckException, InterruptedException {

        try (RemoteSession session = new RemoteSession(aRseSystem)) {

            CommandCall commandCall = session.getCommandCall();

            for (String command : aSetupCommands) {
                checkCanceled(aMonitor);
                if (!run(commandCall, command) && !RemoteJobs.hasMessage(commandCall, CPF2103)) {
                    throw new SyntaxCheckException(Messages.bind(Messages.Error_Command_A_failed_B, command, RemoteJobs.getMessages(commandCall)),
                        null);
                }
            }

            checkCanceled(aMonitor);
            run(commandCall, SyntaxCheckCommands.getRemoveEventsMemberCommand(anObjectName));

            checkCanceled(aMonitor);
            run(commandCall, aCompileCommand);
            String compileMessages = RemoteJobs.getMessages(commandCall);

            checkCanceled(aMonitor);
            String events = readEventsMember(session, anObjectName);
            if (events == null) {
                // The compiler did not create an events file, e.g. because the
                // command is invalid.
                return new SyntaxCheckResult(null, RemoteJobs.CCSID_NOT_SET, compileMessages);
            }

            return new SyntaxCheckResult(events, session.getJobCcsid(), compileMessages);
        }
    }

    /**
     * Reads the member of the events file of the syntax check.
     *
     * @return events or <code>null</code>, if the member does not exist
     */
    private static String readEventsMember(RemoteSession aSession, String anObjectName) throws SyntaxCheckException {

        try {
            return aSession.readMember(SyntaxCheckCommands.LIBRARY, EventsFiles.EVENTS_FILE, anObjectName);
        } catch (Exception e) {
            String member = ClCommands.getMemberPath(SyntaxCheckCommands.LIBRARY, EventsFiles.EVENTS_FILE, anObjectName);
            throw new SyntaxCheckException(Messages.bind(Messages.Error_Could_not_read_A_B, member, ExceptionUtils.getLocalizedMessage(e)), e);
        }
    }

    private static boolean run(CommandCall aCommandCall, String aCommand) throws SyntaxCheckException {

        try {
            return aCommandCall.run(aCommand);
        } catch (Exception e) {
            throw new SyntaxCheckException(Messages.bind(Messages.Error_Command_A_failed_B, aCommand, ExceptionUtils.getLocalizedMessage(e)), e);
        }
    }

    private static void checkCanceled(IProgressMonitor aMonitor) throws InterruptedException {
        if (aMonitor != null && aMonitor.isCanceled()) {
            throw new InterruptedException();
        }
    }
}
