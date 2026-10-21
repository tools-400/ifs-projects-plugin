/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.sources;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.tools400.ifsprojects.utils.StringUtils;

/**
 * Finds the <code>EXEC SQL INCLUDE</code> statements of an RPG source, which
 * may span several lines (IBM: <i>Using SQL statements in ILE RPG</i>):
 * <ul>
 * <li>Free format (<code>**FREE</code> and the <code>/FREE</code> blocks of
 * fixed format sources): the statement starts with <code>EXEC SQL</code>, its
 * text may begin on the same line, it is continued on the following lines
 * without a continuation character and ends with a semicolon.</li>
 * <li>Fixed format: the statement starts with <code>/EXEC SQL</code> in the
 * positions 7 to 15, its text may begin on the same line, it is continued on
 * lines with a <code>+</code> in position 7 and ends with
 * <code>/END-EXEC</code> in the positions 7 to 15. It never reaches beyond
 * position 80, the columns 81 and later are comments.</li>
 * <li>Comment lines and the compile-time data after the first <code>**</code>
 * line are not code, see {@link RpgLineConverter}. The comments of SQL
 * (<code>--</code>) and of free format RPG (<code>//</code>) are removed.</li>
 * </ul>
 * A statement, whose first keyword is <code>INCLUDE</code>, becomes an
 * {@link RpgSqlIncludeDirective}; <code>SQLCA</code> and <code>SQLDA</code> are
 * skipped, because they are no copy books. If the name cannot be isolated,
 * e.g. because the statement is not terminated, the directive has no name, so
 * that the converters report it as a problem.
 */
public final class RpgSqlStatements {

    private static final Pattern EXEC_SQL = Pattern.compile("\\bEXEC\\s+SQL\\b", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$
    private static final Pattern INCLUDE = Pattern.compile("^\\s*(INCLUDE)(?=\\s|$)", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$

    private static final String EXEC_SQL_DELIMITER = "/EXEC SQL"; //$NON-NLS-1$
    private static final String END_EXEC_DELIMITER = "/END-EXEC"; //$NON-NLS-1$
    private static final String FREE_DIRECTIVE = "/FREE"; //$NON-NLS-1$
    private static final String END_FREE_DIRECTIVE = "/END-FREE"; //$NON-NLS-1$
    private static final String SQL_COMMENT = "--"; //$NON-NLS-1$
    private static final String LINE_COMMENT = "//"; //$NON-NLS-1$
    private static final String COMPILE_TIME_DATA = "**"; //$NON-NLS-1$
    private static final String SQLCA = "SQLCA"; //$NON-NLS-1$
    private static final String SQLDA = "SQLDA"; //$NON-NLS-1$
    private static final String EMPTY = ""; //$NON-NLS-1$

    /** Number of columns of fixed format sources, that contain code. */
    private static final int FIXED_LENGTH = 80;

    /** Index of column 7, in which the delimiters of fixed format sources stand. */
    private static final int DELIMITER_INDEX = 6;

    /** Blanks of the columns 1 to 6, in front of which a directive starts. */
    private static final String FIXED_INDENTATION = "      "; //$NON-NLS-1$

    private static final char CONTINUATION = '+';
    private static final char COMMENT = '*';
    private static final char STATEMENT_END = ';';

    private RpgSqlStatements() {
        // Utility class
    }

    /**
     * Returns the <code>EXEC SQL INCLUDE</code> statements of a source in the
     * order of the source.
     *
     * @param aContent - content of the source
     * @return statements
     */
    public static List<RpgSqlIncludeDirective> find(String aContent) {

        List<String> lines = new ArrayList<String>();
        for (String[] line : IncludeConverter.splitLines(aContent)) {
            lines.add(line[0]);
        }

        return find(lines);
    }

    private static List<RpgSqlIncludeDirective> find(List<String> aLines) {

        List<RpgSqlIncludeDirective> includes = new ArrayList<RpgSqlIncludeDirective>();

        boolean isFree = !aLines.isEmpty() && RpgLineConverter.isFree(aLines.get(0));
        boolean isFreeFormat = isFree;
        int maxLength = isFree ? Integer.MAX_VALUE : FIXED_LENGTH;
        Statement statement = null;

        for (int i = 0; i < aLines.size(); i++) {

            String line = aLines.get(i);

            if (i == 0 && isFree) {
                continue;
            }

            if (line.startsWith(COMPILE_TIME_DATA)) {
                break;
            }

            String code = line;
            if (!isFree) {
                if (line.length() > DELIMITER_INDEX && line.charAt(DELIMITER_INDEX) == COMMENT) {
                    continue;
                }
                code = line.length() > FIXED_LENGTH ? line.substring(0, FIXED_LENGTH) : line;
            }

            if (statement != null) {
                append(statement, i, code);
                if (!statement.isOpen) {
                    add(includes, toDirective(statement, aLines, isFree, maxLength));
                    statement = null;
                }
                continue;
            }

            if (!isFree) {
                String directive = code.trim().toUpperCase(Locale.ROOT);
                if (directive.startsWith(END_FREE_DIRECTIVE)) {
                    isFreeFormat = false;
                    continue;
                }
                if (directive.startsWith(FREE_DIRECTIVE)) {
                    isFreeFormat = true;
                    continue;
                }
            }

            statement = start(i, code, isFreeFormat);
            if (statement != null && !statement.isOpen) {
                add(includes, toDirective(statement, aLines, isFree, maxLength));
                statement = null;
            }
        }

        if (statement != null) {
            add(includes, toDirective(statement, aLines, isFree, maxLength));
        }

        return includes;
    }

    private static void add(List<RpgSqlIncludeDirective> anIncludes, RpgSqlIncludeDirective aDirective) {

        if (aDirective != null) {
            anIncludes.add(aDirective);
        }
    }

    /**
     * Starts a statement, if a line contains the starting delimiter.
     *
     * @return statement or <code>null</code>, if the line has no delimiter
     */
    private static Statement start(int aLineIndex, String aCode, boolean anIsFreeFormat) {

        if (!anIsFreeFormat) {
            if (isDelimiter(aCode, EXEC_SQL_DELIMITER)) {
                Statement statement = new Statement(aLineIndex, false);
                append(statement, aLineIndex, aCode, DELIMITER_INDEX + EXEC_SQL_DELIMITER.length());
                return statement;
            }
            return null;
        }

        String code = removeComment(aCode, LINE_COMMENT);
        Matcher matcher = EXEC_SQL.matcher(code);
        while (matcher.find()) {
            String prefix = StringUtils.trimRight(code.substring(0, matcher.start()));
            if (prefix.length() == 0 || prefix.charAt(prefix.length() - 1) == STATEMENT_END) {
                Statement statement = new Statement(aLineIndex, true);
                append(statement, aLineIndex, aCode, matcher.end());
                return statement;
            }
        }

        return null;
    }

    /**
     * Appends the text of a continuation line to a statement. A line, that is
     * no valid continuation, closes the statement without completing it.
     */
    private static void append(Statement aStatement, int aLineIndex, String aCode) {

        if (aStatement.isFreeFormat) {
            append(aStatement, aLineIndex, aCode, 0);
            return;
        }

        if (isDelimiter(aCode, END_EXEC_DELIMITER)) {
            aStatement.lastLine = aLineIndex;
            aStatement.isOpen = false;
            aStatement.isComplete = true;
            return;
        }

        if (aCode.length() > DELIMITER_INDEX && aCode.charAt(DELIMITER_INDEX) == CONTINUATION) {
            append(aStatement, aLineIndex, aCode, DELIMITER_INDEX + 1);
            return;
        }

        aStatement.isOpen = false;
    }

    /**
     * Appends the text of a line, that starts at an index, to a statement and
     * completes the statement, if the text contains the semicolon of free
     * format.
     */
    private static void append(Statement aStatement, int aLineIndex, String aCode, int aStart) {

        String text = aStart >= aCode.length() ? EMPTY : aCode.substring(aStart);
        if (aStatement.isFreeFormat) {
            text = removeComment(text, LINE_COMMENT);
        }
        text = removeComment(text, SQL_COMMENT);

        if (aStatement.isFreeFormat) {
            int end = text.indexOf(STATEMENT_END);
            if (end >= 0) {
                text = text.substring(0, end);
                aStatement.isOpen = false;
                aStatement.isComplete = true;
            }
        }

        aStatement.lastLine = aLineIndex;
        aStatement.fragments.add(new Fragment(aLineIndex, aStart, text));
    }

    /**
     * Returns <code>true</code>, if a line contains a delimiter of fixed
     * format in the positions 7 to 15.
     */
    private static boolean isDelimiter(String aCode, String aDelimiter) {
        return aCode.length() >= DELIMITER_INDEX + aDelimiter.length()
            && aCode.regionMatches(true, DELIMITER_INDEX, aDelimiter, 0, aDelimiter.length());
    }

    private static String removeComment(String aText, String aComment) {

        int index = aText.indexOf(aComment);

        return index < 0 ? aText : aText.substring(0, index);
    }

    /**
     * Returns the directive of a statement, whose first keyword is
     * <code>INCLUDE</code>.
     *
     * @return directive or <code>null</code>, if the statement is no include
     *         or includes <code>SQLCA</code> or <code>SQLDA</code>
     */
    private static RpgSqlIncludeDirective toDirective(Statement aStatement, List<String> aLines, boolean anIsFree, int aMaxLength) {

        StringBuilder buffer = new StringBuilder();
        List<Integer> starts = new ArrayList<Integer>();
        for (Fragment fragment : aStatement.fragments) {
            if (buffer.length() > 0) {
                buffer.append(' ');
            }
            starts.add(Integer.valueOf(buffer.length()));
            buffer.append(fragment.text);
        }

        String text = buffer.toString();
        Matcher include = INCLUDE.matcher(text);
        if (!include.find()) {
            return null;
        }

        String name = null;
        boolean isQuoted = false;
        int nameLine = 0;
        int nameStart = 0;
        int nameEnd = 0;

        int[] span = aStatement.isComplete ? getName(text, include.end()) : null;
        if (span != null) {
            isQuoted = span[2] != 0;
            name = isQuoted ? text.substring(span[0] + 1, span[1] - 1) : text.substring(span[0], span[1]);
            if (!isQuoted && (SQLCA.equalsIgnoreCase(name) || SQLDA.equalsIgnoreCase(name))) {
                return null;
            }

            int index = getFragment(aStatement, starts, span[0], span[1]);
            if (index < 0) {
                name = null;
            } else {
                Fragment fragment = aStatement.fragments.get(index);
                int offset = fragment.start - starts.get(index).intValue();
                nameLine = fragment.lineIndex - aStatement.firstLine;
                nameStart = span[0] + offset;
                nameEnd = span[1] + offset;
            }
        }

        List<String> lines = aLines.subList(aStatement.firstLine, aStatement.lastLine + 1);
        String indentation = anIsFree ? getIndentation(aLines.get(aStatement.firstLine)) : FIXED_INDENTATION;

        return new RpgSqlIncludeDirective(lines, aStatement.firstLine + 1, text.trim(), indentation, include.group(1), name, isQuoted, nameLine,
            nameStart, nameEnd, aMaxLength);
    }

    private static String getIndentation(String aLine) {

        int index = StringUtils.indexOfNonBlank(aLine, 0);

        return index <= 0 ? EMPTY : aLine.substring(0, index);
    }

    /**
     * Returns the name of an include statement.
     *
     * @param aText - text of the statement
     * @param aStart - index behind the keyword <code>INCLUDE</code>
     * @return start index, end index behind the name and 1 for a quoted name,
     *         or <code>null</code>, if the statement has no name
     */
    private static int[] getName(String aText, int aStart) {

        int start = StringUtils.indexOfNonBlank(aText, aStart);
        if (start < 0) {
            return null;
        }

        char quote = aText.charAt(start);
        if (quote == '\'' || quote == '"') {
            int end = aText.indexOf(quote, start + 1);
            if (end <= start + 1) {
                return null;
            }
            return new int[] { start, end + 1, 1 };
        }

        int end = start;
        while (end < aText.length() && !Character.isWhitespace(aText.charAt(end)) && aText.charAt(end) != STATEMENT_END) {
            end++;
        }

        return new int[] { start, end, 0 };
    }

    /**
     * Returns the index of the fragment, that contains the name, or -1, if the
     * name is spread over several lines.
     */
    private static int getFragment(Statement aStatement, List<Integer> aStarts, int aStart, int anEnd) {

        for (int i = 0; i < aStatement.fragments.size(); i++) {
            int start = aStarts.get(i).intValue();
            int end = start + aStatement.fragments.get(i).text.length();
            if (aStart >= start && anEnd <= end) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Statement, that is being collected.
     */
    private static final class Statement {

        private final int firstLine;
        private final boolean isFreeFormat;
        private final List<Fragment> fragments = new ArrayList<Fragment>();

        private int lastLine;
        private boolean isOpen = true;
        private boolean isComplete;

        Statement(int aFirstLine, boolean anIsFreeFormat) {
            this.firstLine = aFirstLine;
            this.isFreeFormat = anIsFreeFormat;
            this.lastLine = aFirstLine;
        }
    }

    /**
     * Text of a statement, that stands in one line.
     */
    private static final class Fragment {

        private final int lineIndex;
        private final int start;
        private final String text;

        Fragment(int aLineIndex, int aStart, String aText) {
            this.lineIndex = aLineIndex;
            this.start = aStart;
            this.text = aText;
        }
    }
}
