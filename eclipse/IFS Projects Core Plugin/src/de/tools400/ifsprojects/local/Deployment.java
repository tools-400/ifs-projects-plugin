/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.local;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.IFSFile;
import com.ibm.as400.access.IFSFileOutputStream;

import de.tools400.ifsprojects.IfsProjectsPlugin;
import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.project.IgnoreRules;
import de.tools400.ifsprojects.project.ProjectFiles;
import de.tools400.ifsprojects.remote.RemoteJobs;

/**
 * Uploads the files of a local project to its deploy directory in the IFS. The
 * files are only copied from the workspace to the IFS, never back.
 * <ul>
 * <li>Only new and changed files are uploaded, see {@link DeployManifest}. A
 * file is unchanged, if its size, modification time and encoding are the same
 * as at the last upload or, otherwise, its content is the same.</li>
 * <li>Files, that have been uploaded before, but do not exist any more or are
 * excluded now, are deleted in the deploy directory, also directories, that
 * are empty then. Other files of the deploy directory, e.g. the output of
 * TOBi, are kept. The files are deleted before the upload, because a file,
 * whose name has only changed in case, is the same stream file.</li>
 * <li>The token of the manifest is written to the file
 * <code>.ifsprojects-deploy</code> of the deploy directory, before the
 * directory is changed. If it differs from the token of the manifest, e.g.
 * because another workspace has uploaded the same project, all files are
 * uploaded.</li>
 * <li>Otherwise the stream files are compared with their size and
 * modification time after the last upload. The user is asked, before files
 * changed in the IFS are replaced or deleted, see {@link ConflictHandler}.
 * Missing stream files are uploaded again.</li>
 * <li>The bytes of a file are copied unchanged. A new file gets the CCSID of
 * the encoding of the local file, so that the compilers convert its text
 * correctly. A file with another CCSID is replaced.</li>
 * <li>The modification time of an uploaded file is the time of the upload, so
 * that make rebuilds the objects of every uploaded change.</li>
 * <li>Not uploaded are derived, team private and hidden resources, the files of
 * git, Eclipse and the plug-in and the output directories of TOBi in the
 * project directory, see {@link ProjectFiles#isExcluded(String)}, and the files excluded
 * by the ignore file, see {@link IgnoreRules}.</li>
 * </ul>
 * The methods access the remote system and must not be called on the UI
 * thread.
 */
public final class Deployment {

    private static final String MANIFEST_DIRECTORY = "deploy"; //$NON-NLS-1$
    private static final String MANIFEST_SUFFIX = ".manifest"; //$NON-NLS-1$
    private static final String HASH_ALGORITHM = "MD5"; //$NON-NLS-1$
    private static final String KEY_ALGORITHM = "SHA-1"; //$NON-NLS-1$
    private static final String KEY_SEPARATOR = "\n"; //$NON-NLS-1$

    private static final String UTF_8 = "UTF-8"; //$NON-NLS-1$
    private static final String WINDOWS_1252 = "windows-1252"; //$NON-NLS-1$
    private static final String ISO_8859_1 = "ISO-8859-1"; //$NON-NLS-1$
    private static final String ISO_8859_15 = "ISO-8859-15"; //$NON-NLS-1$
    private static final String US_ASCII = "US-ASCII"; //$NON-NLS-1$

    private static final int CCSID_WINDOWS_1252 = 1252;
    private static final int CCSID_ISO_8859_1 = 819;
    private static final int CCSID_ISO_8859_15 = 923;
    private static final int CCSID_US_ASCII = 367;

    private static final char SEPARATOR = '/';

    private Deployment() {
        // Utility class
    }

    /**
     * Uploads the files of a project to the deploy directory. The project is
     * refreshed before, so that changes made outside of the workspace, e.g.
     * by git, are uploaded.
     *
     * @param aSystem - system object
     * @param aProject - project
     * @param aConnectionName - name of the RSE connection
     * @param aDeployDirectory - absolute path of the deploy directory
     * @param anIsAll - <code>true</code>, if all files are uploaded, also
     *        unchanged files, without asking for files changed in the IFS
     * @param aConflictHandler - asks the user, before files changed in the
     *        IFS are replaced or deleted
     * @param aMonitor - progress monitor
     * @return result
     * @throws InterruptedException if the user canceled the upload
     * @throws Exception if a file could not be uploaded or deleted
     */
    public static Result deploy(AS400 aSystem, IProject aProject, String aConnectionName, String aDeployDirectory, boolean anIsAll,
        ConflictHandler aConflictHandler, IProgressMonitor aMonitor) throws Exception {

        aProject.refreshLocal(IResource.DEPTH_INFINITE, null);

        Map<String, IFile> files = getFiles(aProject, readIgnoreRules(aProject));

        File manifestFile = getManifestFile(aConnectionName, aDeployDirectory, aProject.getName());
        DeployManifest manifest = readManifest(manifestFile);

        List<String> deletes = new ArrayList<String>();
        for (String path : manifest.getPaths()) {
            if (!files.containsKey(path)) {
                deletes.add(path);
            }
        }

        aMonitor.beginTask(Messages.bind(Messages.Task_Uploading_A_to_B, aProject.getName(), aDeployDirectory), deletes.size() + files.size());

        Set<String> directories = new HashSet<String>();
        createDirectory(aSystem, aDeployDirectory, directories);

        IFSFile tokenFile = new IFSFile(aSystem, aDeployDirectory + SEPARATOR + ProjectFiles.DEPLOY_TOKEN_FILE);
        verifyManifest(aSystem, aDeployDirectory, tokenFile, manifest, anIsAll, aConflictHandler);

        // Invalidates the manifests of other workspaces, before the deploy
        // directory is changed.
        String token = UUID.randomUUID().toString();
        RemoteJobs.writeText(tokenFile, token, RemoteJobs.CCSID_UTF8);
        manifest.setToken(token);

        Result result = new Result(aDeployDirectory, files.keySet());
        try {

            for (String path : deletes) {
                checkCanceled(aMonitor);
                aMonitor.subTask(path);
                delete(aSystem, aDeployDirectory, path);
                manifest.removeEntry(path);
                result.deleted++;
                aMonitor.worked(1);
            }

            for (Map.Entry<String, IFile> entry : files.entrySet()) {
                checkCanceled(aMonitor);
                aMonitor.subTask(entry.getKey());
                if (deployFile(aSystem, aDeployDirectory, entry.getKey(), entry.getValue(), manifest, directories)) {
                    result.uploaded++;
                } else {
                    result.unchanged++;
                }
                aMonitor.worked(1);
            }

            manifest.setRemoteFiles(listRemoteFiles(aSystem, aDeployDirectory));

        } finally {
            // Without manifest, the tokens differ and the next upload uploads
            // all files.
            try {
                writeManifest(manifestFile, manifest);
            } catch (IOException e) {
                IfsProjectsPlugin.logError("Could not write the manifest of the upload: " + manifestFile, e); //$NON-NLS-1$
            }
            aMonitor.done();
        }

        return result;
    }

    /**
     * Checks the manifest of the last upload against the deploy directory:
     * <ul>
     * <li>An untrusted manifest, e.g. of another workspace, only tells, which
     * files have been uploaded before and may be deleted. All files are
     * uploaded.</li>
     * <li>The entries of a trusted manifest are removed for the stream files,
     * that are missing or have been changed in the IFS, so that they are
     * uploaded again. The user is asked, before changed files are
     * replaced.</li>
     * </ul>
     *
     * @throws InterruptedException if the user does not want to replace the
     *         changed files
     */
    private static void verifyManifest(AS400 aSystem, String aDeployDirectory, IFSFile aTokenFile, DeployManifest aManifest, boolean anIsAll,
        ConflictHandler aConflictHandler) throws Exception {

        boolean isTrusted = !anIsAll && aManifest.getToken() != null && aManifest.getToken().equals(readToken(aTokenFile));
        if (!isTrusted) {
            aManifest.removeAllEntries();
            return;
        }

        Map<String, DeployManifest.RemoteFile> remoteFiles = listRemoteFiles(aSystem, aDeployDirectory);
        for (String path : aManifest.getMissingRemoteFiles(remoteFiles)) {
            aManifest.removeEntry(path);
        }

        List<String> changedFiles = aManifest.getChangedRemoteFiles(remoteFiles);
        if (!changedFiles.isEmpty()) {
            if (!aConflictHandler.isOverwrite(changedFiles)) {
                throw new InterruptedException();
            }
            for (String path : changedFiles) {
                aManifest.removeEntry(path);
            }
        }
    }

    /**
     * Uploads a file, unless it is unchanged since the last upload, and
     * updates its entry of the manifest.
     *
     * @return <code>true</code>, if the file has been uploaded
     */
    private static boolean deployFile(AS400 aSystem, String aDeployDirectory, String aPath, IFile aFile, DeployManifest aManifest,
        Set<String> aDirectories) throws Exception {

        long size = aFile.getLocation().toFile().length();
        long modified = aFile.getLocalTimeStamp();
        int ccsid = getCcsid(aFile.getCharset());

        DeployManifest.Entry previous = aManifest.getEntry(aPath);
        if (previous != null && previous.isUnchanged(size, modified, ccsid)) {
            return false;
        }

        if (previous != null && previous.getCcsid() == ccsid && previous.getHash().equals(getHash(aFile))) {
            // Touched, but not changed, e.g. by git
            aManifest.putEntry(aPath, new DeployManifest.Entry(size, modified, ccsid, previous.getHash()));
            return false;
        }

        String hash = upload(aSystem, aFile, aDeployDirectory + SEPARATOR + aPath, ccsid, aDirectories);
        aManifest.putEntry(aPath, new DeployManifest.Entry(size, modified, ccsid, hash));

        return true;
    }

    /**
     * Returns the CCSID of a new stream file, that contains text of the
     * specified encoding.
     *
     * @param aCharset - name of the encoding of the local file, e.g.
     *        <code>UTF-8</code> or <code>Cp1252</code>
     * @return CCSID, 1208 (UTF-8) for unknown encodings
     */
    public static int getCcsid(String aCharset) {

        String charset;
        try {
            charset = Charset.forName(aCharset).name();
        } catch (Exception e) {
            return RemoteJobs.CCSID_UTF8;
        }

        if (UTF_8.equals(charset)) {
            return RemoteJobs.CCSID_UTF8;
        }
        if (WINDOWS_1252.equals(charset)) {
            return CCSID_WINDOWS_1252;
        }
        if (ISO_8859_1.equals(charset)) {
            return CCSID_ISO_8859_1;
        }
        if (ISO_8859_15.equals(charset)) {
            return CCSID_ISO_8859_15;
        }
        if (US_ASCII.equals(charset)) {
            return CCSID_US_ASCII;
        }

        return RemoteJobs.CCSID_UTF8;
    }

    /**
     * Returns the name of the manifest file of a project, that is unique for
     * the connection, the deploy directory and the project.
     *
     * @param aConnectionName - name of the RSE connection
     * @param aDeployDirectory - absolute path of the deploy directory
     * @param aProjectName - name of the project
     * @return name, e.g. <code>0f3c...a1.manifest</code>
     */
    public static String getManifestName(String aConnectionName, String aDeployDirectory, String aProjectName) {

        try {
            String key = aConnectionName + KEY_SEPARATOR + aDeployDirectory + KEY_SEPARATOR + aProjectName;
            return toHex(MessageDigest.getInstance(KEY_ALGORITHM).digest(key.getBytes(UTF_8))) + MANIFEST_SUFFIX;
        } catch (Exception e) {
            // SHA-1 and UTF-8 are always supported
            throw new IllegalStateException(e);
        }
    }

    /**
     * Returns the files of a project, that are uploaded: all files except of
     * the excluded files, see {@link ProjectFiles#isExcluded(String)}, and the files of the
     * ignore file, see {@link IgnoreRules}. The project is not refreshed.
     *
     * @param aProject - project
     * @return files by path relative to the project
     * @throws Exception if the files could not be listed or the ignore file
     *         could not be read
     */
    static Map<String, IFile> getFiles(IProject aProject) throws Exception {
        return getFiles(aProject, readIgnoreRules(aProject));
    }

    private static Map<String, IFile> getFiles(IProject aProject, final IgnoreRules anIgnoreRules) throws CoreException {

        final Map<String, IFile> files = new LinkedHashMap<String, IFile>();

        // Team private and hidden resources are skipped by default.
        aProject.accept(new IResourceVisitor() {
            public boolean visit(IResource aResource) throws CoreException {
                if (aResource.getType() == IResource.PROJECT) {
                    return true;
                }
                String path = aResource.getProjectRelativePath().toString();
                boolean isFile = aResource.getType() == IResource.FILE;
                if (ProjectFiles.isExcluded(path) || anIgnoreRules.isIgnored(path, !isFile) || aResource.getLocation() == null) {
                    return false;
                }
                if (isFile) {
                    files.put(path, (IFile)aResource);
                }
                return true;
            }
        }, IResource.DEPTH_INFINITE, IContainer.EXCLUDE_DERIVED);

        return files;
    }

    /**
     * Reads the rules of the ignore file of a project, see
     * {@link ProjectFiles#readIgnoreRules(ProjectFiles.ProjectDirectory)}.
     */
    private static IgnoreRules readIgnoreRules(final IProject aProject) throws Exception {

        return ProjectFiles.readIgnoreRules(new ProjectFiles.ProjectDirectory() {
            public String readFile(String aName) throws Exception {
                IFile file = aProject.getFile(aName);
                return file.exists() ? LocalProject.readText(file) : null;
            }
        });
    }

    private static File getManifestFile(String aConnectionName, String aDeployDirectory, String aProjectName) {

        File directory = IfsProjectsPlugin.getDefault().getStateLocation().append(MANIFEST_DIRECTORY).toFile();

        return new File(directory, getManifestName(aConnectionName, aDeployDirectory, aProjectName));
    }

    /**
     * Reads a manifest. A missing or invalid manifest is empty, so that all
     * files are uploaded.
     */
    private static DeployManifest readManifest(File aFile) {

        if (!aFile.isFile()) {
            return new DeployManifest();
        }

        try {
            InputStream inputStream = new FileInputStream(aFile);
            try {
                return DeployManifest.read(inputStream);
            } finally {
                inputStream.close();
            }
        } catch (Exception e) {
            IfsProjectsPlugin.logError("Could not read the manifest of the upload: " + aFile, e); //$NON-NLS-1$
            return new DeployManifest();
        }
    }

    private static void writeManifest(File aFile, DeployManifest aManifest) throws IOException {

        File directory = aFile.getParentFile();
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new IOException(Messages.bind(Messages.Error_Could_not_create_directory_A, directory));
        }

        OutputStream outputStream = new FileOutputStream(aFile);
        try {
            aManifest.write(outputStream);
        } finally {
            outputStream.close();
        }
    }

    /**
     * Returns the stream files of the deploy directory, except of the
     * excluded files and directories, see {@link ProjectFiles#isExcluded(String)}. The
     * attributes of the files are returned by the list operation.
     *
     * @return files by path relative to the deploy directory
     */
    private static Map<String, DeployManifest.RemoteFile> listRemoteFiles(AS400 aSystem, String aDeployDirectory) throws Exception {

        Map<String, DeployManifest.RemoteFile> files = new HashMap<String, DeployManifest.RemoteFile>();
        listRemoteFiles(new IFSFile(aSystem, aDeployDirectory), "", files); //$NON-NLS-1$

        return files;
    }

    private static void listRemoteFiles(IFSFile aDirectory, String aPrefix, Map<String, DeployManifest.RemoteFile> aFiles) throws Exception {

        IFSFile[] children = aDirectory.listFiles();
        if (children == null) {
            return;
        }

        for (IFSFile child : children) {
            String path = aPrefix + child.getName();
            if (aPrefix.length() == 0 && ProjectFiles.isExcluded(path)) {
                continue;
            }
            if (child.isDirectory()) {
                listRemoteFiles(child, path + SEPARATOR, aFiles);
            } else if (child.isFile()) {
                aFiles.put(path, new DeployManifest.RemoteFile(child.length(), child.lastModified()));
            }
        }
    }

    private static String readToken(IFSFile aFile) throws Exception {

        if (!aFile.exists()) {
            return null;
        }

        return RemoteJobs.readText(aFile).trim();
    }

    private static void createDirectory(AS400 aSystem, String aDirectory, Set<String> aDirectories) throws Exception {

        if (aDirectories.contains(aDirectory)) {
            return;
        }

        IFSFile directory = new IFSFile(aSystem, aDirectory);
        if (!directory.exists() && !directory.mkdirs() && !directory.exists()) {
            throw new IOException(Messages.bind(Messages.Error_Could_not_create_directory_A, aDirectory));
        }

        aDirectories.add(aDirectory);
    }

    /**
     * Uploads a file.
     *
     * @return hash of the uploaded content
     */
    private static String upload(AS400 aSystem, IFile aFile, String aRemotePath, int aCcsid, Set<String> aDirectories) throws Exception {

        createDirectory(aSystem, aRemotePath.substring(0, aRemotePath.lastIndexOf(SEPARATOR)), aDirectories);

        IFSFile remoteFile = new IFSFile(aSystem, aRemotePath);
        if (remoteFile.exists() && remoteFile.getCCSID() != aCcsid) {
            remoteFile.delete();
        }

        MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
        InputStream inputStream = new DigestInputStream(aFile.getContents(true), digest);
        try {
            // Parameters: file, share option, append, CCSID of a new file
            OutputStream outputStream = new IFSFileOutputStream(remoteFile, IFSFileOutputStream.SHARE_NONE, false, aCcsid);
            try {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = inputStream.read(buffer)) >= 0) {
                    outputStream.write(buffer, 0, count);
                }
            } finally {
                outputStream.close();
            }
        } finally {
            inputStream.close();
        }

        return toHex(digest.digest());
    }

    /**
     * Deletes a file of the deploy directory and its parent directories, that
     * are empty then.
     */
    private static void delete(AS400 aSystem, String aDeployDirectory, String aRelativePath) throws Exception {

        IFSFile file = new IFSFile(aSystem, aDeployDirectory + SEPARATOR + aRelativePath);
        if (file.isFile() && !file.delete()) {
            throw new IOException(Messages.bind(Messages.Error_Could_not_delete_A, file.getAbsolutePath()));
        }

        int index = aRelativePath.lastIndexOf(SEPARATOR);
        while (index > 0) {
            String path = aRelativePath.substring(0, index);
            IFSFile directory = new IFSFile(aSystem, aDeployDirectory + SEPARATOR + path);
            String[] names = directory.list();
            if (names == null || names.length > 0 || !directory.delete()) {
                break;
            }
            index = path.lastIndexOf(SEPARATOR);
        }
    }

    private static String getHash(IFile aFile) throws Exception {

        MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
        InputStream inputStream = aFile.getContents(true);
        try {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = inputStream.read(buffer)) >= 0) {
                digest.update(buffer, 0, count);
            }
        } finally {
            inputStream.close();
        }

        return toHex(digest.digest());
    }

    private static String toHex(byte[] aBytes) {

        StringBuilder buffer = new StringBuilder(aBytes.length * 2);
        for (byte b : aBytes) {
            buffer.append(Character.forDigit((b >> 4) & 0x0F, 16)).append(Character.forDigit(b & 0x0F, 16));
        }

        return buffer.toString();
    }

    private static void checkCanceled(IProgressMonitor aMonitor) throws InterruptedException {
        if (aMonitor.isCanceled()) {
            throw new InterruptedException();
        }
    }

    /**
     * Asks the user, before files changed in the IFS are replaced or deleted.
     */
    public interface ConflictHandler {

        /**
         * Asks, whether the files changed in the IFS since the last upload are
         * replaced or deleted. Called on the thread of the upload.
         *
         * @param aPaths - paths relative to the deploy directory
         * @return <code>true</code> to replace or delete the files,
         *         <code>false</code> to cancel the upload
         */
        boolean isOverwrite(List<String> aPaths);
    }

    /**
     * Result of an upload.
     */
    public static final class Result {

        private final String directory;
        private final Set<String> paths;
        private int uploaded;
        private int deleted;
        private int unchanged;

        private Result(String aDirectory, Set<String> aPaths) {
            this.directory = aDirectory;
            this.paths = Collections.unmodifiableSet(new HashSet<String>(aPaths));
        }

        /**
         * Returns the deploy directory.
         */
        public String getDirectory() {
            return directory;
        }

        /**
         * Returns <code>true</code>, if a file of the project is part of the
         * upload, that means, it is not excluded or ignored.
         *
         * @param aRelativePath - path relative to the project directory
         * @return <code>true</code>, if the file is in the deploy directory
         */
        public boolean isDeployed(String aRelativePath) {
            return paths.contains(aRelativePath);
        }

        /**
         * Returns the number of uploaded files.
         */
        public int getUploaded() {
            return uploaded;
        }

        /**
         * Returns the number of deleted files.
         */
        public int getDeleted() {
            return deleted;
        }

        /**
         * Returns the number of unchanged files, that have not been uploaded.
         */
        public int getUnchanged() {
            return unchanged;
        }
    }
}
