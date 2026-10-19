/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.subsystems.files.core.subsystems.IRemoteFile;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.project.IProjConfig;
import de.tools400.ifsprojects.subsystem.IfsProject;
import de.tools400.ifsprojects.subsystem.IfsProjects;

/**
 * Compiles the selected file of an IFS project with the compile command of the
 * <code>iproj.json</code> file or the default compile command of the
 * preferences. The environment variables of the project are exported before.
 * <p>
 * Depending on the preferences, the command runs in the terminal of the project
 * or in a job of the RSE connection, see {@link BuildLauncher}, which also
 * describes the placeholders of the compile command.
 */
public class CompileIfsFileHandler extends AbstractTerminalHandler {

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

        BuildLauncher launcher = BuildLauncher.create(shell, host);
        if (launcher == null) {
            return null;
        }

        IProjConfig config = ProjectCommands.readConfig(shell, host, project.getDirectory());
        if (config == null) {
            return null;
        }

        launcher.launch(Messages.bind(Messages.Task_Compiling_A, file.getName()), project.getDirectory(), config,
            project.getEnvironmentVariables(), launcher.getCompileCommand(config, file.getName()), null);

        return null;
    }
}
