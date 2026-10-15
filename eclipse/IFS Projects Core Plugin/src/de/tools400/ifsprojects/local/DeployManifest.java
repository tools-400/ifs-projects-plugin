/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.local;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/**
 * Files of a local project, that have been uploaded to a deploy directory,
 * see {@link Deployment}. A file is uploaded again, if it has changed since.
 * Only files of the manifest are deleted in the deploy directory.
 * <p>
 * The token of the manifest is also written to the deploy directory. If the
 * tokens differ, e.g. because another workspace or another tool has changed
 * the deploy directory, the manifest is not trusted and all files are
 * uploaded.
 * <p>
 * The size and modification time of the stream files after the upload show,
 * whether a file has been changed in the IFS since, see
 * {@link #getChangedRemoteFiles(Map)}.
 */
public final class DeployManifest {

    private static final String TOKEN = "token"; //$NON-NLS-1$
    private static final String FILE_PREFIX = "file."; //$NON-NLS-1$
    private static final String SEPARATOR = ","; //$NON-NLS-1$
    private static final String COMMENT = "IFS Projects: files uploaded to the deploy directory"; //$NON-NLS-1$

    private String token;
    private final Map<String, Entry> entries = new TreeMap<String, Entry>();

    /**
     * Reads a manifest. Invalid entries are skipped.
     *
     * @param anInputStream - stream of the properties of the manifest
     * @return manifest
     * @throws IOException if the stream could not be read
     */
    public static DeployManifest read(InputStream anInputStream) throws IOException {

        Properties properties = new Properties();
        properties.load(anInputStream);

        DeployManifest manifest = new DeployManifest();
        manifest.token = properties.getProperty(TOKEN);

        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith(FILE_PREFIX) && key.length() > FILE_PREFIX.length()) {
                Entry entry = Entry.parse(properties.getProperty(key));
                if (entry != null) {
                    manifest.entries.put(key.substring(FILE_PREFIX.length()), entry);
                }
            }
        }

        return manifest;
    }

    /**
     * Writes the manifest as properties.
     *
     * @param anOutputStream - stream
     * @throws IOException if the stream could not be written
     */
    public void write(OutputStream anOutputStream) throws IOException {

        Properties properties = new Properties();
        if (token != null) {
            properties.setProperty(TOKEN, token);
        }
        for (Map.Entry<String, Entry> entry : entries.entrySet()) {
            properties.setProperty(FILE_PREFIX + entry.getKey(), entry.getValue().format());
        }

        properties.store(anOutputStream, COMMENT);
    }

    /**
     * Returns the token, that has been written to the deploy directory with
     * the last upload, or <code>null</code>.
     */
    public String getToken() {
        return token;
    }

    public void setToken(String aToken) {
        this.token = aToken;
    }

    /**
     * Returns the entry of a file.
     *
     * @param aRelativePath - path relative to the project directory
     * @return entry or <code>null</code>, if the file has not been uploaded
     */
    public Entry getEntry(String aRelativePath) {
        return entries.get(aRelativePath);
    }

    public void putEntry(String aRelativePath, Entry anEntry) {
        entries.put(aRelativePath, anEntry);
    }

    public void removeEntry(String aRelativePath) {
        entries.remove(aRelativePath);
    }

    public void removeAllEntries() {
        entries.clear();
    }

    /**
     * Returns the paths of the uploaded files, sorted.
     */
    public List<String> getPaths() {
        return new ArrayList<String>(entries.keySet());
    }

    /**
     * Returns the uploaded files, that have been changed in the IFS since the
     * upload: their size or modification time differs. Files without size and
     * modification time of the stream file are not checked.
     *
     * @param aRemoteFiles - stream files of the deploy directory by path
     *        relative to the deploy directory
     * @return paths, sorted
     */
    public List<String> getChangedRemoteFiles(Map<String, RemoteFile> aRemoteFiles) {

        List<String> paths = new ArrayList<String>();
        for (Map.Entry<String, Entry> entry : entries.entrySet()) {
            RemoteFile remoteFile = aRemoteFiles.get(entry.getKey());
            if (remoteFile != null && entry.getValue().hasRemoteFile() && !entry.getValue().isRemoteUnchanged(remoteFile)) {
                paths.add(entry.getKey());
            }
        }

        return paths;
    }

    /**
     * Returns the uploaded files, that do not exist in the IFS any more.
     *
     * @param aRemoteFiles - stream files of the deploy directory by path
     *        relative to the deploy directory
     * @return paths, sorted
     */
    public List<String> getMissingRemoteFiles(Map<String, RemoteFile> aRemoteFiles) {

        List<String> paths = new ArrayList<String>();
        for (String path : entries.keySet()) {
            if (!aRemoteFiles.containsKey(path)) {
                paths.add(path);
            }
        }

        return paths;
    }

    /**
     * Sets the size and modification time of the stream files of the entries.
     *
     * @param aRemoteFiles - stream files of the deploy directory by path
     *        relative to the deploy directory
     */
    public void setRemoteFiles(Map<String, RemoteFile> aRemoteFiles) {

        for (Map.Entry<String, Entry> entry : entries.entrySet()) {
            RemoteFile remoteFile = aRemoteFiles.get(entry.getKey());
            if (remoteFile != null) {
                entry.setValue(entry.getValue().withRemoteFile(remoteFile));
            }
        }
    }

    /**
     * Size and modification time of a stream file.
     */
    public static final class RemoteFile {

        private final long size;
        private final long modified;

        public RemoteFile(long aSize, long aModified) {
            this.size = aSize;
            this.modified = aModified;
        }

        public long getSize() {
            return size;
        }

        public long getModified() {
            return modified;
        }
    }

    /**
     * Uploaded file: size, modification time and encoding of the local file,
     * hash of its content and size and modification time of the stream file
     * after the upload.
     */
    public static final class Entry {

        private static final long UNKNOWN = -1;

        private final long size;
        private final long modified;
        private final int ccsid;
        private final String hash;
        private final long remoteSize;
        private final long remoteModified;

        public Entry(long aSize, long aModified, int aCcsid, String aHash) {
            this(aSize, aModified, aCcsid, aHash, UNKNOWN, UNKNOWN);
        }

        private Entry(long aSize, long aModified, int aCcsid, String aHash, long aRemoteSize, long aRemoteModified) {
            this.size = aSize;
            this.modified = aModified;
            this.ccsid = aCcsid;
            this.hash = aHash;
            this.remoteSize = aRemoteSize;
            this.remoteModified = aRemoteModified;
        }

        /**
         * Returns a copy of the entry with the size and modification time of
         * the stream file.
         */
        public Entry withRemoteFile(RemoteFile aRemoteFile) {
            return new Entry(size, modified, ccsid, hash, aRemoteFile.getSize(), aRemoteFile.getModified());
        }

        /**
         * Returns <code>true</code>, if the size and modification time of the
         * stream file are known.
         */
        public boolean hasRemoteFile() {
            return remoteSize != UNKNOWN && remoteModified != UNKNOWN;
        }

        /**
         * Returns <code>true</code>, if the stream file has the same size and
         * modification time as after the upload.
         */
        public boolean isRemoteUnchanged(RemoteFile aRemoteFile) {
            return remoteSize == aRemoteFile.getSize() && remoteModified == aRemoteFile.getModified();
        }

        public long getRemoteSize() {
            return remoteSize;
        }

        public long getRemoteModified() {
            return remoteModified;
        }

        public long getSize() {
            return size;
        }

        public long getModified() {
            return modified;
        }

        public int getCcsid() {
            return ccsid;
        }

        public String getHash() {
            return hash;
        }

        /**
         * Returns <code>true</code>, if the local file has the same size,
         * modification time and CCSID as when it was uploaded. Then it is not
         * read again.
         */
        public boolean isUnchanged(long aSize, long aModified, int aCcsid) {
            return size == aSize && modified == aModified && ccsid == aCcsid;
        }

        private String format() {
            return size + SEPARATOR + modified + SEPARATOR + ccsid + SEPARATOR + hash + SEPARATOR + remoteSize + SEPARATOR + remoteModified;
        }

        /**
         * Parses an entry. Entries of the first version have no size and
         * modification time of the stream file.
         */
        private static Entry parse(String aValue) {

            String[] fields = aValue == null ? new String[0] : aValue.split(SEPARATOR);
            if ((fields.length != 4 && fields.length != 6) || fields[3].length() == 0) {
                return null;
            }

            try {
                Entry entry = new Entry(Long.parseLong(fields[0]), Long.parseLong(fields[1]), Integer.parseInt(fields[2]), fields[3]);
                if (fields.length == 6) {
                    entry = new Entry(entry.size, entry.modified, entry.ccsid, entry.hash, Long.parseLong(fields[4]), Long.parseLong(fields[5]));
                }
                return entry;
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }
}
