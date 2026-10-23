/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.utils;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Helpers for exceptions.
 */
public final class ExceptionUtils {

    private ExceptionUtils() {
        // Utility class
    }

    /**
     * Returns the localized message of an exception for a message to the
     * user. An exception without a message, e.g. a
     * <code>NullPointerException</code>, would show <code>null</code>.
     * Therefore the message of its cause is returned, e.g. of the target of an
     * <code>InvocationTargetException</code>, or, if no exception of the chain
     * has a message, the class name of the last one.
     *
     * @param aThrowable - exception
     * @return message, never <code>null</code>
     */
    public static String getLocalizedMessage(Throwable aThrowable) {

        // A chain of causes can be a cycle.
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<Throwable, Boolean>());

        Throwable throwable = aThrowable;
        while (visited.add(throwable)) {
            String message = StringUtils.trimToNull(throwable.getLocalizedMessage());
            if (message != null) {
                return message;
            }
            if (throwable.getCause() == null) {
                break;
            }
            throwable = throwable.getCause();
        }

        return throwable.getClass().getName();
    }

    /**
     * Returns the cause of an exception or the exception itself, if it has no
     * cause, e.g. the exception, that the
     * <code>InvocationTargetException</code> of a runnable context wraps.
     *
     * @param aThrowable - exception
     * @return cause or the exception itself
     */
    public static Throwable getCause(Throwable aThrowable) {
        return aThrowable.getCause() != null ? aThrowable.getCause() : aThrowable;
    }
}
