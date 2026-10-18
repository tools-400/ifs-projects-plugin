/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.export;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Finds the file of a project, that an include directive of a stream file
 * refers to, in the same way as the compilers do. Verified on the IBM i:
 * <ul>
 * <li>RPG (<code>/COPY</code>, <code>/INCLUDE</code>): the current directory,
 * which is the project directory for TOBi, the include directories
 * (<code>INCDIR</code>) and the directory of the including file. In each
 * directory, the name is tried as it is and with the extensions
 * <code>.rpgleinc</code> and <code>.rpgle</code>.</li>
 * <li>CL (<code>INCLUDE SRCSTMF()</code>): the directory of the including file
 * and the include directories. The current directory is not searched.</li>
 * </ul>
 * Absolute paths are found, if they are in the project directory. Like the
 * IFS, the search ignores the case.
 */
public final class IncludeResolver {

    private static final String[] RPG_EXTENSIONS = { "", ".rpgleinc", ".rpgle" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    private static final String[] NO_EXTENSIONS = { "" }; //$NON-NLS-1$

    private static final String ROOT = ""; //$NON-NLS-1$
    private static final String CURRENT = "."; //$NON-NLS-1$
    private static final String PARENT = ".."; //$NON-NLS-1$
    private static final char SEPARATOR = '/';

    private final String projectDirectory;
    private final List<String> includeDirectories;
    private final Map<String, String> files;

    /**
     * Creates a resolver.
     *
     * @param aProjectDirectory - absolute path of the project directory in the
     *        IFS, in which the project is compiled, or <code>null</code>, if
     *        absolute paths are not resolved
     * @param anIncludeDirectories - include directories of
     *        <code>iproj.json</code>, absolute or relative to the project
     *        directory; directories outside of the project are ignored
     * @param aProjectFiles - paths of the files of the project relative to the
     *        project directory
     */
    public IncludeResolver(String aProjectDirectory, List<String> anIncludeDirectories, Collection<String> aProjectFiles) {

        this.projectDirectory = aProjectDirectory == null ? null : normalize(null, aProjectDirectory);

        this.includeDirectories = new ArrayList<String>();
        if (anIncludeDirectories != null) {
            for (String directory : anIncludeDirectories) {
                String relativeDirectory = toRelativePath(directory);
                if (relativeDirectory != null) {
                    includeDirectories.add(relativeDirectory);
                }
            }
        }

        this.files = new HashMap<String, String>();
        for (String path : aProjectFiles) {
            files.put(path.toLowerCase(Locale.ROOT), path);
        }
    }

    /**
     * Returns the file of a <code>/COPY</code> or <code>/INCLUDE</code>
     * directive of RPG.
     *
     * @param anIncludingFile - path of the including file relative to the
     *        project directory
     * @param aName - path of the directive, e.g. <code>inc/consts.rpgleinc</code>
     * @return path of the file relative to the project directory, as passed to
     *         the constructor, or <code>null</code>, if it is not a file of the
     *         project
     */
    public String resolveRpgInclude(String anIncludingFile, String aName) {

        List<String> directories = new ArrayList<String>();
        directories.add(ROOT);
        directories.addAll(includeDirectories);
        directories.add(getDirectory(anIncludingFile));

        return resolve(directories, aName, RPG_EXTENSIONS);
    }

    /**
     * Returns the file of an <code>INCLUDE SRCSTMF()</code> command of CL.
     *
     * @param anIncludingFile - path of the including file relative to the
     *        project directory
     * @param aName - path of the parameter <code>SRCSTMF</code>
     * @return path of the file relative to the project directory, as passed to
     *         the constructor, or <code>null</code>, if it is not a file of the
     *         project
     */
    public String resolveClInclude(String anIncludingFile, String aName) {

        List<String> directories = new ArrayList<String>();
        directories.add(getDirectory(anIncludingFile));
        directories.addAll(includeDirectories);

        return resolve(directories, aName, NO_EXTENSIONS);
    }

    private String resolve(List<String> aDirectories, String aName, String[] anExtensions) {

        if (aName == null || aName.trim().length() == 0) {
            return null;
        }

        String name = aName.trim();
        if (name.charAt(0) == SEPARATOR) {
            String path = toRelativePath(name);
            return path == null ? null : find(path, anExtensions);
        }

        for (String directory : aDirectories) {
            String path = normalize(directory, name);
            if (path != null) {
                String file = find(path, anExtensions);
                if (file != null) {
                    return file;
                }
            }
        }

        return null;
    }

    private String find(String aPath, String[] anExtensions) {

        for (String extension : anExtensions) {
            String file = files.get((aPath + extension).toLowerCase(Locale.ROOT));
            if (file != null) {
                return file;
            }
        }

        return null;
    }

    /**
     * Returns a path relative to the project directory.
     *
     * @param aPath - absolute path or path relative to the project directory
     * @return relative path, <code>""</code> for the project directory, or
     *         <code>null</code>, if it is outside of the project
     */
    private String toRelativePath(String aPath) {

        if (aPath == null || aPath.length() == 0) {
            return null;
        }

        if (aPath.charAt(0) != SEPARATOR) {
            return normalize(ROOT, aPath);
        }

        if (projectDirectory == null) {
            return null;
        }

        String path = normalize(null, aPath);
        if (path == null) {
            return null;
        }

        String prefix = projectDirectory;
        if (path.equalsIgnoreCase(prefix)) {
            return ROOT;
        }

        if (path.length() > prefix.length() && path.charAt(prefix.length()) == SEPARATOR
            && path.substring(0, prefix.length()).equalsIgnoreCase(prefix)) {
            return path.substring(prefix.length() + 1);
        }

        return null;
    }

    /**
     * Joins a directory and a path and resolves <code>.</code> and
     * <code>..</code>.
     *
     * @param aDirectory - directory relative to the project directory, or
     *        <code>null</code> for an absolute path
     * @param aPath - path relative to the directory, or an absolute path
     * @return joined path without leading and trailing slash, or
     *         <code>null</code>, if it leaves the root
     */
    private static String normalize(String aDirectory, String aPath) {

        List<String> segments = new ArrayList<String>();
        String path = aDirectory == null || aDirectory.length() == 0 ? aPath : aDirectory + SEPARATOR + aPath;

        for (String segment : path.split(String.valueOf(SEPARATOR))) {
            if (segment.length() == 0 || segment.equals(CURRENT)) {
                continue;
            }
            if (segment.equals(PARENT)) {
                if (segments.isEmpty()) {
                    return null;
                }
                segments.remove(segments.size() - 1);
            } else {
                segments.add(segment);
            }
        }

        StringBuilder buffer = new StringBuilder();
        for (String segment : segments) {
            if (buffer.length() > 0 || aDirectory == null) {
                buffer.append(SEPARATOR);
            }
            buffer.append(segment);
        }

        return buffer.toString();
    }

    private static String getDirectory(String aPath) {

        int index = aPath.lastIndexOf(SEPARATOR);
        if (index < 0) {
            return ROOT;
        }

        return aPath.substring(0, index);
    }
}
