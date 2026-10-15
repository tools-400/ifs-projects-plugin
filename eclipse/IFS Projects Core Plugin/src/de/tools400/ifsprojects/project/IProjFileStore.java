/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.project;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import org.eclipse.rse.core.model.IHost;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.IFSFile;
import com.ibm.as400.access.IFSFileReader;
import com.ibm.as400.access.IFSFileWriter;
import com.ibm.etools.iseries.subsystems.qsys.api.IBMiConnection;

import de.tools400.ifsprojects.Messages;

/**
 * Reads and writes <code>iproj.json</code> files with IBM Toolbox for Java.
 * <p>
 * The methods of this class access the remote system and must not be called
 * on the UI thread. The connection must have been established before, see
 * {@link #connect(IHost)}.
 */
public final class IProjFileStore {

    private static final int CCSID_UTF8 = 1208;
    private static final int CCSID_NOT_SET = 65535;
    private static final char SEPARATOR = '/';

    private IProjFileStore() {
        // Utility class
    }

    /**
     * Connects the RSE connection, if it is not connected. Must be called on
     * the UI thread, because RSE may prompt for the password.
     *
     * @param aHost - RSE connection
     * @throws IProjFileException if the connection could not be established
     */
    public static void connect(IHost aHost) throws IProjFileException {

        IBMiConnection connection = getConnection(aHost);
        if (connection.isConnected()) {
            return;
        }

        try {
            if (!connection.connect()) {
                throw new IProjFileException(Messages.bind(Messages.Error_Could_not_connect_to_A, aHost.getAliasName()), null);
            }
        } catch (IProjFileException e) {
            throw e;
        } catch (Exception e) {
            throw new IProjFileException(Messages.bind(Messages.Error_Could_not_connect_to_A, aHost.getAliasName()), e);
        }
    }

    /**
     * Connects the RSE connection, if it is not connected, and returns its
     * IBM Toolbox for Java system object. Must be called on the UI thread.
     *
     * @param aHost - RSE connection
     * @return connected system, owned by RSE (do not disconnect)
     * @throws IProjFileException if the connection could not be established
     */
    public static AS400 getConnectedSystem(IHost aHost) throws IProjFileException {
        connect(aHost);
        return getSystem(aHost);
    }

    /**
     * Reads the <code>iproj.json</code> file of a project.
     *
     * @param aHost - RSE connection
     * @param aDirectory - project directory
     * @return content of the file; {@link IProjFileContent#exists()} returns
     *         <code>false</code>, if the file does not exist
     * @throws IProjFileException if the file could not be read
     */
    public static IProjFileContent read(IHost aHost, String aDirectory) throws IProjFileException {

        String path = getPath(aDirectory);

        Reader reader = null;
        try {

            IFSFile file = new IFSFile(getSystem(aHost), path);
            if (!file.exists()) {
                return IProjFileContent.notExisting(path);
            }

            int ccsid = getValidCcsid(file.getCCSID());
            long lastModified = file.lastModified();

            reader = new IFSFileReader(file, ccsid);
            StringBuilder buffer = new StringBuilder();
            char[] chars = new char[4096];
            int count;
            while ((count = reader.read(chars)) >= 0) {
                buffer.append(chars, 0, count);
            }

            return IProjFileContent.existing(path, buffer.toString(), ccsid, lastModified);

        } catch (IProjFileException e) {
            throw e;
        } catch (Exception e) {
            throw new IProjFileException(Messages.bind(Messages.Error_Could_not_read_A_B, path, e.getLocalizedMessage()), e);
        } finally {
            close(reader);
        }
    }

    /**
     * Writes the <code>iproj.json</code> file of a project. An existing file
     * keeps its CCSID. A new file is created with CCSID 1208 (UTF-8).
     *
     * @param aHost - RSE connection
     * @param aDirectory - project directory
     * @param aContent - new content
     * @param aPrevious - content that has been read before or <code>null</code>
     *        to overwrite the file regardless of changes made by others
     * @throws IProjFileException if the file could not be written or has been
     *         changed by someone else ({@link IProjFileException#isConflict()})
     */
    public static void write(IHost aHost, String aDirectory, String aContent, IProjFileContent aPrevious) throws IProjFileException {

        String path = getPath(aDirectory);

        Writer writer = null;
        try {

            IFSFile file = new IFSFile(getSystem(aHost), path);

            int ccsid = CCSID_UTF8;
            if (file.exists()) {
                if (aPrevious != null) {
                    if (!aPrevious.exists() || file.lastModified() != aPrevious.getLastModified()) {
                        throw IProjFileException.conflict(Messages.bind(Messages.Error_File_A_has_been_changed, path));
                    }
                }
                ccsid = getValidCcsid(file.getCCSID());
            } else {
                // IFSFileWriter requires an existing file, because it reads the
                // CCSID of the file. Therefore the file is created and tagged
                // with CCSID 1208 (UTF-8) before it is written.
                if (!file.createNewFile() && !file.exists()) {
                    throw new IProjFileException(Messages.bind(Messages.Error_Could_not_create_file_A, path), null);
                }
                file.setCCSID(CCSID_UTF8);
            }

            // Parameters: file, append, share option, CCSID
            writer = new IFSFileWriter(file, false, IFSFileWriter.SHARE_NONE, ccsid);
            writer.write(aContent);
            writer.close();
            writer = null;

        } catch (IProjFileException e) {
            throw e;
        } catch (Exception e) {
            throw new IProjFileException(Messages.bind(Messages.Error_Could_not_write_A_B, path, e.getLocalizedMessage()), e);
        } finally {
            close(writer);
        }
    }

    /**
     * Returns <code>true</code>, if the specified directory exists.
     *
     * @param aHost - RSE connection
     * @param aDirectory - directory
     * @return <code>true</code>, if the directory exists
     * @throws IProjFileException if the directory could not be checked
     */
    public static boolean existsDirectory(IHost aHost, String aDirectory) throws IProjFileException {

        try {
            IFSFile directory = new IFSFile(getSystem(aHost), aDirectory);
            return directory.exists() && directory.isDirectory();
        } catch (IProjFileException e) {
            throw e;
        } catch (Exception e) {
            throw new IProjFileException(Messages.bind(Messages.Error_Could_not_read_A_B, aDirectory, e.getLocalizedMessage()), e);
        }
    }

    /**
     * Creates a directory including all parent directories.
     *
     * @param aHost - RSE connection
     * @param aDirectory - directory
     * @throws IProjFileException if the directory could not be created
     */
    public static void createDirectory(IHost aHost, String aDirectory) throws IProjFileException {

        try {
            IFSFile directory = new IFSFile(getSystem(aHost), aDirectory);
            if (directory.exists()) {
                return;
            }
            if (!directory.mkdirs()) {
                throw new IProjFileException(Messages.bind(Messages.Error_Could_not_create_directory_A, aDirectory), null);
            }
        } catch (IProjFileException e) {
            throw e;
        } catch (Exception e) {
            throw new IProjFileException(Messages.bind(Messages.Error_Could_not_create_directory_A, aDirectory), e);
        }
    }

    private static String getPath(String aDirectory) {

        if (aDirectory.length() > 0 && aDirectory.charAt(aDirectory.length() - 1) == SEPARATOR) {
            return aDirectory + IProjConfig.FILE_NAME;
        }

        return aDirectory + SEPARATOR + IProjConfig.FILE_NAME;
    }

    private static int getValidCcsid(int aCcsid) {

        if (aCcsid <= 0 || aCcsid == CCSID_NOT_SET) {
            return CCSID_UTF8;
        }

        return aCcsid;
    }

    private static IBMiConnection getConnection(IHost aHost) throws IProjFileException {

        IBMiConnection connection = IBMiConnection.getConnection(aHost);
        if (connection == null) {
            throw new IProjFileException(Messages.bind(Messages.Error_Not_an_IBM_i_connection_A, aHost.getAliasName()), null);
        }

        return connection;
    }

    private static AS400 getSystem(IHost aHost) throws IProjFileException {

        IBMiConnection connection = getConnection(aHost);
        if (!connection.isConnected()) {
            throw new IProjFileException(Messages.bind(Messages.Error_Connection_A_is_not_connected, aHost.getAliasName()), null);
        }

        try {
            // Do not disconnect: the AS400 object is owned by RSE.
            return connection.getAS400ToolboxObject();
        } catch (Exception e) {
            throw new IProjFileException(Messages.bind(Messages.Error_Could_not_connect_to_A, aHost.getAliasName()), e);
        }
    }

    private static void close(java.io.Closeable aCloseable) {

        if (aCloseable == null) {
            return;
        }

        try {
            aCloseable.close();
        } catch (IOException e) {
            // Ignore errors on close
        }
    }
}
