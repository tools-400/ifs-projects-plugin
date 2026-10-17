/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.project.wizards;

import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkingSet;

import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.local.LocalProject;

/**
 * Wizard 'Import &gt; IFS Projects &gt; Existing Local IFS Project', that
 * creates a local IFS project for an existing directory, e.g. a git working
 * tree of a project of Code for IBM i, see {@link LocalProject}.
 * <ol>
 * <li>Directory and name of the project, see
 * {@link ImportLocalIfsProjectWizardPage}.</li>
 * <li>Connection, deploy directory, encoding and <code>iproj.json</code>, see
 * {@link NewLocalIfsProjectWizardPage}.</li>
 * </ol>
 */
public class ImportLocalIfsProjectWizard extends AbstractLocalIfsProjectWizard implements IImportWizard {

    private ImportLocalIfsProjectWizardPage importPage;

    public ImportLocalIfsProjectWizard() {
        super(Messages.Title_Import_local_IFS_project);
    }

    @Override
    public void addPages() {

        importPage = new ImportLocalIfsProjectWizardPage();
        addPage(importPage);

        addSettingsPage(importPage);
    }

    @Override
    public boolean performFinish() {
        return createProject(importPage.createProjectDescription(), new IWorkingSet[0]);
    }
}
