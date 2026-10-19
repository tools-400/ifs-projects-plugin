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

import de.tools400.ifsprojects.rse.RseSelections;
import de.tools400.ifsprojects.rse.RseTerminalRequests;
import de.tools400.ifsprojects.terminal.TerminalRequest;

/**
 * Opens an SSH terminal in the directory of the selected IFS object:
 * <ul>
 * <li>directory: the directory itself</li>
 * <li>file: the parent directory</li>
 * <li>IFS filter or IFS project: the directory of the filter</li>
 * </ul>
 */
public class OpenTerminalHereHandler extends AbstractTerminalHandler {

    public Object execute(ExecutionEvent anEvent) throws ExecutionException {

        Shell shell = HandlerUtil.getActiveShell(anEvent);

        Object element = getSelectedElement(anEvent);
        IHost host = RseSelections.getHost(element);
        String directory = RseSelections.getDirectory(element);
        if (host == null || directory == null) {
            return null;
        }

        TerminalRequest.Builder builder = RseTerminalRequests.createBuilder(shell, host);
        if (builder == null) {
            return null;
        }

        builder.directory(directory);
        builder.forceNew(isForceNew());

        launch(shell, builder.build());

        return null;
    }
}
