/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.project.propertypages;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.rse.core.filters.ISystemFilter;
import org.eclipse.rse.core.filters.ISystemFilterReference;
import org.eclipse.rse.core.model.IHost;

import de.tools400.ifsprojects.IfsProjectsPlugin;
import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.project.IProjConfig;
import de.tools400.ifsprojects.project.IProjFileContent;
import de.tools400.ifsprojects.project.IProjFileException;
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

        ISystemFilterReference filterReference = getFilterReference();
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

        IProjFileStore.connect(host);
        IProjFileContent fileContent = read();

        return IProjConfig.parse(fileContent.getContent());
    }

    private IProjFileContent read() throws IProjFileException {

        final IProjFileContent[] result = new IProjFileContent[1];

        try {
            new ProgressMonitorDialog(getShell()).run(true, false, new IRunnableWithProgress() {
                public void run(IProgressMonitor aMonitor) throws InvocationTargetException, InterruptedException {
                    aMonitor.beginTask(Messages.bind(Messages.Task_Reading_A, directory), IProgressMonitor.UNKNOWN);
                    try {
                        result[0] = IProjFileStore.read(host, directory);
                    } catch (IProjFileException e) {
                        throw new InvocationTargetException(e);
                    } finally {
                        aMonitor.done();
                    }
                }
            });
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof IProjFileException) {
                throw (IProjFileException)e.getCause();
            }
            IfsProjectsPlugin.logError("Unexpected error reading iproj.json: " + directory, e.getCause()); //$NON-NLS-1$
            throw new IProjFileException(Messages.bind(Messages.Error_Unexpected_error_A, e.getCause().getLocalizedMessage()), e.getCause());
        } catch (InterruptedException e) {
            throw new IProjFileException(e.getLocalizedMessage(), e);
        }

        return result[0];
    }

    private ISystemFilterReference getFilterReference() {

        IAdaptable element = getElement();
        if (element instanceof ISystemFilterReference) {
            return (ISystemFilterReference)element;
        }

        if (element != null) {
            Object adapter = element.getAdapter(ISystemFilterReference.class);
            if (adapter instanceof ISystemFilterReference) {
                return (ISystemFilterReference)adapter;
            }
        }

        return null;
    }
}
