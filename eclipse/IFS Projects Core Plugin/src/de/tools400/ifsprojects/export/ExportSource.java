/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.export;

import java.util.List;
import java.util.Map;

import org.eclipse.rse.core.model.IHost;

import com.ibm.as400.access.AS400;

import de.tools400.ifsprojects.project.IProjConfig;

/**
 * Project, that is exported: an IFS project or a local IFS project. The
 * methods with a system object access the remote system and must not be called
 * on the UI thread.
 */
public interface ExportSource {

    /**
     * Returns the name of the project.
     */
    String getName();

    /**
     * Returns the RSE connection of the export.
     */
    IHost getHost();

    /**
     * Returns the content of <code>iproj.json</code>.
     *
     * @return configuration, empty if the file does not exist
     */
    IProjConfig getConfig();

    /**
     * Returns the environment variables of the project, including the default
     * environment variables.
     */
    Map<String, String> getVariables();

    /**
     * Returns the directory in the IFS, in which the project is compiled.
     * Absolute include paths and the include directories of
     * <code>iproj.json</code> are resolved with it.
     *
     * @param aSystem - system object
     * @return absolute directory
     * @throws Exception if the directory could not be determined
     */
    String getProjectDirectory(AS400 aSystem) throws Exception;

    /**
     * Returns the files of the project, except of the files, that are never
     * uploaded or are excluded by the ignore file.
     *
     * @param aSystem - system object
     * @return paths relative to the project directory, separated by slashes
     * @throws Exception if the files could not be listed
     */
    List<String> listFiles(AS400 aSystem) throws Exception;

    /**
     * Reads a text file of the project.
     *
     * @param aSystem - system object
     * @param aPath - path relative to the project directory
     * @return content
     * @throws Exception if the file could not be read
     */
    String readFile(AS400 aSystem, String aPath) throws Exception;

    /**
     * Returns the settings of the last export of the project.
     *
     * @param aDefaults - settings, if the project has not been exported before
     * @return settings
     */
    ExportSettings loadSettings(ExportSettings aDefaults);

    /**
     * Saves the settings of an export.
     *
     * @param aSettings - settings
     * @throws Exception if the settings could not be saved
     */
    void saveSettings(ExportSettings aSettings) throws Exception;
}
