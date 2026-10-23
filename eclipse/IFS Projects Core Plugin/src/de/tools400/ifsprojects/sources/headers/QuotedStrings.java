/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.sources.headers;

/**
 * Quoted strings of CL commands, e.g. <code>TEXT('Order (new)')</code>. A
 * string is enclosed in quotes; two quotes are one quote of the string. The
 * parsers of the build tools skip them, so that a marker in a string, e.g. the
 * end of a command, does not count, see {@link ISourceHeaderParser}.
 */
public final class QuotedStrings {

    /** Start and end of a quoted string. */
    public static final char QUOTE = '\'';

    private QuotedStrings() {
        // Utility class
    }

    /**
     * Returns the index behind the quoted string, that starts at the given
     * index.
     *
     * @param aText - text, e.g. a command
     * @param anIndex - index of the quote, that starts the string
     * @return index behind the closing quote or the length of the text, if the
     *         string is not closed
     */
    public static int skip(CharSequence aText, int anIndex) {

        int i = anIndex + 1;
        while (i < aText.length()) {
            if (aText.charAt(i) == QUOTE) {
                if (i + 1 < aText.length() && aText.charAt(i + 1) == QUOTE) {
                    i += 2;
                    continue;
                }
                return i + 1;
            }
            i++;
        }

        return aText.length();
    }
}
