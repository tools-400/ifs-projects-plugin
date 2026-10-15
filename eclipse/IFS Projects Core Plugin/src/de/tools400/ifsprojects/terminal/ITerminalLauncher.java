/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.terminal;

/**
 * A terminal backend. Backends are contributed via extension point
 * <code>de.tools400.ifsprojects.core.plugin.terminalLaunchers</code>. The
 * first available backend with the highest priority is used.
 * <p>
 * Implementations must not reference classes of optional terminal bundles in
 * their class signature, because {@link #isAvailable()} is called before it is
 * known whether these bundles are installed.
 */
public interface ITerminalLauncher {

    /**
     * Returns <code>true</code>, if the terminal implementation required by
     * this backend is installed.
     *
     * @return <code>true</code>, if the backend can be used
     */
    boolean isAvailable();

    /**
     * Opens a terminal as described by the request. Must be called from the UI
     * thread.
     * <ul>
     * <li>Requests with a reuse key (build): a connected terminal with the same
     * reuse key, whose command is not running (see
     * {@link ITerminalCommandLine#setRunning(boolean)}), is brought to the
     * front and receives the command line. Otherwise a disconnected terminal
     * with the same reuse key, e.g. a terminal restored after a restart, is
     * connected again and receives the command line, once the shell is
     * ready.</li>
     * <li>Requests without command: a terminal with the same session key
     * ({@link TerminalRequest#getSessionKey()}) is brought to the front and
     * connected again, if it is disconnected.</li>
     * <li>Otherwise and if {@link TerminalRequest#isForceNew()} returns
     * <code>true</code>, a new terminal is opened.</li>
     * </ul>
     *
     * @param aRequest - terminal request
     * @return command line of the request or <code>null</code>, if the
     *         request has no command
     * @throws TerminalException
     */
    ITerminalCommandLine launch(TerminalRequest aRequest) throws TerminalException;

    /**
     * Closes all terminals of this backend. Called when the workbench shuts
     * down, unless the terminals shall be restored after a restart. Must be
     * called from the UI thread.
     */
    void closeAllTerminals();
}
