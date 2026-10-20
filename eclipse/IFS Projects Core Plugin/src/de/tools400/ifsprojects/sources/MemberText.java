/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.sources;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import de.tools400.ifsprojects.utils.StringUtils;

/**
 * Reads and writes the member text in the comment block, that
 * <code>makei cvtsrcpf --text</code> of TOBi inserts at the beginning of a
 * stream file and TOBi uses as object text:
 *
 * <pre>
 *       *%METADATA                                                       *
 *       * %TEXT Display a message                                        *
 *       *%EMETADATA                                                      *
 * </pre>
 *
 * Like TOBi, <code>%METADATA</code> must be in the first 15 lines, followed by
 * <code>%TEXT</code> within 15 lines. The comment characters (<code>*</code>,
 * <code>//</code>, <code>/* *&#47;</code>, <code>--</code>, <code>.*</code>)
 * depend on the source type.
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

    /** Length of the lines of the comment block, including the comment end. */
    private static final int BLOCK_LINE_LENGTH = 72;

    /** Indentation of the comment block, except for panel groups. */
    private static final String INDENTATION = "      "; //$NON-NLS-1$

    private static final String FREE_COMMENT_START = "//"; //$NON-NLS-1$
    private static final String DEFAULT_LINE_SEPARATOR = "\n"; //$NON-NLS-1$

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

    /**
     * Inserts the comment block with the member text at the beginning of a
     * source, like <code>makei cvtsrcpf --text</code> of TOBi 3.4. In a
     * <code>**FREE</code> source, it is inserted after the first line. Unlike
     * TOBi, panel groups (<code>PNLGRP</code>) get the comment block, too.
     *
     * @param aContent - content of the source
     * @param aSourceType - source type of the member, e.g. <code>RPGLE</code>
     * @param aText - member text
     * @return content with the comment block or the content itself, if the
     *         text is empty, the source type has no comment style or the
     *         source has a member text already
     */
    public static String insert(String aContent, String aSourceType, String aText) {

        CommentStyle style = CommentStyle.forSourceType(aSourceType);
        if (style == null || StringUtils.isBlank(aText) || parse(aContent) != null) {
            return aContent;
        }

        String separator = getLineSeparator(aContent);
        int firstLineEnd = getFirstLineEnd(aContent);

        String commentStart = style.start;
        String head = ""; //$NON-NLS-1$
        String tail = aContent;
        if (style == CommentStyle.FIXED_FORM && RpgLineConverter.isFree(aContent.substring(0, firstLineEnd))) {
            commentStart = FREE_COMMENT_START;
            head = aContent.substring(0, firstLineEnd) + separator;
            tail = aContent.substring(Math.min(firstLineEnd + separator.length(), aContent.length()));
        }

        StringBuilder block = new StringBuilder();
        block.append(createBlockLine(style, commentStart, METADATA + ' ')).append(separator);
        block.append(createBlockLine(style, commentStart, ' ' + TEXT + ' ' + aText.trim())).append(separator);
        block.append(createBlockLine(style, commentStart, END_METADATA + ' ')).append(separator);

        return head + block + tail;
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

    private static String createBlockLine(CommentStyle aStyle, String aCommentStart, String aContent) {
        String line = aStyle.indentation + aCommentStart + aContent;
        return StringUtils.padRight(line, BLOCK_LINE_LENGTH - aStyle.end.length()) + aStyle.end;
    }

    private static String getLineSeparator(String aContent) {

        int index = getFirstLineEnd(aContent);
        if (index >= aContent.length()) {
            return DEFAULT_LINE_SEPARATOR;
        }

        if (aContent.startsWith("\r\n", index)) { //$NON-NLS-1$
            return "\r\n"; //$NON-NLS-1$
        }

        return aContent.substring(index, index + 1);
    }

    private static int getFirstLineEnd(String aContent) {

        for (int i = 0; i < aContent.length(); i++) {
            char c = aContent.charAt(i);
            if (c == '\n' || c == '\r') {
                return i;
            }
        }

        return aContent.length();
    }

    /**
     * Comment characters of the source types, from <code>COMMENT_STYLES</code>
     * of <code>makei/const.py</code> of TOBi.
     */
    private enum CommentStyle {

        /** C and CL style. */
        BLOCK("/*", "*/", INDENTATION, "CMD", "CMDSRC", "C", "CPP", "CLLE", "CLP", "SQLC", "SQLCPP", "BND", "ILESRVPGM", "BNDDIR", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$ //$NON-NLS-13$ //$NON-NLS-14$
            "DTAARA", "SYSTRG", "MSGF"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        /** SQL style. */
        SQL("--", "*", INDENTATION, "TABLE", "PFSQL", "VIEW", "SQLUDT", "SQLALIAS", "SQLSEQ", "SQLPRC", "SQLTRG", "SQLUDF", "SQL", "INDEX"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$ //$NON-NLS-13$

        /** Fixed form of RPG, DDS and COBOL ("COBOL" style of TOBi). */
        FIXED_FORM("*", "*", INDENTATION, "DSPF", "LF", "PF", "PRTF", "RPGLE", "SQLRPGLE", "CBLLE", "SQLCBLLE", "CBL", "RPG"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$

        /** Panel groups and menus. */
        PANEL(".*", "*", "", "PNLGRPSRC", "MENUSRC", "PNLGRP"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$

        private final String start;
        private final String end;
        private final String indentation;
        private final Set<String> sourceTypes;

        private CommentStyle(String aStart, String anEnd, String anIndentation, String... aSourceTypes) {
            this.start = aStart;
            this.end = anEnd;
            this.indentation = anIndentation;
            this.sourceTypes = new HashSet<String>(Arrays.asList(aSourceTypes));
        }

        static CommentStyle forSourceType(String aSourceType) {

            if (aSourceType == null) {
                return null;
            }

            String sourceType = aSourceType.toUpperCase(Locale.ROOT);
            for (CommentStyle style : values()) {
                if (style.sourceTypes.contains(sourceType)) {
                    return style;
                }
            }

            return null;
        }
    }
}
