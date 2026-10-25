/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.ibmi;

import java.util.Map;

import de.tools400.ifsprojects.utils.StringUtils;

/**
 * Syntax of the commands of the PASE shells: quoting, the export of environment
 * variables, placeholders of the build and compile commands and the call of a
 * shell script from CL.
 */
public final class PaseCommands {

    /** Bash of the IBM i open source packages. */
    public static final String BASH = "/QOpenSys/pkgs/bin/bash"; //$NON-NLS-1$

    /** Default shell of PASE, used if bash is not installed. */
    public static final String SH = "/QOpenSys/usr/bin/sh"; //$NON-NLS-1$

    private static final String VARIABLE_NAME_PATTERN = "[A-Za-z_][A-Za-z0-9_]*"; //$NON-NLS-1$

    private PaseCommands() {
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
     * Returns <code>true</code>, if the name can be used as name of a shell
     * variable.
     *
     * @param aName - name of the variable
     * @return <code>true</code> for valid names
     */
    public static boolean isVariableName(String aName) {
        return aName != null && aName.matches(VARIABLE_NAME_PATTERN);
    }

    /**
     * Returns the command, preceded by the export of environment variables. The
     * variables are assigned first and then exported, because the Bourne shell
     * (bsh) does not support <code>export NAME=value</code>:
     * <code>A='1' B='2' &amp;&amp; export A B &amp;&amp; command</code>. The
     * variables stay exported in the shell of the terminal.
     *
     * @param aVariables - environment variables, the names must be valid shell
     *        names, see {@link #isVariableName(String)}
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
            if (!isVariableName(name)) {
                throw new IllegalArgumentException("Invalid name of environment variable: " + name); //$NON-NLS-1$
            }
            if (StringUtils.containsLineBreak(value)) {
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
     * Returns the CL command, that runs a shell script with
     * <code>QP2SHELL2</code>.
     *
     * @param aShell - shell, {@link #BASH} or {@link #SH}
     * @param aScript - script
     * @return CL command
     */
    public static String getCallCommand(String aShell, String aScript) {
        return "CALL PGM(QP2SHELL2) PARM(" + ClCommands.quote(aShell) + " '-c' " + ClCommands.quote(aScript) + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }
}
