/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.terminal;

public class TerminalException extends Exception {

    private static final long serialVersionUID = 1L;

    public TerminalException(String aMessage) {
        super(aMessage);
    }

    public TerminalException(String aMessage, Throwable aCause) {
        super(aMessage, aCause);
    }
}
