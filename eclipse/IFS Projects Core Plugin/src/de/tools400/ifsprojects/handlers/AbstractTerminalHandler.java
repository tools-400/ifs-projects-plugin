/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import de.tools400.ifsprojects.IfsProjectsPlugin;
import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.rse.RseSelections;
import de.tools400.ifsprojects.terminal.ITerminalCommandLine;
import de.tools400.ifsprojects.terminal.TerminalException;
import de.tools400.ifsprojects.terminal.TerminalLaunchers;
import de.tools400.ifsprojects.terminal.TerminalRequest;
import de.tools400.ifsprojects.utils.ExceptionUtils;

/**
 * Base class of the handlers that open a terminal.
 */
public abstract class AbstractTerminalHandler extends AbstractHandler {

    /**
     * Returns the selected element, if exactly one element is selected.
     *
     * @param anEvent - execution event
     * @return selected element or <code>null</code>
     */
    protected Object getSelectedElement(ExecutionEvent anEvent) {
        return RseSelections.getSingleElement(HandlerUtil.getCurrentSelection(anEvent));
    }

    /**
     * Specifies whether a new terminal is opened, even if a terminal of the
     * same connection and directory is already open.
     *
     * @return <code>true</code> to always open a new terminal
     */
    protected boolean isForceNew() {
        return false;
    }

    /**
     * Opens a terminal. Errors are reported to the user.
     *
     * @param aShell - parent shell for error messages
     * @param aRequest - terminal request
     * @return command line of the request or <code>null</code>, if the request
     *         has no command or the terminal could not be opened
     */
    protected static ITerminalCommandLine launch(Shell aShell, TerminalRequest aRequest) {
        try {
            return TerminalLaunchers.getLauncher().launch(aRequest);
        } catch (TerminalException e) {
            IfsProjectsPlugin.logError("Could not open terminal: " + aRequest.getToolTip(), e); //$NON-NLS-1$
            MessageDialog.openError(aShell, Messages.Title_IFS_Projects, ExceptionUtils.getLocalizedMessage(e));
        } catch (RuntimeException e) {
            IfsProjectsPlugin.logError("Unexpected error opening terminal: " + aRequest.getToolTip(), e); //$NON-NLS-1$
            MessageDialog.openError(aShell, Messages.Title_IFS_Projects,
                Messages.bind(Messages.Error_Unexpected_error_A, ExceptionUtils.getLocalizedMessage(e)));
        }

        return null;
    }
}
