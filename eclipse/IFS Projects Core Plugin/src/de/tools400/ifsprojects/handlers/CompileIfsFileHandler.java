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

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.subsystems.files.core.subsystems.IRemoteFile;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import com.ibm.as400.access.AS400;

import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.build.BuildMode;
import de.tools400.ifsprojects.preferences.Preferences;
import de.tools400.ifsprojects.project.IProjConfig;
import de.tools400.ifsprojects.rse.RseConnections;
import de.tools400.ifsprojects.subsystem.IfsProject;
import de.tools400.ifsprojects.subsystem.IfsProjects;
import de.tools400.ifsprojects.terminal.ShellCommands;
import de.tools400.ifsprojects.terminal.TerminalRequest;

/**
 * Compiles the selected file of an IFS project with the compile command of the
 * <code>iproj.json</code> file or the default compile command of the
 * preferences. The environment variables of the project are exported before.
 * <p>
 * Depending on the preferences, the command runs in the terminal of the project,
 * see {@link TerminalBuildLauncher}, or in a job of the RSE connection, see
 * {@link RseBuildLauncher}.
 * <p>
 * Placeholders: <code>{filename}</code> and <code>{files}</code> (name of the
 * file without directory), <code>{host}</code> (host name) and
 * <code>{usrprf}</code> (user profile).
 */
public class CompileIfsFileHandler extends AbstractTerminalHandler {

    public static final String PLACEHOLDER_FILENAME = "filename"; //$NON-NLS-1$
    public static final String PLACEHOLDER_FILES = "files"; //$NON-NLS-1$
    public static final String PLACEHOLDER_HOST = "host"; //$NON-NLS-1$
    public static final String PLACEHOLDER_USRPRF = "usrprf"; //$NON-NLS-1$

    /** File names that can be passed to the shell without quotes. */
    private static final String SAFE_FILE_NAME = "[A-Za-z0-9._+@%-]+"; //$NON-NLS-1$

    public Object execute(ExecutionEvent anEvent) throws ExecutionException {

        Shell shell = HandlerUtil.getActiveShell(anEvent);

        Object element = getSelectedElement(anEvent);
        if (!(element instanceof IRemoteFile) || ((IRemoteFile)element).isDirectory()) {
            return null;
        }

        IRemoteFile file = (IRemoteFile)element;
        IHost host = file.getHost();

        IfsProject project = IfsProjects.findProject(host, file.getAbsolutePath());
        if (project == null) {
            return null;
        }

        boolean isRse = Preferences.getInstance().getBuildMode() == BuildMode.RSE;

        TerminalRequest.Builder builder = null;
        if (!isRse) {
            builder = RseConnections.createRequestBuilder(shell, host);
            if (builder == null) {
                return null;
            }
        }

        IProjConfig config = ProjectCommands.readConfig(shell, host, project.getDirectory());
        if (config == null) {
            return null;
        }

        String compileCommand = config.getCompileCommand();
        if (compileCommand == null) {
            compileCommand = Preferences.getInstance().getDefaultCompileCommand();
        }

        AS400 system = null;
        TerminalRequest request = null;
        String userId;
        if (isRse) {
            system = RseBuildLauncher.getConnectedSystem(shell, host);
            if (system == null) {
                return null;
            }
            userId = system.getUserId();
        } else {
            request = builder.build();
            userId = request.getUserId();
        }

        String fileName = toShellFileName(file.getName());
        Map<String, String> values = new HashMap<String, String>();
        values.put(PLACEHOLDER_FILENAME, fileName);
        values.put(PLACEHOLDER_FILES, fileName);
        values.put(PLACEHOLDER_HOST, host.getHostName());
        values.put(PLACEHOLDER_USRPRF, userId.toUpperCase(Locale.ROOT));

        String command = ShellCommands.replacePlaceholders(compileCommand, values);
        String jobName = Messages.bind(Messages.Task_Compiling_A, file.getName());

        if (isRse) {
            RseBuildLauncher.launch(jobName, host, system, project.getDirectory(), config, project.getEnvironmentVariables(), command, null);
            return null;
        }

        TerminalRequest.Builder requestBuilder = request.toBuilder().directory(project.getDirectory())
            .reuseKey(ProjectCommands.getReuseKey(host, project.getDirectory()));

        TerminalBuildLauncher.launch(shell, jobName, host, project.getDirectory(), config, project.getEnvironmentVariables(), command,
            requestBuilder, null);

        return null;
    }

    /**
     * Returns the name of a file for the command line: quoted, unless it
     * consists of characters, that can be passed to the shell without
     * quotes.
     */
    static String toShellFileName(String aFileName) {

        if (aFileName.matches(SAFE_FILE_NAME)) {
            return aFileName;
        }

        return ShellCommands.quote(aFileName);
    }
}
