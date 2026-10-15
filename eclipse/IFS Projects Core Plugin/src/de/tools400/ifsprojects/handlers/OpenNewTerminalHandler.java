/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.handlers;

/**
 * Opens an additional SSH terminal for the selected IBM i connection, even if
 * a terminal of the connection is already open.
 */
public class OpenNewTerminalHandler extends OpenTerminalHandler {

    @Override
    protected boolean isForceNew() {
        return true;
    }
}
