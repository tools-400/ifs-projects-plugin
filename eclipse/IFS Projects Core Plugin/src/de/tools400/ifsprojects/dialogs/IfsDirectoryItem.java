/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.dialogs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.IFSFile;
import com.ibm.as400.access.IFSFileFilter;

import de.tools400.ifsprojects.remote.RemoteJobs;

/**
 * Directory of the IFS, as shown in the {@link SelectIfsDirectoryDialog}. The
 * sub-directories are retrieved, when they are requested for the first time.
 * <p>
 * Based on <code>DirectoryItem</code> of iSphere.
 */
class IfsDirectoryItem {

    static final String ROOT_DIRECTORY = "/"; //$NON-NLS-1$

    private final AS400 system;
    private final String name;
    private final String path;
    private final boolean isError;

    private IfsDirectoryItem[] children;

    private IfsDirectoryItem(AS400 aSystem, String aName, String aPath, boolean anIsError) {
        this.system = aSystem;
        this.name = aName;
        this.path = aPath;
        this.isError = anIsError;
    }

    static IfsDirectoryItem createRootDirectory(AS400 aSystem) {
        return new IfsDirectoryItem(aSystem, ROOT_DIRECTORY, ROOT_DIRECTORY, false);
    }

    private static IfsDirectoryItem createError(String aMessage) {
        return new IfsDirectoryItem(null, aMessage, null, true);
    }

    String getName() {
        return name;
    }

    /**
     * Returns the absolute path of the directory or <code>null</code> for an
     * error item.
     */
    String getPath() {
        return path;
    }

    boolean isError() {
        return isError;
    }

    boolean hasChildren() {
        return !isError;
    }

    IfsDirectoryItem[] getChildren() {

        if (children == null) {
            children = resolveChildren();
        }

        return children;
    }

    private IfsDirectoryItem[] resolveChildren() {

        if (isError) {
            return new IfsDirectoryItem[0];
        }

        List<IfsDirectoryItem> items = new ArrayList<IfsDirectoryItem>();

        try {

            IFSFile[] directories = RemoteJobs.listFiles(new IFSFile(system, path), new IFSFileFilter() {
                public boolean accept(IFSFile aFile) {
                    try {
                        return aFile.isDirectory();
                    } catch (Exception e) {
                        return false;
                    }
                }
            });

            if (directories != null) {
                Arrays.sort(directories, new Comparator<IFSFile>() {
                    public int compare(IFSFile aFile1, IFSFile aFile2) {
                        return aFile1.getName().compareToIgnoreCase(aFile2.getName());
                    }
                });

                for (IFSFile directory : directories) {
                    items.add(new IfsDirectoryItem(system, directory.getName(), directory.getAbsolutePath(), false));
                }
            }

        } catch (Exception e) {
            items.add(createError(e.getLocalizedMessage()));
        }

        return items.toArray(new IfsDirectoryItem[items.size()]);
    }
}
