/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.export;

import java.io.UnsupportedEncodingException;

import com.ibm.as400.access.CharConverter;

/**
 * Checks the text of a stream file before it is copied into a member, so that
 * no characters are lost. The lines are measured like <code>CPYFRMSTMF</code>
 * does, verified on the IBM i:
 * <ul>
 * <li>Characters are counted, not bytes, e.g. one for an umlaut in UTF-8 or
 * for a character outside of the BMP.</li>
 * <li>A tab moves to the next tab stop (columns 1, 9, 17, ...).</li>
 * <li>Trailing blanks are not counted. <code>CPYFRMSTMF</code> reports them as
 * truncated (CPIA083), but only blanks are lost.</li>
 * <li>A byte order mark is removed.</li>
 * </ul>
 * Characters, that do not exist in the CCSID of the source file, are replaced
 * without a message.
 */
public final class SourceCheck {

    private static final int TAB_WIDTH = 8;
    private static final String SPACE = " "; //$NON-NLS-1$
    private static final byte EBCDIC_SPACE = 0x40;
    private static final String DOUBLE_BYTE_CHARACTER = "\u3042"; //$NON-NLS-1$
    private static final char BYTE_ORDER_MARK = '\uFEFF';
    private static final String LINE_SEPARATOR_PATTERN = "\\r\\n|\\r|\\n"; //$NON-NLS-1$

    private SourceCheck() {
        // Utility class
    }

    /**
     * Returns <code>true</code>, if a CCSID is an EBCDIC CCSID with one byte
     * per character, e.g. 1141, 273 or 37, which is the CCSID of new source
     * files. <code>CRTSRCPF</code> rejects ASCII and Unicode CCSIDs except of
     * 1208 (verified on the IBM i), and the lines are only measured correctly,
     * if each character is one byte, see {@link #getLength(String)}. Mixed
     * CCSIDs with double-byte characters, e.g. 930, are not supported.
     *
     * @param aCcsid - CCSID
     * @return <code>true</code> for single-byte EBCDIC CCSIDs, that IBM
     *         Toolbox for Java can convert
     */
    public static boolean isSingleByteEbcdic(int aCcsid) {

        try {
            CharConverter converter = new CharConverter(aCcsid);
            byte[] space = converter.stringToByteArray(SPACE);
            return space.length == 1 && space[0] == EBCDIC_SPACE && converter.stringToByteArray(DOUBLE_BYTE_CHARACTER).length == 1;
        } catch (UnsupportedEncodingException e) {
            return false;
        }
    }

    /**
     * Removes the byte order mark at the beginning of a text.
     *
     * @param aText - text
     * @return text without byte order mark
     */
    public static String removeByteOrderMark(String aText) {

        if (aText.length() > 0 && aText.charAt(0) == BYTE_ORDER_MARK) {
            return aText.substring(1);
        }

        return aText;
    }

    /**
     * Returns the length of a line in a member.
     *
     * @param aLine - line without line separator
     * @return number of characters without trailing blanks
     */
    public static int getLength(String aLine) {

        int end = aLine.length();
        while (end > 0 && Character.isWhitespace(aLine.charAt(end - 1))) {
            end--;
        }

        int length = 0;
        int i = 0;
        while (i < end) {
            int codePoint = aLine.codePointAt(i);
            if (codePoint == '\t') {
                length = (length / TAB_WIDTH + 1) * TAB_WIDTH;
            } else {
                length++;
            }
            i += Character.charCount(codePoint);
        }

        return length;
    }

    /**
     * Returns the longest line of a text.
     *
     * @param aText - text without byte order mark
     * @return longest line, the first one of equal lines
     */
    public static Line getLongestLine(String aText) {

        String[] lines = aText.split(LINE_SEPARATOR_PATTERN, -1);

        Line longestLine = new Line(0, 0);
        for (int i = 0; i < lines.length; i++) {
            int length = getLength(lines[i]);
            if (length > longestLine.length) {
                longestLine = new Line(i + 1, length);
            }
        }

        return longestLine;
    }

    /**
     * Returns the first line, that contains characters, that do not exist in a
     * CCSID.
     *
     * @param aText - text
     * @param aConverter - converter of the CCSID of the source file
     * @return line number or 0, if all characters exist
     */
    public static int findUnmappableLine(String aText, CharConverter aConverter) {

        String[] lines = aText.split(LINE_SEPARATOR_PATTERN, -1);
        for (int i = 0; i < lines.length; i++) {
            if (!lines[i].equals(aConverter.byteArrayToString(aConverter.stringToByteArray(lines[i])))) {
                return i + 1;
            }
        }

        return 0;
    }

    /**
     * Line number and length of a line.
     */
    public static final class Line {

        private final int number;
        private final int length;

        Line(int aNumber, int aLength) {
            this.number = aNumber;
            this.length = aLength;
        }

        /**
         * Returns the line number, starting at 1, or 0 for an empty text.
         */
        public int getNumber() {
            return number;
        }

        /**
         * Returns the length, see {@link SourceCheck#getLength(String)}.
         */
        public int getLength() {
            return length;
        }
    }
}
