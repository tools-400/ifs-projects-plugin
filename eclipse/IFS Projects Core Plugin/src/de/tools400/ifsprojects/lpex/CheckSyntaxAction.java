/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.lpex;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.ibm.lpex.core.LpexAction;
import com.ibm.lpex.core.LpexView;

import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.handlers.SyntaxCheckLauncher;
import de.tools400.ifsprojects.local.LocalProject;
import de.tools400.ifsprojects.rse.RemoteEditors;
import de.tools400.ifsprojects.syntaxcheck.SourceType;
import de.tools400.ifsprojects.utils.PlatformUtils;

/**
 * LPEX user action, that checks the syntax of the RPGLE or SQLRPGLE stream file
 * of an IFS project or of the file of a local IFS project of the editor and
 * shows the errors in the 'Error List' view of RDi.
 * <p>
 * The user action is configured on preference page <i>LPEX Editor &gt; User
 * Actions</i>, e.g. name <code>ifsCheckSyntax</code> and class
 * <code>de.tools400.ifsprojects.lpex.CheckSyntaxAction</code>. A key is
 * assigned on page <i>User Key Actions</i>. LPEX can load the class, because
 * the plug-in registers with LPEX, see {@link LpexRegistration}.
 */
public class CheckSyntaxAction implements LpexAction {

    /** Name of the document, i.e. path of the local file. */
    private static final String PARAMETER_NAME = "name"; //$NON-NLS-1$

    public boolean available(LpexView aView) {
        return SourceType.fromFileName(aView.query(PARAMETER_NAME)) != null;
    }

    public void doAction(LpexView aView) {

        Shell shell = PlatformUtils.getWorkbenchShell();

        IFile file = getFile(aView);
        IHost host = file == null ? null : RemoteEditors.getHost(file);
        String path = file == null ? null : RemoteEditors.getRemotePath(file);
        if (host != null && path != null) {
            SyntaxCheckLauncher.launch(shell, host, path);
            return;
        }

        if (file != null && LocalProject.get(file.getProject()) != null) {
            SyntaxCheckLauncher.launch(shell, file);
            return;
        }

        MessageDialog.openError(shell, Messages.Title_IFS_Projects, Messages.Error_Editor_does_not_edit_a_file_of_an_IFS_project);
    }

    /**
     * Returns the local file of the document. RSE edits remote files as local
     * copies in the workspace.
     */
    private IFile getFile(LpexView aView) {

        String name = aView.query(PARAMETER_NAME);
        if (name != null) {
            IFile file = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(Path.fromOSString(name));
            if (file != null) {
                return file;
            }
        }

        IWorkbenchPage page = getActivePage();
        IEditorPart editor = page == null ? null : page.getActiveEditor();
        if (editor == null) {
            return null;
        }

        return RemoteEditors.getFile(editor);
    }

    private IWorkbenchPage getActivePage() {

        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window == null) {
            return null;
        }

        return window.getActivePage();
    }
}
