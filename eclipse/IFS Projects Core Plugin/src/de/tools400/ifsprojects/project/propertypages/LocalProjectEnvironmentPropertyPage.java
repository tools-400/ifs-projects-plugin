/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.project.propertypages;

import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.local.LocalProject;
import de.tools400.ifsprojects.project.IProjConfig;

/**
 * Property page of the environment variables of a local IFS project. The
 * variables are stored in the workspace for the connection of the project,
 * see {@link LocalProject#getEnvironmentVariables(IProject, String)}, and are
 * exported before the build and compile commands are executed, see
 * {@link AbstractEnvironmentPropertyPage}.
 */
public class LocalProjectEnvironmentPropertyPage extends AbstractEnvironmentPropertyPage {

    private LocalProject localProject;

    public LocalProjectEnvironmentPropertyPage() {
        super(Messages.Description_Local_environment_page, Messages.Tooltip_Local_environment_variables);
    }

    @Override
    protected boolean initialize() {

        IAdaptable element = getElement();
        Object resource = element == null ? null : element.getAdapter(IResource.class);
        if (element instanceof IProject) {
            resource = element;
        }

        localProject = resource instanceof IProject ? LocalProject.get((IProject)resource) : null;

        return localProject != null;
    }

    @Override
    protected void createInfo(Composite aParent) {

        Label label = new Label(aParent, SWT.WRAP);
        label.setText(Messages.Label_Connection_colon + " " + localProject.getConnectionName()); //$NON-NLS-1$
        label.setLayoutData(createWrapGridData());
    }

    @Override
    protected String getProjectName() {
        return localProject.getName();
    }

    @Override
    protected Map<String, String> loadVariables() {
        return localProject.getEnvironmentVariables();
    }

    @Override
    protected void saveVariables(Map<String, String> aVariables) throws Exception {
        LocalProject.setEnvironmentVariables(localProject.getProject(), localProject.getConnectionName(), aVariables);
    }

    @Override
    protected IProjConfig readConfig() throws Exception {
        return localProject.readConfig();
    }
}
