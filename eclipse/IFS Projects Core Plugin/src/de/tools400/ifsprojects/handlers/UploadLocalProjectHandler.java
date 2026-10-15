/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import com.ibm.as400.access.AS400;

import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.local.Deployment;
import de.tools400.ifsprojects.local.LocalProject;

/**
 * Uploads the new and changed files of the selected local IFS project to its
 * deploy directory and deletes the files, that do not exist any more, see
 * {@link Deployment}.
 */
public class UploadLocalProjectHandler extends AbstractHandler {

    /**
     * Specifies whether all files are uploaded, also unchanged files.
     *
     * @return <code>true</code> to upload all files
     */
    protected boolean isAll() {
        return false;
    }

    public Object execute(ExecutionEvent anEvent) throws ExecutionException {

        Shell shell = HandlerUtil.getActiveShell(anEvent);

        IProject project = LocalProjectCommands.getSelectedProject(anEvent);
        LocalProject localProject = LocalProjectCommands.getLocalProject(shell, project);
        if (localProject == null) {
            return null;
        }

        IHost host = LocalProjectCommands.getHost(shell, localProject);
        if (host == null || !LocalProjectCommands.saveEditors(project)) {
            return null;
        }

        AS400 system = RseBuildLauncher.getConnectedSystem(shell, host);
        if (system == null) {
            return null;
        }

        Deployment.Result upload = LocalProjectCommands.upload(shell, system, localProject, isAll());
        if (upload != null) {
            MessageDialog.openInformation(shell, Messages.Title_IFS_Projects, Messages.bind(Messages.Info_Upload_to_A_B_uploaded_C_deleted_D_unchanged,
                new Object[] { upload.getDirectory(), Integer.toString(upload.getUploaded()), Integer.toString(upload.getDeleted()),
                    Integer.toString(upload.getUnchanged()) }));
        }

        return null;
    }
}
