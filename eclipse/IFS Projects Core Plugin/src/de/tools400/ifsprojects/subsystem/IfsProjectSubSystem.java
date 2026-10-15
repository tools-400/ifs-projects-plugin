/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.subsystem;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.rse.core.filters.ISystemFilterString;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.core.model.ISystemMessageObject;
import org.eclipse.rse.core.model.SystemMessageObject;
import org.eclipse.rse.core.subsystems.IConnectorService;
import org.eclipse.rse.core.subsystems.ISubSystem;
import org.eclipse.rse.core.subsystems.SubSystem;
import org.eclipse.rse.services.clientserver.messages.SimpleSystemMessage;

import com.ibm.etools.iseries.connectorservice.IToolboxSubSystem;

import de.tools400.ifsprojects.IfsProjectsPlugin;
import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.rse.IfsFilterStrings;

/**
 * The 'IFS Projects' subsystem of an IBM i connection. Each filter is an IFS
 * project. The contents of a project are retrieved from IBM's IFS subsystem
 * of the same connection. Therefore the files and directories are IBM's
 * IFSRemoteFile objects, which keep all actions of IBM.
 */
public class IfsProjectSubSystem extends SubSystem implements IToolboxSubSystem {

    private static final char SEPARATOR = '/';

    public IfsProjectSubSystem(IHost aHost, IConnectorService aConnectorService) {
        super(aHost, aConnectorService);
    }

    @Override
    protected Object[] internalResolveFilterString(String aFilterString, IProgressMonitor aMonitor) throws InvocationTargetException,
        InterruptedException {

        ISubSystem ifsSubSystem = IfsProjects.getIfsFileSubSystem(getHost());
        if (ifsSubSystem == null) {
            return createErrorMessage(Messages.bind(Messages.Error_IFS_subsystem_not_found_A, getHost().getAliasName()));
        }

        try {
            return ifsSubSystem.resolveFilterString(aFilterString, aMonitor);
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            IfsProjectsPlugin.logError("Could not list the contents of IFS project: " + aFilterString, e); //$NON-NLS-1$
            return createErrorMessage(e.getLocalizedMessage());
        }
    }

    /**
     * Returns <code>true</code>, if the remote object is listed by the filter
     * string, i.e. if its parent directory is the project directory.
     */
    @Override
    public boolean doesFilterStringMatch(String aFilterString, String aRemoteObjectAbsoluteName, boolean aCaseSensitive) {

        String directory = IfsFilterStrings.getDirectory(aFilterString);
        String parentDirectory = getParentDirectory(aRemoteObjectAbsoluteName);
        if (directory == null || parentDirectory == null) {
            return false;
        }

        if (aCaseSensitive) {
            return directory.equals(parentDirectory);
        }

        return directory.equalsIgnoreCase(parentDirectory);
    }

    /**
     * Returns <code>true</code>, if the filter string lists the contents of the
     * specified directory, i.e. if it is the project directory.
     */
    @Override
    public boolean doesFilterStringListContentsOf(ISystemFilterString aFilterString, String aRemoteObjectAbsoluteName) {

        String directory = IfsFilterStrings.getDirectory(aFilterString.getString());
        String remoteDirectory = IfsFilterStrings.normalizeDirectory(aRemoteObjectAbsoluteName);
        if (directory == null || remoteDirectory == null) {
            return false;
        }

        return directory.equals(remoteDirectory);
    }

    @Override
    public boolean isPrimarySubSystem() {
        return false;
    }

    private Object[] createErrorMessage(String aMessage) {
        SimpleSystemMessage message = new SimpleSystemMessage(IfsProjectsPlugin.PLUGIN_ID, IStatus.ERROR, aMessage);
        return new Object[] { new SystemMessageObject(message, ISystemMessageObject.MSGTYPE_ERROR, null) };
    }

    private String getParentDirectory(String anAbsoluteName) {

        String path = IfsFilterStrings.normalizeDirectory(anAbsoluteName);
        if (path == null || path.length() <= 1) {
            return null;
        }

        int index = path.lastIndexOf(SEPARATOR);
        if (index == 0) {
            return String.valueOf(SEPARATOR);
        }

        return path.substring(0, index);
    }
}
