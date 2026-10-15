/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.rse;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.rse.core.RSECorePlugin;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.core.subsystems.ISubSystem;
import org.eclipse.rse.subsystems.files.core.SystemIFileProperties;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.ResourceUtil;

/**
 * Finds the editors of remote files. RSE edits a remote file as local copy in
 * the workspace, whose properties identify the remote file.
 * <p>
 * Must be called on the UI thread.
 */
public final class RemoteEditors {

    private RemoteEditors() {
        // Utility class
    }

    /**
     * Returns the editors with unsaved changes of a remote file.
     *
     * @param aHost - RSE connection
     * @param aPath - absolute path of the remote file
     * @return editors, never <code>null</code>
     */
    public static List<IEditorPart> findDirtyEditors(IHost aHost, String aPath) {

        List<IEditorPart> editors = new ArrayList<IEditorPart>();
        for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
            for (IWorkbenchPage page : window.getPages()) {
                for (IEditorPart editor : page.getDirtyEditors()) {
                    IFile file = getFile(editor);
                    if (file != null && isLocalCopyOf(file, aHost, aPath) && !editors.contains(editor)) {
                        editors.add(editor);
                    }
                }
            }
        }

        return editors;
    }

    /**
     * Returns the local file edited by an editor.
     *
     * @param anEditor - editor
     * @return file or <code>null</code>
     */
    public static IFile getFile(IEditorPart anEditor) {
        return ResourceUtil.getFile(anEditor.getEditorInput());
    }

    /**
     * Returns the RSE connection of the remote file, whose local copy is
     * edited.
     *
     * @param aFile - local copy of a remote file
     * @return connection or <code>null</code>, if the file is not the local
     *         copy of a remote file
     */
    public static IHost getHost(IFile aFile) {

        String subSystemName = new SystemIFileProperties(aFile).getRemoteFileSubSystem();
        if (subSystemName == null) {
            return null;
        }

        ISubSystem subSystem = RSECorePlugin.getTheSystemRegistry().getSubSystem(subSystemName);
        if (subSystem == null) {
            return null;
        }

        return subSystem.getHost();
    }

    /**
     * Returns the absolute path of the remote file, whose local copy is
     * edited.
     *
     * @param aFile - local copy of a remote file
     * @return path or <code>null</code>, if the file is not the local copy of a
     *         remote file
     */
    public static String getRemotePath(IFile aFile) {
        return new SystemIFileProperties(aFile).getRemoteFilePath();
    }

    private static boolean isLocalCopyOf(IFile aFile, IHost aHost, String aPath) {
        return aPath.equals(getRemotePath(aFile)) && aHost.equals(getHost(aFile));
    }
}
