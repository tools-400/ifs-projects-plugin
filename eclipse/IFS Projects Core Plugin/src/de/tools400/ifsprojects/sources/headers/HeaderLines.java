/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.sources.headers;

import java.util.ArrayList;
import java.util.List;

/**
 * The first lines of a source, in which the parsers of the build tools search
 * the header, see {@link ISourceHeaderParser}. A tag further down in the
 * source is no header, e.g. in a comment of the code.
 */
public final class HeaderLines {

    /** Number of lines at the start of a source, in which the header is searched. */
    public static final int MAX_LINES = 100;

    private static final char LF = '\n';
    private static final char CR = '\r';
    private static final char TABULATOR = '\t';
    private static final char BLANK = ' ';

    private HeaderLines() {
        // Utility class
    }

    /**
     * Returns the first lines of a source without trailing blanks, see
     * {@link #trimRight(String)}.
     *
     * @param aContent - content of the source with the line separator
     *        <code>\n</code>
     * @return at most {@link #MAX_LINES} lines
     */
    public static List<String> read(String aContent) {

        List<String> lines = new ArrayList<String>();

        int start = 0;
        while (lines.size() < MAX_LINES && start <= aContent.length()) {
            int end = aContent.indexOf(LF, start);
            if (end < 0) {
                lines.add(trimRight(aContent.substring(start)));
                break;
            }
            lines.add(trimRight(aContent.substring(start, end)));
            start = end + 1;
        }

        return lines;
    }

    /**
     * Returns a line without trailing blanks, tabulators and carriage returns.
     *
     * @param aLine - line
     * @return line without trailing white space
     */
    public static String trimRight(String aLine) {

        int end = aLine.length();
        while (end > 0 && (aLine.charAt(end - 1) == BLANK || aLine.charAt(end - 1) == TABULATOR || aLine.charAt(end - 1) == CR)) {
            end--;
        }

        return aLine.substring(0, end);
    }
}
