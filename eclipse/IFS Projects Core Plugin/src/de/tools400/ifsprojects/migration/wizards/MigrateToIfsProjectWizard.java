/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.migration.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.rse.core.model.IHost;

import com.ibm.as400.access.AS400;

import de.tools400.ifsprojects.IfsProjectsPlugin;
import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.dialogs.OverwriteQueryDialog;
import de.tools400.ifsprojects.migration.MigrationAnalyzer;
import de.tools400.ifsprojects.migration.MigrationFiles;
import de.tools400.ifsprojects.migration.MigrationJob;
import de.tools400.ifsprojects.migration.MigrationPlan;
import de.tools400.ifsprojects.migration.MigrationSettings;
import de.tools400.ifsprojects.migration.SourceFile;
import de.tools400.ifsprojects.preferences.Preferences;
import de.tools400.ifsprojects.project.BuildTool;
import de.tools400.ifsprojects.project.IfsProjectCreator;
import de.tools400.ifsprojects.project.wizards.NewIfsProjectWizardPage;
import de.tools400.ifsprojects.remote.RemoteSession;

/**
 * Migrates the members of source files into a new IFS project:
 * <ol>
 * <li>{@link SourceFilesWizardPage}: source files and options; leaving the
 * page reads the members and computes the {@link MigrationPlan}.</li>
 * <li>{@link MigrationPreviewWizardPage}: the stream files and the
 * problems.</li>
 * <li>{@link NewIfsProjectWizardPage}: the new IFS project. Finish creates it
 * and starts the {@link MigrationJob}, which writes the files.</li>
 * </ol>
 */
public class MigrateToIfsProjectWizard extends Wizard {

    private final IHost host;
    private final AS400 rseSystem;
    private final String library;
    private final List<SourceFile> sourceFiles;
    private final String initialDirectory;

    private SourceFilesWizardPage sourceFilesPage;
    private MigrationPreviewWizardPage previewPage;
    private NewIfsProjectWizardPage projectPage;

    /**
     * Creates the wizard.
     *
     * @param aHost - RSE connection
     * @param aRseSystem - connected system object of RSE, which is copied
     * @param aLibrary - library of the source files
     * @param aSourceFiles - source files, that can be migrated
     * @param anInitialDirectory - proposed project directory
     */
    public MigrateToIfsProjectWizard(IHost aHost, AS400 aRseSystem, String aLibrary, List<SourceFile> aSourceFiles, String anInitialDirectory) {
        this.host = aHost;
        this.rseSystem = aRseSystem;
        this.library = aLibrary;
        this.sourceFiles = aSourceFiles;
        this.initialDirectory = anInitialDirectory;
        setWindowTitle(Messages.Title_Migrate_to_IFS_project);
        setNeedsProgressMonitor(true);
    }

    @Override
    public void addPages() {
        sourceFilesPage = new SourceFilesWizardPage(library, sourceFiles, Preferences.getInstance().getExportCcsid());
        addPage(sourceFilesPage);
        previewPage = new MigrationPreviewWizardPage();
        addPage(previewPage);
        projectPage = new NewIfsProjectWizardPage(host, initialDirectory);
        projectPage.setTitle(Messages.Title_Migrate_to_IFS_project);
        projectPage.setDescription(Messages.bind(Messages.Description_Migration_project_page, library));
        addPage(projectPage);
    }

    @Override
    public boolean canFinish() {
        return getContainer().getCurrentPage() == projectPage && projectPage.isPageComplete() && previewPage.isPageComplete();
    }

    @Override
    public boolean performFinish() {

        try {
            IfsProjectCreator.create(getContainer(), host, projectPage.getProjectName(), projectPage.getDirectory(), projectPage
                .isCreateDirectory(), projectPage.isCreateIProj() ? projectPage.createConfig() : null);
        } catch (CoreException e) {
            projectPage.setErrorMessage(e.getStatus().getMessage());
            return false;
        } catch (InterruptedException e) {
            return false;
        }

        boolean isTobi = projectPage.getBuildTool() == BuildTool.TOBI;
        Map<String, String> files = MigrationFiles.getFiles(previewPage.getPlan(), sourceFilesPage.getSelectedSourceFiles(), isTobi);

        new MigrationJob(rseSystem, library, projectPage.getDirectory(), files, new OverwriteQueryDialog(Messages.Title_Migrate_to_IFS_project,
            Messages.Question_Replace_file_A)).schedule();

        return true;
    }

    /**
     * Reads the members of the selected source files and computes the plan.
     *
     * @return preview page or <code>null</code>, if the members could not be
     *         read
     */
    IWizardPage analyze() {

        final List<SourceFile> selectedSourceFiles = sourceFilesPage.getSelectedSourceFiles();
        final MigrationSettings settings = sourceFilesPage.getSettings();
        final MigrationPlan[] plan = new MigrationPlan[1];

        try {
            getContainer().run(true, true, new IRunnableWithProgress() {
                public void run(IProgressMonitor aMonitor) throws InvocationTargetException, InterruptedException {
                    try (RemoteSession session = new RemoteSession(rseSystem)) {
                        plan[0] = MigrationAnalyzer.analyze(session, library, selectedSourceFiles, settings, aMonitor);
                    } catch (InterruptedException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new InvocationTargetException(e);
                    } finally {
                        aMonitor.done();
                    }
                }
            });
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            IfsProjectsPlugin.logError("Could not read the source files of: " + library, cause); //$NON-NLS-1$
            sourceFilesPage.setErrorMessage(Messages.bind(Messages.Error_Could_not_read_A_B, library, cause.getLocalizedMessage()));
            return null;
        } catch (InterruptedException e) {
            return null;
        }

        sourceFilesPage.setErrorMessage(null);
        previewPage.setPlan(plan[0]);

        return previewPage;
    }
}
