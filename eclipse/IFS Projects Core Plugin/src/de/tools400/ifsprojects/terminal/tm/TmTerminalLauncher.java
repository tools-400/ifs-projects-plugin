/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.terminal.tm;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.terminal.ITerminalCommandLine;
import de.tools400.ifsprojects.terminal.ITerminalLauncher;
import de.tools400.ifsprojects.terminal.TerminalException;
import de.tools400.ifsprojects.terminal.TerminalRequest;

/**
 * Terminal backend for the TM terminal that is shipped with RDi 9.6, 9.8 and
 * 9.9 (<code>org.eclipse.tm.terminal</code> and
 * <code>org.eclipse.tm.terminal.control</code>). All sessions are hosted by
 * one {@link TmTerminalView}.
 * <p>
 * This class must not reference TM classes in its signature. It is loaded
 * before it is known, whether the TM bundles are installed.
 */
public class TmTerminalLauncher implements ITerminalLauncher {

    static final String ATTRIBUTE_ID = "id"; //$NON-NLS-1$

    private static final String BUNDLE_TM_TERMINAL_CONTROL = "org.eclipse.tm.terminal.control"; //$NON-NLS-1$
    private static final String BUNDLE_TM_TERMINAL = "org.eclipse.tm.terminal"; //$NON-NLS-1$

    /**
     * Pairs of extension point ID and SSH connector ID.
     * <ul>
     * <li>RDi 9.8: new TM terminal (<code>org.eclipse.tm.terminal.control</code>)</li>
     * <li>RDi 9.6, 9.9: old TM terminal (<code>org.eclipse.tm.terminal</code>)</li>
     * </ul>
     */
    private static final String[][] SSH_CONNECTORS = { //
        { "org.eclipse.tm.terminal.control.connectors", "org.eclipse.tm.terminal.connector.ssh.SshConnector" }, //$NON-NLS-1$ //$NON-NLS-2$
        { "org.eclipse.tm.terminal.terminalConnectors", "org.eclipse.tm.internal.terminal.ssh.SshConnector" } }; //$NON-NLS-1$ //$NON-NLS-2$

    public boolean isAvailable() {

        if (Platform.getBundle(BUNDLE_TM_TERMINAL_CONTROL) == null && Platform.getBundle(BUNDLE_TM_TERMINAL) == null) {
            return false;
        }

        return findSshConnectorElement() != null;
    }

    public ITerminalCommandLine launch(TerminalRequest aRequest) throws TerminalException {

        IWorkbenchPage page = getActivePage();
        if (page == null) {
            throw new TerminalException(Messages.Error_No_active_workbench_page);
        }

        TmTerminalView view;
        try {
            view = (TmTerminalView)page.showView(TmTerminalView.ID);
        } catch (PartInitException e) {
            throw new TerminalException(e.getLocalizedMessage(), e);
        }

        if (!aRequest.isForceNew()) {
            if (aRequest.getReuseKey() != null) {
                TerminalSession session = view.findReusableSession(aRequest.getReuseKey());
                if (session != null) {
                    return toCommandLine(aRequest, view.sendCommandLine(session, aRequest));
                }
                session = view.findReconnectableSession(aRequest.getReuseKey());
                if (session != null) {
                    return toCommandLine(aRequest, view.reconnectSession(session, aRequest));
                }
            } else if (aRequest.getCommand() == null) {
                TerminalSession session = view.findSession(aRequest.getSessionKey());
                if (session != null) {
                    view.activateSession(session);
                    return null;
                }
            }
        }

        return toCommandLine(aRequest, view.openSession(aRequest));
    }

    /**
     * Closes all sessions of the terminal views. The views stay open, so that
     * the workbench opens them again after a restart, like the other views. A
     * view, that has not been created, has no sessions.
     */
    public void closeAllTerminals() {

        if (!PlatformUI.isWorkbenchRunning()) {
            return;
        }

        IWorkbench workbench = PlatformUI.getWorkbench();
        for (IWorkbenchWindow window : workbench.getWorkbenchWindows()) {
            for (IWorkbenchPage page : window.getPages()) {
                for (IViewReference reference : page.getViewReferences()) {
                    if (TmTerminalView.ID.equals(reference.getId())) {
                        IViewPart view = reference.getView(false);
                        if (view instanceof TmTerminalView) {
                            ((TmTerminalView)view).closeAllSessions();
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns the configuration element of the SSH connector.
     *
     * @return configuration element or <code>null</code>, if the SSH connector
     *         is not installed
     */
    static IConfigurationElement findSshConnectorElement() {

        for (String[] candidate : SSH_CONNECTORS) {
            IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(candidate[0]);
            if (extensionPoint == null) {
                continue;
            }
            for (IConfigurationElement element : extensionPoint.getConfigurationElements()) {
                if (candidate[1].equals(element.getAttribute(ATTRIBUTE_ID))) {
                    return element;
                }
            }
        }

        return null;
    }

    private ITerminalCommandLine toCommandLine(TerminalRequest aRequest, ITerminalCommandLine aCommandLine) {

        if (aRequest.getCommand() == null) {
            return null;
        }

        return aCommandLine;
    }

    private IWorkbenchPage getActivePage() {

        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window == null) {
            return null;
        }

        return window.getActivePage();
    }
}
