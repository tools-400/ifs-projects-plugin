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
import org.eclipse.core.resources.IProject;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.handlers.HandlerUtil;

import de.tools400.ifsprojects.project.propertypages.LocalProjectPropertyPage;

/**
 * Opens the property page, that configures the selected project of the
 * workspace as local IFS project.
 */
public class ConfigureLocalProjectHandler extends AbstractHandler {

    public Object execute(ExecutionEvent anEvent) throws ExecutionException {

        IProject project = LocalProjectCommands.getSelectedProject(anEvent);
        if (project == null) {
            return null;
        }

        PreferencesUtil.createPropertyDialogOn(HandlerUtil.getActiveShell(anEvent), project, LocalProjectPropertyPage.ID,
            new String[] { LocalProjectPropertyPage.ID }, null).open();

        return null;
    }
}
