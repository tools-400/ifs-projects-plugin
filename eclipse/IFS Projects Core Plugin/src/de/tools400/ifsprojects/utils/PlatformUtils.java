/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.utils;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * Returns the parent shells of dialogs and shows the dialogs of jobs.
 */
public final class PlatformUtils {

    private PlatformUtils() {
        // Utility class
    }

    /**
     * Returns the shell of the active workbench window, e.g. the parent of the
     * dialogs of jobs and LPEX actions, which have no shell of their own. Must
     * be called on the UI thread.
     *
     * @return shell or <code>null</code>, if no workbench window is active
     */
    public static Shell getWorkbenchShell() {

        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window == null) {
            return null;
        }

        return window.getShell();
    }

    /**
     * Shows an error message in a dialog of the active workbench window. May
     * be called from any thread, e.g. by a job: the dialog is opened
     * asynchronously on the UI thread.
     *
     * @param aTitle - title of the dialog
     * @param aMessage - message
     */
    public static void openErrorAsync(final String aTitle, final String aMessage) {

        Display.getDefault().asyncExec(new Runnable() {
            public void run() {
                MessageDialog.openError(getWorkbenchShell(), aTitle, aMessage);
            }
        });
    }
}
