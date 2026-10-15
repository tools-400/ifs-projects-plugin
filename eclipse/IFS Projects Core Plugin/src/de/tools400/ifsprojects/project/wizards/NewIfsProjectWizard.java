/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.project.wizards;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.rse.core.model.IHost;

import de.tools400.ifsprojects.IfsProjectsPlugin;
import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.project.IProjConfig;
import de.tools400.ifsprojects.project.IProjFileContent;
import de.tools400.ifsprojects.project.IProjFileException;
import de.tools400.ifsprojects.project.IProjFileStore;
import de.tools400.ifsprojects.project.IfsProjectFilters;

/**
 * Wizard that creates an IFS project: a filter of the 'IFS Projects' subsystem
 * and optionally the project directory and its <code>iproj.json</code> file.
 * An existing <code>iproj.json</code> file is never replaced.
 */
public class NewIfsProjectWizard extends Wizard {

    private final IHost host;
    private final String initialDirectory;

    private NewIfsProjectWizardPage page;

    public NewIfsProjectWizard(IHost aHost, String anInitialDirectory) {
        this.host = aHost;
        this.initialDirectory = anInitialDirectory;
        setWindowTitle(Messages.Title_New_IFS_project);
        setNeedsProgressMonitor(true);
    }

    @Override
    public void addPages() {
        page = new NewIfsProjectWizardPage(host, initialDirectory);
        addPage(page);
    }

    @Override
    public boolean performFinish() {

        final String name = page.getProjectName();
        final String directory = page.getDirectory();
        final boolean isCreateDirectory = page.isCreateDirectory();
        final boolean isCreateIProj = page.isCreateIProj();
        final String description = page.getProjectDescription();
        final String buildCommand = page.getBuildCommand();
        final String compileCommand = page.getCompileCommand();
        final String repository = page.getRepository();

        try {
            IProjFileStore.connect(host);
        } catch (IProjFileException e) {
            page.setErrorMessage(e.getLocalizedMessage());
            return false;
        }

        try {
            getContainer().run(true, false, new IRunnableWithProgress() {
                public void run(IProgressMonitor aMonitor) throws InvocationTargetException, InterruptedException {
                    aMonitor.beginTask(Messages.bind(Messages.Task_Creating_IFS_project_A, name), IProgressMonitor.UNKNOWN);
                    try {
                        prepareDirectory(host, directory, isCreateDirectory, isCreateIProj, description, buildCommand, compileCommand,
                            repository);
                    } catch (IProjFileException e) {
                        throw new InvocationTargetException(e);
                    } finally {
                        aMonitor.done();
                    }
                }
            });
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            IfsProjectsPlugin.logError("Could not prepare directory of IFS project: " + directory, //$NON-NLS-1$
                cause.getCause() != null ? cause.getCause() : cause);
            page.setErrorMessage(cause.getLocalizedMessage());
            return false;
        } catch (InterruptedException e) {
            return false;
        }

        try {
            IfsProjectFilters.create(host, name, directory);
        } catch (Exception e) {
            IfsProjectsPlugin.logError("Could not create IFS project filter: " + name, e); //$NON-NLS-1$
            page.setErrorMessage(Messages.bind(Messages.Error_Could_not_create_project_filter_A, e.getLocalizedMessage()));
            return false;
        }

        return true;
    }

    private void prepareDirectory(IHost aHost, String aDirectory, boolean anIsCreateDirectory, boolean anIsCreateIProj, String aDescription,
        String aBuildCommand, String aCompileCommand, String aRepository) throws IProjFileException {

        if (!IProjFileStore.existsDirectory(aHost, aDirectory)) {
            if (!anIsCreateDirectory) {
                throw new IProjFileException(Messages.bind(Messages.Error_Directory_A_does_not_exist, aDirectory), null);
            }
            IProjFileStore.createDirectory(aHost, aDirectory);
        }

        if (anIsCreateIProj) {
            IProjFileContent fileContent = IProjFileStore.read(aHost, aDirectory);
            if (!fileContent.exists()) {
                IProjConfig config = new IProjConfig();
                config.setString(IProjConfig.DESCRIPTION, aDescription);
                config.setString(IProjConfig.REPOSITORY, aRepository);
                config.setString(IProjConfig.BUILD_COMMAND, aBuildCommand);
                config.setString(IProjConfig.COMPILE_COMMAND, aCompileCommand);
                IProjFileStore.write(aHost, aDirectory, config.toJson(), fileContent);
            }
        }
    }
}
