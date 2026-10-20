/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.ibmi;

/**
 * Paths of the IFS, separated by slashes.
 */
public final class IfsPaths {

    private static final char SEPARATOR = '/';
    private static final String ROOT = "/"; //$NON-NLS-1$
    private static final String PARENT = ".."; //$NON-NLS-1$

    private IfsPaths() {
        // Utility class
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

    /**
     * Returns the parent directory of an absolute path.
     *
     * @param aPath - absolute path, e.g. <code>/home/jsmith/src</code>
     * @return parent directory, e.g. <code>/home/jsmith</code>, or
     *         <code>null</code> for the root directory and for paths, that
     *         are not absolute, see {@link #normalizeDirectory(String)}
     */
    public static String getParent(String aPath) {

        String path = normalizeDirectory(aPath);
        if (path == null || ROOT.equals(path)) {
            return null;
        }

        int index = path.lastIndexOf(SEPARATOR);
        if (index == 0) {
            return ROOT;
        }

        return path.substring(0, index);
    }

    /**
     * Returns the name of a file: the part of the path after the last slash.
     *
     * @param aPath - path, e.g. <code>/home/jsmith/src/hello.rpgle</code>
     * @return file name, e.g. <code>hello.rpgle</code>, or the path itself, if
     *         it has no slash
     */
    public static String getFileName(String aPath) {
        return aPath.substring(aPath.lastIndexOf(SEPARATOR) + 1);
    }

    /**
     * Appends a relative path to a directory.
     *
     * @param aDirectory - directory, with or without trailing slash
     * @param aRelativePath - relative path, e.g. <code>iproj.json</code>
     * @return path, e.g. <code>/home/jsmith/src/iproj.json</code>
     */
    public static String append(String aDirectory, String aRelativePath) {

        if (aDirectory.length() > 0 && aDirectory.charAt(aDirectory.length() - 1) == SEPARATOR) {
            return aDirectory + aRelativePath;
        }

        return aDirectory + SEPARATOR + aRelativePath;
    }

    /**
     * Returns the path of a file relative to the directory of another file.
     * Both paths are relative to the same directory, e.g. a project
     * directory.
     *
     * @param aFromFile - file, from whose directory the path starts, e.g.
     *        <code>qclsrc/start.clle</code>
     * @param aToFile - file, to which the path leads, e.g.
     *        <code>qcpysrc/vars.clleinc</code>
     * @return relative path, e.g. <code>../qcpysrc/vars.clleinc</code>
     */
    public static String getRelativePath(String aFromFile, String aToFile) {

        String[] from = aFromFile.split(String.valueOf(SEPARATOR));
        String[] to = aToFile.split(String.valueOf(SEPARATOR));

        int common = 0;
        while (common < from.length - 1 && common < to.length - 1 && from[common].equals(to[common])) {
            common++;
        }

        StringBuilder path = new StringBuilder();
        for (int i = common; i < from.length - 1; i++) {
            path.append(PARENT).append(SEPARATOR);
        }
        for (int i = common; i < to.length; i++) {
            if (i > common) {
                path.append(SEPARATOR);
            }
            path.append(to[i]);
        }

        return path.toString();
    }
}
