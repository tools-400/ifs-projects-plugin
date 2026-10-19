/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.local;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.rse.core.model.IHost;

import de.tools400.ifsprojects.IfsProjectsPlugin;
import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.project.EnvironmentVariables;
import de.tools400.ifsprojects.project.IProjConfig;
import de.tools400.ifsprojects.project.IProjFileContent;
import de.tools400.ifsprojects.project.IProjFileException;
import de.tools400.ifsprojects.project.IProjParseException;
import de.tools400.ifsprojects.remote.RemoteJobs;
import de.tools400.ifsprojects.rse.IfsFilterStrings;
import de.tools400.ifsprojects.rse.RseConnections;
import de.tools400.ifsprojects.utils.StringUtils;

/**
 * Local IFS project: a project of the workspace, whose files are uploaded to a
 * deploy directory in the IFS and built there, see {@link Deployment}.
 * <p>
 * The RSE connection, the deploy directory and the environment variables of
 * each connection are stored as persistent properties of the project. They are
 * specific for the user and the workspace and therefore are not stored in the
 * files of the project (<code>.project</code>, <code>.settings</code>), which
 * are usually shared.
 */
public final class LocalProject {

    private static final QualifiedName CONNECTION = new QualifiedName(IfsProjectsPlugin.PLUGIN_ID, "connection"); //$NON-NLS-1$
    private static final String DEPLOY_DIRECTORY_PREFIX = "deployDirectory."; //$NON-NLS-1$
    private static final String ENVIRONMENT_PREFIX = "environment."; //$NON-NLS-1$
    private static final String EXPORT_PREFIX = "export."; //$NON-NLS-1$

    /**
     * Characters of a project name, that are kept in the default deploy
     * directory.
     */
    private static final String INVALID_DIRECTORY_CHARACTERS = "[^A-Za-z0-9._-]"; //$NON-NLS-1$
    private static final String REPLACEMENT = "_"; //$NON-NLS-1$
    private static final char SEPARATOR = '/';
    private static final String ROOT_DIRECTORY = "/"; //$NON-NLS-1$

    private final IProject project;
    private final String connectionName;

    private LocalProject(IProject aProject, String aConnectionName) {
        this.project = aProject;
        this.connectionName = aConnectionName;
    }

    /**
     * Returns the local IFS project of a project of the workspace.
     *
     * @param aProject - project or <code>null</code>
     * @return local IFS project or <code>null</code>, if the project is closed
     *         or not configured as IFS project
     */
    public static LocalProject get(IProject aProject) {

        if (aProject == null || !aProject.isOpen()) {
            return null;
        }

        String connectionName = getConnectionName(aProject);
        if (connectionName == null) {
            return null;
        }

        return new LocalProject(aProject, connectionName);
    }

    /**
     * Returns the name of the RSE connection of a project.
     *
     * @param aProject - open project
     * @return name of the connection or <code>null</code>, if the project is
     *         not configured as IFS project
     */
    public static String getConnectionName(IProject aProject) {
        return getProperty(aProject, CONNECTION);
    }

    /**
     * Returns the deploy directory of a project for a connection, that has been
     * entered by the user.
     *
     * @param aProject - open project
     * @param aConnectionName - name of the RSE connection
     * @return deploy directory or <code>null</code> for the default directory,
     *         see {@link #getDefaultDeployDirectory}
     */
    public static String getDeployDirectory(IProject aProject, String aConnectionName) {
        return getProperty(aProject, getDeployDirectoryKey(aConnectionName));
    }

    /**
     * Configures a project as local IFS project or removes the configuration.
     * The deploy directories of other connections are kept.
     *
     * @param aProject - open project
     * @param aConnectionName - name of the RSE connection or <code>null</code>
     *        to remove the configuration
     * @param aDeployDirectory - deploy directory of the connection or
     *        <code>null</code> for the default directory
     * @throws CoreException if the properties could not be saved
     */
    public static void configure(IProject aProject, String aConnectionName, String aDeployDirectory) throws CoreException {

        aProject.setPersistentProperty(CONNECTION, StringUtils.trimToNull(aConnectionName));
        if (StringUtils.trimToNull(aConnectionName) != null) {
            aProject.setPersistentProperty(getDeployDirectoryKey(aConnectionName), StringUtils.trimToNull(aDeployDirectory));
        }
    }

    /**
     * Returns the environment variables of a project for a connection, that are
     * exported before the build and compile commands, like the variables of IFS
     * projects, see
     * {@link de.tools400.ifsprojects.subsystem.IfsProjectEnvironment}.
     *
     * @param aProject - open project
     * @param aConnectionName - name of the RSE connection
     * @return variables, never <code>null</code>
     */
    public static Map<String, String> getEnvironmentVariables(IProject aProject, String aConnectionName) {

        String text = getProperty(aProject, getEnvironmentKey(aConnectionName));
        if (text == null) {
            return new LinkedHashMap<String, String>();
        }

        return EnvironmentVariables.parse(text);
    }

    /**
     * Sets the environment variables of a project for a connection.
     *
     * @param aProject - open project
     * @param aConnectionName - name of the RSE connection
     * @param aVariables - variables, an empty map removes all variables
     * @throws CoreException if the variables could not be saved, e.g. because
     *         they are longer than 2048 characters
     */
    public static void setEnvironmentVariables(IProject aProject, String aConnectionName, Map<String, String> aVariables) throws CoreException {
        aProject.setPersistentProperty(getEnvironmentKey(aConnectionName), aVariables.isEmpty() ? null : EnvironmentVariables.format(aVariables));
    }

    /**
     * Returns the settings of the last export of a project to a library of a
     * connection, see {@link de.tools400.ifsprojects.export.ExportSettings}.
     *
     * @param aProject - open project
     * @param aConnectionName - name of the RSE connection
     * @return values, empty, if the project has not been exported or the
     *         property is invalid
     */
    public static Map<String, String> getExportSettings(IProject aProject, String aConnectionName) {

        // Same format as the environment variables: one value per line.
        String text = getProperty(aProject, getExportKey(aConnectionName));
        if (text == null || EnvironmentVariables.findInvalidLine(text) > 0) {
            return new LinkedHashMap<String, String>();
        }

        return EnvironmentVariables.parse(text);
    }

    /**
     * Sets the settings of the last export of a project to a library of a
     * connection.
     *
     * @param aProject - open project
     * @param aConnectionName - name of the RSE connection
     * @param aSettings - values, see
     *        {@link de.tools400.ifsprojects.export.ExportSettings#toMap()}
     * @throws CoreException if the settings could not be saved
     */
    public static void setExportSettings(IProject aProject, String aConnectionName, Map<String, String> aSettings) throws CoreException {
        aProject.setPersistentProperty(getExportKey(aConnectionName), aSettings.isEmpty() ? null : EnvironmentVariables.format(aSettings));
    }

    /**
     * Returns the default deploy directory of a project.
     *
     * @param aHomeDirectory - home directory of the user profile
     * @param aProjectName - name of the project
     * @return directory, e.g.
     *         <code>/home/jsmith/.ifsprojects/builds/demo</code>. Characters of
     *         the project name, that are not letters, digits, dots, underscores
     *         or hyphens, are replaced by underscores, because make does not
     *         support blanks in paths.
     */
    public static String getDefaultDeployDirectory(String aHomeDirectory, String aProjectName) {
        return RemoteJobs.getBuildsDirectory(aHomeDirectory) + SEPARATOR + aProjectName.replaceAll(INVALID_DIRECTORY_CHARACTERS, REPLACEMENT);
    }

    /**
     * Normalizes a deploy directory entered by the user.
     *
     * @param aDirectory - directory
     * @return directory without trailing slashes, <code>""</code> for an empty
     *         directory (default directory) or <code>null</code>, if the
     *         directory is invalid: not absolute, the root directory or with
     *         wild cards
     */
    public static String normalizeDeployDirectory(String aDirectory) {

        if (StringUtils.isBlank(aDirectory)) {
            return ""; //$NON-NLS-1$
        }

        String directory = IfsFilterStrings.normalizeDirectory(aDirectory);
        if (directory == null || ROOT_DIRECTORY.equals(directory)) {
            return null;
        }

        return directory;
    }

    public IProject getProject() {
        return project;
    }

    public String getName() {
        return project.getName();
    }

    public String getConnectionName() {
        return connectionName;
    }

    /**
     * Returns the RSE connection of the project.
     *
     * @return connection or <code>null</code>, if it does not exist
     */
    public IHost getHost() {
        return RseConnections.findIBMiHost(connectionName);
    }

    /**
     * Returns the deploy directory of the project.
     *
     * @param aHomeDirectory - home directory of the user profile of the
     *        connection
     * @return deploy directory entered by the user or the default directory
     */
    public String getDeployDirectory(String aHomeDirectory) {

        String directory = getDeployDirectory(project, connectionName);
        if (directory != null) {
            return directory;
        }

        return getDefaultDeployDirectory(aHomeDirectory, project.getName());
    }

    /**
     * Returns the environment variables of the project for its connection.
     *
     * @return variables, never <code>null</code>
     */
    public Map<String, String> getEnvironmentVariables() {
        return getEnvironmentVariables(project, connectionName);
    }

    /**
     * Reads the <code>iproj.json</code> file of the project.
     *
     * @return configuration, empty, if the file does not exist
     * @throws CoreException if the file could not be read
     * @throws IOException if the file could not be read
     * @throws IProjParseException if the file is invalid
     */
    public IProjConfig readConfig() throws CoreException, IOException, IProjParseException {

        IFile file = project.getFile(IProjConfig.FILE_NAME);
        if (!file.exists()) {
            return IProjConfig.parse(null);
        }

        return IProjConfig.parse(readText(file));
    }

    /**
     * Reads the <code>iproj.json</code> file of a project, e.g. for editing.
     * The file is refreshed before, so that changes made outside of the
     * workspace are read.
     *
     * @param aProject - open project
     * @return content of the file with the CCSID, that the file gets in the
     *         IFS, see {@link Deployment#getCcsid(String)}
     * @throws IProjFileException if the file could not be read
     */
    public static IProjFileContent readConfigFile(IProject aProject) throws IProjFileException {

        IFile file = aProject.getFile(IProjConfig.FILE_NAME);
        String path = file.getFullPath().toString();

        try {
            file.refreshLocal(IResource.DEPTH_ZERO, null);
            if (!file.exists()) {
                return IProjFileContent.notExisting(path);
            }
            return IProjFileContent.existing(path, readText(file), Deployment.getCcsid(file.getCharset()), file.getLocalTimeStamp());
        } catch (Exception e) {
            throw new IProjFileException(Messages.bind(Messages.Error_Could_not_read_A_B, path, e.getLocalizedMessage()), e);
        }
    }

    /**
     * Writes the <code>iproj.json</code> file of a project with its encoding
     * or, if it does not exist, with the default encoding of the project.
     *
     * @param aProject - open project
     * @param aContent - text of the file
     * @param aPrevious - content read before or <code>null</code> to overwrite
     *        the file without checking for changes
     * @throws IProjFileException if the file could not be written or has been
     *         changed since it was read
     *         ({@link IProjFileException#isConflict()})
     */
    public static void writeConfigFile(IProject aProject, String aContent, IProjFileContent aPrevious) throws IProjFileException {

        IFile file = aProject.getFile(IProjConfig.FILE_NAME);
        String path = file.getFullPath().toString();

        try {
            file.refreshLocal(IResource.DEPTH_ZERO, null);
            InputStream content = new ByteArrayInputStream(aContent.getBytes(file.getCharset()));
            if (file.exists()) {
                if (aPrevious != null && (!aPrevious.exists() || file.getLocalTimeStamp() != aPrevious.getLastModified())) {
                    throw IProjFileException.conflict(Messages.bind(Messages.Error_File_A_has_been_changed, path));
                }
                file.setContents(content, IResource.KEEP_HISTORY, null);
            } else {
                file.create(content, false, null);
            }
        } catch (IProjFileException e) {
            throw e;
        } catch (Exception e) {
            throw new IProjFileException(Messages.bind(Messages.Error_Could_not_write_A_B, path, e.getLocalizedMessage()), e);
        }
    }

    /**
     * Reads a text file of the workspace with its encoding.
     *
     * @param aFile - file
     * @return content
     * @throws CoreException if the file could not be read
     * @throws IOException if the file could not be read
     */
    static String readText(IFile aFile) throws CoreException, IOException {

        String charset = aFile.getCharset();
        InputStream inputStream = aFile.getContents(true);
        try {
            Reader reader = new InputStreamReader(inputStream, charset);
            StringBuilder buffer = new StringBuilder();
            char[] chars = new char[4096];
            int count;
            while ((count = reader.read(chars)) >= 0) {
                buffer.append(chars, 0, count);
            }
            return buffer.toString();
        } finally {
            inputStream.close();
        }
    }

    private static QualifiedName getDeployDirectoryKey(String aConnectionName) {
        return new QualifiedName(IfsProjectsPlugin.PLUGIN_ID, DEPLOY_DIRECTORY_PREFIX + aConnectionName);
    }

    private static QualifiedName getEnvironmentKey(String aConnectionName) {
        return new QualifiedName(IfsProjectsPlugin.PLUGIN_ID, ENVIRONMENT_PREFIX + aConnectionName);
    }

    private static QualifiedName getExportKey(String aConnectionName) {
        return new QualifiedName(IfsProjectsPlugin.PLUGIN_ID, EXPORT_PREFIX + aConnectionName);
    }

    private static String getProperty(IProject aProject, QualifiedName aKey) {

        try {
            return StringUtils.trimToNull(aProject.getPersistentProperty(aKey));
        } catch (CoreException e) {
            IfsProjectsPlugin.logError("Could not read property " + aKey.getLocalName() + " of project: " + aProject.getName(), e); //$NON-NLS-1$ //$NON-NLS-2$
            return null;
        }
    }
}
