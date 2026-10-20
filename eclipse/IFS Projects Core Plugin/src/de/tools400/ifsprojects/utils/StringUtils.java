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

    /**
     * Returns <code>true</code>, if the string contains a carriage return or
     * a line feed.
     *
     * @param aValue - string
     * @return <code>true</code>, if the string contains a line break
     */
    public static boolean containsLineBreak(String aValue) {
        return aValue.indexOf('\r') >= 0 || aValue.indexOf('\n') >= 0;
    }

    /**
     * Returns the index of the first character, that is not a whitespace.
     *
     * @param aValue - string
     * @param aStart - index, at which the search starts
     * @return index or -1, if there is no such character at or after the start
     */
    public static int indexOfNonBlank(String aValue, int aStart) {

        for (int i = Math.max(aStart, 0); i < aValue.length(); i++) {
            if (!Character.isWhitespace(aValue.charAt(i))) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Appends blanks to a string up to a length.
     *
     * @param aValue - string
     * @param aLength - length of the result
     * @return string with blanks appended or the string itself, if it is not
     *         shorter than the length
     */
    public static String padRight(String aValue, int aLength) {

        if (aValue.length() >= aLength) {
            return aValue;
        }

        StringBuilder buffer = new StringBuilder(aLength).append(aValue);
        while (buffer.length() < aLength) {
            buffer.append(' ');
        }

        return buffer.toString();
    }
}
