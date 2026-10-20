/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.project.wizards;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.rse.core.model.IHost;

import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.project.IfsProjectCreator;

/**
 * Wizard that creates an IFS project: a filter of the 'IFS Projects' subsystem
 * and optionally the project directory and its <code>iproj.json</code> file,
 * see {@link IfsProjectCreator}.
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

        try {
            IfsProjectCreator.create(getContainer(), host, page.getProjectName(), page.getDirectory(), page.isCreateDirectory(),
                page.isCreateIProj() ? page.createConfig() : null);
        } catch (CoreException e) {
            page.setErrorMessage(e.getStatus().getMessage());
            return false;
        } catch (InterruptedException e) {
            return false;
        }

        return true;
    }
}
