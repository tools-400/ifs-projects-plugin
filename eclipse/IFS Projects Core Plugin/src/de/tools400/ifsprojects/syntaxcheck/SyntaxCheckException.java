/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.syntaxcheck;

/**
 * Thrown, if the syntax check could not be performed. The message is shown to
 * the user.
 */
public class SyntaxCheckException extends Exception {

    private static final long serialVersionUID = -2683127417541307186L;

    public SyntaxCheckException(String aMessage, Throwable aCause) {
        super(aMessage, aCause);
    }
}
