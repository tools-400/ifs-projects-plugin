/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.build;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import de.tools400.ifsprojects.ibmi.PaseCommands;
import de.tools400.ifsprojects.preferences.Preferences;
import de.tools400.ifsprojects.project.IProjConfig;

/**
 * Build command and compile command of an IFS project.
 * <p>
 * Placeholders of the compile command: <code>{filename}</code> and
 * <code>{files}</code> (name of the file without directory),
 * <code>{host}</code> (host name) and <code>{usrprf}</code> (user profile).
 */
public final class BuildCommands {

    private static final String PLACEHOLDER_FILENAME = "filename"; //$NON-NLS-1$
    private static final String PLACEHOLDER_FILES = "files"; //$NON-NLS-1$
    private static final String PLACEHOLDER_HOST = "host"; //$NON-NLS-1$
    private static final String PLACEHOLDER_USRPRF = "usrprf"; //$NON-NLS-1$

    /** File names that can be passed to the shell without quotes. */
    private static final String SAFE_FILE_NAME = "[A-Za-z0-9._+@%-]+"; //$NON-NLS-1$

    private BuildCommands() {
        // Utility class
    }

    /**
     * Returns the build command of the <code>iproj.json</code> file or the
     * default build command of the preferences.
     *
     * @param aConfig - content of <code>iproj.json</code>
     * @return build command
     */
    public static String getBuildCommand(IProjConfig aConfig) {

        String command = aConfig.getBuildCommand();
        if (command == null) {
            return Preferences.getInstance().getDefaultBuildCommand();
        }

        return command;
    }

    /**
     * Returns the compile command of the <code>iproj.json</code> file or the
     * default compile command of the preferences with replaced placeholders.
     *
     * @param aConfig - content of <code>iproj.json</code>
     * @param aFileName - name of the compiled file without directory
     * @param aHostName - host name of the IBM i
     * @param aUserId - user profile, that runs the command
     * @return compile command
     */
    public static String getCompileCommand(IProjConfig aConfig, String aFileName, String aHostName, String aUserId) {

        String command = aConfig.getCompileCommand();
        if (command == null) {
            command = Preferences.getInstance().getDefaultCompileCommand();
        }

        String fileName = toShellFileName(aFileName);
        Map<String, String> values = new HashMap<>();
        values.put(PLACEHOLDER_FILENAME, fileName);
        values.put(PLACEHOLDER_FILES, fileName);
        values.put(PLACEHOLDER_HOST, aHostName);
        values.put(PLACEHOLDER_USRPRF, aUserId.toUpperCase(Locale.ROOT));

        return PaseCommands.replacePlaceholders(command, values);
    }

    /**
     * Returns the name of a file for the command line: quoted, unless it
     * consists of characters, that can be passed to the shell without quotes.
     */
    private static String toShellFileName(String aFileName) {

        if (aFileName.matches(SAFE_FILE_NAME)) {
            return aFileName;
        }

        return PaseCommands.quote(aFileName);
    }
}
