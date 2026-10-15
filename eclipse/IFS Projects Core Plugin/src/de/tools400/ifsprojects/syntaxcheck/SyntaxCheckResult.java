/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.syntaxcheck;

/**
 * Result of a syntax check: the events file written by the compiler and the
 * messages of the compile command.
 */
public final class SyntaxCheckResult {

    private final String events;
    private final int ccsid;
    private final String compileMessages;

    SyntaxCheckResult(String anEvents, int aCcsid, String aCompileMessages) {
        this.events = anEvents;
        this.ccsid = aCcsid;
        this.compileMessages = aCompileMessages;
    }

    /**
     * Returns the content of the events file.
     *
     * @return events or <code>null</code>, if the compiler did not create an
     *         events file
     */
    public String getEvents() {
        return events;
    }

    /**
     * Returns the CCSID of the job, in which the events file was written.
     */
    public int getCcsid() {
        return ccsid;
    }

    /**
     * Returns the messages of the compile command, one message per line.
     */
    public String getCompileMessages() {
        return compileMessages;
    }
}
