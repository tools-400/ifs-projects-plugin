/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.handlers;

import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import com.ibm.as400.access.AS400;

import de.tools400.ifsprojects.export.wizards.ExportToLibraryWizard;
import de.tools400.ifsprojects.local.LocalProject;
import de.tools400.ifsprojects.local.LocalProjectExportSource;
import de.tools400.ifsprojects.preferences.Preferences;
import de.tools400.ifsprojects.project.EnvironmentVariables;
import de.tools400.ifsprojects.project.IProjConfig;

/**
 * Exports the source files of the selected local IFS project into members of a
 * library on the connection of the project, see {@link ExportToLibraryWizard}.
 * The files are read from the workspace; unsaved editors are saved before, if
 * the user agrees.
 */
public class ExportLocalProjectHandler extends AbstractHandler {

    public Object execute(ExecutionEvent anEvent) throws ExecutionException {

        Shell shell = HandlerUtil.getActiveShell(anEvent);

        IProject project = LocalProjectCommands.getSelectedProject(anEvent);
        LocalProject localProject = LocalProjectCommands.getLocalProject(shell, project);
        if (localProject == null) {
            return null;
        }

        IHost host = LocalProjectCommands.getHost(shell, localProject);
        if (host == null || !LocalProjectCommands.saveEditors(project)) {
            return null;
        }

        AS400 system = RseBuildLauncher.getConnectedSystem(shell, host);
        if (system == null) {
            return null;
        }

        IProjConfig config = LocalProjectCommands.readConfig(shell, localProject);
        if (config == null) {
            return null;
        }

        Map<String, String> variables = EnvironmentVariables.merge(Preferences.getInstance().getDefaultEnvironmentVariables(),
            localProject.getEnvironmentVariables());

        new WizardDialog(shell, new ExportToLibraryWizard(new LocalProjectExportSource(localProject, host, config, variables), system)).open();

        return null;
    }
}
