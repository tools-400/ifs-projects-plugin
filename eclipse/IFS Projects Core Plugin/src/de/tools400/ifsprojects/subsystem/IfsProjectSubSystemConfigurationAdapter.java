/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.subsystem;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.rse.core.filters.ISystemFilter;
import org.eclipse.rse.core.filters.ISystemFilterPool;
import org.eclipse.rse.core.subsystems.ISubSystemConfiguration;
import org.eclipse.rse.ui.view.SubSystemConfigurationAdapter;
import org.eclipse.swt.widgets.Shell;

import de.tools400.ifsprojects.IfsProjectsPlugin;

/**
 * UI adapter of the 'IFS Projects' subsystem: project icon for the filters
 * and the 'New > IFS Project' action instead of 'New > Filter'.
 * <p>
 * Do not override getPasswordValidator() and launchCommand(), because their
 * signatures differ between RDi 9.6 and 9.8.
 */
public class IfsProjectSubSystemConfigurationAdapter extends SubSystemConfigurationAdapter {

    @Override
    public ImageDescriptor getSystemFilterImage(ISystemFilter aFilter) {
        return IfsProjectsPlugin.getImageDescriptor(IfsProjectsPlugin.IMAGE_IFS_PROJECT);
    }

    @Override
    protected IAction getNewFilterPoolFilterAction(ISubSystemConfiguration aConfiguration, ISystemFilterPool aFilterPool, Shell aShell) {
        return new NewIfsProjectAction(aShell);
    }
}
