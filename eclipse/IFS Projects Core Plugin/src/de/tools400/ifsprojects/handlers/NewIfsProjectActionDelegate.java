/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.handlers;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.rse.subsystems.files.core.subsystems.IRemoteFile;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import de.tools400.ifsprojects.project.wizards.NewIfsProjectWizard;
import de.tools400.ifsprojects.rse.RseSelections;

/**
 * Action 'New > IFS Project...' of IFS directories. It is contributed with
 * org.eclipse.ui.popupMenus, because only that extension point can add actions
 * to the 'New' menu of the Remote Systems view.
 */
public class NewIfsProjectActionDelegate implements IObjectActionDelegate {

    private Shell shell;
    private IRemoteFile directory;

    public void setActivePart(IAction anAction, IWorkbenchPart aTargetPart) {
        shell = aTargetPart.getSite().getShell();
    }

    public void selectionChanged(IAction anAction, ISelection aSelection) {

        Object element = RseSelections.getSingleElement(aSelection);
        if (element instanceof IRemoteFile && ((IRemoteFile)element).isDirectory()) {
            directory = (IRemoteFile)element;
        } else {
            directory = null;
        }
    }

    public void run(IAction anAction) {

        if (directory == null || shell == null) {
            return;
        }

        WizardDialog dialog = new WizardDialog(shell, new NewIfsProjectWizard(directory.getHost(), directory.getAbsolutePath()));
        dialog.open();
    }
}
