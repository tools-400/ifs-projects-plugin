/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.subsystem;

import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.core.subsystems.IConnectorService;
import org.eclipse.rse.core.subsystems.ISubSystem;
import org.eclipse.rse.core.subsystems.SubSystemConfiguration;

import com.ibm.etools.iseries.connectorservice.IToolboxSubSystem;
import com.ibm.etools.iseries.connectorservice.ToolboxConnectorServiceManager;

/**
 * Configuration of the 'IFS Projects' subsystem of IBM i connections. Each
 * filter of the subsystem is an IFS project.
 * <p>
 * The subsystem shares the connection to the IBM i with the IBM subsystems,
 * exactly like IBM's IFS subsystem does.
 */
public class IfsProjectSubSystemConfiguration extends SubSystemConfiguration {

    public static final String ID = "de.tools400.ifsprojects.subsystem"; //$NON-NLS-1$

    public IfsProjectSubSystemConfiguration() {
        super();
    }

    @Override
    public ISubSystem createSubSystemInternal(IHost aHost) {
        return new IfsProjectSubSystem(aHost, getConnectorService(aHost));
    }

    @Override
    public IConnectorService getConnectorService(IHost aHost) {
        return ToolboxConnectorServiceManager.getInstance().getConnectorService(aHost, IToolboxSubSystem.class);
    }

    @Override
    public void setConnectorService(IHost aHost, IConnectorService aConnectorService) {
        ToolboxConnectorServiceManager.getInstance().setConnectorService(aHost, IToolboxSubSystem.class, aConnectorService);
    }

    @Override
    public boolean supportsNestedFilters() {
        return false;
    }

    @Override
    public boolean supportsMultipleFilterStrings() {
        return false;
    }

    @Override
    public boolean supportsDropInFilters() {
        return false;
    }
}
