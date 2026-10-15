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
import de.tools400.ifsprojects.remote.RemoteJobs;

/**
 * Executes the commands of a syntax check with IBM Toolbox for Java.
 * <p>
 * The commands run in a remote command server job of their own, because they
 * change the current directory and the library list and use
 * <code>QTEMP</code>. Therefore a copy of the system object of RSE is used,
 * which is disconnected afterwards. The events file is copied to a temporary
 * stream file in the same job and read with the file server.
 * <p>
 * The methods of this class access the remote system and must not be called
 * on the UI thread.
 */
public final class SyntaxCheckRunner {

    /** Library already exists in library list. */
    private static final String CPF2103 = "CPF2103"; //$NON-NLS-1$

    private static final String TEMPORARY_FILE_SUFFIX = ".evfevent"; //$NON-NLS-1$

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
            throw new SyntaxCheckException(Messages.bind(Messages.Error_Could_not_read_A_B, aPath, e.getLocalizedMessage()), e);
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
     * @param aTemporaryDirectory - directory of temporary files, see
     *        {@link RemoteJobs#getTemporaryDirectory}
     * @param aMonitor - progress monitor
     * @return result of the syntax check
     * @throws SyntaxCheckException if the syntax check could not be performed
     * @throws InterruptedException if the user canceled the syntax check
     */
    public static SyntaxCheckResult run(AS400 aRseSystem, List<String> aSetupCommands, String anObjectName, String aCompileCommand,
        String aTemporaryDirectory, IProgressMonitor aMonitor) throws SyntaxCheckException, InterruptedException {

        AS400 system = RemoteJobs.copySystem(aRseSystem);
        try {

            CommandCall commandCall = RemoteJobs.createCommandCall(system);
            RemoteJobs.deleteObsoleteTemporaryFiles(system, aTemporaryDirectory);

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
            String streamFile = RemoteJobs.getTemporaryFile(aTemporaryDirectory, TEMPORARY_FILE_SUFFIX);
            if (!run(commandCall, SyntaxCheckCommands.getCopyEventsMemberCommand(anObjectName, streamFile))) {
                // The compiler did not create an events file, e.g. because the
                // command is invalid.
                return new SyntaxCheckResult(null, RemoteJobs.CCSID_NOT_SET, compileMessages);
            }

            IFSFile file = new IFSFile(system, streamFile);
            String events;
            try {
                events = RemoteJobs.readText(file, RemoteJobs.CCSID_UTF8);
            } catch (Exception e) {
                throw new SyntaxCheckException(Messages.bind(Messages.Error_Could_not_read_A_B, streamFile, e.getLocalizedMessage()), e);
            } finally {
                RemoteJobs.delete(file);
            }

            return new SyntaxCheckResult(events, RemoteJobs.getJobCcsid(commandCall, system), compileMessages);

        } finally {
            system.disconnectAllServices();
        }
    }

    private static boolean run(CommandCall aCommandCall, String aCommand) throws SyntaxCheckException {

        try {
            return aCommandCall.run(aCommand);
        } catch (Exception e) {
            throw new SyntaxCheckException(Messages.bind(Messages.Error_Command_A_failed_B, aCommand, e.getLocalizedMessage()), e);
        }
    }

    private static void checkCanceled(IProgressMonitor aMonitor) throws InterruptedException {
        if (aMonitor != null && aMonitor.isCanceled()) {
            throw new InterruptedException();
        }
    }
}
