/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.project;

import org.eclipse.rse.core.filters.ISystemFilter;
import org.eclipse.rse.core.filters.ISystemFilterPool;
import org.eclipse.rse.core.model.IHost;

import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.rse.IfsFilterStrings;
import de.tools400.ifsprojects.subsystem.IfsProjectSubSystem;
import de.tools400.ifsprojects.subsystem.IfsProjects;

/**
 * Creates the RSE filters of IFS projects in the 'IFS Projects' subsystem.
 * Must be called on the UI thread.
 */
public final class IfsProjectFilters {

    private IfsProjectFilters() {
        // Utility class
    }

    /**
     * Returns <code>true</code>, if a project with the specified name exists
     * in the filter pool that is used for new IFS projects.
     *
     * @param aHost - RSE connection
     * @param aName - project name
     * @return <code>true</code>, if the project exists
     */
    public static boolean exists(IHost aHost, String aName) {

        ISystemFilterPool filterPool = getFilterPool(aHost, false);
        if (filterPool == null) {
            return false;
        }

        return filterPool.getSystemFilter(aName) != null;
    }

    /**
     * Creates the filter of an IFS project in the connection private filter
     * pool of the 'IFS Projects' subsystem.
     *
     * @param aHost - RSE connection
     * @param aName - project name
     * @param aDirectory - project directory
     * @return new filter
     * @throws Exception if the filter could not be created
     */
    public static ISystemFilter create(IHost aHost, String aName, String aDirectory) throws Exception {

        ISystemFilterPool filterPool = getFilterPool(aHost, true);
        if (filterPool == null) {
            throw new IllegalStateException(Messages.bind(Messages.Error_IFS_projects_subsystem_not_found_A, aHost.getAliasName()));
        }

        // The filter string must end with '/*', because RSE uses its last
        // part as name filter, when sub-directories are expanded.
        String[] filterStrings = new String[] { IfsFilterStrings.toFilterString(aDirectory) };

        return filterPool.getSystemFilterPoolManager().createSystemFilter(filterPool, aName, filterStrings);
    }

    private static ISystemFilterPool getFilterPool(IHost aHost, boolean aCreate) {

        IfsProjectSubSystem subSystem = IfsProjects.getProjectSubSystem(aHost);
        if (subSystem == null) {
            return null;
        }

        // Connection private filter pool, e.g. 'CN-MYIBMI-...subsystem'.
        return subSystem.getUniqueOwningSystemFilterPool(aCreate);
    }
}
