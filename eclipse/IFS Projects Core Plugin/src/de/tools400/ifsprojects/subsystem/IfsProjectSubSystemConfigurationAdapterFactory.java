/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.subsystem;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.rse.ui.subsystems.ISubSystemConfigurationAdapter;

/**
 * Provides the UI adapter of the 'IFS Projects' subsystem configuration.
 * Registered in plugin.xml (org.eclipse.core.runtime.adapters).
 */
public class IfsProjectSubSystemConfigurationAdapterFactory implements IAdapterFactory {

    private final ISubSystemConfigurationAdapter adapter = new IfsProjectSubSystemConfigurationAdapter();

    public <T> T getAdapter(Object anAdaptableObject, Class<T> anAdapterType) {

        if (anAdapterType == ISubSystemConfigurationAdapter.class && anAdaptableObject instanceof IfsProjectSubSystemConfiguration) {
            // The UI of the subsystem is used: refresh projects on remote
            // changes.
            IfsProjectRefreshListener.ensureRegistered();
            return anAdapterType.cast(adapter);
        }

        return null;
    }

    public Class<?>[] getAdapterList() {
        return new Class<?>[] { ISubSystemConfigurationAdapter.class };
    }
}
