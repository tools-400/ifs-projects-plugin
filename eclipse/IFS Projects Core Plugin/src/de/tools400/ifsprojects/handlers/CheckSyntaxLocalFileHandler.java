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
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Checks the syntax of the selected RPGLE or SQLRPGLE file of a local IFS
 * project, see {@link SyntaxCheckLauncher#launch(org.eclipse.swt.widgets.Shell, IFile)}.
 */
public class CheckSyntaxLocalFileHandler extends AbstractHandler {

    public Object execute(ExecutionEvent anEvent) throws ExecutionException {

        IResource resource = LocalProjectCommands.getSelectedResource(anEvent);
        if (!(resource instanceof IFile)) {
            return null;
        }

        SyntaxCheckLauncher.launch(HandlerUtil.getActiveShell(anEvent), (IFile)resource);

        return null;
    }
}
