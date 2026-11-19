/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.decorators;

import java.util.List;

import de.tools400.ifsprojects.ibmi.IfsPaths;

/**
 * Suffixes of the labels of IFS projects, see {@link IfsProjectDecorator}.
 */
public final class IfsProjectDecorations {

    private static final String SUFFIX_START = " ["; //$NON-NLS-1$
    private static final String SUFFIX_END = "]"; //$NON-NLS-1$
    private static final String NAME_SEPARATOR = ", "; //$NON-NLS-1$

    private IfsProjectDecorations() {
        // Utility class
    }

    /**
     * Returns the suffix of a local IFS project and of its directory in the
     * subsystem 'Local Files' of RSE: the RSE connection, on which the project
     * is built.
     *
     * @param aConnectionName - name of the RSE connection of the project
     * @return suffix, e.g. <code> [GFD400]</code>
     */
    public static String getLocalSuffix(String aConnectionName) {
        return SUFFIX_START + aConnectionName + SUFFIX_END;
    }

    /**
     * Returns the suffix of an IFS directory, that is the directory of IFS
     * projects: the names of the projects, unless each project has the name of
     * the directory.
     *
     * @param aDirectory - absolute IFS directory, e.g.
     *        <code>/home/jsmith/peek400</code>
     * @param aProjectNames - names of the projects of the directory
     * @return suffix, e.g. <code> [Peek/400]</code>, or <code>null</code>, if
     *         there is no project or if the names of all projects are equal to
     *         the name of the directory, ignoring the case
     */
    public static String getRemoteSuffix(String aDirectory, List<String> aProjectNames) {

        if (aProjectNames == null || aProjectNames.isEmpty()) {
            return null;
        }

        String directoryName = IfsPaths.getLastSegment(aDirectory);
        if (hasOnlyName(aProjectNames, directoryName)) {
            return null;
        }

        StringBuilder buffer = new StringBuilder(SUFFIX_START);
        for (String projectName : aProjectNames) {
            if (buffer.length() > SUFFIX_START.length()) {
                buffer.append(NAME_SEPARATOR);
            }
            buffer.append(projectName);
        }
        buffer.append(SUFFIX_END);

        return buffer.toString();
    }

    private static boolean hasOnlyName(List<String> aNames, String aName) {

        if (aName == null) {
            return false;
        }

        for (String name : aNames) {
            if (!aName.equalsIgnoreCase(name)) {
                return false;
            }
        }

        return true;
    }
}
