/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import de.tools400.ifsprojects.rse.RseConnections;
import de.tools400.ifsprojects.rse.RseSelections;
import de.tools400.ifsprojects.terminal.TerminalRequest;

/**
 * Opens an SSH terminal for the selected IBM i connection. The terminal starts
 * in the home directory of the user.
 */
public class OpenTerminalHandler extends AbstractTerminalHandler {

    public Object execute(ExecutionEvent anEvent) throws ExecutionException {

        Shell shell = HandlerUtil.getActiveShell(anEvent);

        IHost host = RseSelections.getHost(getSelectedElement(anEvent));
        if (host == null) {
            return null;
        }

        TerminalRequest.Builder builder = RseConnections.createRequestBuilder(shell, host);
        if (builder == null) {
            return null;
        }

        builder.forceNew(isForceNew());

        launch(shell, builder.build());

        return null;
    }
}
