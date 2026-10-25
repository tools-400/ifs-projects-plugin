/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.terminal;

/**
 * Command line of a {@link TerminalRequest}, that has been passed to a terminal
 * session. A new session sends it to the shell, once the shell is ready.
 * <p>
 * The methods may be called on any thread.
 */
public interface ITerminalCommandLine {

    /**
     * Returns <code>true</code>, if the terminal session has been disconnected
     * or closed since the command line was passed to it, or if the command line
     * could not be sent. A command line, that the shell has not read until
     * then, is never executed.
     *
     * @return <code>true</code>, if the session has been closed
     */
    boolean isSessionClosed();

    /**
     * Returns <code>true</code>, if the command line has been written to the
     * terminal. A new session writes it, once the shell is ready.
     *
     * @return <code>true</code>, if the command line has been sent
     */
    boolean isSent();

    /**
     * Specifies whether the command of the command line is running. While it is
     * running, the session does not receive the command lines of other requests
     * with a reuse key, because the shell reads them only after the command,
     * and discards them, if the command is interrupted with Ctrl+C.
     * <p>
     * Initially the command is not running, because the end of a command is
     * only known to callers, that observe it, e.g. the
     * <code>TerminalBuildJob</code>.
     *
     * @param anIsRunning - <code>true</code>, if the command is running
     */
    void setRunning(boolean anIsRunning);
}
