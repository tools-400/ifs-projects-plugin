/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.api;

/**
 * Thrown, if a file of a local IFS project could not be compiled or checked,
 * e.g. because its project is not configured as IFS project or its RSE
 * connection is not connected. The message tells the reason.
 */
public class CompileException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     *
     * @param aMessage - reason
     * @param aCause - cause or <code>null</code>
     */
    public CompileException(String aMessage, Throwable aCause) {
        super(aMessage, aCause);
    }
}
