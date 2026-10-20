/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.sources;

import java.util.regex.Pattern;

import de.tools400.ifsprojects.utils.StringUtils;

/**
 * Finds the <code>/COPY</code> and <code>/INCLUDE</code> directives in the
 * lines of an RPG source or copy book and lets the subclass convert them.
 * <ul>
 * <li>Fixed format: the directive starts in column 7 or later, columns 81 and
 * later are comments and keep their position. Comment lines (<code>*</code> in
 * column 7) are not changed. <code>**FREE</code>: any column.</li>
 * <li>The lines after the first line starting with <code>**</code> (except
 * <code>**FREE</code> in line 1) are compile-time data and are not
 * changed.</li>
 * </ul>
 */
public abstract class RpgLineConverter implements IncludeConverter.LineConverter {

    private static final Pattern FREE = Pattern.compile("^\\uFEFF?\\*\\*FREE\\b", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$
    private static final String COMPILE_TIME_DATA = "**"; //$NON-NLS-1$

    /** Number of columns of fixed format sources, that contain code. */
    private static final int FIXED_LENGTH = 80;

    /** Index of column 6 (form type) of fixed format sources. */
    private static final int FORM_TYPE_INDEX = 5;

    private static final char COMMENT = '*';

    /**
     * Separator of the lines, that {@link #convertDirective} may return instead
     * of one line.
     */
    public static final char LINE_SEPARATOR = '\n';

    private boolean isFree;
    private boolean isData;

    /**
     * Returns <code>true</code>, if the first line of a source starts with
     * <code>**FREE</code>.
     *
     * @param aFirstLine - first line of the source
     * @return <code>true</code>, if the source is in free format
     */
    static boolean isFree(String aFirstLine) {
        return FREE.matcher(aFirstLine).find();
    }

    public final String convert(int aLineNumber, String aLine) {

        if (aLineNumber == 1 && isFree(aLine)) {
            isFree = true;
            return aLine;
        }

        if (isData || aLine.startsWith(COMPILE_TIME_DATA)) {
            isData = true;
            return aLine;
        }

        return isFree ? convertFree(aLineNumber, aLine) : convertFixed(aLineNumber, aLine);
    }

    /**
     * Converts a directive.
     *
     * @param aLineNumber - line number, starting at 1
     * @param aDirective - directive
     * @return converted line, see {@link RpgIncludeDirective#replaceName}, or
     *         <code>null</code>, if the line is not changed
     */
    protected abstract String convertDirective(int aLineNumber, RpgIncludeDirective aDirective);

    /**
     * Checks a line of code, which has not been changed by
     * {@link #convertDirective}, e.g. for <code>EXEC SQL INCLUDE</code>.
     *
     * @param aLineNumber - line number, starting at 1
     * @param aCode - line without the comment columns of fixed format sources
     */
    protected abstract void checkCode(int aLineNumber, String aCode);

    private String convertFree(int aLineNumber, String aLine) {

        String converted = convertDirectiveAt(aLineNumber, aLine, StringUtils.indexOfNonBlank(aLine, 0), Integer.MAX_VALUE);
        if (converted != null) {
            return converted;
        }

        checkCode(aLineNumber, aLine);

        return aLine;
    }

    private String convertFixed(int aLineNumber, String aLine) {

        if (aLine.length() <= FORM_TYPE_INDEX + 1 || aLine.charAt(FORM_TYPE_INDEX + 1) == COMMENT) {
            return aLine;
        }

        String code = aLine.length() > FIXED_LENGTH ? aLine.substring(0, FIXED_LENGTH) : aLine;
        String comment = aLine.substring(code.length());

        if (Character.isWhitespace(code.charAt(FORM_TYPE_INDEX))) {
            String converted = convertDirectiveAt(aLineNumber, code, StringUtils.indexOfNonBlank(code, FORM_TYPE_INDEX), FIXED_LENGTH);
            if (converted != null) {
                return appendComment(converted, comment);
            }
        }

        checkCode(aLineNumber, code);

        return aLine;
    }

    /**
     * Appends the comment columns of a fixed format line to the converted
     * line. If the conversion produced several lines, the comment is appended
     * to the last one.
     */
    private static String appendComment(String aConverted, String aComment) {

        if (aComment.length() == 0) {
            return aConverted;
        }

        int lastLineStart = aConverted.lastIndexOf(LINE_SEPARATOR) + 1;
        String lastLine = aConverted.substring(lastLineStart);

        return aConverted.substring(0, lastLineStart) + StringUtils.padRight(lastLine, FIXED_LENGTH) + aComment;
    }

    /**
     * Converts the directive, that starts at an index of a line.
     *
     * @return converted line or <code>null</code>, if the line is not changed
     */
    private String convertDirectiveAt(int aLineNumber, String aCode, int aStart, int aMaxLength) {

        RpgIncludeDirective directive = RpgIncludeDirective.parse(aCode, aStart, aMaxLength);
        if (directive == null) {
            return null;
        }

        return convertDirective(aLineNumber, directive);
    }
}
