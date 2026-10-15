/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.handlers;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.swt.widgets.Shell;

import de.tools400.ifsprojects.IfsProjectsPlugin;
import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.preferences.Preferences;
import de.tools400.ifsprojects.project.IProjConfig;
import de.tools400.ifsprojects.project.IProjFileContent;
import de.tools400.ifsprojects.project.IProjFileException;
import de.tools400.ifsprojects.project.IProjFileStore;
import de.tools400.ifsprojects.project.IProjParseException;

/**
 * Common code of the handlers that execute project commands (build, compile).
 */
final class ProjectCommands {

    private static final String REUSE_KEY_PREFIX = "build:"; //$NON-NLS-1$
    private static final String REUSE_KEY_SEPARATOR = ":"; //$NON-NLS-1$

    private ProjectCommands() {
        // Utility class
    }

    /**
     * Reads the <code>iproj.json</code> file of a project. Errors are reported
     * to the user.
     *
     * @param aShell - parent shell
     * @param aHost - RSE connection
     * @param aDirectory - project directory
     * @return configuration (empty, if the file does not exist) or
     *         <code>null</code>, if the file could not be read
     */
    static IProjConfig readConfig(Shell aShell, final IHost aHost, final String aDirectory) {

        final IProjFileContent[] fileContent = new IProjFileContent[1];
        try {

            IProjFileStore.connect(aHost);

            new ProgressMonitorDialog(aShell).run(true, false, new IRunnableWithProgress() {
                public void run(IProgressMonitor aMonitor) throws InvocationTargetException, InterruptedException {
                    aMonitor.beginTask(Messages.bind(Messages.Task_Reading_A, aDirectory), IProgressMonitor.UNKNOWN);
                    try {
                        fileContent[0] = IProjFileStore.read(aHost, aDirectory);
                    } catch (IProjFileException e) {
                        throw new InvocationTargetException(e);
                    } finally {
                        aMonitor.done();
                    }
                }
            });

        } catch (IProjFileException e) {
            MessageDialog.openError(aShell, Messages.Title_IFS_Projects, e.getLocalizedMessage());
            return null;
        } catch (InvocationTargetException e) {
            IfsProjectsPlugin.logError("Could not read iproj.json: " + aDirectory, e.getCause()); //$NON-NLS-1$
            MessageDialog.openError(aShell, Messages.Title_IFS_Projects, e.getCause().getLocalizedMessage());
            return null;
        } catch (InterruptedException e) {
            return null;
        }

        try {
            return IProjConfig.parse(fileContent[0].getContent());
        } catch (IProjParseException e) {
            MessageDialog.openError(aShell, Messages.Title_IFS_Projects, Messages.bind(Messages.Error_Invalid_iproj_json_A, e.getLocalizedMessage()));
            return null;
        }
    }

    /**
     * Returns the key of the terminal that is reused for building and
     * compiling a project or <code>null</code>, if terminals are not reused.
     *
     * @param aHost - RSE connection
     * @param aDirectory - project directory
     * @return reuse key or <code>null</code>
     */
    static String getReuseKey(IHost aHost, String aDirectory) {

        if (!Preferences.getInstance().isReuseBuildTerminal()) {
            return null;
        }

        return REUSE_KEY_PREFIX + aHost.getAliasName() + REUSE_KEY_SEPARATOR + aDirectory;
    }
}
