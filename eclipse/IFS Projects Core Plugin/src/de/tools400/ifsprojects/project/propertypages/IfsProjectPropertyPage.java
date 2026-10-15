/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.project.propertypages;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.rse.core.filters.ISystemFilterReference;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.swt.widgets.Composite;

import de.tools400.ifsprojects.IfsProjectsPlugin;
import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.project.IProjFileContent;
import de.tools400.ifsprojects.project.IProjFileException;
import de.tools400.ifsprojects.project.IProjFileStore;
import de.tools400.ifsprojects.rse.RseSelections;

/**
 * Property page of IFS projects. Edits the <code>iproj.json</code> file of the
 * project directory in the IFS, see {@link AbstractIProjPropertyPage}.
 */
public class IfsProjectPropertyPage extends AbstractIProjPropertyPage {

    private ISystemFilterReference filterReference;
    private IHost host;
    private String directory;

    @Override
    protected boolean initialize() {

        filterReference = getFilterReference();
        host = RseSelections.getHost(filterReference);
        directory = RseSelections.getDirectory(filterReference);

        return filterReference != null && host != null && directory != null;
    }

    @Override
    protected void createHeader(Composite aParent) {
        createInfo(aParent, Messages.Label_Project_name_colon, filterReference.getReferencedFilter().getName());
        createInfo(aParent, Messages.Label_Connection_colon, host.getAliasName());
        createInfo(aParent, Messages.Label_Directory_colon, directory);
    }

    @Override
    protected IProjFileContent readFile() throws IProjFileException {

        IProjFileStore.connect(host);

        final IProjFileContent[] result = new IProjFileContent[1];

        run(new IRunnableWithProgress() {
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

        return result[0];
    }

    @Override
    protected void writeFile(final String aContent, final IProjFileContent aPrevious) throws IProjFileException {

        run(new IRunnableWithProgress() {
            public void run(IProgressMonitor aMonitor) throws InvocationTargetException, InterruptedException {
                aMonitor.beginTask(Messages.bind(Messages.Task_Writing_A, directory), IProgressMonitor.UNKNOWN);
                try {
                    IProjFileStore.write(host, directory, aContent, aPrevious);
                } catch (IProjFileException e) {
                    throw new InvocationTargetException(e);
                } finally {
                    aMonitor.done();
                }
            }
        });
    }

    private void run(IRunnableWithProgress aRunnable) throws IProjFileException {

        try {
            new ProgressMonitorDialog(getShell()).run(true, false, aRunnable);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof IProjFileException) {
                throw (IProjFileException)e.getCause();
            }
            IfsProjectsPlugin.logError("Unexpected error accessing iproj.json: " + directory, e.getCause()); //$NON-NLS-1$
            throw new IProjFileException(Messages.bind(Messages.Error_Unexpected_error_A, e.getCause().getLocalizedMessage()), e.getCause());
        } catch (InterruptedException e) {
            throw new IProjFileException(e.getLocalizedMessage(), e);
        }
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
