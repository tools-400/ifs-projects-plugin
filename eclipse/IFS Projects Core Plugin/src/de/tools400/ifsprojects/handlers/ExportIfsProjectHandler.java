/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.rse.core.filters.ISystemFilter;
import org.eclipse.rse.core.filters.ISystemFilterReference;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import com.ibm.as400.access.AS400;

import de.tools400.ifsprojects.export.IfsProjectExportSource;
import de.tools400.ifsprojects.export.wizards.ExportToLibraryWizard;
import de.tools400.ifsprojects.project.IProjConfig;
import de.tools400.ifsprojects.rse.RseSelections;
import de.tools400.ifsprojects.subsystem.IfsProjectEnvironment;

/**
 * Exports the source files of the selected IFS project into members of a
 * library, see {@link ExportToLibraryWizard}.
 */
public class ExportIfsProjectHandler extends AbstractHandler {

    public Object execute(ExecutionEvent anEvent) throws ExecutionException {

        Shell shell = HandlerUtil.getActiveShell(anEvent);

        Object element = RseSelections.getSingleElement(HandlerUtil.getCurrentSelection(anEvent));
        if (!(element instanceof ISystemFilterReference)) {
            return null;
        }

        IHost host = RseSelections.getHost(element);
        String directory = RseSelections.getDirectory(element);
        if (host == null || directory == null) {
            return null;
        }

        IProjConfig config = ProjectCommands.readConfig(shell, host, directory);
        if (config == null) {
            return null;
        }

        AS400 system = RseBuildLauncher.getConnectedSystem(shell, host);
        if (system == null) {
            return null;
        }

        ISystemFilter filter = ((ISystemFilterReference)element).getReferencedFilter();
        IfsProjectExportSource source = new IfsProjectExportSource(filter, host, directory, config,
            IfsProjectEnvironment.getEffectiveVariables(filter));

        new WizardDialog(shell, new ExportToLibraryWizard(source, system)).open();

        return null;
    }
}
