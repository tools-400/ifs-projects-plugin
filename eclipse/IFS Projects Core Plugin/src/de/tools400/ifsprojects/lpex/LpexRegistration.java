/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.lpex;

import com.ibm.lpex.alef.LpexPreload;

/**
 * Registers the plug-in with LPEX. LPEX adds the class loader of each plug-in,
 * that contributes to extension point <code>com.ibm.lpex.preload</code>, to the
 * class loaders of user actions. Therefore the user actions of this plug-in,
 * e.g. {@link CheckSyntaxAction}, can be configured in the LPEX preferences.
 * <p>
 * The profile of LPEX is not changed: the user actions are added on the LPEX
 * preference pages.
 */
public class LpexRegistration implements LpexPreload {

    public void preload() {
        // Nothing to do: LPEX registers the class loader of the plug-in.
    }
}
