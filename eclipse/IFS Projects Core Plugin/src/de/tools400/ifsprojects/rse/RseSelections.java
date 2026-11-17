/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.rse;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.rse.core.filters.ISystemFilterReference;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.core.subsystems.ISubSystem;
import org.eclipse.rse.subsystems.files.core.subsystems.IRemoteFile;

/**
 * Retrieves host, directory and resource of the workspace from objects selected
 * in the Remote Systems view.
 */
public final class RseSelections {

    private static final String ROOT = "/"; //$NON-NLS-1$

    /** Separator of the segments of a path of the workspace. */
    private static final String SEPARATOR = "/"; //$NON-NLS-1$

    private RseSelections() {
        // Utility class
    }

    /**
     * Returns the selected element, if exactly one element is selected.
     *
     * @param aSelection - current selection
     * @return selected element or <code>null</code>
     */
    public static Object getSingleElement(ISelection aSelection) {

        if (!(aSelection instanceof IStructuredSelection)) {
            return null;
        }

        IStructuredSelection structuredSelection = (IStructuredSelection)aSelection;
        if (structuredSelection.size() != 1) {
            return null;
        }

        return structuredSelection.getFirstElement();
    }

    /**
     * Returns the filter reference of an element, e.g. of the element of a
     * property page.
     *
     * @param anElement - filter reference or element, that adapts to a filter
     *        reference
     * @return filter reference or <code>null</code>
     */
    public static ISystemFilterReference getFilterReference(Object anElement) {

        if (anElement instanceof ISystemFilterReference) {
            return (ISystemFilterReference)anElement;
        }

        if (anElement instanceof IAdaptable) {
            Object adapter = ((IAdaptable)anElement).getAdapter(ISystemFilterReference.class);
            if (adapter instanceof ISystemFilterReference) {
                return (ISystemFilterReference)adapter;
            }
        }

        return null;
    }

    /**
     * Returns the RSE connection of a selected element.
     *
     * @param anElement - connection, subsystem, filter reference or remote file
     * @return host or <code>null</code>
     */
    public static IHost getHost(Object anElement) {

        if (anElement instanceof IHost) {
            return (IHost)anElement;
        }

        if (anElement instanceof IRemoteFile) {
            return ((IRemoteFile)anElement).getHost();
        }

        if (anElement instanceof ISystemFilterReference) {
            ISubSystem subSystem = ((ISystemFilterReference)anElement).getSubSystem();
            if (subSystem != null) {
                return subSystem.getHost();
            }
        }

        if (anElement instanceof ISubSystem) {
            return ((ISubSystem)anElement).getHost();
        }

        return null;
    }

    /**
     * Returns the directory of a selected element:
     * <ul>
     * <li>directory: the directory itself</li>
     * <li>file: the parent directory</li>
     * <li>filter: the directory of the IFS project or filter string</li>
     * </ul>
     *
     * @param anElement - remote file or filter reference
     * @return directory or <code>null</code>
     */
    public static String getDirectory(Object anElement) {

        if (anElement instanceof IRemoteFile) {
            IRemoteFile remoteFile = (IRemoteFile)anElement;
            if (remoteFile.isDirectory()) {
                return remoteFile.getAbsolutePath();
            }
            String parentPath = remoteFile.getParentPath();
            if (parentPath == null || parentPath.length() == 0) {
                return ROOT;
            }
            return parentPath;
        }

        if (anElement instanceof ISystemFilterReference) {
            return IfsFilterStrings.getDirectory(((ISystemFilterReference)anElement).getReferencedFilter().getFilterStrings());
        }

        return null;
    }

    /**
     * Returns the directory of a selected element of the subsystem 'Local
     * Files' of RSE:
     * <ul>
     * <li>directory: the directory itself</li>
     * <li>filter: the directory of its filter string</li>
     * </ul>
     * The file system is not accessed.
     *
     * @param anElement - remote file or filter reference
     * @return local directory or <code>null</code>, e.g. for a file, an IFS
     *         directory or a filter with several filter strings
     */
    public static File getLocalDirectory(Object anElement) {

        String directory = null;

        if (anElement instanceof IRemoteFile) {
            IRemoteFile remoteFile = (IRemoteFile)anElement;
            if (remoteFile.isDirectory() && isLocalFiles(remoteFile.getParentRemoteFileSubSystem())) {
                directory = remoteFile.getAbsolutePath();
            }
        } else if (anElement instanceof ISystemFilterReference) {
            ISystemFilterReference filterReference = (ISystemFilterReference)anElement;
            if (isLocalFiles(filterReference.getSubSystem())) {
                directory = LocalFilterStrings.getDirectory(filterReference.getReferencedFilter().getFilterStrings());
            }
        }

        if (directory == null || !new File(directory).isAbsolute()) {
            return null;
        }

        return new File(directory);
    }

    /**
     * Returns the resource of the workspace, that is located at a selected
     * element of the subsystem 'Local Files' of RSE:
     * <ul>
     * <li>directory: the project, whose location it is, or the folder of a
     * project</li>
     * <li>file: the file of a project</li>
     * <li>filter: the project or folder of the directory of its filter
     * string</li>
     * </ul>
     * If projects are nested, the resource of the innermost project is
     * returned. Only open projects are considered. The resource does not need
     * to exist in the workspace, e.g. if the file has been created outside of
     * Eclipse.
     *
     * @param anElement - remote file or filter reference
     * @return resource or <code>null</code>, if the element is not located in
     *         an open project of the workspace
     */
    public static IResource getLocalResource(Object anElement) {

        File directory = getLocalDirectory(anElement);
        if (directory != null) {
            return findResource(directory, true);
        }

        if (anElement instanceof IRemoteFile) {
            IRemoteFile remoteFile = (IRemoteFile)anElement;
            if (remoteFile.isFile() && isLocalFiles(remoteFile.getParentRemoteFileSubSystem())) {
                File file = new File(remoteFile.getAbsolutePath());
                if (file.isAbsolute()) {
                    return findResource(file, false);
                }
            }
        }

        return null;
    }

    private static IResource findResource(File aFile, boolean anIsDirectory) {

        IProject innermostProject = null;
        String innermostPath = null;

        for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
            IPath location = project.isOpen() ? project.getLocation() : null;
            if (location != null) {
                String relativePath = getRelativePath(location.toFile(), aFile);
                if (relativePath != null && (innermostPath == null || relativePath.length() < innermostPath.length())) {
                    innermostProject = project;
                    innermostPath = relativePath;
                }
            }
        }

        if (innermostProject == null) {
            return null;
        }

        if (innermostPath.length() == 0) {
            return anIsDirectory ? innermostProject : null;
        }

        if (anIsDirectory) {
            return innermostProject.getFolder(innermostPath);
        }

        return innermostProject.getFile(innermostPath);
    }

    /**
     * Returns the path of a file relative to a directory. The names are
     * compared like {@link File#equals(Object)} does, that is ignoring the case
     * on Windows.
     *
     * @return relative path, an empty string for the directory itself or
     *         <code>null</code>, if the file is not located in the directory
     */
    private static String getRelativePath(File aDirectory, File aFile) {

        String relativePath = ""; //$NON-NLS-1$

        File file = aFile;
        while (file != null) {
            if (file.equals(aDirectory)) {
                return relativePath;
            }
            relativePath = relativePath.length() == 0 ? file.getName() : file.getName() + SEPARATOR + relativePath;
            file = file.getParentFile();
        }

        return null;
    }

    private static boolean isLocalFiles(ISubSystem aSubSystem) {
        return aSubSystem != null && LocalFilterStrings.SUBSYSTEM_CONFIGURATION_ID.equals(aSubSystem.getConfigurationId());
    }
}
