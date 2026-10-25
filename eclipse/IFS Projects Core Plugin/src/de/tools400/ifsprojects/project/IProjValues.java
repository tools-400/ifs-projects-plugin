/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.project;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import de.tools400.ifsprojects.ibmi.ClCommands;
import de.tools400.ifsprojects.ibmi.IfsPaths;
import de.tools400.ifsprojects.utils.StringUtils;

/**
 * Values of the attributes of <code>iproj.json</code>, in which the variables
 * (<code>&amp;NAME</code>) are replaced by the environment variables of the
 * project, see {@link EnvironmentVariables#resolveReferences(String, Map)}.
 */
public final class IProjValues {

    private static final String WHITE_SPACE_PATTERN = "\\s+"; //$NON-NLS-1$
    private static final char SEPARATOR = '/';

    private IProjValues() {
        // Utility class
    }

    /**
     * Returns the values of an attribute, that is a list of strings or a
     * string. Empty values are removed.
     *
     * @param aConfig - content of <code>iproj.json</code>
     * @param aKey - attribute name, e.g. {@link IProjConfig#SET_IBMI_ENV_CMD}
     * @param aVariables - environment variables of the project
     * @return trimmed values
     */
    public static List<String> getValues(IProjConfig aConfig, String aKey, Map<String, String> aVariables) {

        List<String> values = new ArrayList<>();
        for (String rawValue : aConfig.getStrings(aKey)) {
            String value = StringUtils.trimToNull(EnvironmentVariables.resolveReferences(rawValue, aVariables));
            if (value != null) {
                values.add(value);
            }
        }

        return values;
    }

    /**
     * Returns the libraries of an attribute. A value may contain several
     * libraries separated by blanks. Duplicates are removed.
     *
     * @param aConfig - content of <code>iproj.json</code>
     * @param aKey - attribute name, e.g. {@link IProjConfig#PRE_USRLIBL}
     * @param aVariables - environment variables of the project
     * @return libraries in the order of the attribute
     */
    public static List<String> getLibraries(IProjConfig aConfig, String aKey, Map<String, String> aVariables) {

        List<String> libraries = new ArrayList<>();
        Set<String> uniqueLibraries = new HashSet<>();
        for (String value : getValues(aConfig, aKey, aVariables)) {
            for (String library : value.split(WHITE_SPACE_PATTERN)) {
                if (library.length() > 0 && uniqueLibraries.add(library.toUpperCase(Locale.ROOT))) {
                    libraries.add(library);
                }
            }
        }

        return libraries;
    }

    /**
     * Returns the library, whose events file (EVFEVENT) contains the events of
     * the objects created by a build: <code>objlib</code> or, without
     * <code>objlib</code>, <code>curlib</code>.
     *
     * @param aConfig - content of <code>iproj.json</code>
     * @param aVariables - environment variables of the project
     * @return library or <code>null</code>, if the library is unknown
     */
    public static String getObjectLibrary(IProjConfig aConfig, Map<String, String> aVariables) {

        String library = getLibrary(aConfig, IProjConfig.OBJLIB, aVariables);
        if (library == null) {
            library = getLibrary(aConfig, IProjConfig.CURLIB, aVariables);
        }

        return library;
    }

    /**
     * Returns the absolute include directories of <code>includePath</code>.
     * Relative directories are relative to the project directory.
     *
     * @param aProjectDirectory - absolute project directory
     * @param aConfig - content of <code>iproj.json</code>
     * @param aVariables - environment variables of the project
     * @return directories
     */
    public static List<String> getIncludeDirectories(String aProjectDirectory, IProjConfig aConfig, Map<String, String> aVariables) {

        List<String> directories = new ArrayList<>();
        for (String directory : getValues(aConfig, IProjConfig.INCLUDE_PATH, aVariables)) {
            if (directory.charAt(0) == SEPARATOR) {
                directories.add(directory);
            } else {
                directories.add(IfsPaths.append(aProjectDirectory, directory));
            }
        }

        return directories;
    }

    private static String getLibrary(IProjConfig aConfig, String aKey, Map<String, String> aVariables) {

        String value = EnvironmentVariables.resolveReferences(aConfig.getString(aKey), aVariables);
        if (value == null) {
            return null;
        }

        String library = value.trim().toUpperCase(Locale.ROOT);
        if (!ClCommands.isObjectName(library)) {
            return null;
        }

        return library;
    }
}
