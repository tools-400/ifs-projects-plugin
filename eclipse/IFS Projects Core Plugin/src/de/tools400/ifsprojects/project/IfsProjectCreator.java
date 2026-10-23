/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.project;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.rse.core.filters.ISystemFilter;
import org.eclipse.rse.core.model.IHost;

import de.tools400.ifsprojects.IfsProjectsPlugin;
import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.utils.ExceptionUtils;

/**
 * Creates an IFS project: optionally the project directory and its
 * <code>iproj.json</code> file, then the filter of the 'IFS Projects'
 * subsystem. An existing <code>iproj.json</code> file is never replaced.
 */
public final class IfsProjectCreator {

    private IfsProjectCreator() {
        // Utility class
    }

    /**
     * Creates an IFS project. Must be called on the UI thread, because the
     * filter is created there. The directory is prepared in the runnable
     * context.
     *
     * @param aContext - context, in which the directory is prepared, e.g. the
     *        container of a wizard
     * @param aHost - RSE connection
     * @param aName - project name
     * @param aDirectory - project directory
     * @param anIsCreateDirectory - <code>true</code>, if a missing directory
     *        shall be created
     * @param aConfig - content of a new <code>iproj.json</code> file or
     *        <code>null</code>, if no file shall be created
     * @return filter of the project
     * @throws CoreException if the project could not be created, with a
     *         message for the user; the error has been logged
     * @throws InterruptedException if the user canceled the operation
     */
    public static ISystemFilter create(IRunnableContext aContext, final IHost aHost, final String aName, final String aDirectory,
        final boolean anIsCreateDirectory, final IProjConfig aConfig) throws CoreException, InterruptedException {

        try {
            IProjFileStore.connect(aHost);
        } catch (IProjFileException e) {
            throw newCoreException(ExceptionUtils.getLocalizedMessage(e), null);
        }

        try {
            aContext.run(true, false, new IRunnableWithProgress() {
                public void run(IProgressMonitor aMonitor) throws InvocationTargetException, InterruptedException {
                    aMonitor.beginTask(Messages.bind(Messages.Task_Creating_IFS_project_A, aName), IProgressMonitor.UNKNOWN);
                    try {
                        prepareDirectory(aHost, aDirectory, anIsCreateDirectory, aConfig);
                    } catch (IProjFileException e) {
                        throw new InvocationTargetException(e);
                    } finally {
                        aMonitor.done();
                    }
                }
            });
        } catch (InvocationTargetException e) {
            Throwable cause = ExceptionUtils.getCause(e);
            IfsProjectsPlugin.logError("Could not prepare directory of IFS project: " + aDirectory, //$NON-NLS-1$
                ExceptionUtils.getCause(cause));
            throw newCoreException(ExceptionUtils.getLocalizedMessage(cause), cause);
        }

        try {
            return IfsProjectFilters.create(aHost, aName, aDirectory);
        } catch (Exception e) {
            IfsProjectsPlugin.logError("Could not create IFS project filter: " + aName, e); //$NON-NLS-1$
            throw newCoreException(Messages.bind(Messages.Error_Could_not_create_project_filter_A, ExceptionUtils.getLocalizedMessage(e)), e);
        }
    }

    private static void prepareDirectory(IHost aHost, String aDirectory, boolean anIsCreateDirectory, IProjConfig aConfig)
        throws IProjFileException {

        if (!IProjFileStore.existsDirectory(aHost, aDirectory)) {
            if (!anIsCreateDirectory) {
                throw new IProjFileException(Messages.bind(Messages.Error_Directory_A_does_not_exist, aDirectory), null);
            }
            IProjFileStore.createDirectory(aHost, aDirectory);
        }

        if (aConfig != null) {
            IProjFileContent fileContent = IProjFileStore.read(aHost, aDirectory);
            if (!fileContent.exists()) {
                IProjFileStore.write(aHost, aDirectory, aConfig.toJson(), fileContent);
            }
        }
    }

    private static CoreException newCoreException(String aMessage, Throwable aCause) {
        return new CoreException(new Status(IStatus.ERROR, IfsProjectsPlugin.PLUGIN_ID, aMessage, aCause));
    }
}
