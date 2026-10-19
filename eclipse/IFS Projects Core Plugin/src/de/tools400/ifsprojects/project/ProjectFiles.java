/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.project;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Selection of the files of a project directory, that are uploaded to the
 * deploy directory of a local IFS project or exported to a library: all files
 * except of the excluded files, see {@link #isExcluded(String)}, and the
 * files of the ignore file, see {@link #readIgnoreRules(ProjectDirectory)}.
 */
public final class ProjectFiles {

    /** File of the deploy directory, that contains the token of the upload. */
    public static final String DEPLOY_TOKEN_FILE = ".ifsprojects-deploy"; //$NON-NLS-1$

    /** Files and directories of the project directory, that are excluded. */
    private static final Set<String> EXCLUDED_NAMES = new HashSet<String>(Arrays.asList(".git", ".project", ".settings", ".logs", ".evfevent", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
        ".deps", IgnoreRules.DEPLOY_IGNORE_FILE, DEPLOY_TOKEN_FILE)); //$NON-NLS-1$

    private static final char SEPARATOR = '/';

    private ProjectFiles() {
        // Utility class
    }

    /**
     * Returns <code>true</code>, if a file or directory of a project is
     * excluded, because its first segment is excluded: <code>.git</code>,
     * <code>.project</code>, <code>.settings</code>, the output directories of
     * TOBi <code>.logs</code>, <code>.evfevent</code> and <code>.deps</code>,
     * <code>.deployignore</code> and <code>.ifsprojects-deploy</code>.
     *
     * @param aRelativePath - path relative to the project, e.g.
     *        <code>src/hello.rpgle</code>
     * @return <code>true</code>, if the resource is excluded
     */
    public static boolean isExcluded(String aRelativePath) {

        int index = aRelativePath.indexOf(SEPARATOR);
        String firstSegment = index < 0 ? aRelativePath : aRelativePath.substring(0, index);

        return EXCLUDED_NAMES.contains(firstSegment);
    }

    /**
     * Reads the rules of <code>.deployignore</code> or, if it does not exist,
     * of <code>.gitignore</code> of a project directory.
     *
     * @param aDirectory - project directory
     * @return rules, that do not exclude any file, if neither file exists
     * @throws Exception if the ignore file could not be read
     */
    public static IgnoreRules readIgnoreRules(ProjectDirectory aDirectory) throws Exception {

        String content = aDirectory.readFile(IgnoreRules.DEPLOY_IGNORE_FILE);
        if (content == null) {
            content = aDirectory.readFile(IgnoreRules.GIT_IGNORE_FILE);
        }

        if (content == null) {
            return IgnoreRules.empty();
        }

        return IgnoreRules.parse(content);
    }

    /**
     * Directory of a project in the workspace or in the IFS.
     */
    public interface ProjectDirectory {

        /**
         * Reads a file of the project directory.
         *
         * @param aName - name of the file
         * @return content or <code>null</code>, if the file does not exist
         * @throws Exception if the file could not be read
         */
        String readFile(String aName) throws Exception;
    }
}
