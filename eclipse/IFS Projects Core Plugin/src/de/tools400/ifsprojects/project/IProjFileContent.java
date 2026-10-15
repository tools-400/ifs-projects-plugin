/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.project;

/**
 * Content of an <code>iproj.json</code> file, as read by
 * {@link IProjFileStore}.
 */
public final class IProjFileContent {

    private final String path;
    private final boolean exists;
    private final String content;
    private final int ccsid;
    private final long lastModified;

    private IProjFileContent(String aPath, boolean anExists, String aContent, int aCcsid, long aLastModified) {
        this.path = aPath;
        this.exists = anExists;
        this.content = aContent;
        this.ccsid = aCcsid;
        this.lastModified = aLastModified;
    }

    /**
     * Returns the content of a file, that does not exist.
     *
     * @param aPath - path of the file
     * @return content
     */
    public static IProjFileContent notExisting(String aPath) {
        return new IProjFileContent(aPath, false, null, 0, 0);
    }

    /**
     * Returns the content of an existing file.
     *
     * @param aPath - path of the file
     * @param aContent - text of the file
     * @param aCcsid - CCSID of the file
     * @param aLastModified - modification time, used to detect changes by
     *        others
     * @return content
     */
    public static IProjFileContent existing(String aPath, String aContent, int aCcsid, long aLastModified) {
        return new IProjFileContent(aPath, true, aContent, aCcsid, aLastModified);
    }

    public String getPath() {
        return path;
    }

    public boolean exists() {
        return exists;
    }

    /**
     * Returns the content of the file or <code>null</code>, if the file does
     * not exist.
     */
    public String getContent() {
        return content;
    }

    public int getCcsid() {
        return ccsid;
    }

    public long getLastModified() {
        return lastModified;
    }
}
