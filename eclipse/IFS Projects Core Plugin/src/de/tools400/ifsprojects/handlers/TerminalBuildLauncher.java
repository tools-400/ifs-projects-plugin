/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.handlers;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.ibm.as400.access.AS400;

import de.tools400.ifsprojects.IfsProjectsPlugin;
import de.tools400.ifsprojects.build.RseBuildCommands;
import de.tools400.ifsprojects.build.TerminalBuildJob;
import de.tools400.ifsprojects.local.SourceMapping;
import de.tools400.ifsprojects.project.IProjConfig;
import de.tools400.ifsprojects.project.IProjFileException;
import de.tools400.ifsprojects.project.IProjFileStore;
import de.tools400.ifsprojects.terminal.ITerminalCommandLine;
import de.tools400.ifsprojects.terminal.ShellCommands;
import de.tools400.ifsprojects.terminal.TerminalRequest;

/**
 * Starts a build or compile command of an IFS project in the SSH terminal of
 * the project. The command is wrapped, so that it writes marker files, and a
 * {@link TerminalBuildJob} shows the errors in the 'Error List' view, when
 * the command has ended.
 * <p>
 * If the build cannot be prepared or the command cannot be wrapped, the
 * command runs unchanged and the errors are not shown.
 */
final class TerminalBuildLauncher {

    private TerminalBuildLauncher() {
        // Utility class
    }

    /**
     * Starts the command. Must be called on the UI thread.
     *
     * @param aShell - parent shell
     * @param aJobName - name of the job
     * @param aHost - RSE connection
     * @param aDirectory - project directory
     * @param aConfig - content of <code>iproj.json</code>
     * @param aVariables - environment variables of the project
     * @param aCommand - command with replaced placeholders
     * @param aBuilder - terminal request with directory and reuse key, but
     *        without command
     * @param aSourceMapping - mapping of the project directory to a local
     *        project or <code>null</code>
     */
    static void launch(Shell aShell, String aJobName, IHost aHost, String aDirectory, IProjConfig aConfig, Map<String, String> aVariables,
        String aCommand, TerminalRequest.Builder aBuilder, SourceMapping aSourceMapping) {

        TerminalBuildJob job = prepare(aJobName, aHost, aDirectory, RseBuildCommands.getObjectLibrary(aConfig, aVariables), aSourceMapping);

        String command = null;
        if (job != null) {
            command = job.wrap(aCommand);
            if (command == null) {
                IfsProjectsPlugin.logWarning("The errors of the command are not shown in the Error List, because it cannot be wrapped: " + aCommand); //$NON-NLS-1$
                job = null;
            }
        }

        if (command == null) {
            command = aCommand;
        }

        ITerminalCommandLine commandLine = AbstractTerminalHandler.launch(aShell,
            aBuilder.command(ShellCommands.addExports(aVariables, command)).build());

        if (job != null && commandLine != null) {
            job.start(commandLine);
        }
    }

    /**
     * Prepares the job, that shows the errors. Errors are logged.
     *
     * @return job or <code>null</code>, if the build could not be prepared
     */
    private static TerminalBuildJob prepare(final String aJobName, final IHost aHost, final String aDirectory, final String anObjectLibrary,
        final SourceMapping aSourceMapping) {

        final AS400 system;
        try {
            system = IProjFileStore.getConnectedSystem(aHost);
        } catch (IProjFileException e) {
            IfsProjectsPlugin.logError("Could not prepare the Error List of the build in the terminal: " + aDirectory, e); //$NON-NLS-1$
            return null;
        }

        final TerminalBuildJob[] job = new TerminalBuildJob[1];
        try {
            PlatformUI.getWorkbench().getProgressService().busyCursorWhile(new IRunnableWithProgress() {
                public void run(IProgressMonitor aMonitor) throws InvocationTargetException {
                    try {
                        job[0] = TerminalBuildJob.prepare(aJobName, aHost, system, aDirectory, anObjectLibrary, aSourceMapping);
                    } catch (Exception e) {
                        throw new InvocationTargetException(e);
                    }
                }
            });
        } catch (InvocationTargetException e) {
            IfsProjectsPlugin.logError("Could not prepare the Error List of the build in the terminal: " + aDirectory, e.getCause()); //$NON-NLS-1$
            return null;
        } catch (InterruptedException e) {
            return null;
        }

        return job[0];
    }
}
