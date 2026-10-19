/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.terminal;

import de.tools400.ifsprojects.ibmi.PaseCommands;
import de.tools400.ifsprojects.utils.StringUtils;

/**
 * Builds the command line that is sent to the remote shell (bsh, ksh, bash)
 * after the terminal has been connected.
 */
public final class ShellCommands {

    /** Line terminator sent to the terminal (Enter key). */
    public static final String ENTER = "\r"; //$NON-NLS-1$

    private ShellCommands() {
        // Utility class
    }

    /**
     * Returns the command line that changes to the specified directory and
     * then executes the specified command.
     *
     * @param aDirectory - directory to change to or <code>null</code>
     * @param aCommand - command that is executed or <code>null</code>. The
     *        command is sent as is (user shell syntax).
     * @return command line terminated by {@link #ENTER} or <code>null</code>, if
     *         neither a directory nor a command has been specified
     */
    public static String toCommandLine(String aDirectory, String aCommand) {

        // Do not trim the directory: IFS names may start or end with blanks.
        String directory = (aDirectory == null || aDirectory.length() == 0) ? null : aDirectory;
        String command = StringUtils.trimToNull(aCommand);

        if (directory == null && command == null) {
            return null;
        }

        if (directory != null && (directory.indexOf('\r') >= 0 || directory.indexOf('\n') >= 0)) {
            throw new IllegalArgumentException("Directory must not contain line breaks: " + directory); //$NON-NLS-1$
        }

        if (command != null && (command.indexOf('\r') >= 0 || command.indexOf('\n') >= 0)) {
            throw new IllegalArgumentException("Command must not contain line breaks: " + command); //$NON-NLS-1$
        }

        StringBuilder buffer = new StringBuilder();
        if (directory != null) {
            buffer.append("cd "); //$NON-NLS-1$
            buffer.append(PaseCommands.quote(directory));
        }

        if (command != null) {
            if (buffer.length() > 0) {
                buffer.append(" && "); //$NON-NLS-1$
            }
            buffer.append(command);
        }

        buffer.append(ENTER);

        return buffer.toString();
    }
}
