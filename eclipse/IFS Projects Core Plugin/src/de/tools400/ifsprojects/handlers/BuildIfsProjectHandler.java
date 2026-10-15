/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.handlers;

import java.util.Map;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.rse.core.filters.ISystemFilter;
import org.eclipse.rse.core.filters.ISystemFilterReference;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import com.ibm.as400.access.AS400;

import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.build.BuildMode;
import de.tools400.ifsprojects.preferences.Preferences;
import de.tools400.ifsprojects.project.IProjConfig;
import de.tools400.ifsprojects.rse.RseConnections;
import de.tools400.ifsprojects.rse.RseSelections;
import de.tools400.ifsprojects.subsystem.IfsProjectEnvironment;
import de.tools400.ifsprojects.terminal.TerminalRequest;

/**
 * Builds the selected IFS project with the build command of the
 * <code>iproj.json</code> file or the default build command of the
 * preferences. The environment variables of the project are exported before.
 * <p>
 * Depending on the preferences, the command runs in the terminal of the project,
 * see {@link TerminalBuildLauncher}, or in a job of the RSE connection, see
 * {@link RseBuildLauncher}.
 */
public class BuildIfsProjectHandler extends AbstractTerminalHandler {

    public Object execute(ExecutionEvent anEvent) throws ExecutionException {

        Shell shell = HandlerUtil.getActiveShell(anEvent);

        Object element = getSelectedElement(anEvent);
        if (!(element instanceof ISystemFilterReference)) {
            return null;
        }

        IHost host = RseSelections.getHost(element);
        String directory = RseSelections.getDirectory(element);
        if (host == null || directory == null) {
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

        IProjConfig config = ProjectCommands.readConfig(shell, host, directory);
        if (config == null) {
            return null;
        }

        String buildCommand = config.getBuildCommand();
        if (buildCommand == null) {
            buildCommand = Preferences.getInstance().getDefaultBuildCommand();
        }

        ISystemFilter filter = ((ISystemFilterReference)element).getReferencedFilter();
        Map<String, String> variables = IfsProjectEnvironment.getEffectiveVariables(filter);

        String jobName = Messages.bind(Messages.Task_Building_A, filter.getName());

        if (isRse) {
            AS400 system = RseBuildLauncher.getConnectedSystem(shell, host);
            if (system != null) {
                RseBuildLauncher.launch(jobName, host, system, directory, config, variables, buildCommand, null);
            }
            return null;
        }

        builder.directory(directory);
        builder.reuseKey(ProjectCommands.getReuseKey(host, directory));

        TerminalBuildLauncher.launch(shell, jobName, host, directory, config, variables, buildCommand, builder, null);

        return null;
    }
}
