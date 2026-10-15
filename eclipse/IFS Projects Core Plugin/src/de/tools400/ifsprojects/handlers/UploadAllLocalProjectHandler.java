/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.handlers;

/**
 * Uploads all files of the selected local IFS project to its deploy
 * directory, also unchanged files, e.g. after the deploy directory has been
 * changed outside of the plug-in.
 */
public class UploadAllLocalProjectHandler extends UploadLocalProjectHandler {

    @Override
    protected boolean isAll() {
        return true;
    }
}
