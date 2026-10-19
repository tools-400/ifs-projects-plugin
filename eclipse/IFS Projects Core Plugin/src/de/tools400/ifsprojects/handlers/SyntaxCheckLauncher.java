/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.handlers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.ibm.as400.access.AS400;

import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.ibmi.IfsPaths;
import de.tools400.ifsprojects.local.Deployment;
import de.tools400.ifsprojects.local.LocalProject;
import de.tools400.ifsprojects.local.SourceMapping;
import de.tools400.ifsprojects.project.IProjConfig;
import de.tools400.ifsprojects.rse.RemoteEditors;
import de.tools400.ifsprojects.subsystem.IfsProject;
import de.tools400.ifsprojects.subsystem.IfsProjects;
import de.tools400.ifsprojects.syntaxcheck.SourceType;
import de.tools400.ifsprojects.syntaxcheck.SyntaxCheckJob;

/**
 * Starts the syntax check of an RPGLE or SQLRPGLE stream file of an IFS
 * project or of a file of a local IFS project. Used by the context menus of
 * the Remote Systems view and of the projects of the workspace and by the
 * LPEX user action.
 * <p>
 * Unsaved changes of the file are saved before, if the user agrees.
 */
public final class SyntaxCheckLauncher {

    private static final int BUTTON_YES = 0;
    private static final int BUTTON_NO = 1;
    private static final char SEPARATOR = '/';

    private SyntaxCheckLauncher() {
        // Utility class
    }

    /**
     * Starts the syntax check. Errors are reported to the user. Must be called
     * on the UI thread.
     *
     * @param aShell - parent shell
     * @param aHost - RSE connection
     * @param aPath - absolute path of the stream file
     */
    public static void launch(Shell aShell, IHost aHost, String aPath) {

        String fileName = IfsPaths.getFileName(aPath);

        SourceType sourceType = SourceType.fromFileName(fileName);
        if (sourceType == null) {
            MessageDialog.openError(aShell, Messages.Title_IFS_Projects, Messages.bind(Messages.Error_Syntax_check_of_A_not_supported, fileName));
            return;
        }

        IfsProject project = IfsProjects.findProject(aHost, aPath);
        if (project == null) {
            MessageDialog.openError(aShell, Messages.Title_IFS_Projects, Messages.bind(Messages.Error_File_A_is_not_part_of_an_IFS_project, aPath));
            return;
        }

        List<IFile> savedFiles = saveEditors(aShell, aHost, aPath);
        if (savedFiles == null) {
            return;
        }

        IProjConfig config = ProjectCommands.readConfig(aShell, aHost, project.getDirectory());
        if (config == null) {
            return;
        }

        AS400 system = ProjectCommands.getConnectedSystem(aShell, aHost);
        if (system == null) {
            return;
        }

        new SyntaxCheckJob(aHost, system, project.getDirectory(), config, project.getEnvironmentVariables(), aPath, fileName, sourceType,
            savedFiles, null).schedule();
    }

    /**
     * Starts the syntax check of an RPGLE or SQLRPGLE file of a local IFS
     * project: uploads the changes of the project and checks the copy of the
     * file in the deploy directory. The errors are shown for the local files.
     * Errors are reported to the user. Must be called on the UI thread.
     *
     * @param aShell - parent shell
     * @param aFile - file of a local IFS project
     */
    public static void launch(Shell aShell, IFile aFile) {

        SourceType sourceType = SourceType.fromFileName(aFile.getName());
        if (sourceType == null) {
            MessageDialog.openError(aShell, Messages.Title_IFS_Projects, Messages.bind(Messages.Error_Syntax_check_of_A_not_supported, aFile.getName()));
            return;
        }

        LocalProject localProject = LocalProjectCommands.getLocalProject(aShell, aFile.getProject());
        if (localProject == null) {
            return;
        }

        IHost host = LocalProjectCommands.getHost(aShell, localProject);
        if (host == null || !saveEditors(aShell, aFile)) {
            return;
        }

        AS400 system = ProjectCommands.getConnectedSystem(aShell, host);
        if (system == null) {
            return;
        }

        IProjConfig config = LocalProjectCommands.readConfig(aShell, localProject);
        if (config == null) {
            return;
        }

        Deployment.Result upload = LocalProjectCommands.uploadChanges(aShell, system, localProject, aFile);
        if (upload == null) {
            return;
        }

        String directory = upload.getDirectory();
        String path = directory + SEPARATOR + aFile.getProjectRelativePath().toString();

        new SyntaxCheckJob(host, system, directory, config, LocalProjectCommands.getEffectiveVariables(localProject), path, aFile.getName(),
            sourceType, new ArrayList<IFile>(), new SourceMapping(directory, aFile.getProject().getName())).schedule();
    }

    /**
     * Saves the editors of a local file, that have unsaved changes, if the
     * user agrees.
     *
     * @return <code>false</code>, if the user canceled the syntax check or an
     *         editor could not be saved
     */
    private static boolean saveEditors(Shell aShell, IFile aFile) {

        List<IEditorPart> editors = new ArrayList<IEditorPart>();
        for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
            for (IWorkbenchPage page : window.getPages()) {
                for (IEditorPart editor : page.getDirtyEditors()) {
                    if (aFile.equals(editor.getEditorInput().getAdapter(IFile.class))) {
                        editors.add(editor);
                    }
                }
            }
        }

        return saveEditors(aShell, editors, aFile.getFullPath().toString()) != null;
    }

    /**
     * Saves the editors of the file, that have unsaved changes, if the user
     * agrees.
     *
     * @return local files of the saved editors or <code>null</code>, if the
     *         user canceled the syntax check or an editor could not be saved
     */
    private static List<IFile> saveEditors(Shell aShell, IHost aHost, String aPath) {
        return saveEditors(aShell, RemoteEditors.findDirtyEditors(aHost, aPath), aPath);
    }

    /**
     * Saves editors with unsaved changes, if the user agrees.
     *
     * @return local files of the saved editors or <code>null</code>, if the
     *         user canceled the syntax check or an editor could not be saved
     */
    private static List<IFile> saveEditors(Shell aShell, List<IEditorPart> anEditors, String aPath) {

        List<IFile> savedFiles = new ArrayList<IFile>();

        if (anEditors.isEmpty()) {
            return savedFiles;
        }

        MessageDialog dialog = new MessageDialog(aShell, Messages.Title_IFS_Projects, null,
            Messages.bind(Messages.Question_Save_A_before_syntax_check, aPath), MessageDialog.QUESTION,
            new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.CANCEL_LABEL }, BUTTON_YES);

        int answer = dialog.open();
        if (answer == BUTTON_NO) {
            return savedFiles;
        }

        if (answer != BUTTON_YES) {
            return null;
        }

        for (IEditorPart editor : anEditors) {
            editor.doSave(new NullProgressMonitor());
            if (editor.isDirty()) {
                MessageDialog.openError(aShell, Messages.Title_IFS_Projects, Messages.bind(Messages.Error_Could_not_save_A, aPath));
                return null;
            }
            IFile localFile = RemoteEditors.getFile(editor);
            if (localFile != null) {
                savedFiles.add(localFile);
            }
        }

        return savedFiles;
    }
}
