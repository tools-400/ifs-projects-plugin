/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.project.propertypages;

import java.util.Map;

import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.rse.core.filters.ISystemFilter;
import org.eclipse.rse.core.filters.ISystemFilterReference;
import org.eclipse.rse.core.model.IHost;

import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.project.IProjConfig;
import de.tools400.ifsprojects.project.IProjFileStore;
import de.tools400.ifsprojects.rse.RseSelections;
import de.tools400.ifsprojects.subsystem.IfsProjectEnvironment;

/**
 * Property page of the environment variables of an IFS project. The variables
 * are stored in the IFS project filter and are exported before the build and
 * compile commands are executed, see {@link AbstractEnvironmentPropertyPage}.
 */
public class IfsProjectEnvironmentPropertyPage extends AbstractEnvironmentPropertyPage {

    private ISystemFilter filter;
    private IHost host;
    private String directory;

    public IfsProjectEnvironmentPropertyPage() {
        super(Messages.Description_Environment_page, Messages.Tooltip_Environment_variables);
    }

    @Override
    protected boolean initialize() {

        ISystemFilterReference filterReference = RseSelections.getFilterReference(getElement());
        host = RseSelections.getHost(filterReference);
        directory = RseSelections.getDirectory(filterReference);
        filter = filterReference == null ? null : filterReference.getReferencedFilter();

        return filter != null && host != null && directory != null;
    }

    @Override
    protected String getProjectName() {
        return filter.getName();
    }

    @Override
    protected Map<String, String> loadVariables() {
        return IfsProjectEnvironment.getVariables(filter);
    }

    @Override
    protected void saveVariables(Map<String, String> aVariables) throws Exception {
        IfsProjectEnvironment.setVariables(filter, aVariables);
    }

    @Override
    protected IProjConfig readConfig() throws Exception {
        return IProjConfig.parse(IProjFileStore.read(new ProgressMonitorDialog(getShell()), host, directory).getContent());
    }
}
