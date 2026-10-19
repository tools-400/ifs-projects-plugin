/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.remote;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400Message;
import com.ibm.as400.access.CommandCall;
import com.ibm.as400.access.IFSFile;
import com.ibm.as400.access.IFSFileFilter;
import com.ibm.as400.access.IFSFileInputStream;
import com.ibm.as400.access.IFSFileReader;
import com.ibm.as400.access.IFSFileWriter;
import com.ibm.as400.access.Job;
import com.ibm.as400.access.User;

import de.tools400.ifsprojects.IfsProjectsPlugin;
import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.utils.StringUtils;

/**
 * Helper methods for commands and files of the jobs, that perform syntax checks
 * and builds with IBM Toolbox for Java. The jobs use a {@link RemoteSession}.
 * <p>
 * All files of the plug-in are stored below the root directory
 * <code>&lt;home&gt;/.ifsprojects</code> of the user profile: temporary files
 * in <code>tmp</code>, the copies of local projects in <code>builds</code>.
 * <p>
 * The methods access the remote system and must not be called on the UI
 * thread.
 */
public final class RemoteJobs {

    public static final int CCSID_UTF8 = 1208;
    public static final int CCSID_NOT_SET = 65535;

    /** Age, after which a temporary file is regarded as left over. */
    public static final long OBSOLETE_FILE_AGE_MILLIS = 24L * 60 * 60 * 1000;

    private static final String TEMPORARY_FILE_PREFIX = "ifsprojects_"; //$NON-NLS-1$
    private static final String HOME_PARENT = "/home/"; //$NON-NLS-1$
    private static final String ROOT_DIRECTORY = ".ifsprojects"; //$NON-NLS-1$
    private static final String TEMPORARY_DIRECTORY = "tmp"; //$NON-NLS-1$
    private static final String BUILDS_DIRECTORY = "builds"; //$NON-NLS-1$

    /** Home directories by system name and user profile. */
    private static final Map<String, String> HOME_DIRECTORIES = new ConcurrentHashMap<String, String>();
    private static final String LINE_SEPARATOR = "\n"; //$NON-NLS-1$
    private static final char SEPARATOR = '/';

    private static final Random RANDOM = new Random();

    private RemoteJobs() {
        // Utility class
    }

    /**
     * Returns a new system object, that is a copy of the system object of RSE.
     * Its commands run in a remote command server job of their own. The copy
     * must be disconnected with {@link AS400#disconnectAllServices()}.
     *
     * @param aRseSystem - system object of RSE
     * @return copy
     */
    static AS400 copySystem(AS400 aRseSystem) {
        return new AS400(aRseSystem);
    }

    /**
     * Returns a command call, whose commands run in the remote command server
     * job, so that they share QTEMP, the current directory and the library
     * list.
     *
     * @param aSystem - system object
     * @return command call
     */
    static CommandCall createCommandCall(AS400 aSystem) {

        CommandCall commandCall = new CommandCall(aSystem);
        commandCall.setThreadSafe(false);

        return commandCall;
    }

    /**
     * Returns the directory of temporary files of the plug-in,
     * <code>&lt;home&gt;/.ifsprojects/tmp</code>, and creates it, if it does
     * not exist.
     *
     * @param aSystem - system object
     * @return directory without trailing slash
     * @throws IOException if the directory could not be created
     */
    public static String getTemporaryDirectory(AS400 aSystem) throws IOException {

        String directory = getTemporaryDirectory(getHomeDirectory(aSystem));

        try {
            IFSFile file = new IFSFile(aSystem, directory);
            if (!file.exists() && !file.mkdirs() && !file.exists()) {
                throw new IOException(Messages.bind(Messages.Error_Could_not_create_directory_A, directory));
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(Messages.bind(Messages.Error_Could_not_create_directory_A, directory), e);
        }

        return directory;
    }

    /**
     * Returns the root directory of the files of the plug-in.
     *
     * @param aHomeDirectory - home directory of the user profile
     * @return directory, e.g. <code>/home/jsmith/.ifsprojects</code>
     */
    public static String getRootDirectory(String aHomeDirectory) {

        String directory = aHomeDirectory;
        while (directory.length() > 0 && directory.charAt(directory.length() - 1) == SEPARATOR) {
            directory = directory.substring(0, directory.length() - 1);
        }

        return directory + SEPARATOR + ROOT_DIRECTORY;
    }

    /**
     * Returns the directory of temporary files of the plug-in.
     *
     * @param aHomeDirectory - home directory of the user profile
     * @return directory, e.g. <code>/home/jsmith/.ifsprojects/tmp</code>
     */
    public static String getTemporaryDirectory(String aHomeDirectory) {
        return getRootDirectory(aHomeDirectory) + SEPARATOR + TEMPORARY_DIRECTORY;
    }

    /**
     * Returns the parent directory of the copies of local projects.
     *
     * @param aHomeDirectory - home directory of the user profile
     * @return directory, e.g. <code>/home/jsmith/.ifsprojects/builds</code>
     */
    public static String getBuildsDirectory(String aHomeDirectory) {
        return getRootDirectory(aHomeDirectory) + SEPARATOR + BUILDS_DIRECTORY;
    }

    /**
     * Returns the path of a new temporary file.
     *
     * @param aDirectory - directory of temporary files, see
     *        {@link #getTemporaryDirectory(AS400)}
     * @param aSuffix - suffix, e.g. <code>.log</code>
     * @return path
     */
    public static String getTemporaryFile(String aDirectory, String aSuffix) {
        return getTemporaryFile(aDirectory, createTemporaryFileId(), aSuffix);
    }

    /**
     * Returns a new ID of temporary files, e.g. for files, that belong to the
     * same command.
     *
     * @return ID, e.g. <code>1791972000000_42</code>
     */
    public static String createTemporaryFileId() {
        return System.currentTimeMillis() + "_" + RANDOM.nextInt(Integer.MAX_VALUE); //$NON-NLS-1$
    }

    /**
     * Returns the path of the temporary file with the specified ID.
     *
     * @param aDirectory - directory of temporary files, see
     *        {@link #getTemporaryDirectory(AS400)}
     * @param anId - ID, see {@link #createTemporaryFileId()}
     * @param aSuffix - suffix, e.g. <code>.log</code>
     * @return path
     */
    public static String getTemporaryFile(String aDirectory, String anId, String aSuffix) {
        return getTemporaryFilePrefix(aDirectory) + anId + aSuffix;
    }

    /**
     * Returns the common beginning of the paths of the temporary files.
     *
     * @param aDirectory - directory of temporary files, see
     *        {@link #getTemporaryDirectory(AS400)}
     * @return prefix, e.g. <code>/home/jsmith/.ifsprojects/tmp/ifsprojects_</code>
     */
    public static String getTemporaryFilePrefix(String aDirectory) {
        return aDirectory + SEPARATOR + TEMPORARY_FILE_PREFIX;
    }

    /**
     * Deletes the temporary files, that have been left over by earlier syntax
     * checks and builds, e.g. because RDi ended or the connection was lost
     * while a command was running. Only files older than
     * {@link #OBSOLETE_FILE_AGE_MILLIS} are deleted, so that the files of
     * running commands, also of other RDi instances, are kept.
     * <p>
     * Files that cannot be deleted are skipped.
     *
     * @param aSystem - system object
     * @param aDirectory - directory of temporary files, see
     *        {@link #getTemporaryDirectory(AS400)}
     */
    static void deleteObsoleteTemporaryFiles(AS400 aSystem, String aDirectory) {

        try {
            IFSFile[] files = new IFSFile(aSystem, aDirectory).listFiles(TEMPORARY_FILE_PREFIX + "*"); //$NON-NLS-1$
            if (files == null) {
                return;
            }
            long currentTime = System.currentTimeMillis();
            for (IFSFile file : files) {
                if (file.isFile() && isObsoleteTemporaryFile(file.getName(), file.lastModified(), currentTime)) {
                    file.delete();
                }
            }
        } catch (Exception e) {
            IfsProjectsPlugin.logError("Could not delete obsolete temporary files in: " + aDirectory, e); //$NON-NLS-1$
        }
    }

    /**
     * Returns <code>true</code>, if a file is a temporary file of the plug-in,
     * that is older than {@link #OBSOLETE_FILE_AGE_MILLIS}. The modification
     * time is the time of the server, the current time the time of the PC; a
     * difference of the clocks does not matter compared to the age.
     *
     * @param aName - name of the file
     * @param aLastModified - modification time of the file, 0 if unknown
     * @param aCurrentTime - current time
     * @return <code>true</code>, if the file can be deleted
     */
    public static boolean isObsoleteTemporaryFile(String aName, long aLastModified, long aCurrentTime) {

        if (aName == null || !aName.startsWith(TEMPORARY_FILE_PREFIX) || aLastModified <= 0) {
            return false;
        }

        return aCurrentTime - aLastModified > OBSOLETE_FILE_AGE_MILLIS;
    }

    /**
     * Returns the home directory of the user profile of a system object. The
     * directory is retrieved once per system and user profile.
     *
     * @param aSystem - system object
     * @return home directory
     */
    public static String getHomeDirectory(AS400 aSystem) {

        String key = aSystem.getSystemName() + SEPARATOR + aSystem.getUserId();
        String directory = HOME_DIRECTORIES.get(key);
        if (directory != null) {
            return directory;
        }

        try {
            directory = new User(aSystem, aSystem.getUserId()).getHomeDirectory();
        } catch (Exception e) {
            IfsProjectsPlugin.logError("Could not retrieve the home directory of: " + aSystem.getUserId(), e); //$NON-NLS-1$
            return HOME_PARENT + aSystem.getUserId().toLowerCase();
        }

        directory = StringUtils.trimToNull(directory);
        if (directory == null) {
            directory = HOME_PARENT + aSystem.getUserId().toLowerCase();
        }

        HOME_DIRECTORIES.put(key, directory);

        return directory;
    }

    /**
     * Returns <code>true</code>, if the last command sent the specified
     * message.
     */
    public static boolean hasMessage(CommandCall aCommandCall, String aMessageId) {

        for (AS400Message message : aCommandCall.getMessageList()) {
            if (aMessageId.equals(message.getID())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns the messages of the last command, one message per line.
     */
    public static String getMessages(CommandCall aCommandCall) {

        StringBuilder buffer = new StringBuilder();
        for (AS400Message message : aCommandCall.getMessageList()) {
            if (buffer.length() > 0) {
                buffer.append(LINE_SEPARATOR);
            }
            buffer.append(message.getID()).append(": ").append(message.getText()); //$NON-NLS-1$
        }

        return buffer.toString();
    }

    /**
     * Returns the CCSID of the remote command server job, e.g. the CCSID, in
     * which a compiler wrote an events file.
     */
    static int getJobCcsid(CommandCall aCommandCall, AS400 aSystem) {

        try {
            Job job = aCommandCall.getServerJob();
            int ccsid = job.getCodedCharacterSetID();
            if (ccsid == CCSID_NOT_SET) {
                ccsid = job.getDefaultCodedCharacterSetIdentifier();
            }
            return ccsid;
        } catch (Exception e) {
            IfsProjectsPlugin.logError("Could not retrieve the CCSID of the remote command server job.", e); //$NON-NLS-1$
            return aSystem.getCcsid();
        }
    }

    /**
     * Reads a text stream file.
     *
     * @param aFile - stream file
     * @param aCcsid - CCSID of the file
     * @return content
     * @throws Exception if the file could not be read
     */
    public static String readText(IFSFile aFile, int aCcsid) throws Exception {

        Reader reader = new IFSFileReader(aFile, aCcsid);
        try {
            StringBuilder buffer = new StringBuilder();
            char[] chars = new char[4096];
            int count;
            while ((count = reader.read(chars)) >= 0) {
                buffer.append(chars, 0, count);
            }
            return buffer.toString();
        } finally {
            reader.close();
        }
    }

    /**
     * Reads a text stream file with the CCSID of the file. Files without CCSID
     * are read as UTF-8.
     */
    public static String readText(IFSFile aFile) throws Exception {
        return readText(aFile, getValidCcsid(aFile.getCCSID()));
    }

    /**
     * Writes a text stream file. A new file is created with the specified
     * CCSID, an existing file is replaced and keeps its CCSID.
     *
     * @param aFile - stream file
     * @param aText - content
     * @param aCcsid - CCSID of a new file
     * @throws Exception if the file could not be written
     */
    public static void writeText(IFSFile aFile, String aText, int aCcsid) throws Exception {

        // IFSFileWriter requires an existing file, because it reads the CCSID
        // of the file.
        if (!aFile.exists()) {
            if (!aFile.createNewFile() && !aFile.exists()) {
                throw new IOException(Messages.bind(Messages.Error_Could_not_create_file_A, aFile.getAbsolutePath()));
            }
            aFile.setCCSID(aCcsid);
        }

        // Parameters: file, append, share option, CCSID
        Writer writer = new IFSFileWriter(aFile, false, IFSFileWriter.SHARE_NONE, getValidCcsid(aFile.getCCSID()));
        try {
            writer.write(aText);
        } finally {
            writer.close();
        }
    }

    /**
     * Reads the bytes of a stream file starting at an offset.
     *
     * @param aFile - stream file
     * @param anOffset - offset
     * @return bytes
     * @throws Exception if the file could not be read
     */
    public static byte[] readBytes(IFSFile aFile, long anOffset) throws Exception {

        InputStream inputStream = new IFSFileInputStream(aFile);
        try {
            long skipped = 0;
            while (skipped < anOffset) {
                long count = inputStream.skip(anOffset - skipped);
                if (count <= 0) {
                    return new byte[0];
                }
                skipped += count;
            }

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] bytes = new byte[8192];
            int count;
            while ((count = inputStream.read(bytes)) >= 0) {
                buffer.write(bytes, 0, count);
            }
            return buffer.toByteArray();
        } finally {
            inputStream.close();
        }
    }

    /**
     * Lists the files of a directory, also the files, whose names start with
     * a period. By default, IBM Toolbox for Java uses POSIX pattern matching,
     * which skips them.
     *
     * @param aDirectory - directory
     * @return files or <code>null</code>, see {@link IFSFile#listFiles()}
     * @throws IOException if the directory could not be listed
     */
    public static IFSFile[] listFiles(IFSFile aDirectory) throws IOException {
        aDirectory.setPatternMatching(IFSFile.PATTERN_POSIX_ALL);
        return aDirectory.listFiles();
    }

    /**
     * Lists the files of a directory, that are accepted by a filter, also the
     * files, whose names start with a period, see {@link #listFiles(IFSFile)}.
     *
     * @param aDirectory - directory
     * @param aFilter - filter
     * @return files or <code>null</code>, see
     *         {@link IFSFile#listFiles(IFSFileFilter)}
     * @throws IOException if the directory could not be listed
     */
    public static IFSFile[] listFiles(IFSFile aDirectory, IFSFileFilter aFilter) throws IOException {
        aDirectory.setPatternMatching(IFSFile.PATTERN_POSIX_ALL);
        return aDirectory.listFiles(aFilter);
    }

    /**
     * Deletes a stream file. Errors are logged.
     */
    public static void delete(IFSFile aFile) {

        try {
            if (aFile.exists() && !aFile.delete()) {
                IfsProjectsPlugin.logWarning("Could not delete temporary file: " + aFile.getAbsolutePath()); //$NON-NLS-1$
            }
        } catch (IOException e) {
            IfsProjectsPlugin.logError("Could not delete temporary file: " + aFile.getAbsolutePath(), e); //$NON-NLS-1$
        }
    }

    /**
     * Returns the CCSID, with which a text stream file is read: the CCSID of
     * the file or 1208 (UTF-8) for files without CCSID.
     *
     * @param aCcsid - CCSID of the file
     * @return CCSID
     */
    public static int getValidCcsid(int aCcsid) {

        if (aCcsid <= 0 || aCcsid == CCSID_NOT_SET) {
            return CCSID_UTF8;
        }

        return aCcsid;
    }
}
