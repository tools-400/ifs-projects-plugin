/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.rse;

/**
 * Reads the directory of the filter strings of the subsystem 'Local Files' of
 * RSE, such as <code>C:\git\project\*</code>.
 */
public final class LocalFilterStrings {

    /**
     * ID of the subsystem configuration 'Local Files' of RSE.
     */
    public static final String SUBSYSTEM_CONFIGURATION_ID = "local.files"; //$NON-NLS-1$

    private static final char BACKSLASH = '\\';
    private static final char SLASH = '/';
    private static final char DRIVE_SEPARATOR = ':';

    private LocalFilterStrings() {
        // Utility class
    }

    /**
     * Returns the directory of a filter, if the filter has exactly one filter
     * string.
     *
     * @param aFilterStrings - filter strings of a filter
     * @return directory or <code>null</code>
     */
    public static String getDirectory(String[] aFilterStrings) {

        if (aFilterStrings == null || aFilterStrings.length != 1) {
            return null;
        }

        return getDirectory(aFilterStrings[0]);
    }

    /**
     * Returns the directory part of a filter string.
     *
     * @param aFilterString - filter string, e.g. <code>C:\git\*.rpgle</code>
     * @return directory, e.g. <code>C:\git</code>, or <code>null</code>, if the
     *         filter string has no directory, like the filter string
     *         <code>*</code> of the filter 'Drives'
     */
    public static String getDirectory(String aFilterString) {

        if (aFilterString == null) {
            return null;
        }

        String filterString = IfsFilterStrings.removeSwitches(aFilterString.trim());

        int index = Math.max(filterString.lastIndexOf(BACKSLASH), filterString.lastIndexOf(SLASH));
        if (index < 0) {
            return null;
        }

        String directory = filterString.substring(0, index);
        if (directory.length() == 0 || directory.charAt(directory.length() - 1) == DRIVE_SEPARATOR) {
            // Root directory, e.g. 'C:\' or '/'
            return filterString.substring(0, index + 1);
        }

        return directory;
    }
}
