/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.terminal;

import java.util.Map;

import de.tools400.ifsprojects.project.EnvironmentVariables;
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
     * Quotes a value for a POSIX shell. The value is enclosed in single quotes
     * and embedded single quotes are replaced by <code>'\''</code>.
     *
     * @param aValue - value that is quoted
     * @return quoted value
     */
    public static String quote(String aValue) {
        return "'" + aValue.replace("'", "'\\''") + "'"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
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
            buffer.append(quote(directory));
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

    /**
     * Replaces the placeholders of a command, e.g. <code>{filename}</code>.
     * Unknown placeholders are kept.
     *
     * @param aCommand - command with placeholders
     * @param aValues - values of the placeholders, the keys are the names of
     *        the placeholders without braces
     * @return command with replaced placeholders
     */
    public static String replacePlaceholders(String aCommand, Map<String, String> aValues) {

        if (aCommand == null) {
            return null;
        }

        String command = aCommand;
        for (Map.Entry<String, String> entry : aValues.entrySet()) {
            String value = entry.getValue();
            command = command.replace("{" + entry.getKey() + "}", StringUtils.nullToEmpty(value)); //$NON-NLS-1$ //$NON-NLS-2$
        }

        return command;
    }

    /**
     * Returns the command, preceded by the export of environment variables.
     * The variables are assigned first and then exported, because the Bourne
     * shell (bsh) does not support <code>export NAME=value</code>:
     * <code>A='1' B='2' &amp;&amp; export A B &amp;&amp; command</code>. The
     * variables stay exported in the shell of the terminal.
     *
     * @param aVariables - environment variables, the names must be valid
     *        shell names
     * @param aCommand - command or <code>null</code>
     * @return command line without line terminator or <code>null</code>, if
     *         neither variables nor a command have been specified
     */
    public static String addExports(Map<String, String> aVariables, String aCommand) {

        String command = StringUtils.trimToNull(aCommand);
        if (aVariables == null || aVariables.isEmpty()) {
            return command;
        }

        StringBuilder assignments = new StringBuilder();
        StringBuilder names = new StringBuilder();
        for (Map.Entry<String, String> entry : aVariables.entrySet()) {
            String name = entry.getKey();
            String value = StringUtils.nullToEmpty(entry.getValue());
            if (!EnvironmentVariables.isValidName(name)) {
                throw new IllegalArgumentException("Invalid name of environment variable: " + name); //$NON-NLS-1$
            }
            if (value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0) {
                throw new IllegalArgumentException("Environment variable must not contain line breaks: " + name); //$NON-NLS-1$
            }
            if (assignments.length() > 0) {
                assignments.append(' ');
                names.append(' ');
            }
            assignments.append(name).append('=').append(quote(value));
            names.append(name);
        }

        StringBuilder buffer = new StringBuilder();
        buffer.append(assignments);
        buffer.append(" && export "); //$NON-NLS-1$
        buffer.append(names);
        if (command != null) {
            buffer.append(" && "); //$NON-NLS-1$
            buffer.append(command);
        }

        return buffer.toString();
    }
}
