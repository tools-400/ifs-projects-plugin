/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.terminal.tm;

import org.eclipse.ui.IMemento;

import de.tools400.ifsprojects.IfsProjectsPlugin;
import de.tools400.ifsprojects.terminal.TerminalRequest;

/**
 * Saves the terminal request of a session in the memento of the
 * {@link TmTerminalView} and restores it, when the view is opened again.
 * Password and command are never saved.
 */
public final class TerminalRequestMemento {

    private static final String CONNECTION_NAME = "connectionName"; //$NON-NLS-1$
    private static final String HOST_NAME = "hostName"; //$NON-NLS-1$
    private static final String PORT = "port"; //$NON-NLS-1$
    private static final String USER_ID = "userId"; //$NON-NLS-1$
    private static final String ENCODING = "encoding"; //$NON-NLS-1$
    private static final String TIMEOUT = "timeout"; //$NON-NLS-1$
    private static final String KEEP_ALIVE = "keepAlive"; //$NON-NLS-1$
    private static final String DIRECTORY = "directory"; //$NON-NLS-1$
    private static final String REUSE_KEY = "reuseKey"; //$NON-NLS-1$

    private TerminalRequestMemento() {
        // Utility class
    }

    /**
     * Saves a request.
     *
     * @param aMemento - memento of the session
     * @param aRequest - request of the session
     */
    public static void save(IMemento aMemento, TerminalRequest aRequest) {

        aMemento.putString(CONNECTION_NAME, aRequest.getConnectionName());
        aMemento.putString(HOST_NAME, aRequest.getHostName());
        aMemento.putInteger(PORT, aRequest.getPort());
        aMemento.putString(USER_ID, aRequest.getUserId());
        aMemento.putString(ENCODING, aRequest.getEncoding());
        aMemento.putInteger(TIMEOUT, aRequest.getTimeout());
        aMemento.putInteger(KEEP_ALIVE, aRequest.getKeepAlive());
        if (aRequest.getDirectory() != null) {
            aMemento.putString(DIRECTORY, aRequest.getDirectory());
        }
        // A restored build terminal is reused, see
        // TmTerminalView.findReconnectableSession().
        if (aRequest.getReuseKey() != null) {
            aMemento.putString(REUSE_KEY, aRequest.getReuseKey());
        }
    }

    /**
     * Restores a request. Errors are logged.
     *
     * @param aMemento - memento of the session
     * @return request or <code>null</code>, if the memento is incomplete
     */
    public static TerminalRequest restore(IMemento aMemento) {

        try {

            TerminalRequest.Builder builder = TerminalRequest.builder();
            builder.connectionName(aMemento.getString(CONNECTION_NAME));
            builder.hostName(aMemento.getString(HOST_NAME));
            builder.userId(aMemento.getString(USER_ID));
            builder.directory(aMemento.getString(DIRECTORY));
            builder.reuseKey(aMemento.getString(REUSE_KEY));

            String encoding = aMemento.getString(ENCODING);
            if (encoding != null) {
                builder.encoding(encoding);
            }

            Integer port = aMemento.getInteger(PORT);
            if (port != null) {
                builder.port(port.intValue());
            }

            Integer timeout = aMemento.getInteger(TIMEOUT);
            if (timeout != null) {
                builder.timeout(timeout.intValue());
            }

            Integer keepAlive = aMemento.getInteger(KEEP_ALIVE);
            if (keepAlive != null) {
                builder.keepAlive(keepAlive.intValue());
            }

            return builder.build();

        } catch (IllegalStateException e) {
            IfsProjectsPlugin.logWarning("Could not restore terminal session: " + e.getLocalizedMessage()); //$NON-NLS-1$
            return null;
        }
    }
}
