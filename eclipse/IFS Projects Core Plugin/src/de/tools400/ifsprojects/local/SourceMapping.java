/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.local;

/**
 * Maps the copy of a local project in the IFS (its deploy directory) to the
 * project in the workspace, so that the errors of a build are shown for the
 * local files.
 * <p>
 * The 'Error List' view of RDi opens the local file of an error, if the
 * location of the error is <code>/&lt;project&gt;/&lt;relative path&gt;</code>
 * and its attribute <code>projectName</code> is the name of the project, see
 * {@link EventsFileMapper}.
 */
public final class SourceMapping {

    private static final char SEPARATOR = '/';

    private final String remoteDirectory;
    private final String projectName;

    /**
     * Creates a mapping.
     *
     * @param aRemoteDirectory - absolute path of the deploy directory
     * @param aProjectName - name of the project in the workspace
     */
    public SourceMapping(String aRemoteDirectory, String aProjectName) {

        String directory = aRemoteDirectory;
        while (directory.length() > 1 && directory.charAt(directory.length() - 1) == SEPARATOR) {
            directory = directory.substring(0, directory.length() - 1);
        }

        this.remoteDirectory = directory;
        this.projectName = aProjectName;
    }

    /**
     * Returns the absolute path of the deploy directory without trailing
     * slash.
     */
    public String getRemoteDirectory() {
        return remoteDirectory;
    }

    /**
     * Returns the name of the project in the workspace.
     */
    public String getProjectName() {
        return projectName;
    }

    /**
     * Returns the location of a file of the deploy directory in the local
     * project. The case of the deploy directory is ignored, because the root
     * file system of the IFS is not case sensitive: the compilers write the
     * path with the case of the directory, e.g. <code>/home/Jsmith</code>.
     *
     * @param aRemotePath - absolute path of the file, e.g.
     *        <code>/home/Jsmith/.ifsprojects/builds/demo/src/hello.rpgle</code>
     * @return location, e.g. <code>/demo/src/hello.rpgle</code>, or
     *         <code>null</code>, if the file is not part of the deploy
     *         directory
     */
    public String toProjectLocation(String aRemotePath) {

        if (aRemotePath == null) {
            return null;
        }

        String prefix = remoteDirectory + SEPARATOR;
        if (aRemotePath.length() <= prefix.length() || !aRemotePath.regionMatches(true, 0, prefix, 0, prefix.length())) {
            return null;
        }

        return SEPARATOR + projectName + SEPARATOR + aRemotePath.substring(prefix.length());
    }
}
