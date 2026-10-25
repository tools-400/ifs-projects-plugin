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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.local.Deployment;

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

        LocalProjectCommands.ConnectedProject project = LocalProjectCommands.prepare(shell, LocalProjectCommands.getSelectedProject(anEvent));
        if (project == null) {
            return null;
        }

        Deployment.Result upload = LocalProjectCommands.upload(shell, project.getSystem(), project.getLocalProject(), isAll());
        if (upload != null) {
            MessageDialog.openInformation(shell, Messages.Title_IFS_Projects,
                Messages.bind(Messages.Info_Upload_to_A_B_uploaded_C_deleted_D_unchanged, new Object[] { upload.getDirectory(),
                    Integer.toString(upload.getUploaded()), Integer.toString(upload.getDeleted()), Integer.toString(upload.getUnchanged()) }));
        }

        return null;
    }
}
