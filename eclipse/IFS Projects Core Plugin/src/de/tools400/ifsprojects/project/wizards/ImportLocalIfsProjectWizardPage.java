/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.project.wizards;

import java.io.File;
import java.net.URI;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import de.tools400.ifsprojects.IfsProjectsPlugin;
import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.utils.ExceptionUtils;
import de.tools400.ifsprojects.utils.StringUtils;

/**
 * First page of the 'Import Local IFS Project' wizard: the directory of the
 * project and its name. The name is taken from the directory or, if the
 * directory contains a <code>.project</code> file, from that file. Like
 * 'Existing Projects into Workspace', the description of an existing
 * <code>.project</code> file is used.
 */
public class ImportLocalIfsProjectWizardPage extends WizardPage implements NewLocalIfsProjectWizardPage.IProjectLocation {

    private static final String PAGE_NAME = "ImportLocalIfsProjectWizardPage"; //$NON-NLS-1$

    private Text directoryText;
    private Text nameText;

    /* The name has been entered by the user and follows the directory no more. */
    private boolean isNameEntered;
    private boolean isUpdatingName;

    public ImportLocalIfsProjectWizardPage() {
        super(PAGE_NAME);
        setTitle(Messages.Title_Import_local_IFS_project);
        setDescription(Messages.Description_Import_local_IFS_project_page);
    }

    public void createControl(Composite aParent) {

        Composite main = new Composite(aParent, SWT.NONE);
        main.setLayout(new GridLayout(3, false));

        Label directoryLabel = new Label(main, SWT.NONE);
        directoryLabel.setText(Messages.Label_Directory_colon);

        directoryText = new Text(main, SWT.BORDER);
        directoryText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        Button browseButton = new Button(main, SWT.PUSH);
        browseButton.setText(Messages.Label_Browse);

        Label nameLabel = new Label(main, SWT.NONE);
        nameLabel.setText(Messages.Label_Project_name_colon);

        nameText = new Text(main, SWT.BORDER);
        GridData nameData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        nameData.horizontalSpan = 2;
        nameText.setLayoutData(nameData);

        directoryText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent anEvent) {
                directoryChanged();
            }
        });

        nameText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent anEvent) {
                if (!isUpdatingName) {
                    isNameEntered = nameText.getText().length() > 0;
                }
                validate();
            }
        });

        browseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent anEvent) {
                browse();
            }
        });

        setControl(main);
        Dialog.applyDialogFont(main);

        validate();
    }

    public String getProjectName() {
        return nameText.getText().trim();
    }

    public File getProjectDirectory() {

        String directory = directoryText.getText().trim();
        if (directory.length() == 0) {
            return null;
        }

        File file = new File(directory).getAbsoluteFile();
        if (!file.isDirectory()) {
            return null;
        }

        return file;
    }

    /**
     * Returns the description of the new project: the description of an
     * existing <code>.project</code> file or a new one, with the entered name
     * and the directory as location.
     */
    public IProjectDescription createProjectDescription() {

        File directory = getProjectDirectory();

        IProjectDescription description = loadDescription(directory);
        if (description == null) {
            description = ResourcesPlugin.getWorkspace().newProjectDescription(getProjectName());
        } else {
            description.setName(getProjectName());
        }
        description.setLocationURI(getLocationURI(directory));

        return description;
    }

    private void browse() {

        DirectoryDialog dialog = new DirectoryDialog(getShell());
        dialog.setText(Messages.Title_Select_project_directory);
        dialog.setMessage(Messages.Label_Select_a_directory_colon);
        dialog.setFilterPath(directoryText.getText().trim());

        String directory = dialog.open();
        if (directory != null) {
            directoryText.setText(directory);
        }
    }

    private void directoryChanged() {

        if (!isNameEntered) {
            isUpdatingName = true;
            try {
                nameText.setText(getDefaultName());
            } finally {
                isUpdatingName = false;
            }
        }

        validate();
    }

    /**
     * Returns the name of the project in the <code>.project</code> file of
     * the directory or else the name of the directory.
     */
    private String getDefaultName() {

        File directory = getProjectDirectory();
        if (directory == null) {
            return ""; //$NON-NLS-1$
        }

        IProjectDescription description = loadDescription(directory);
        if (description != null) {
            return description.getName();
        }

        return directory.getName();
    }

    /**
     * Returns the description of the <code>.project</code> file of the
     * directory or <code>null</code>, if there is no valid file.
     */
    private IProjectDescription loadDescription(File aDirectory) {

        if (aDirectory == null) {
            return null;
        }

        File file = new File(aDirectory, IProjectDescription.DESCRIPTION_FILE_NAME);
        if (!file.isFile()) {
            return null;
        }

        try {
            return ResourcesPlugin.getWorkspace().loadProjectDescription(Path.fromOSString(file.getPath()));
        } catch (CoreException e) {
            IfsProjectsPlugin.logWarning("Could not read project description: " + file + ": " + ExceptionUtils.getLocalizedMessage(e)); //$NON-NLS-1$ //$NON-NLS-2$
            return null;
        }
    }

    /**
     * Returns the location of the project or <code>null</code>, if the
     * directory is the default location of the project in the workspace.
     */
    private URI getLocationURI(File aDirectory) {

        File defaultDirectory = ResourcesPlugin.getWorkspace().getRoot().getLocation().append(getProjectName()).toFile();
        if (defaultDirectory.equals(aDirectory)) {
            return null;
        }

        return aDirectory.toURI();
    }

    private void validate() {

        String errorMessage = null;

        if (StringUtils.isBlank(directoryText.getText())) {
            setErrorMessage(null);
            setPageComplete(false);
            return;
        }

        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        File directory = getProjectDirectory();
        String name = getProjectName();

        if (directory == null) {
            errorMessage = Messages.bind(Messages.Error_Directory_A_does_not_exist, directoryText.getText().trim());
        } else if (name.length() == 0) {
            errorMessage = Messages.Error_Project_name_must_not_be_empty;
        } else {
            IStatus status = workspace.validateName(name, IResource.PROJECT);
            IProject project = workspace.getRoot().getProject(name);
            if (!status.isOK()) {
                errorMessage = status.getMessage();
            } else if (project.exists()) {
                errorMessage = Messages.bind(Messages.Error_Workspace_project_A_already_exists, name);
            } else {
                URI location = getLocationURI(directory);
                if (location != null) {
                    status = workspace.validateProjectLocationURI(project, location);
                    if (!status.isOK()) {
                        errorMessage = status.getMessage();
                    }
                }
            }
        }

        setErrorMessage(errorMessage);
        setPageComplete(errorMessage == null);
    }
}
