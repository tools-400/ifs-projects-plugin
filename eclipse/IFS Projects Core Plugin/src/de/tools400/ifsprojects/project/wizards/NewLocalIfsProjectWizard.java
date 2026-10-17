/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.project.wizards;

import java.io.File;

import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;

import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.local.LocalProject;

/**
 * Wizard 'New &gt; Project... &gt; IFS Projects &gt; Local IFS Project', that
 * creates a project in the workspace, which is built on an IBM i, see
 * {@link LocalProject}.
 * <ol>
 * <li>Name, location and working sets: standard page of Eclipse.</li>
 * <li>Connection, deploy directory, encoding and <code>iproj.json</code>, see
 * {@link NewLocalIfsProjectWizardPage}.</li>
 * </ol>
 */
public class NewLocalIfsProjectWizard extends AbstractLocalIfsProjectWizard implements INewWizard {

    private static final String PAGE_NAME_PROJECT = "NewLocalIfsProjectPage"; //$NON-NLS-1$
    private static final String RESOURCE_WORKING_SET_PAGE = "org.eclipse.ui.resourceWorkingSetPage"; //$NON-NLS-1$

    private WizardNewProjectCreationPage projectPage;

    public NewLocalIfsProjectWizard() {
        super(Messages.Title_New_local_IFS_project);
    }

    @Override
    public void addPages() {

        projectPage = new WizardNewProjectCreationPage(PAGE_NAME_PROJECT) {
            @Override
            public void createControl(Composite aParent) {
                super.createControl(aParent);
                createWorkingSetGroup((Composite)getControl(), getSelection(), new String[] { RESOURCE_WORKING_SET_PAGE });
                Dialog.applyDialogFont(getControl());
            }
        };
        projectPage.setTitle(Messages.Title_New_local_IFS_project);
        projectPage.setDescription(Messages.Description_New_local_IFS_project_page);
        addPage(projectPage);

        addSettingsPage(new NewLocalIfsProjectWizardPage.IProjectLocation() {
            public String getProjectName() {
                return projectPage.getProjectName();
            }

            /*
             * The default location is the directory of the project in the
             * workspace.
             */
            public File getProjectDirectory() {

                IPath location = projectPage.getLocationPath();
                if (location == null || projectPage.getProjectName().length() == 0) {
                    return null;
                }

                if (projectPage.useDefaults()) {
                    location = location.append(projectPage.getProjectName());
                }

                return location.toFile();
            }
        });
    }

    @Override
    public boolean performFinish() {

        IProjectDescription description = ResourcesPlugin.getWorkspace().newProjectDescription(projectPage.getProjectHandle().getName());
        description.setLocationURI(projectPage.useDefaults() ? null : projectPage.getLocationURI());

        return createProject(description, projectPage.getSelectedWorkingSets());
    }
}
