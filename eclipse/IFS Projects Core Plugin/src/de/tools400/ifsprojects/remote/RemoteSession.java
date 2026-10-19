/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.remote;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.CommandCall;
import com.ibm.as400.access.IFSFile;

import de.tools400.ifsprojects.ibmi.ClCommands;

/**
 * Session of a job of the plug-in on the remote system: a copy of the system
 * object of RSE, whose commands run in a remote command server job of their
 * own, see {@link RemoteJobs#copySystem(AS400)}, with its command call and the
 * temporary files of the job.
 * <p>
 * Closing the session deletes the temporary files and disconnects the copy:
 *
 * <pre>
 * try (RemoteSession session = new RemoteSession(rseSystem)) {
 *     session.getCommandCall().run(command);
 * }
 * </pre>
 *
 * The methods access the remote system and must not be called on the UI
 * thread.
 */
public final class RemoteSession implements AutoCloseable {

    private static final String MEMBER_FILE_SUFFIX = ".member"; //$NON-NLS-1$

    private final AS400 system;
    private final List<IFSFile> temporaryFiles;

    private CommandCall commandCall;
    private String temporaryDirectory;

    /**
     * Creates the session. The copy of the system object connects, when it is
     * used first.
     *
     * @param aRseSystem - connected system object of RSE, which is copied
     */
    public RemoteSession(AS400 aRseSystem) {
        this.system = RemoteJobs.copySystem(aRseSystem);
        this.temporaryFiles = new ArrayList<IFSFile>();
    }

    /**
     * Returns the copy of the system object.
     *
     * @return system object, owned by the session
     */
    public AS400 getSystem() {
        return system;
    }

    /**
     * Returns the command call of the session. Its commands run in the same
     * remote command server job, see {@link RemoteJobs#createCommandCall}.
     *
     * @return command call
     */
    public CommandCall getCommandCall() {

        if (commandCall == null) {
            commandCall = RemoteJobs.createCommandCall(system);
        }

        return commandCall;
    }

    /**
     * Returns the directory of temporary files and creates it, if it does not
     * exist. When the directory is used first, the temporary files left over by
     * earlier jobs are deleted, see
     * {@link RemoteJobs#deleteObsoleteTemporaryFiles}.
     *
     * @return directory without trailing slash
     * @throws IOException if the directory could not be created
     */
    public String getTemporaryDirectory() throws IOException {

        if (temporaryDirectory == null) {
            temporaryDirectory = RemoteJobs.getTemporaryDirectory(system);
            RemoteJobs.deleteObsoleteTemporaryFiles(system, temporaryDirectory);
        }

        return temporaryDirectory;
    }

    /**
     * Returns a new temporary file, which is deleted, when the session is
     * closed. The file is not created.
     *
     * @param aSuffix - suffix, e.g. <code>.log</code>
     * @return file
     * @throws IOException if the directory of temporary files could not be
     *         created
     */
    public IFSFile newTemporaryFile(String aSuffix) throws IOException {
        return newTemporaryFile(RemoteJobs.createTemporaryFileId(), aSuffix);
    }

    /**
     * Returns the temporary file with the specified ID, which is deleted, when
     * the session is closed. The file is not created.
     *
     * @param anId - ID, see {@link RemoteJobs#createTemporaryFileId()}
     * @param aSuffix - suffix, e.g. <code>.log</code>
     * @return file
     * @throws IOException if the directory of temporary files could not be
     *         created
     */
    public IFSFile newTemporaryFile(String anId, String aSuffix) throws IOException {

        IFSFile file = new IFSFile(system, RemoteJobs.getTemporaryFile(getTemporaryDirectory(), anId, aSuffix));
        temporaryFiles.add(file);

        return file;
    }

    /**
     * Reads a member as text: copies it in the remote command server job of
     * the session to a temporary stream file with CCSID 1208 (UTF-8) and reads
     * the stream file.
     *
     * @param aLibrary - library
     * @param aFile - file
     * @param aMember - member
     * @return content or <code>null</code>, if the member could not be copied,
     *         see the messages of {@link #getCommandCall()}
     * @throws Exception if the command could not be run or the stream file
     *         could not be read
     */
    public String readMember(String aLibrary, String aFile, String aMember) throws Exception {

        IFSFile streamFile = newTemporaryFile(MEMBER_FILE_SUFFIX);
        String command = ClCommands.getCopyToStreamFileCommand(ClCommands.getMemberPath(aLibrary, aFile, aMember), streamFile.getAbsolutePath());
        if (!getCommandCall().run(command)) {
            return null;
        }

        try {
            return RemoteJobs.readText(streamFile, RemoteJobs.CCSID_UTF8);
        } finally {
            RemoteJobs.delete(streamFile);
            // Deleted already, so that close() does not access it again, e.g.
            // once for each events file of a build.
            temporaryFiles.remove(streamFile);
        }
    }

    /**
     * Returns the CCSID of the remote command server job of the session, see
     * {@link RemoteJobs#getJobCcsid}.
     *
     * @return CCSID
     */
    public int getJobCcsid() {
        return RemoteJobs.getJobCcsid(getCommandCall(), system);
    }

    /**
     * Deletes the temporary files of the session and disconnects the copy of
     * the system object, also if a file could not be deleted. Errors are
     * logged.
     */
    public void close() {

        try {
            for (IFSFile file : temporaryFiles) {
                RemoteJobs.delete(file);
            }
        } finally {
            system.disconnectAllServices();
        }
    }
}
