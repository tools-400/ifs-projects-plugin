/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.handlers;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.eclipse.rse.core.model.IHost;
import org.eclipse.swt.widgets.Shell;

import com.ibm.as400.access.AS400;

import de.tools400.ifsprojects.build.BuildMode;
import de.tools400.ifsprojects.ibmi.PaseCommands;
import de.tools400.ifsprojects.local.SourceMapping;
import de.tools400.ifsprojects.preferences.Preferences;
import de.tools400.ifsprojects.project.IProjConfig;
import de.tools400.ifsprojects.rse.RseTerminalRequests;
import de.tools400.ifsprojects.terminal.TerminalRequest;

/**
 * Starts the build of a project or the compile of a file of IFS projects and
 * local IFS projects. Depending on the preferences, the command runs in the
 * terminal of the project, see {@link TerminalBuildLauncher}, or in a job of
 * the RSE connection, see {@link RseBuildLauncher}.
 * <p>
 * Placeholders of the compile command: <code>{filename}</code> and
 * <code>{files}</code> (name of the file without directory),
 * <code>{host}</code> (host name) and <code>{usrprf}</code> (user profile).
 * <p>
 * Errors are reported to the user. The methods must be called on the UI thread.
 */
final class BuildLauncher {

    private static final String PLACEHOLDER_FILENAME = "filename"; //$NON-NLS-1$
    private static final String PLACEHOLDER_FILES = "files"; //$NON-NLS-1$
    private static final String PLACEHOLDER_HOST = "host"; //$NON-NLS-1$
    private static final String PLACEHOLDER_USRPRF = "usrprf"; //$NON-NLS-1$

    /** File names that can be passed to the shell without quotes. */
    private static final String SAFE_FILE_NAME = "[A-Za-z0-9._+@%-]+"; //$NON-NLS-1$

    private final Shell shell;
    private final IHost host;

    /** Connected system object of RSE, if the command runs in an RSE job. */
    private final AS400 system;

    /** Terminal request, if the command runs in the terminal. */
    private final TerminalRequest.Builder requestBuilder;

    private BuildLauncher(Shell aShell, IHost aHost, AS400 aSystem, TerminalRequest.Builder aRequestBuilder) {
        this.shell = aShell;
        this.host = aHost;
        this.system = aSystem;
        this.requestBuilder = aRequestBuilder;
    }

    /**
     * Creates the launcher of the build mode of the preferences. Connects the
     * RSE connection or prepares the terminal request, which may prompt for the
     * user ID.
     *
     * @param aShell - parent shell
     * @param aHost - RSE connection
     * @return launcher or <code>null</code>, if the connection could not be
     *         established or the user canceled
     */
    static BuildLauncher create(Shell aShell, IHost aHost) {

        if (Preferences.getInstance().getBuildMode() == BuildMode.RSE) {
            AS400 system = ProjectCommands.getConnectedSystem(aShell, aHost);
            if (system == null) {
                return null;
            }
            return new BuildLauncher(aShell, aHost, system, null);
        }

        TerminalRequest.Builder requestBuilder = RseTerminalRequests.createBuilder(aShell, aHost);
        if (requestBuilder == null) {
            return null;
        }

        return new BuildLauncher(aShell, aHost, null, requestBuilder);
    }

    /**
     * Returns the build command of the <code>iproj.json</code> file or the
     * default build command of the preferences.
     *
     * @param aConfig - content of <code>iproj.json</code>
     * @return build command
     */
    static String getBuildCommand(IProjConfig aConfig) {

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
     * @return compile command
     */
    String getCompileCommand(IProjConfig aConfig, String aFileName) {

        String command = aConfig.getCompileCommand();
        if (command == null) {
            command = Preferences.getInstance().getDefaultCompileCommand();
        }

        String fileName = toShellFileName(aFileName);
        Map<String, String> values = new HashMap<>();
        values.put(PLACEHOLDER_FILENAME, fileName);
        values.put(PLACEHOLDER_FILES, fileName);
        values.put(PLACEHOLDER_HOST, host.getHostName());
        values.put(PLACEHOLDER_USRPRF, getUserId().toUpperCase(Locale.ROOT));

        return PaseCommands.replacePlaceholders(command, values);
    }

    /**
     * Starts the command.
     *
     * @param aJobName - name of the job
     * @param aDirectory - project directory
     * @param aConfig - content of <code>iproj.json</code>
     * @param aVariables - environment variables of the project
     * @param aCommand - command with replaced placeholders
     * @param aSourceMapping - mapping of the project directory to a local
     *        project or <code>null</code>
     */
    void launch(String aJobName, String aDirectory, IProjConfig aConfig, Map<String, String> aVariables, String aCommand,
        SourceMapping aSourceMapping) {

        if (system != null) {
            RseBuildLauncher.launch(aJobName, host, system, aDirectory, aConfig, aVariables, aCommand, aSourceMapping);
            return;
        }

        requestBuilder.directory(aDirectory).reuseKey(ProjectCommands.getReuseKey(host, aDirectory));

        TerminalBuildLauncher.launch(shell, aJobName, host, aDirectory, aConfig, aVariables, aCommand, requestBuilder, aSourceMapping);
    }

    private String getUserId() {

        if (system != null) {
            return system.getUserId();
        }

        return requestBuilder.build().getUserId();
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
