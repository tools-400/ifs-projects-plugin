/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.project.wizards;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import org.eclipse.ui.ide.undo.CreateProjectOperation;
import org.eclipse.ui.ide.undo.WorkspaceUndoUtil;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

import de.tools400.ifsprojects.IfsProjectsPlugin;
import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.local.LocalProject;
import de.tools400.ifsprojects.project.IProjConfig;

/**
 * Wizard 'New &gt; Project... &gt; IFS Projects &gt; Local IFS Project', that
 * creates a project in the workspace, which is built on an IBM i, see
 * {@link LocalProject}.
 * <ol>
 * <li>Name, location and working sets: standard page of Eclipse.</li>
 * <li>Connection, deploy directory, encoding and <code>iproj.json</code>, see
 * {@link NewLocalIfsProjectWizardPage}.</li>
 * </ol>
 * The project is created like the general project of Eclipse, so that its
 * creation can be undone. The files are uploaded with the first build.
 */
public class NewLocalIfsProjectWizard extends Wizard implements INewWizard {

    private static final String PAGE_NAME_PROJECT = "NewLocalIfsProjectPage"; //$NON-NLS-1$
    private static final String RESOURCE_WORKING_SET_PAGE = "org.eclipse.ui.resourceWorkingSetPage"; //$NON-NLS-1$
    private static final String UTF_8 = "UTF-8"; //$NON-NLS-1$

    private IWorkbench workbench;
    private IStructuredSelection selection;

    private WizardNewProjectCreationPage projectPage;
    private NewLocalIfsProjectWizardPage settingsPage;

    public NewLocalIfsProjectWizard() {
        setWindowTitle(Messages.Title_New_local_IFS_project);
        setNeedsProgressMonitor(true);
    }

    public void init(IWorkbench aWorkbench, IStructuredSelection aSelection) {
        this.workbench = aWorkbench;
        this.selection = aSelection;
    }

    @Override
    public void addPages() {

        projectPage = new WizardNewProjectCreationPage(PAGE_NAME_PROJECT) {
            @Override
            public void createControl(Composite aParent) {
                super.createControl(aParent);
                createWorkingSetGroup((Composite)getControl(), selection, new String[] { RESOURCE_WORKING_SET_PAGE });
                Dialog.applyDialogFont(getControl());
            }
        };
        projectPage.setTitle(Messages.Title_New_local_IFS_project);
        projectPage.setDescription(Messages.Description_New_local_IFS_project_page);
        addPage(projectPage);

        settingsPage = new NewLocalIfsProjectWizardPage(projectPage);
        addPage(settingsPage);
    }

    @Override
    public boolean performFinish() {

        final IProject project = projectPage.getProjectHandle();
        URI location = projectPage.useDefaults() ? null : projectPage.getLocationURI();

        final boolean isUtf8 = settingsPage.isUtf8();
        final IProjConfig config = settingsPage.isCreateIProj() ? settingsPage.createConfig() : null;
        final String connectionName = settingsPage.getConnectionName();
        final String deployDirectory = settingsPage.getDeployDirectory();

        IProjectDescription description = ResourcesPlugin.getWorkspace().newProjectDescription(project.getName());
        description.setLocationURI(location);

        final CreateProjectOperation operation = new CreateProjectOperation(description, Messages.Title_New_local_IFS_project);
        final IAdaptable uiInfo = WorkspaceUndoUtil.getUIInfoAdapter(getShell());

        try {
            getContainer().run(true, true, new IRunnableWithProgress() {
                public void run(IProgressMonitor aMonitor) throws InvocationTargetException {
                    try {
                        IStatus status = workbench.getOperationSupport().getOperationHistory().execute(operation, aMonitor, uiInfo);
                        if (!status.isOK()) {
                            throw new InvocationTargetException(status.getException(), status.getMessage());
                        }
                        configure(project, isUtf8, config, connectionName, deployDirectory, aMonitor);
                    } catch (InvocationTargetException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new InvocationTargetException(e);
                    }
                }
            });
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            String message = cause.getLocalizedMessage() != null ? cause.getLocalizedMessage() : e.getMessage();
            IfsProjectsPlugin.logError("Could not create local IFS project: " + project.getName(), cause); //$NON-NLS-1$
            MessageDialog.openError(getShell(), Messages.Title_New_local_IFS_project, Messages.bind(Messages.Error_Could_not_create_project_A, message));
            return false;
        } catch (InterruptedException e) {
            return false;
        }

        workbench.getWorkingSetManager().addToWorkingSets(project, projectPage.getSelectedWorkingSets());
        BasicNewResourceWizard.selectAndReveal(project, workbench.getActiveWorkbenchWindow());

        return true;
    }

    /**
     * Configures the new project: encoding, <code>iproj.json</code> (unless
     * the location already contains one) and the settings of the local IFS
     * project.
     */
    private void configure(IProject aProject, boolean anIsUtf8, IProjConfig aConfig, String aConnectionName, String aDeployDirectory,
        IProgressMonitor aMonitor) throws Exception {

        if (anIsUtf8) {
            aProject.setDefaultCharset(UTF_8, aMonitor);
        }

        IFile file = aProject.getFile(IProjConfig.FILE_NAME);
        if (aConfig != null && !file.exists()) {
            LocalProject.writeConfigFile(aProject, aConfig.toJson(), null);
        }

        if (aConnectionName != null) {
            LocalProject.configure(aProject, aConnectionName, aDeployDirectory);
        }
    }
}
