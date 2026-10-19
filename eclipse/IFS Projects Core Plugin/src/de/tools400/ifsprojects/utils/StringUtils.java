/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.utils;

/**
 * Helpers for strings.
 */
public final class StringUtils {

    private StringUtils() {
        // Utility class
    }

    /**
     * Returns the trimmed string or <code>null</code>, if the trimmed string
     * is empty.
     *
     * @param aValue - string to trim or <code>null</code>
     * @return trimmed string or <code>null</code>, if the string is
     *         <code>null</code> or consists of blanks only
     */
    public static String trimToNull(String aValue) {

        if (aValue == null) {
            return null;
        }

        String value = aValue.trim();
        if (value.length() == 0) {
            return null;
        }

        return value;
    }

    /**
     * Returns <code>true</code>, if the string is <code>null</code> or
     * consists of blanks only.
     *
     * @param aValue - string or <code>null</code>
     * @return <code>true</code>, if the string is <code>null</code>, empty or
     *         consists of blanks only
     */
    public static boolean isBlank(String aValue) {
        return aValue == null || aValue.trim().length() == 0;
    }

    /**
     * Returns the string or an empty string, if the string is
     * <code>null</code>.
     *
     * @param aValue - string or <code>null</code>
     * @return string, never <code>null</code>
     */
    public static String nullToEmpty(String aValue) {

        if (aValue == null) {
            return ""; //$NON-NLS-1$
        }

        return aValue;
    }
}
