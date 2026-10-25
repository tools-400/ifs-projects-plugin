/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.handlers;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.swt.widgets.Shell;

import com.ibm.as400.access.AS400;

import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.preferences.Preferences;
import de.tools400.ifsprojects.project.IProjConfig;
import de.tools400.ifsprojects.project.IProjFileContent;
import de.tools400.ifsprojects.project.IProjFileException;
import de.tools400.ifsprojects.project.IProjFileStore;
import de.tools400.ifsprojects.project.IProjParseException;
import de.tools400.ifsprojects.utils.ExceptionUtils;

/**
 * Common code of the handlers that execute project commands (build, compile,
 * syntax check, export). Errors are reported to the user. The methods must be
 * called on the UI thread.
 */
final class ProjectCommands {

    private static final String REUSE_KEY_PREFIX = "build:"; //$NON-NLS-1$
    private static final String REUSE_KEY_SEPARATOR = ":"; //$NON-NLS-1$

    private ProjectCommands() {
        // Utility class
    }

    /**
     * Returns the connected system object of RSE.
     *
     * @param aShell - parent shell
     * @param aHost - RSE connection
     * @return system or <code>null</code>
     */
    static AS400 getConnectedSystem(Shell aShell, IHost aHost) {

        try {
            return IProjFileStore.getConnectedSystem(aHost);
        } catch (IProjFileException e) {
            MessageDialog.openError(aShell, Messages.Title_IFS_Projects, ExceptionUtils.getLocalizedMessage(e));
            return null;
        }
    }

    /**
     * Reads the <code>iproj.json</code> file of an IFS project.
     *
     * @param aShell - parent shell
     * @param aHost - RSE connection
     * @param aDirectory - project directory
     * @return configuration (empty, if the file does not exist) or
     *         <code>null</code>, if the file could not be read
     */
    static IProjConfig readConfig(Shell aShell, IHost aHost, String aDirectory) {

        IProjFileContent fileContent;
        try {
            fileContent = IProjFileStore.read(new ProgressMonitorDialog(aShell), aHost, aDirectory);
        } catch (IProjFileException e) {
            MessageDialog.openError(aShell, Messages.Title_IFS_Projects, ExceptionUtils.getLocalizedMessage(e));
            return null;
        }

        try {
            return IProjConfig.parse(fileContent.getContent());
        } catch (IProjParseException e) {
            MessageDialog.openError(aShell, Messages.Title_IFS_Projects,
                Messages.bind(Messages.Error_Invalid_iproj_json_A, ExceptionUtils.getLocalizedMessage(e)));
            return null;
        }
    }

    /**
     * Returns the key of the terminal that is reused for building and compiling
     * a project or <code>null</code>, if terminals are not reused.
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
