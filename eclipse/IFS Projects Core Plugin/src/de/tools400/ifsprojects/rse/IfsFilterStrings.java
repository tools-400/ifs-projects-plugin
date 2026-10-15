/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.rse;

/**
 * Converts between IFS directories and IFS filter strings, such as
 * <code>/home/jsmith/src/*</code>.
 */
public final class IfsFilterStrings {

    private static final char SEPARATOR = '/';
    private static final String ROOT = "/"; //$NON-NLS-1$
    private static final String ALL_FILES = "*"; //$NON-NLS-1$

    /** Switches of RSE remote file filter strings (no sub-directories, no files). */
    private static final String[] SWITCHES = { " /ns", " /nf" }; //$NON-NLS-1$ //$NON-NLS-2$

    private IfsFilterStrings() {
        // Utility class
    }

    /**
     * Returns the filter string that lists all files and directories of the
     * specified directory.
     *
     * @param aDirectory - absolute IFS directory
     * @return filter string, e.g. <code>/home/jsmith/src/*</code>
     */
    public static String toFilterString(String aDirectory) {

        String directory = normalizeDirectory(aDirectory);
        if (directory == null) {
            throw new IllegalArgumentException("Not an absolute IFS directory: " + aDirectory); //$NON-NLS-1$
        }

        if (ROOT.equals(directory)) {
            return ROOT + ALL_FILES;
        }

        return directory + SEPARATOR + ALL_FILES;
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
     * @param aFilterString - filter string, e.g. <code>/home/jsmith/*.rpgle</code>
     * @return directory, e.g. <code>/home/jsmith</code> or <code>null</code>, if
     *         the filter string does not start with a slash
     */
    public static String getDirectory(String aFilterString) {

        if (aFilterString == null) {
            return null;
        }

        String filterString = removeSwitches(aFilterString.trim());
        if (filterString.length() == 0 || filterString.charAt(0) != SEPARATOR) {
            return null;
        }

        int index = filterString.lastIndexOf(SEPARATOR);
        if (index == 0) {
            return ROOT;
        }

        return filterString.substring(0, index);
    }

    /**
     * Normalizes an absolute IFS directory: removes trailing slashes, except
     * for the root directory.
     *
     * @param aDirectory - directory
     * @return normalized directory or <code>null</code>, if the directory is
     *         empty, not absolute or contains wild cards
     */
    public static String normalizeDirectory(String aDirectory) {

        if (aDirectory == null) {
            return null;
        }

        String directory = aDirectory.trim();
        if (directory.length() == 0 || directory.charAt(0) != SEPARATOR || directory.indexOf('*') >= 0 || directory.indexOf('?') >= 0) {
            return null;
        }

        while (directory.length() > 1 && directory.charAt(directory.length() - 1) == SEPARATOR) {
            directory = directory.substring(0, directory.length() - 1);
        }

        return directory;
    }

    /**
     * Returns the last segment of a directory.
     *
     * @param aDirectory - directory, e.g. <code>/home/jsmith/src</code>
     * @return last segment, e.g. <code>src</code>, or <code>/</code> for the
     *         root directory
     */
    public static String getLastSegment(String aDirectory) {

        String directory = normalizeDirectory(aDirectory);
        if (directory == null || ROOT.equals(directory)) {
            return directory;
        }

        return directory.substring(directory.lastIndexOf(SEPARATOR) + 1);
    }

    private static String removeSwitches(String aFilterString) {

        String filterString = aFilterString;
        boolean found = true;
        while (found) {
            found = false;
            for (String option : SWITCHES) {
                if (filterString.toLowerCase().endsWith(option)) {
                    filterString = filterString.substring(0, filterString.length() - option.length()).trim();
                    found = true;
                }
            }
        }

        return filterString;
    }
}
