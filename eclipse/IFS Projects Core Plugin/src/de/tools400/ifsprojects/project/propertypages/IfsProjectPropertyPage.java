/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.project.propertypages;

import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.rse.core.filters.ISystemFilterReference;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.swt.widgets.Composite;

import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.project.IProjFileContent;
import de.tools400.ifsprojects.project.IProjFileException;
import de.tools400.ifsprojects.project.IProjFileStore;
import de.tools400.ifsprojects.rse.RseSelections;

/**
 * Property page of IFS projects. Edits the <code>iproj.json</code> file of the
 * project directory in the IFS, see {@link AbstractIProjPropertyPage}.
 */
public class IfsProjectPropertyPage extends AbstractIProjPropertyPage {

    private ISystemFilterReference filterReference;
    private IHost host;
    private String directory;

    @Override
    protected boolean initialize() {

        filterReference = RseSelections.getFilterReference(getElement());
        host = RseSelections.getHost(filterReference);
        directory = RseSelections.getDirectory(filterReference);

        return filterReference != null && host != null && directory != null;
    }

    @Override
    protected void createHeader(Composite aParent) {
        createInfo(aParent, Messages.Label_Project_name_colon, filterReference.getReferencedFilter().getName());
        createInfo(aParent, Messages.Label_Connection_colon, host.getAliasName());
        createInfo(aParent, Messages.Label_Directory_colon, directory);
    }

    @Override
    protected IProjFileContent readFile() throws IProjFileException {
        return IProjFileStore.read(new ProgressMonitorDialog(getShell()), host, directory);
    }

    @Override
    protected void writeFile(String aContent, IProjFileContent aPrevious) throws IProjFileException {
        IProjFileStore.write(new ProgressMonitorDialog(getShell()), host, directory, aContent, aPrevious);
    }
}
