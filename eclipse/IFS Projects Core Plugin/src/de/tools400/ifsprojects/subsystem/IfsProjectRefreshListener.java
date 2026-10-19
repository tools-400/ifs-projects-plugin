/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.subsystem;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.rse.core.RSECorePlugin;
import org.eclipse.rse.core.events.ISystemRemoteChangeEvent;
import org.eclipse.rse.core.events.ISystemRemoteChangeEvents;
import org.eclipse.rse.core.events.ISystemRemoteChangeListener;
import org.eclipse.rse.core.events.ISystemResourceChangeEvents;
import org.eclipse.rse.core.events.SystemResourceChangeEvent;
import org.eclipse.rse.core.filters.ISystemFilterReference;
import org.eclipse.rse.core.subsystems.ISubSystem;
import org.eclipse.rse.core.subsystems.ISubSystemConfiguration;
import org.eclipse.rse.subsystems.files.core.subsystems.IRemoteFile;
import org.eclipse.rse.ui.RSEUIPlugin;

import com.ibm.etools.iseries.subsystems.ifs.files.IFSFileSubSystemConfiguration;

import de.tools400.ifsprojects.IfsProjectsPlugin;
import de.tools400.ifsprojects.ibmi.IfsPaths;

/**
 * Refreshes IFS projects, when files or directories are created, deleted or
 * renamed directly in a project directory.
 * <p>
 * RSE does not refresh the project filters itself, because the remote change
 * events of IFS objects carry IBM's IFS subsystem, not the 'IFS Projects'
 * subsystem.
 */
public final class IfsProjectRefreshListener implements ISystemRemoteChangeListener {


    private static IfsProjectRefreshListener instance;

    private IfsProjectRefreshListener() {
    }

    /**
     * Registers the listener, if it has not yet been registered.
     */
    public static synchronized void ensureRegistered() {
        if (instance == null) {
            instance = new IfsProjectRefreshListener();
            RSECorePlugin.getTheSystemRegistry().addSystemRemoteChangeListener(instance);
        }
    }

    /**
     * Removes the listener. Called when the plug-in is stopped.
     */
    public static synchronized void unregister() {
        if (instance != null) {
            try {
                RSECorePlugin.getTheSystemRegistry().removeSystemRemoteChangeListener(instance);
            } catch (Throwable e) {
                // Ignore errors on shutdown
            }
            instance = null;
        }
    }

    public void systemRemoteResourceChanged(ISystemRemoteChangeEvent anEvent) {

        try {

            int eventType = anEvent.getEventType();
            if (eventType != ISystemRemoteChangeEvents.SYSTEM_REMOTE_RESOURCE_CREATED
                && eventType != ISystemRemoteChangeEvents.SYSTEM_REMOTE_RESOURCE_DELETED
                && eventType != ISystemRemoteChangeEvents.SYSTEM_REMOTE_RESOURCE_RENAMED) {
                return;
            }

            ISubSystem subSystem = anEvent.getSubSystem();
            if (!isIfsFileSubSystem(subSystem)) {
                return;
            }

            Set<String> parentDirectories = new HashSet<String>();
            addDirectory(anEvent.getResourceParent(), parentDirectories);
            addParentDirectories(anEvent.getResource(), parentDirectories);

            for (String parentDirectory : parentDirectories) {
                for (IfsProject project : IfsProjects.getProjectsOfDirectory(subSystem.getHost(), parentDirectory)) {
                    refresh(project);
                }
            }

        } catch (Throwable e) {
            IfsProjectsPlugin.logError("Could not refresh IFS project.", e); //$NON-NLS-1$
        }
    }

    private void refresh(IfsProject aProject) {

        ISystemFilterReference filterReference = aProject.getFilterReference();
        if (filterReference != null) {
            RSEUIPlugin.getTheSystemRegistryUI().postEvent(
                new SystemResourceChangeEvent(filterReference, ISystemResourceChangeEvents.EVENT_REFRESH, null));
        }
    }

    private boolean isIfsFileSubSystem(ISubSystem aSubSystem) {

        if (aSubSystem == null) {
            return false;
        }

        ISubSystemConfiguration configuration = aSubSystem.getSubSystemConfiguration();
        return configuration != null && IFSFileSubSystemConfiguration.CONFIGURATION_ID.equals(configuration.getId());
    }

    private void addDirectory(Object aDirectory, Set<String> aDirectories) {

        String path = null;
        if (aDirectory instanceof IRemoteFile) {
            path = ((IRemoteFile)aDirectory).getAbsolutePath();
        } else if (aDirectory instanceof String) {
            path = (String)aDirectory;
        }

        String directory = IfsPaths.normalizeDirectory(path);
        if (directory != null) {
            aDirectories.add(directory);
        }
    }

    private void addParentDirectories(Object aResource, Set<String> aDirectories) {

        if (aResource instanceof Object[]) {
            for (Object resource : (Object[])aResource) {
                addParentDirectories(resource, aDirectories);
            }
        } else if (aResource instanceof Collection) {
            for (Object resource : (Collection<?>)aResource) {
                addParentDirectories(resource, aDirectories);
            }
        } else if (aResource instanceof IRemoteFile) {
            addDirectory(((IRemoteFile)aResource).getParentPath(), aDirectories);
        } else if (aResource instanceof String) {
            String parentDirectory = IfsPaths.getParent((String)aResource);
            if (parentDirectory != null) {
                addDirectory(parentDirectory, aDirectories);
            }
        }
    }
}
