/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.export;

import java.util.Locale;

/**
 * Reads the member text from the comment block, that
 * <code>makei cvtsrcpf --text</code> of TOBi inserts at the beginning of a
 * stream file and TOBi uses as object text:
 *
 * <pre>
 *      * %METADATA                                                      *
 *      *  %TEXT Display a message                                       *
 *      * %EMETADATA                                                     *
 * </pre>
 *
 * Like TOBi, <code>%METADATA</code> must be in the first 15 lines, followed by
 * <code>%TEXT</code> within 15 lines. The comment characters (<code>*</code>,
 * <code>//</code>, <code>/* *&#47;</code>, <code>--</code>, <code>.*</code>)
 * are removed.
 */
public final class MemberText {

    /** Maximum length of the text of a member. */
    public static final int MAX_LENGTH = 50;

    /** Number of lines, in which TOBi searches the keywords. */
    private static final int LINES_TO_CHECK = 15;

    private static final String METADATA = "%METADATA"; //$NON-NLS-1$
    private static final String END_METADATA = "%EMETADATA"; //$NON-NLS-1$
    private static final String TEXT = "%TEXT"; //$NON-NLS-1$
    private static final String LINE_SEPARATOR_PATTERN = "\\r?\\n|\\r"; //$NON-NLS-1$
    private static final String COMMENT_END_CHARACTERS = "*/"; //$NON-NLS-1$

    private MemberText() {
        // Utility class
    }

    /**
     * Returns the member text of a source.
     *
     * @param aContent - content of the stream file
     * @return text, at most {@link #MAX_LENGTH} characters, or
     *         <code>null</code>, if the source has no text
     */
    public static String parse(String aContent) {

        if (aContent == null) {
            return null;
        }

        String[] lines = aContent.split(LINE_SEPARATOR_PATTERN, 2 * LINES_TO_CHECK + 1);

        int metadataLine = -1;
        for (int i = 0; i < lines.length && i < LINES_TO_CHECK; i++) {
            if (lines[i].toUpperCase(Locale.ROOT).contains(METADATA)) {
                metadataLine = i;
                break;
            }
        }

        if (metadataLine < 0) {
            return null;
        }

        for (int i = metadataLine + 1; i < lines.length && i < metadataLine + LINES_TO_CHECK; i++) {
            String line = lines[i].toUpperCase(Locale.ROOT);
            if (line.contains(END_METADATA)) {
                return null;
            }
            int index = line.indexOf(TEXT);
            if (index >= 0) {
                return getText(lines[i].substring(index + TEXT.length()));
            }
        }

        return null;
    }

    private static String getText(String aValue) {

        String text = aValue.trim();
        int end = text.length();
        while (end > 0 && (COMMENT_END_CHARACTERS.indexOf(text.charAt(end - 1)) >= 0 || Character.isWhitespace(text.charAt(end - 1)))) {
            end--;
        }
        text = text.substring(0, end);

        if (text.length() == 0) {
            return null;
        }

        if (text.length() > MAX_LENGTH) {
            text = text.substring(0, MAX_LENGTH).trim();
        }

        return text;
    }
}
