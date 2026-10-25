/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.handlers;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import com.ibm.as400.access.AS400;
import com.ibm.etools.iseries.subsystems.qsys.objects.QSYSRemoteLibrary;
import com.ibm.etools.iseries.subsystems.qsys.objects.QSYSRemoteObject;
import com.ibm.etools.iseries.subsystems.qsys.objects.QSYSRemoteSourceFile;

import de.tools400.ifsprojects.IfsProjectsPlugin;
import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.ibmi.IfsPaths;
import de.tools400.ifsprojects.migration.SourceFile;
import de.tools400.ifsprojects.migration.SourceFileCatalog;
import de.tools400.ifsprojects.migration.wizards.MigrateToIfsProjectWizard;
import de.tools400.ifsprojects.remote.RemoteJobs;
import de.tools400.ifsprojects.remote.RemoteSession;
import de.tools400.ifsprojects.utils.ExceptionUtils;

/**
 * Migrates source files into a new IFS project: all source files of a selected
 * library or the selected source files of one library, see
 * {@link MigrateToIfsProjectWizard}.
 */
public class MigrateSourceFilesHandler extends AbstractHandler {

    public Object execute(ExecutionEvent anEvent) throws ExecutionException {

        Shell shell = HandlerUtil.getActiveShell(anEvent);

        List<QSYSRemoteObject> objects = getSelectedObjects(HandlerUtil.getCurrentSelection(anEvent));
        if (objects.isEmpty()) {
            return null;
        }

        QSYSRemoteObject first = objects.get(0);
        IHost host = first.getRemoteObjectContext().getObjectSubsystem().getHost();
        final String library = first instanceof QSYSRemoteLibrary ? first.getName() : first.getLibrary();
        final Set<String> selectedFiles = getSelectedFiles(objects);

        AS400 system = ProjectCommands.getConnectedSystem(shell, host);
        if (system == null) {
            return null;
        }

        final List<SourceFile> sourceFiles = new ArrayList<>();
        final String[] homeDirectory = new String[1];
        try {
            readSourceFiles(shell, system, library, selectedFiles, sourceFiles, homeDirectory);
        } catch (InvocationTargetException e) {
            Throwable cause = ExceptionUtils.getCause(e);
            IfsProjectsPlugin.logError("Could not read the source files of: " + library, cause); //$NON-NLS-1$
            MessageDialog.openError(shell, Messages.Title_Migrate_to_IFS_project,
                Messages.bind(Messages.Error_Could_not_read_A_B, library, ExceptionUtils.getLocalizedMessage(cause)));
            return null;
        } catch (InterruptedException e) {
            return null;
        }

        if (sourceFiles.isEmpty()) {
            MessageDialog.openInformation(shell, Messages.Title_Migrate_to_IFS_project,
                Messages.bind(Messages.Info_Library_A_has_no_source_files, library));
            return null;
        }

        String initialDirectory = IfsPaths.append(homeDirectory[0], library.toLowerCase(Locale.ROOT));
        new WizardDialog(shell, new MigrateToIfsProjectWizard(host, system, library, sourceFiles, initialDirectory)).open();

        return null;
    }

    /**
     * Returns the selected library or the selected source files of the library
     * of the first one.
     */
    private static List<QSYSRemoteObject> getSelectedObjects(ISelection aSelection) {

        List<QSYSRemoteObject> objects = new ArrayList<>();
        if (!(aSelection instanceof IStructuredSelection)) {
            return objects;
        }

        for (Object element : ((IStructuredSelection)aSelection).toList()) {
            if (element instanceof QSYSRemoteLibrary && ((IStructuredSelection)aSelection).size() == 1) {
                objects.add((QSYSRemoteLibrary)element);
            } else if (element instanceof QSYSRemoteSourceFile) {
                QSYSRemoteSourceFile sourceFile = (QSYSRemoteSourceFile)element;
                if (objects.isEmpty() || objects.get(0).getLibrary().equals(sourceFile.getLibrary())) {
                    objects.add(sourceFile);
                }
            }
        }

        return objects;
    }

    /**
     * Returns the names of the selected source files or <code>null</code>, if a
     * library is selected.
     */
    private static Set<String> getSelectedFiles(List<QSYSRemoteObject> anObjects) {

        if (anObjects.get(0) instanceof QSYSRemoteLibrary) {
            return null;
        }

        Set<String> files = new HashSet<>();
        for (QSYSRemoteObject object : anObjects) {
            files.add(object.getName());
        }

        return files;
    }

    private static void readSourceFiles(Shell aShell, final AS400 aSystem, final String aLibrary, final Set<String> aSelectedFiles,
        final List<SourceFile> aSourceFiles, final String[] aHomeDirectory) throws InvocationTargetException, InterruptedException {

        new ProgressMonitorDialog(aShell).run(true, false, new IRunnableWithProgress() {
            public void run(IProgressMonitor aMonitor) throws InvocationTargetException {
                aMonitor.beginTask(Messages.bind(Messages.Task_Reading_library_A, aLibrary), IProgressMonitor.UNKNOWN);
                try (RemoteSession session = new RemoteSession(aSystem)) {
                    for (SourceFile sourceFile : SourceFileCatalog.getSourceFiles(session.getSystem(), aLibrary, null)) {
                        if (aSelectedFiles == null || aSelectedFiles.contains(sourceFile.getName())) {
                            aSourceFiles.add(sourceFile);
                        }
                    }
                    aHomeDirectory[0] = RemoteJobs.getHomeDirectory(session.getSystem());
                } catch (Exception e) {
                    throw new InvocationTargetException(e);
                } finally {
                    aMonitor.done();
                }
            }
        });
    }
}
