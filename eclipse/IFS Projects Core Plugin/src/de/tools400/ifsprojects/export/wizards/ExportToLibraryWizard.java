/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.export.wizards;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;

import com.ibm.as400.access.AS400;

import de.tools400.ifsprojects.IfsProjectsPlugin;
import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.dialogs.OverwriteQueryDialog;
import de.tools400.ifsprojects.export.ExportAnalyzer;
import de.tools400.ifsprojects.export.ExportJob;
import de.tools400.ifsprojects.export.ExportPlan;
import de.tools400.ifsprojects.export.ExportSettings;
import de.tools400.ifsprojects.export.ExportSource;
import de.tools400.ifsprojects.preferences.Preferences;
import de.tools400.ifsprojects.project.IProjValues;
import de.tools400.ifsprojects.remote.RemoteSession;

/**
 * Exports the source files of a project into members of a library, see
 * {@link ExportPlan} and {@link ExportJob}. The plan is computed by the
 * {@link ExportAnalyzer}.
 */
public class ExportToLibraryWizard extends Wizard {

    private final ExportSource source;
    private final AS400 rseSystem;
    private final ExportSettings initialSettings;

    private final ExportAnalyzer analyzer;

    private ExportSettingsWizardPage settingsPage;
    private ExportPreviewWizardPage previewPage;

    /**
     * Creates the wizard.
     *
     * @param aSource - project
     * @param aRseSystem - connected system object of RSE, which is copied
     */
    public ExportToLibraryWizard(ExportSource aSource, AS400 aRseSystem) {
        this.source = aSource;
        this.rseSystem = aRseSystem;
        this.analyzer = new ExportAnalyzer(aSource);

        Preferences preferences = Preferences.getInstance();
        String objectLibrary = IProjValues.getObjectLibrary(aSource.getConfig(), aSource.getVariables());
        this.initialSettings = aSource.loadSettings(new ExportSettings(objectLibrary, preferences.getExportCcsid(),
            preferences.getExportRecordLength()));

        setWindowTitle(Messages.Title_Export_to_library);
        setNeedsProgressMonitor(true);
    }

    @Override
    public void addPages() {
        settingsPage = new ExportSettingsWizardPage(source.getName(), initialSettings);
        addPage(settingsPage);
        previewPage = new ExportPreviewWizardPage();
        addPage(previewPage);
    }

    @Override
    public boolean canFinish() {
        return getContainer().getCurrentPage() == previewPage && previewPage.isPageComplete();
    }

    @Override
    public boolean performFinish() {

        ExportPlan plan = previewPage.getPlan();

        try {
            source.saveSettings(plan.getSettings());
        } catch (Exception e) {
            IfsProjectsPlugin.logError("Could not save the export settings of: " + source.getName(), e); //$NON-NLS-1$
        }

        new ExportJob(source.getHost(), rseSystem, source.getName(), plan, new OverwriteQueryDialog(Messages.Title_Export_to_library,
            Messages.Question_Replace_member_A)).schedule();

        return true;
    }

    /**
     * Reads the project and the library with the settings of the first page and
     * computes the plan of the export. Asks the user, whether a missing library
     * shall be created.
     *
     * @return preview page or <code>null</code>, if the export is not possible
     */
    IWizardPage analyze() {

        final ExportSettings settings = settingsPage.getSettings();
        final ExportPlan[] plan = new ExportPlan[1];

        try {
            getContainer().run(true, true, new IRunnableWithProgress() {
                public void run(IProgressMonitor aMonitor) throws InvocationTargetException, InterruptedException {
                    try (RemoteSession session = new RemoteSession(rseSystem)) {
                        plan[0] = analyzer.analyze(session.getSystem(), settings, aMonitor);
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
            IfsProjectsPlugin.logError("Could not read the project for the export: " + source.getName(), cause); //$NON-NLS-1$
            settingsPage.setErrorMessage(Messages.bind(Messages.Error_Could_not_read_A_B, source.getName(), cause.getLocalizedMessage()));
            return null;
        } catch (InterruptedException e) {
            return null;
        }

        if (plan[0].isCreateLibrary()
            && !MessageDialog.openQuestion(getShell(), Messages.Title_Export_to_library, Messages.bind(Messages.Question_Create_library_A,
                settings.getLibrary()))) {
            return null;
        }

        settingsPage.setErrorMessage(null);
        previewPage.setPlan(plan[0]);

        return previewPage;
    }
}
