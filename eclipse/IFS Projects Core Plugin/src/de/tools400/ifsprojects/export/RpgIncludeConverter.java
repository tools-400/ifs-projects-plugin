/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.export;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts the <code>/COPY</code> and <code>/INCLUDE</code> directives of RPG
 * sources and copy books.
 * <ul>
 * <li>A quoted name (<code>'...'</code>, <code>"..."</code>) is a stream file,
 * an unquoted name with a comma a member (unchanged). An unquoted name without
 * comma is converted, if it is a file of the project; otherwise it is kept as
 * member name, unless it contains a slash or a period.</li>
 * <li>The converted directive refers to the member without library, e.g.
 * <code>/COPY INC,CONSTS</code>, in the case of the directive.</li>
 * <li>Fixed format: the directive starts in column 7 or later, columns 81 and
 * later are comments and keep their position. <code>**FREE</code>: any
 * column.</li>
 * <li>The lines after the first line starting with <code>**</code> (except
 * <code>**FREE</code> in line 1) are compile-time data and are not
 * changed.</li>
 * <li><code>EXEC SQL INCLUDE</code> of a stream file cannot be converted,
 * because a member can only be specified with the parameter
 * <code>INCFILE</code> of the compiler.</li>
 * </ul>
 */
final class RpgIncludeConverter extends IncludeConverter {

    private static final Pattern FREE = Pattern.compile("^\\uFEFF?\\*\\*FREE\\b", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$
    private static final Pattern DIRECTIVE = Pattern.compile("(/COPY|/INCLUDE)(?=\\s|$)", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$
    private static final Pattern SQL_INCLUDE = Pattern.compile("\\bEXEC\\s+SQL\\s+INCLUDE\\s+(['\"])(.*?)(\\1|$)", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$

    private static final String COMPILE_TIME_DATA = "**"; //$NON-NLS-1$
    private static final String LINE_COMMENT = "//"; //$NON-NLS-1$

    /** Number of columns of fixed format sources, that contain code. */
    private static final int FIXED_LENGTH = 80;

    /** Index of column 6 (form type) of fixed format sources. */
    private static final int FORM_TYPE_INDEX = 5;

    private static final char COMMENT = '*';
    private static final char MEMBER_SEPARATOR = ',';
    private static final char SEPARATOR = '/';
    private static final char EXTENSION_SEPARATOR = '.';

    RpgIncludeConverter(IncludeResolver aResolver, Map<String, ExportMapping.Entry> aMembers) {
        super(aResolver, aMembers);
    }

    @Override
    protected LineConverter createLineConverter(final String aPath, final Result aResult) {

        return new LineConverter() {

            private boolean isFree;
            private boolean isData;

            public String convert(int aLineNumber, String aLine) {

                if (aLineNumber == 1 && FREE.matcher(aLine).find()) {
                    isFree = true;
                    return aLine;
                }

                if (isData || aLine.startsWith(COMPILE_TIME_DATA)) {
                    isData = true;
                    return aLine;
                }

                return isFree ? convertFree(aPath, aLineNumber, aLine, aResult) : convertFixed(aPath, aLineNumber, aLine, aResult);
            }
        };
    }

    private String convertFree(String aPath, int aLineNumber, String aLine, Result aResult) {

        String converted = convertDirective(aPath, aLineNumber, aLine, firstNonBlank(aLine, 0), Integer.MAX_VALUE, aResult);
        if (converted != null) {
            return converted;
        }

        checkSqlInclude(aLineNumber, aLine, aResult);

        return aLine;
    }

    private String convertFixed(String aPath, int aLineNumber, String aLine, Result aResult) {

        if (aLine.length() <= FORM_TYPE_INDEX + 1 || aLine.charAt(FORM_TYPE_INDEX + 1) == COMMENT) {
            return aLine;
        }

        String code = aLine.length() > FIXED_LENGTH ? aLine.substring(0, FIXED_LENGTH) : aLine;
        String comment = aLine.substring(code.length());

        if (Character.isWhitespace(code.charAt(FORM_TYPE_INDEX))) {
            String converted = convertDirective(aPath, aLineNumber, code, firstNonBlank(code, FORM_TYPE_INDEX), FIXED_LENGTH, aResult);
            if (converted != null) {
                if (comment.length() == 0) {
                    return converted;
                }
                StringBuilder line = new StringBuilder(converted);
                while (line.length() < FIXED_LENGTH) {
                    line.append(' ');
                }
                return line.append(comment).toString();
            }
        }

        checkSqlInclude(aLineNumber, code, aResult);

        return aLine;
    }

    /**
     * Converts a <code>/COPY</code> or <code>/INCLUDE</code> directive.
     *
     * @param aStart - index of the first non-blank character, at which the
     *        directive must start, or -1
     * @param aMaxLength - maximum length of the converted line
     * @return converted line or <code>null</code>, if the line has not been
     *         changed
     */
    private String convertDirective(String aPath, int aLineNumber, String aLine, int aStart, int aMaxLength, Result aResult) {

        if (aStart < 0) {
            return null;
        }

        Matcher matcher = DIRECTIVE.matcher(aLine);
        matcher.region(aStart, aLine.length());
        if (!matcher.lookingAt()) {
            return null;
        }

        String keyword = matcher.group(1);
        int nameStart = firstNonBlank(aLine, matcher.end());
        if (nameStart < 0) {
            return null;
        }

        String name;
        int nameEnd;
        boolean isPath;
        char quote = aLine.charAt(nameStart);
        if (quote == '\'' || quote == '"') {
            int end = aLine.indexOf(quote, nameStart + 1);
            if (end < 0) {
                return null;
            }
            name = aLine.substring(nameStart + 1, end);
            nameEnd = end + 1;
            isPath = true;
        } else {
            nameEnd = nameStart;
            while (nameEnd < aLine.length() && !Character.isWhitespace(aLine.charAt(nameEnd))) {
                nameEnd++;
            }
            name = aLine.substring(nameStart, nameEnd);
            if (name.indexOf(MEMBER_SEPARATOR) >= 0) {
                return null;
            }
            isPath = name.indexOf(SEPARATOR) >= 0 || name.indexOf(EXTENSION_SEPARATOR) >= 0;
        }

        String file = getResolver().resolveRpgInclude(aPath, name);
        if (file == null) {
            if (isPath) {
                aResult.addProblem(aLineNumber, name, Problem.NOT_FOUND);
            }
            return null;
        }

        ExportMapping.Entry member = getMember(file);
        if (member == null) {
            aResult.addProblem(aLineNumber, name, Problem.NOT_EXPORTED);
            return null;
        }

        String converted = aLine.substring(0, nameStart) + toCaseOf(member.getTarget(), keyword) + aLine.substring(nameEnd);
        if (converted.length() > aMaxLength) {
            aResult.addProblem(aLineNumber, name, Problem.TOO_LONG);
            return null;
        }

        aResult.addConverted(aLineNumber, name, member);

        return converted;
    }

    private void checkSqlInclude(int aLineNumber, String aLine, Result aResult) {

        int comment = aLine.indexOf(LINE_COMMENT);
        String code = comment < 0 ? aLine : aLine.substring(0, comment);

        Matcher matcher = SQL_INCLUDE.matcher(code);
        if (matcher.find()) {
            aResult.addProblem(aLineNumber, matcher.group(2), Problem.NOT_SUPPORTED);
        }
    }

    private static int firstNonBlank(String aLine, int aStart) {

        for (int i = aStart; i < aLine.length(); i++) {
            if (!Character.isWhitespace(aLine.charAt(i))) {
                return i;
            }
        }

        return -1;
    }
}
