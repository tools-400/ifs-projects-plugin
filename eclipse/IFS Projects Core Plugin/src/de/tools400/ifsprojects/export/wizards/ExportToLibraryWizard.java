/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.export.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Display;

import com.ibm.as400.access.AS400;

import de.tools400.ifsprojects.IfsProjectsPlugin;
import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.export.ExportJob;
import de.tools400.ifsprojects.export.ExportMapping;
import de.tools400.ifsprojects.export.ExportPlan;
import de.tools400.ifsprojects.export.ExportSettings;
import de.tools400.ifsprojects.export.ExportSource;
import de.tools400.ifsprojects.export.IncludeResolver;
import de.tools400.ifsprojects.export.LibraryInfo;
import de.tools400.ifsprojects.ibmi.IfsPaths;
import de.tools400.ifsprojects.preferences.Preferences;
import de.tools400.ifsprojects.project.IProjValues;
import de.tools400.ifsprojects.remote.RemoteJobs;
import de.tools400.ifsprojects.utils.PlatformUtils;

/**
 * Exports the source files of a project into members of a library, see
 * {@link ExportPlan} and {@link ExportJob}.
 * <p>
 * The files of the project are read once, when the preview is shown first.
 * The library is read again, if another library is entered.
 */
public class ExportToLibraryWizard extends Wizard {

    private static final ExportJob.OverwriteQuery.Answer[] ANSWERS = { ExportJob.OverwriteQuery.Answer.YES,
        ExportJob.OverwriteQuery.Answer.YES_TO_ALL, ExportJob.OverwriteQuery.Answer.NO, ExportJob.OverwriteQuery.Answer.NO_TO_ALL,
        ExportJob.OverwriteQuery.Answer.CANCEL };

    private final ExportSource source;
    private final AS400 rseSystem;
    private final ExportSettings initialSettings;

    private ExportSettingsWizardPage settingsPage;
    private ExportPreviewWizardPage previewPage;

    private String projectDirectory;
    private List<String> files;
    private Map<String, String> contents;
    private Map<String, String> readErrors;

    private String libraryName;
    private LibraryInfo libraryInfo;

    /**
     * Creates the wizard.
     *
     * @param aSource - project
     * @param aRseSystem - connected system object of RSE, which is copied
     */
    public ExportToLibraryWizard(ExportSource aSource, AS400 aRseSystem) {
        this.source = aSource;
        this.rseSystem = aRseSystem;

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

        new ExportJob(source.getHost(), rseSystem, source.getName(), plan, new ReplaceMemberQuery()).schedule();

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
                    AS400 system = RemoteJobs.copySystem(rseSystem);
                    try {
                        readProject(system, aMonitor);
                        if (!settings.getLibrary().equals(libraryName)) {
                            aMonitor.subTask(Messages.bind(Messages.Task_Reading_library_A, settings.getLibrary()));
                            libraryInfo = LibraryInfo.read(system, settings.getLibrary());
                            libraryName = settings.getLibrary();
                        }
                        List<String> includeDirectories = IProjValues.getIncludeDirectories(projectDirectory, source.getConfig(),
                            source.getVariables());
                        IncludeResolver resolver = new IncludeResolver(projectDirectory, includeDirectories, files);
                        plan[0] = new ExportPlan(settings, files, contents, readErrors, resolver, libraryInfo);
                    } catch (InterruptedException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new InvocationTargetException(e);
                    } finally {
                        system.disconnectAllServices();
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

    /**
     * Lists the files of the project and reads the files with an exported
     * source type, once per wizard.
     */
    private void readProject(AS400 aSystem, IProgressMonitor aMonitor) throws Exception {

        if (files != null) {
            return;
        }

        aMonitor.beginTask(Messages.bind(Messages.Task_Reading_files_of_A, source.getName()), IProgressMonitor.UNKNOWN);

        String directory = source.getProjectDirectory(aSystem);
        List<String> projectFiles = source.listFiles(aSystem);
        Map<String, String> fileContents = new HashMap<String, String>();
        Map<String, String> errors = new HashMap<String, String>();

        for (String path : projectFiles) {
            if (aMonitor.isCanceled()) {
                throw new InterruptedException();
            }
            if (ExportMapping.getSourceType(IfsPaths.getFileName(path)) == null) {
                continue;
            }
            aMonitor.subTask(path);
            try {
                fileContents.put(path, source.readFile(aSystem, path));
            } catch (Exception e) {
                IfsProjectsPlugin.logError("Could not read the file for the export: " + path, e); //$NON-NLS-1$
                errors.put(path, e.getLocalizedMessage());
            }
        }

        projectDirectory = directory;
        contents = fileContents;
        readErrors = errors;
        files = projectFiles;
    }

    /**
     * Asks the user with Yes, Yes To All, No, No To All and Cancel, whether an
     * existing member is replaced.
     */
    private static class ReplaceMemberQuery implements ExportJob.OverwriteQuery {

        public Answer ask(final String aMember) {

            final Answer[] answer = { Answer.CANCEL };
            Display.getDefault().syncExec(new Runnable() {
                public void run() {
                    MessageDialog dialog = new MessageDialog(PlatformUtils.getWorkbenchShell(), Messages.Title_Export_to_library, null, Messages.bind(
                        Messages.Question_Replace_member_A, aMember), MessageDialog.QUESTION, new String[] { IDialogConstants.YES_LABEL,
                            IDialogConstants.YES_TO_ALL_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.NO_TO_ALL_LABEL,
                            IDialogConstants.CANCEL_LABEL }, 0);
                    int index = dialog.open();
                    if (index >= 0 && index < ANSWERS.length) {
                        answer[0] = ANSWERS[index];
                    }
                }
            });

            return answer[0];
        }
    }
}
