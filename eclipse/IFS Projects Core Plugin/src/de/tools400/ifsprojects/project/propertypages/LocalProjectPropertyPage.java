/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.project.propertypages;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import de.tools400.ifsprojects.IfsProjectsPlugin;
import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.local.LocalProject;
import de.tools400.ifsprojects.project.IProjFileContent;
import de.tools400.ifsprojects.project.IProjFileException;
import de.tools400.ifsprojects.utils.ExceptionUtils;

/**
 * Property page of the projects of the workspace. Configures a project as local
 * IFS project, see {@link LocalProjectSettingsGroup}. These settings are stored
 * in the workspace, not in the files of the project. Like the property page of
 * IFS projects, it edits the <code>iproj.json</code> file of the project, see
 * {@link AbstractIProjPropertyPage}.
 */
public class LocalProjectPropertyPage extends AbstractIProjPropertyPage {

    public static final String ID = "de.tools400.ifsprojects.propertypages.LocalProjectPropertyPage"; //$NON-NLS-1$

    private IProject project;
    private LocalProjectSettingsGroup settingsGroup;

    @Override
    protected boolean initialize() {

        IAdaptable element = getElement();
        if (element instanceof IProject) {
            project = (IProject)element;
        } else if (element != null) {
            Object resource = element.getAdapter(IResource.class);
            if (resource instanceof IProject) {
                project = (IProject)resource;
            }
        }

        return project != null;
    }

    @Override
    protected void createHeader(Composite aParent) {

        Label description = new Label(aParent, SWT.WRAP);
        description.setText(Messages.Description_Local_project_page);
        description.setLayoutData(createWrapGridData());

        createInfo(aParent, Messages.Label_Project_name_colon, project.getName());

        settingsGroup = new LocalProjectSettingsGroup(aParent, project, new LocalProjectSettingsGroup.IChangeListener() {
            public void settingsChanged() {
                validate();
            }
        });

        String missingConnectionName = settingsGroup.getMissingConnectionName();
        if (missingConnectionName != null) {
            setMessage(Messages.bind(Messages.Error_Connection_A_of_B_not_found, missingConnectionName, project.getName()), IMessageProvider.WARNING);
        }

        validate();
    }

    @Override
    protected IProjFileContent readFile() throws IProjFileException {
        return LocalProject.readConfigFile(project);
    }

    @Override
    protected void writeFile(String aContent, IProjFileContent aPrevious) throws IProjFileException {
        LocalProject.writeConfigFile(project, aContent, aPrevious);
    }

    @Override
    protected boolean performOkHeader() {

        validate();
        if (!isValid()) {
            return false;
        }

        String connectionName = settingsGroup.getConnectionName();
        String directory = connectionName == null ? null : settingsGroup.getDeployDirectory();

        try {
            LocalProject.configure(project, connectionName, directory);
        } catch (CoreException e) {
            IfsProjectsPlugin.logError("Could not save the settings of project: " + project.getName(), e); //$NON-NLS-1$
            MessageDialog.openError(getShell(), Messages.Title_IFS_Projects,
                Messages.bind(Messages.Error_Could_not_save_settings_A, ExceptionUtils.getLocalizedMessage(e)));
            return false;
        }

        return true;
    }

    /**
     * Validates the settings. A message, why <code>iproj.json</code> cannot be
     * edited, is shown, as long as the settings are valid.
     */
    private void validate() {

        String errorMessage = settingsGroup.validate();

        setErrorMessage(errorMessage == null ? getReadOnlyMessage() : errorMessage);
        setValid(errorMessage == null);
    }
}
