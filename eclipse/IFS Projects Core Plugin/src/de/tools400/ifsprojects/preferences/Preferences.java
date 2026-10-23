/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.preferences;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jface.preference.IPreferenceStore;

import de.tools400.ifsprojects.IfsProjectsPlugin;
import de.tools400.ifsprojects.build.BuildMode;
import de.tools400.ifsprojects.export.ExportSettings;
import de.tools400.ifsprojects.project.BuildTool;
import de.tools400.ifsprojects.project.EnvironmentVariables;
import de.tools400.ifsprojects.terminal.TerminalRequest;
import de.tools400.ifsprojects.utils.StringUtils;

/**
 * Preferences of the IFS Projects plug-in.
 */
public final class Preferences {

    private static Preferences instance;

    private IPreferenceStore preferenceStore;

    public static final String PORT = "port"; //$NON-NLS-1$
    public static final String TIMEOUT = "timeout"; //$NON-NLS-1$
    public static final String KEEP_ALIVE = "keepAlive"; //$NON-NLS-1$
    public static final String ENCODING = "encoding"; //$NON-NLS-1$
    public static final String LOWERCASE_USER_ID = "lowercaseUserId"; //$NON-NLS-1$
    public static final String USE_SAVED_PASSWORD = "useSavedPassword"; //$NON-NLS-1$
    public static final String DEFAULT_BUILD_COMMAND = "defaultBuildCommand"; //$NON-NLS-1$
    public static final String DEFAULT_COMPILE_COMMAND = "defaultCompileCommand"; //$NON-NLS-1$
    public static final String REUSE_BUILD_TERMINAL = "reuseBuildTerminal"; //$NON-NLS-1$
    public static final String BUILD_MODE = "buildMode"; //$NON-NLS-1$
    public static final String DEFAULT_ENVIRONMENT_VARIABLES = "defaultEnvironmentVariables"; //$NON-NLS-1$
    public static final String RESTORE_TERMINALS = "restoreTerminals"; //$NON-NLS-1$
    public static final String EXPORT_CCSID = "exportCcsid"; //$NON-NLS-1$
    public static final String EXPORT_RECORD_LENGTH = "exportRecordLength"; //$NON-NLS-1$
    public static final String DISABLED_SOURCE_HEADER_PARSERS = "disabledSourceHeaderParsers"; //$NON-NLS-1$

    private static final int DEFAULT_PORT = TerminalRequest.DEFAULT_PORT;
    private static final int DEFAULT_TIMEOUT = TerminalRequest.DEFAULT_TIMEOUT;
    private static final int DEFAULT_KEEP_ALIVE = TerminalRequest.DEFAULT_KEEP_ALIVE;
    private static final String DEFAULT_ENCODING = TerminalRequest.DEFAULT_ENCODING;
    private static final boolean DEFAULT_LOWERCASE_USER_ID = true;
    private static final boolean DEFAULT_USE_SAVED_PASSWORD = false;
    private static final String DEFAULT_DEFAULT_BUILD_COMMAND = BuildTool.TOBI.getBuildCommand();
    private static final String DEFAULT_DEFAULT_COMPILE_COMMAND = BuildTool.TOBI.getCompileCommand();
    private static final boolean DEFAULT_REUSE_BUILD_TERMINAL = true;
    private static final String DEFAULT_BUILD_MODE = BuildMode.TERMINAL.getValue();
    private static final String DEFAULT_DEFAULT_ENVIRONMENT_VARIABLES = ""; //$NON-NLS-1$
    private static final boolean DEFAULT_RESTORE_TERMINALS = false;
    private static final int DEFAULT_EXPORT_CCSID = 1141;
    private static final int DEFAULT_EXPORT_RECORD_LENGTH = 112;
    private static final String DEFAULT_DISABLED_SOURCE_HEADER_PARSERS = ""; //$NON-NLS-1$

    /** Separator of the IDs of the disabled parsers. */
    private static final String ID_SEPARATOR = ","; //$NON-NLS-1$

    private Preferences() {
        // Singleton class
    }

    public synchronized static Preferences getInstance() {
        if (instance == null) {
            instance = new Preferences();
            instance.preferenceStore = IfsProjectsPlugin.getDefault().getPreferenceStore();
        }
        return instance;
    }

    public IPreferenceStore getPreferenceStore() {
        return preferenceStore;
    }

    public void initializeDefaultPreferences() {
        preferenceStore.setDefault(PORT, DEFAULT_PORT);
        preferenceStore.setDefault(TIMEOUT, DEFAULT_TIMEOUT);
        preferenceStore.setDefault(KEEP_ALIVE, DEFAULT_KEEP_ALIVE);
        preferenceStore.setDefault(ENCODING, DEFAULT_ENCODING);
        preferenceStore.setDefault(LOWERCASE_USER_ID, DEFAULT_LOWERCASE_USER_ID);
        preferenceStore.setDefault(USE_SAVED_PASSWORD, DEFAULT_USE_SAVED_PASSWORD);
        preferenceStore.setDefault(DEFAULT_BUILD_COMMAND, DEFAULT_DEFAULT_BUILD_COMMAND);
        preferenceStore.setDefault(DEFAULT_COMPILE_COMMAND, DEFAULT_DEFAULT_COMPILE_COMMAND);
        preferenceStore.setDefault(REUSE_BUILD_TERMINAL, DEFAULT_REUSE_BUILD_TERMINAL);
        preferenceStore.setDefault(BUILD_MODE, DEFAULT_BUILD_MODE);
        preferenceStore.setDefault(DEFAULT_ENVIRONMENT_VARIABLES, DEFAULT_DEFAULT_ENVIRONMENT_VARIABLES);
        preferenceStore.setDefault(RESTORE_TERMINALS, DEFAULT_RESTORE_TERMINALS);
        preferenceStore.setDefault(EXPORT_CCSID, DEFAULT_EXPORT_CCSID);
        preferenceStore.setDefault(EXPORT_RECORD_LENGTH, DEFAULT_EXPORT_RECORD_LENGTH);
        preferenceStore.setDefault(DISABLED_SOURCE_HEADER_PARSERS, DEFAULT_DISABLED_SOURCE_HEADER_PARSERS);
    }

    /*
     * Preferences: GETTER
     */

    public int getPort() {
        return getPositiveInt(PORT, DEFAULT_PORT);
    }

    public int getTimeout() {
        return getPositiveInt(TIMEOUT, DEFAULT_TIMEOUT);
    }

    public int getKeepAlive() {
        return getPositiveInt(KEEP_ALIVE, DEFAULT_KEEP_ALIVE);
    }

    public String getEncoding() {
        return getString(ENCODING, DEFAULT_ENCODING);
    }

    public boolean isLowercaseUserId() {
        return preferenceStore.getBoolean(LOWERCASE_USER_ID);
    }

    public boolean isUseSavedPassword() {
        return preferenceStore.getBoolean(USE_SAVED_PASSWORD);
    }

    public String getDefaultBuildCommand() {
        return getString(DEFAULT_BUILD_COMMAND, DEFAULT_DEFAULT_BUILD_COMMAND);
    }

    /**
     * Returns the command that compiles a single file, if the iproj.json file
     * of the project does not specify a compile command.
     */
    public String getDefaultCompileCommand() {
        return getString(DEFAULT_COMPILE_COMMAND, DEFAULT_DEFAULT_COMPILE_COMMAND);
    }

    public boolean isReuseBuildTerminal() {
        return preferenceStore.getBoolean(REUSE_BUILD_TERMINAL);
    }

    /**
     * Returns, where the build and compile commands of the IFS projects run: in
     * the SSH terminal or in a job of the RSE connection.
     */
    public BuildMode getBuildMode() {
        return BuildMode.fromValue(preferenceStore.getString(BUILD_MODE));
    }

    /**
     * Returns the default environment variables of all IFS projects, e.g. the
     * value of <code>&amp;ENVCMD</code> in <code>setIBMiEnvCmd</code> of
     * <code>iproj.json</code>. The variables of an IFS project override them.
     *
     * @return variables in the order of the definition, never <code>null</code>
     */
    public Map<String, String> getDefaultEnvironmentVariables() {

        String text = preferenceStore.getString(DEFAULT_ENVIRONMENT_VARIABLES);
        int invalidLine = EnvironmentVariables.findInvalidLine(text);
        if (invalidLine > 0) {
            IfsProjectsPlugin.logError("Default environment variables ignored, invalid line: " + invalidLine, null); //$NON-NLS-1$
            return new LinkedHashMap<String, String>();
        }

        return EnvironmentVariables.parse(text);
    }

    /**
     * Returns <code>true</code>, if the terminals are restored (disconnected)
     * after a restart of RDi. Otherwise they are closed on shutdown.
     */
    public boolean isRestoreTerminals() {
        return preferenceStore.getBoolean(RESTORE_TERMINALS);
    }

    /**
     * Returns the CCSID of the source files, that the export to a library
     * creates, unless the project has been exported before.
     */
    public int getExportCcsid() {
        return getInt(EXPORT_CCSID, ExportSettings.MIN_CCSID, ExportSettings.MAX_CCSID, DEFAULT_EXPORT_CCSID);
    }

    /**
     * Returns the minimum record length of the source files, that the export to
     * a library creates, unless the project has been exported before.
     */
    public int getExportRecordLength() {
        return getInt(EXPORT_RECORD_LENGTH, ExportSettings.MIN_RECORD_LENGTH, ExportSettings.MAX_RECORD_LENGTH, DEFAULT_EXPORT_RECORD_LENGTH);
    }

    /**
     * Returns the IDs of the parsers of the headers of the build tools, that
     * are switched off on the preference page of the migration. The disabled
     * parsers are stored, so that a parser, that is installed later, is used
     * without a change of the preferences.
     *
     * @return IDs, never <code>null</code>
     */
    public Set<String> getDisabledSourceHeaderParsers() {

        Set<String> ids = new HashSet<String>();
        for (String id : preferenceStore.getString(DISABLED_SOURCE_HEADER_PARSERS).split(ID_SEPARATOR)) {
            String trimmed = StringUtils.trimToNull(id);
            if (trimmed != null) {
                ids.add(trimmed);
            }
        }

        return ids;
    }

    /**
     * Stores the IDs of the parsers of the headers of the build tools, that
     * are switched off.
     *
     * @param anIds - IDs of the disabled parsers
     */
    public void setDisabledSourceHeaderParsers(Collection<String> anIds) {

        StringBuilder value = new StringBuilder();
        for (String id : new TreeSet<String>(anIds)) {
            if (value.length() > 0) {
                value.append(ID_SEPARATOR);
            }
            value.append(id);
        }

        preferenceStore.setValue(DISABLED_SOURCE_HEADER_PARSERS, value.toString());
    }

    private int getInt(String aKey, int aMinimum, int aMaximum, int aDefault) {
        int value = preferenceStore.getInt(aKey);
        if (value < aMinimum || value > aMaximum) {
            return aDefault;
        }
        return value;
    }

    /**
     * Returns the trimmed value of a string preference or the default value, if
     * the preference is empty.
     */
    private String getString(String aKey, String aDefault) {
        String value = StringUtils.trimToNull(preferenceStore.getString(aKey));
        if (value == null) {
            return aDefault;
        }
        return value;
    }

    private int getPositiveInt(String aKey, int aDefault) {
        int value = preferenceStore.getInt(aKey);
        if (value <= 0) {
            return aDefault;
        }
        return value;
    }
}
