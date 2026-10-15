/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.project;

/**
 * Thrown when an <code>iproj.json</code> file cannot be read or written.
 */
public class IProjFileException extends Exception {

    private static final long serialVersionUID = 1L;

    private final boolean isConflict;

    public IProjFileException(String aMessage, Throwable aCause) {
        this(aMessage, aCause, false);
    }

    private IProjFileException(String aMessage, Throwable aCause, boolean anIsConflict) {
        super(aMessage, aCause);
        this.isConflict = anIsConflict;
    }

    /**
     * Creates an exception that reports that the file has been changed by
     * someone else, since it has been read.
     *
     * @param aMessage - message
     * @return exception
     */
    public static IProjFileException conflict(String aMessage) {
        return new IProjFileException(aMessage, null, true);
    }

    /**
     * Returns <code>true</code>, if the file has been changed by someone else.
     */
    public boolean isConflict() {
        return isConflict;
    }
}
