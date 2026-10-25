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
import org.eclipse.rse.core.filters.ISystemFilter;
import org.eclipse.rse.core.filters.ISystemFilterReference;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.project.IProjConfig;
import de.tools400.ifsprojects.rse.RseSelections;
import de.tools400.ifsprojects.subsystem.IfsProjectEnvironment;

/**
 * Builds the selected IFS project with the build command of the
 * <code>iproj.json</code> file or the default build command of the preferences.
 * The environment variables of the project are exported before.
 * <p>
 * Depending on the preferences, the command runs in the terminal of the project
 * or in a job of the RSE connection, see {@link BuildLauncher}.
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

        BuildLauncher launcher = BuildLauncher.create(shell, host);
        if (launcher == null) {
            return null;
        }

        IProjConfig config = ProjectCommands.readConfig(shell, host, directory);
        if (config == null) {
            return null;
        }

        ISystemFilter filter = ((ISystemFilterReference)element).getReferencedFilter();

        launcher.launch(Messages.bind(Messages.Task_Building_A, filter.getName()), directory, config,
            IfsProjectEnvironment.getEffectiveVariables(filter), BuildLauncher.getBuildCommand(config), null);

        return null;
    }
}
