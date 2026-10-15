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
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Builds the selected local IFS project: uploads its new and changed files to
 * the deploy directory and runs the build command of the
 * <code>iproj.json</code> file or the default build command of the preferences
 * there, like {@link BuildIfsProjectHandler}. The errors are shown for the
 * local files. See {@link LocalProjectCommands#build}.
 */
public class BuildLocalProjectHandler extends AbstractHandler {

    public Object execute(ExecutionEvent anEvent) throws ExecutionException {

        LocalProjectCommands.build(HandlerUtil.getActiveShell(anEvent), LocalProjectCommands.getSelectedProject(anEvent), null);

        return null;
    }
}
