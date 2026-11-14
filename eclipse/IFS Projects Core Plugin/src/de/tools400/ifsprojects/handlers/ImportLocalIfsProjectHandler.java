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
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.handlers.HandlerUtil;

import de.tools400.ifsprojects.project.wizards.ImportLocalIfsProjectWizard;
import de.tools400.ifsprojects.rse.RseSelections;

/**
 * Opens the wizard 'Import Local IFS Project' for the selected directory or
 * directory filter of the subsystem 'Local Files' of RSE. The wizard fills in
 * the directory.
 */
public class ImportLocalIfsProjectHandler extends AbstractHandler {

    public Object execute(ExecutionEvent anEvent) throws ExecutionException {

        ISelection selection = HandlerUtil.getCurrentSelection(anEvent);
        if (RseSelections.getLocalDirectory(RseSelections.getSingleElement(selection)) == null) {
            return null;
        }

        ImportLocalIfsProjectWizard wizard = new ImportLocalIfsProjectWizard();
        wizard.init(HandlerUtil.getActiveWorkbenchWindowChecked(anEvent).getWorkbench(), (IStructuredSelection)selection);

        WizardDialog dialog = new WizardDialog(HandlerUtil.getActiveShellChecked(anEvent), wizard);
        dialog.open();

        return null;
    }
}
