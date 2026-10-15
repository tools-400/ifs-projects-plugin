/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.build;

import org.eclipse.rse.core.model.IHost;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.ibm.etools.iseries.rse.ui.view.IBMiCommandsLogViewPart;
import com.ibm.etools.iseries.subsystems.qsys.api.IBMiConnection;
import com.ibm.etools.iseries.subsystems.qsys.commands.QSYSCommandSubSystem;

import de.tools400.ifsprojects.IfsProjectsPlugin;

/**
 * Writes the output of the builds to the 'Commands Log' view of RDi. Each line
 * is logged as explanation (<code>/* text *&#47;</code>) of the IBM i
 * connection.
 * <p>
 * The methods may be called on any thread. The lines are logged on the UI
 * thread in the order of the calls.
 */
public final class CommandsLog {

    private CommandsLog() {
        // Utility class
    }

    /**
     * Shows the 'Commands Log' view with the log of a connection, without
     * activating it.
     *
     * @param aHost - RSE connection
     */
    public static void show(final IHost aHost) {

        Display.getDefault().asyncExec(new Runnable() {
            public void run() {
                try {
                    IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                    IWorkbenchPage page = window == null ? null : window.getActivePage();
                    if (page == null) {
                        return;
                    }
                    IViewPart view = page.showView(IBMiCommandsLogViewPart.ID, null, IWorkbenchPage.VIEW_VISIBLE);
                    QSYSCommandSubSystem commandSubSystem = getCommandSubSystem(aHost);
                    if (view instanceof IBMiCommandsLogViewPart && commandSubSystem != null) {
                        ((IBMiCommandsLogViewPart)view).showSubSystem(commandSubSystem);
                    }
                } catch (Exception e) {
                    IfsProjectsPlugin.logError("Could not show the Commands Log view.", e); //$NON-NLS-1$
                }
            }
        });
    }

    /**
     * Logs a line.
     *
     * @param aHost - RSE connection
     * @param aLine - line
     */
    public static void log(final IHost aHost, final String aLine) {

        Display.getDefault().asyncExec(new Runnable() {
            public void run() {
                QSYSCommandSubSystem commandSubSystem = getCommandSubSystem(aHost);
                if (commandSubSystem != null) {
                    commandSubSystem.logExplanation(aLine);
                }
            }
        });
    }

    private static QSYSCommandSubSystem getCommandSubSystem(IHost aHost) {

        IBMiConnection connection = IBMiConnection.getConnection(aHost);
        if (connection == null) {
            return null;
        }

        return connection.getCommandSubSystem();
    }
}
