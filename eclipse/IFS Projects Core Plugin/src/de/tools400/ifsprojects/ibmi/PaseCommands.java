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
 * Syntax of the commands of the PASE shells: quoting, placeholders of the
 * build and compile commands and the call of a shell script from CL.
 */
public final class PaseCommands {

    /** Bash of the IBM i open source packages. */
    public static final String BASH = "/QOpenSys/pkgs/bin/bash"; //$NON-NLS-1$

    /** Default shell of PASE, used if bash is not installed. */
    public static final String SH = "/QOpenSys/usr/bin/sh"; //$NON-NLS-1$

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
