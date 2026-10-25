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
 * Compiles the selected file of a local IFS project: uploads the new and
 * changed files of the project to the deploy directory and runs the compile
 * command of the <code>iproj.json</code> file or the default compile command of
 * the preferences there, like {@link CompileIfsFileHandler}. All changes are
 * uploaded, so that changed copy books are compiled too. See
 * {@link LocalProjectCommands#build}.
 */
public class CompileLocalFileHandler extends AbstractHandler {

    public Object execute(ExecutionEvent anEvent) throws ExecutionException {

        IResource resource = LocalProjectCommands.getSelectedResource(anEvent);
        if (!(resource instanceof IFile)) {
            return null;
        }

        LocalProjectCommands.build(HandlerUtil.getActiveShell(anEvent), resource.getProject(), (IFile)resource);

        return null;
    }
}
