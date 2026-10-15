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
import org.eclipse.rse.subsystems.files.core.subsystems.IRemoteFile;
import org.eclipse.ui.handlers.HandlerUtil;

import de.tools400.ifsprojects.rse.RseSelections;

/**
 * Checks the syntax of the selected RPGLE or SQLRPGLE file of an IFS project
 * and shows the errors in the 'Error List' view of RDi, see
 * {@link SyntaxCheckLauncher}.
 */
public class CheckSyntaxHandler extends AbstractHandler {

    public Object execute(ExecutionEvent anEvent) throws ExecutionException {

        Object element = RseSelections.getSingleElement(HandlerUtil.getCurrentSelection(anEvent));
        if (!(element instanceof IRemoteFile) || ((IRemoteFile)element).isDirectory()) {
            return null;
        }

        IRemoteFile file = (IRemoteFile)element;
        SyntaxCheckLauncher.launch(HandlerUtil.getActiveShell(anEvent), file.getHost(), file.getAbsolutePath());

        return null;
    }
}
