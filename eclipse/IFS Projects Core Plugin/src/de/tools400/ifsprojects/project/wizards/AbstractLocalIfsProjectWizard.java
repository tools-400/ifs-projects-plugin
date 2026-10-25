/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.project.wizards;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.ide.undo.CreateProjectOperation;
import org.eclipse.ui.ide.undo.WorkspaceUndoUtil;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

import de.tools400.ifsprojects.IfsProjectsPlugin;
import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.local.LocalProject;
import de.tools400.ifsprojects.project.IProjConfig;
import de.tools400.ifsprojects.utils.ExceptionUtils;

/**
 * Base of the wizards, that create a local IFS project in the workspace, see
 * {@link LocalProject}. The last page defines connection, deploy directory,
 * encoding and <code>iproj.json</code>, see
 * {@link NewLocalIfsProjectWizardPage}.
 * <p>
 * The project is created like the general project of Eclipse, so that its
 * creation can be undone. The files are uploaded with the first build.
 */
abstract class AbstractLocalIfsProjectWizard extends Wizard {

    private static final String UTF_8 = "UTF-8"; //$NON-NLS-1$

    private final String title;

    private IWorkbench workbench;
    private IStructuredSelection selection;

    private NewLocalIfsProjectWizardPage settingsPage;

    /**
     * Creates the wizard.
     *
     * @param aTitle - title of the wizard
     */
    protected AbstractLocalIfsProjectWizard(String aTitle) {
        this.title = aTitle;
        setWindowTitle(aTitle);
        setNeedsProgressMonitor(true);
    }

    public void init(IWorkbench aWorkbench, IStructuredSelection aSelection) {
        this.workbench = aWorkbench;
        this.selection = aSelection;
    }

    protected IStructuredSelection getSelection() {
        return selection;
    }

    /**
     * Adds the last page: connection, deploy directory, encoding and
     * <code>iproj.json</code>.
     *
     * @param aProjectLocation - name and directory of the project
     */
    protected void addSettingsPage(NewLocalIfsProjectWizardPage.IProjectLocation aProjectLocation) {
        settingsPage = new NewLocalIfsProjectWizardPage(aProjectLocation, title);
        addPage(settingsPage);
    }

    /**
     * Creates the project and configures it with the settings of the last page.
     *
     * @param aDescription - description with name and location of the project
     * @param aWorkingSets - working sets of the project
     * @return <code>true</code>, if the project has been created
     */
    protected boolean createProject(IProjectDescription aDescription, IWorkingSet[] aWorkingSets) {

        final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(aDescription.getName());

        final boolean isUtf8 = settingsPage.isUtf8();
        final IProjConfig config = settingsPage.isCreateIProj() ? settingsPage.createConfig() : null;
        final String connectionName = settingsPage.getConnectionName();
        final String deployDirectory = settingsPage.getDeployDirectory();

        final CreateProjectOperation operation = new CreateProjectOperation(aDescription, title);
        final IAdaptable uiInfo = WorkspaceUndoUtil.getUIInfoAdapter(getShell());

        try {
            getContainer().run(true, true, new IRunnableWithProgress() {
                public void run(IProgressMonitor aMonitor) throws InvocationTargetException {
                    try {
                        IStatus status = workbench.getOperationSupport().getOperationHistory().execute(operation, aMonitor, uiInfo);
                        if (!status.isOK()) {
                            throw new InvocationTargetException(status.getException(), status.getMessage());
                        }
                    } catch (ExecutionException e) {
                        throw new InvocationTargetException(e);
                    }
                }
            });
        } catch (InvocationTargetException e) {
            IfsProjectsPlugin.logError("Could not create local IFS project: " + project.getName(), ExceptionUtils.getCause(e)); //$NON-NLS-1$
            MessageDialog.openError(getShell(), title,
                Messages.bind(Messages.Error_Could_not_create_project_A, ExceptionUtils.getLocalizedMessage(e)));
            return false;
        } catch (InterruptedException e) {
            return false;
        }

        // The project exists now. If it cannot be configured, it is kept and
        // the wizard is closed: Finish would fail on the existing project, and
        // the settings can be completed on the property pages of the project.
        try {
            getContainer().run(true, false, new IRunnableWithProgress() {
                public void run(IProgressMonitor aMonitor) throws InvocationTargetException {
                    try {
                        configure(project, isUtf8, config, connectionName, deployDirectory, aMonitor);
                    } catch (Exception e) {
                        throw new InvocationTargetException(e);
                    }
                }
            });
        } catch (InvocationTargetException e) {
            IfsProjectsPlugin.logError("Could not configure local IFS project: " + project.getName(), ExceptionUtils.getCause(e)); //$NON-NLS-1$
            MessageDialog.openWarning(getShell(), title,
                Messages.bind(Messages.Warning_Project_A_created_but_not_configured_B, project.getName(), ExceptionUtils.getLocalizedMessage(e)));
        } catch (InterruptedException e) {
            // The configuration cannot be canceled.
        }

        workbench.getWorkingSetManager().addToWorkingSets(project, aWorkingSets);
        BasicNewResourceWizard.selectAndReveal(project, workbench.getActiveWorkbenchWindow());

        return true;
    }

    /**
     * Configures the new project: encoding, <code>iproj.json</code> (unless the
     * location already contains one) and the settings of the local IFS project.
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
