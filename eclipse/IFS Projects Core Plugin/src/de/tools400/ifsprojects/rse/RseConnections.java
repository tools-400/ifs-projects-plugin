/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.rse;

import org.eclipse.rse.core.model.IHost;

import com.ibm.etools.iseries.subsystems.qsys.api.IBMiConnection;

/**
 * Finds RSE connections.
 */
public final class RseConnections {

    private RseConnections() {
        // Utility class
    }

    /**
     * Returns the IBM i connection with the specified name of the active
     * profiles.
     *
     * @param aConnectionName - name of the connection
     * @return connection or <code>null</code>, if it does not exist
     */
    public static IHost findIBMiHost(String aConnectionName) {

        if (aConnectionName == null) {
            return null;
        }

        for (IBMiConnection connection : IBMiConnection.getConnections()) {
            if (aConnectionName.equals(connection.getConnectionName())) {
                return connection.getHost();
            }
        }

        return null;
    }
}
