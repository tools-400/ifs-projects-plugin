/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.rse;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.rse.core.filters.ISystemFilterReference;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.core.subsystems.ISubSystem;
import org.eclipse.rse.subsystems.files.core.subsystems.IRemoteFile;

/**
 * Retrieves host and directory from objects selected in the Remote Systems
 * view.
 */
public final class RseSelections {

    private static final String ROOT = "/"; //$NON-NLS-1$

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
}
