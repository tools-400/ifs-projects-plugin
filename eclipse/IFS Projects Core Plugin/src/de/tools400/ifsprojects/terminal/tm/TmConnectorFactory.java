/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.terminal.tm;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.tm.internal.terminal.connector.TerminalConnector;
import org.eclipse.tm.internal.terminal.provisional.api.ITerminalConnector;
import org.eclipse.tm.internal.terminal.provisional.api.provider.TerminalConnectorImpl;

import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.terminal.TerminalException;

/**
 * Creates the SSH connector of the TM terminal.
 * <p>
 * The connector is created from the extension registry, because
 * <code>TerminalConnectorExtension</code> does not work in all RDi versions
 * (RDi 9.9 reads the wrong extension point).
 */
@SuppressWarnings("restriction")
final class TmConnectorFactory {

    /** Settings keys of the SSH connector (SshSettings.load()). */
    static final String SETTING_HOST = "Host"; //$NON-NLS-1$
    static final String SETTING_USER = "User"; //$NON-NLS-1$
    static final String SETTING_PASSWORD = "Password"; //$NON-NLS-1$
    static final String SETTING_PORT = "Port"; //$NON-NLS-1$
    static final String SETTING_TIMEOUT = "Timeout"; //$NON-NLS-1$
    static final String SETTING_KEEP_ALIVE = "Keepalive"; //$NON-NLS-1$

    /** ID of the SSH connector of the new TM terminal (RDi 9.8). */
    static final String SSH_CONNECTOR_ID = "org.eclipse.tm.terminal.connector.ssh.SshConnector"; //$NON-NLS-1$

    /** ID of the SSH connector of the old TM terminal (RDi 9.6, 9.9). */
    static final String OLD_SSH_CONNECTOR_ID = "org.eclipse.tm.internal.terminal.ssh.SshConnector"; //$NON-NLS-1$

    private static final String ATTRIBUTE_NAME = "name"; //$NON-NLS-1$
    private static final String ATTRIBUTE_CLASS = "class"; //$NON-NLS-1$

    private TmConnectorFactory() {
        // Utility class
    }

    /**
     * Creates a new SSH connector.
     *
     * @return SSH connector
     * @throws TerminalException if the SSH connector is not installed
     */
    static ITerminalConnector createSshConnector() throws TerminalException {

        final IConfigurationElement element = TmTerminalLauncher.findSshConnectorElement();
        if (element == null) {
            throw new TerminalException(Messages.Error_No_SSH_connector_found);
        }

        TerminalConnector.Factory factory = new TerminalConnector.Factory() {
            public TerminalConnectorImpl makeConnector() throws Exception {
                return (TerminalConnectorImpl)element.createExecutableExtension(ATTRIBUTE_CLASS);
            }
        };

        return new TerminalConnector(factory, element.getAttribute(TmTerminalLauncher.ATTRIBUTE_ID), element.getAttribute(ATTRIBUTE_NAME), true);
    }

    /**
     * Returns <code>true</code>, if the SSH connector requests the terminal
     * type <code>ansi</code> for the shell, like the SSH connector of the old TM
     * terminal does. IBM i PASE does not know it, so that e.g.
     * <code>clear</code> does nothing. The SSH connector of the new TM terminal
     * requests <code>xterm</code>.
     *
     * @param aConnector - SSH connector
     * @return <code>true</code>, if the terminal type is <code>ansi</code>
     */
    static boolean isAnsiTerminalType(ITerminalConnector aConnector) {
        return OLD_SSH_CONNECTOR_ID.equals(aConnector.getId());
    }
}
