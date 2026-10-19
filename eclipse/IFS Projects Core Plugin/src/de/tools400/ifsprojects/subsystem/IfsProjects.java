/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.subsystem;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rse.core.RSECorePlugin;
import org.eclipse.rse.core.filters.ISystemFilter;
import org.eclipse.rse.core.filters.ISystemFilterPool;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.core.subsystems.ISubSystem;
import org.eclipse.rse.core.subsystems.ISubSystemConfiguration;

import com.ibm.etools.iseries.subsystems.ifs.files.IFSFileSubSystemConfiguration;

import de.tools400.ifsprojects.ibmi.IfsPaths;
import de.tools400.ifsprojects.rse.IfsFilterStrings;

/**
 * Provides access to the IFS projects of IBM i connections. Only uses the
 * local RSE model, the remote system is not accessed.
 */
public final class IfsProjects {

    private static final char SEPARATOR = '/';

    private IfsProjects() {
        // Utility class
    }

    /**
     * Returns the 'IFS Projects' subsystem of a connection.
     *
     * @param aHost - RSE connection
     * @return subsystem or <code>null</code>
     */
    public static IfsProjectSubSystem getProjectSubSystem(IHost aHost) {

        ISubSystem subSystem = getSubSystem(aHost, IfsProjectSubSystemConfiguration.ID);
        if (subSystem instanceof IfsProjectSubSystem) {
            return (IfsProjectSubSystem)subSystem;
        }

        return null;
    }

    /**
     * Returns IBM's IFS subsystem of a connection.
     *
     * @param aHost - RSE connection
     * @return subsystem or <code>null</code>
     */
    public static ISubSystem getIfsFileSubSystem(IHost aHost) {
        return getSubSystem(aHost, IFSFileSubSystemConfiguration.CONFIGURATION_ID);
    }

    /**
     * Returns the IFS projects of a connection.
     *
     * @param aHost - RSE connection
     * @return projects
     */
    public static List<IfsProject> getProjects(IHost aHost) {

        List<IfsProject> projects = new ArrayList<IfsProject>();

        IfsProjectSubSystem subSystem = getProjectSubSystem(aHost);
        if (subSystem == null) {
            return projects;
        }

        ISystemFilterPool[] filterPools = subSystem.getFilterPoolReferenceManager().getReferencedSystemFilterPools();
        for (ISystemFilterPool filterPool : filterPools) {
            for (ISystemFilter filter : filterPool.getSystemFilters()) {
                String directory = IfsFilterStrings.getDirectory(filter.getFilterStrings());
                if (directory != null) {
                    projects.add(new IfsProject(subSystem, filter, directory));
                }
            }
        }

        return projects;
    }

    /**
     * Returns the project of an IFS object. If projects are nested, the
     * innermost project is returned.
     *
     * @param aHost - RSE connection
     * @param anAbsolutePath - absolute path of a file or directory
     * @return project or <code>null</code>, if the object is not part of a
     *         project
     */
    public static IfsProject findProject(IHost aHost, String anAbsolutePath) {

        String path = IfsPaths.normalizeDirectory(anAbsolutePath);
        if (path == null) {
            return null;
        }

        IfsProject project = null;
        for (IfsProject candidate : getProjects(aHost)) {
            if (isInDirectory(path, candidate.getDirectory())) {
                if (project == null || candidate.getDirectory().length() > project.getDirectory().length()) {
                    project = candidate;
                }
            }
        }

        return project;
    }

    /**
     * Returns the projects whose directory is the specified directory.
     *
     * @param aHost - RSE connection
     * @param aDirectory - absolute directory
     * @return projects
     */
    public static List<IfsProject> getProjectsOfDirectory(IHost aHost, String aDirectory) {

        List<IfsProject> projects = new ArrayList<IfsProject>();

        String directory = IfsPaths.normalizeDirectory(aDirectory);
        if (directory == null) {
            return projects;
        }

        for (IfsProject project : getProjects(aHost)) {
            if (project.getDirectory().equals(directory)) {
                projects.add(project);
            }
        }

        return projects;
    }

    private static boolean isInDirectory(String aPath, String aDirectory) {

        if (String.valueOf(SEPARATOR).equals(aDirectory)) {
            return true;
        }

        return aPath.equals(aDirectory) || aPath.startsWith(aDirectory + SEPARATOR);
    }

    private static ISubSystem getSubSystem(IHost aHost, String aConfigurationId) {

        if (aHost == null) {
            return null;
        }

        ISubSystemConfiguration configuration = RSECorePlugin.getTheSystemRegistry().getSubSystemConfiguration(aConfigurationId);
        if (configuration == null) {
            return null;
        }

        ISubSystem[] subSystems = configuration.getSubSystems(aHost, true);
        if (subSystems == null || subSystems.length == 0) {
            return null;
        }

        return subSystems[0];
    }
}
