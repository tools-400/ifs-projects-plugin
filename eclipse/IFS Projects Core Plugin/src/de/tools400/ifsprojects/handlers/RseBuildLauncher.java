/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.handlers;

import java.util.Map;

import org.eclipse.rse.core.model.IHost;

import com.ibm.as400.access.AS400;

import de.tools400.ifsprojects.build.RseBuildCommands;
import de.tools400.ifsprojects.build.RseBuildJob;
import de.tools400.ifsprojects.local.SourceMapping;
import de.tools400.ifsprojects.project.IProjConfig;
import de.tools400.ifsprojects.terminal.ShellCommands;

/**
 * Starts a build or compile command of an IFS project in a job of the RSE
 * connection, see {@link RseBuildJob}. Used by {@link BuildLauncher}.
 */
final class RseBuildLauncher {

    private RseBuildLauncher() {
        // Utility class
    }

    /**
     * Starts the command.
     *
     * @param aJobName - name of the job
     * @param aHost - RSE connection
     * @param aSystem - connected system object of RSE
     * @param aDirectory - project directory
     * @param aConfig - content of <code>iproj.json</code>
     * @param aVariables - environment variables of the project
     * @param aCommand - command with replaced placeholders
     * @param aSourceMapping - mapping of the project directory to a local
     *        project or <code>null</code>
     */
    static void launch(String aJobName, IHost aHost, AS400 aSystem, String aDirectory, IProjConfig aConfig, Map<String, String> aVariables,
        String aCommand, SourceMapping aSourceMapping) {

        String objectLibrary = RseBuildCommands.getObjectLibrary(aConfig, aVariables);
        String command = ShellCommands.addExports(aVariables, aCommand);

        new RseBuildJob(aJobName, aHost, aSystem, aDirectory, objectLibrary, aCommand.trim(), command, aSourceMapping).schedule();
    }
}
