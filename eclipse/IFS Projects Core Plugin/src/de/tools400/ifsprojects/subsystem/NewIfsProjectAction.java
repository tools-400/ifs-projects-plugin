/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.subsystem;

import org.eclipse.jface.wizard.IWizard;
import org.eclipse.rse.core.filters.ISystemFilterPoolReference;
import org.eclipse.rse.core.filters.ISystemFilterReference;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.core.subsystems.ISubSystem;
import org.eclipse.rse.ui.ISystemContextMenuConstants;
import org.eclipse.rse.ui.actions.SystemBaseWizardAction;
import org.eclipse.swt.widgets.Shell;

import de.tools400.ifsprojects.IfsProjectsPlugin;
import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.project.wizards.NewIfsProjectWizard;

/**
 * Action 'New > IFS Project...' of the 'IFS Projects' subsystem. Replaces the
 * generic 'New > Filter...' action of RSE.
 */
public class NewIfsProjectAction extends SystemBaseWizardAction {

    public NewIfsProjectAction(Shell aShell) {
        super(Messages.Label_IFS_project_dots, IfsProjectsPlugin.getImageDescriptor(IfsProjectsPlugin.IMAGE_IFS_PROJECT), aShell);
        setContextMenuGroup(ISystemContextMenuConstants.GROUP_NEW);
    }

    @Override
    public void run() {

        if (getHost() == null) {
            return;
        }

        super.run();
    }

    @Override
    protected IWizard createWizard() {
        return new NewIfsProjectWizard(getHost(), null);
    }

    private IHost getHost() {

        Object element = getFirstSelection();

        if (element instanceof ISubSystem) {
            return ((ISubSystem)element).getHost();
        }

        if (element instanceof ISystemFilterPoolReference) {
            Object provider = ((ISystemFilterPoolReference)element).getProvider();
            if (provider instanceof ISubSystem) {
                return ((ISubSystem)provider).getHost();
            }
        }

        if (element instanceof ISystemFilterReference) {
            ISubSystem subSystem = ((ISystemFilterReference)element).getSubSystem();
            if (subSystem != null) {
                return subSystem.getHost();
            }
        }

        return null;
    }
}
