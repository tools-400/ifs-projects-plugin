/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.sources;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.tools400.ifsprojects.utils.StringUtils;

/**
 * An <code>EXEC SQL INCLUDE</code> statement of an RPG source, found by
 * {@link RpgSqlStatements}. Unlike {@link RpgIncludeDirective}, the statement
 * may span several lines. Its name is either quoted (<code>'...'</code>,
 * <code>"..."</code>), which is a stream file, or unquoted, which is a member.
 * The name always stands in one line, because neither a name nor a literal can
 * be continued; therefore {@link #replaceName(String)} changes one line of the
 * statement and keeps the others.
 */
public final class RpgSqlIncludeDirective {

    private final List<String> lines;
    private final int lineNumber;
    private final String text;
    private final String indentation;
    private final String keyword;
    private final String name;
    private final boolean isQuoted;
    private final int nameLine;
    private final int nameStart;
    private final int nameEnd;
    private final int maxLength;

    RpgSqlIncludeDirective(List<String> aLines, int aLineNumber, String aText, String anIndentation, String aKeyword, String aName,
        boolean anIsQuoted, int aNameLine, int aNameStart, int aNameEnd, int aMaxLength) {
        this.lines = Collections.unmodifiableList(new ArrayList<String>(aLines));
        this.lineNumber = aLineNumber;
        this.text = aText;
        this.indentation = anIndentation;
        this.keyword = aKeyword;
        this.name = aName;
        this.isQuoted = anIsQuoted;
        this.nameLine = aNameLine;
        this.nameStart = aNameStart;
        this.nameEnd = aNameEnd;
        this.maxLength = aMaxLength;
    }

    /**
     * Returns the line number of the first line of the statement, starting
     * at 1.
     */
    public int getFirstLineNumber() {
        return lineNumber;
    }

    /**
     * Returns the number of lines of the statement.
     */
    public int getLineCount() {
        return lines.size();
    }

    /**
     * Returns the line number of the name, starting at 1, which is the line
     * that {@link #replaceName(String)} changes.
     */
    public int getLineNumber() {
        return lineNumber + nameLine;
    }

    /**
     * Returns the text of the statement without the line breaks, e.g.
     * <code>INCLUDE HOSTVARS</code>, for messages.
     */
    public String getText() {
        return text;
    }

    /**
     * Returns the characters, in front of which a compiler directive of the
     * source would start: the blanks of the columns 1 to 6 of a fixed format
     * source, otherwise the indentation of the first line.
     */
    public String getIndentation() {
        return indentation;
    }

    /**
     * Returns the keyword <code>INCLUDE</code> in the case of the source.
     */
    public String getKeyword() {
        return keyword;
    }

    /**
     * Returns the name without quotes, e.g. <code>HOSTVARS</code> or
     * <code>qcpysrc/hostvars.sqlrpgleinc</code>, or <code>null</code>, if the
     * statement has no name, that can be replaced, e.g. because it is
     * continued in the middle of the name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns <code>true</code>, if the name is quoted.
     */
    public boolean isQuoted() {
        return isQuoted;
    }

    /**
     * Returns the lines of the statement, unchanged, separated by
     * {@link RpgLineConverter#LINE_SEPARATOR}.
     */
    public String getStatement() {
        return join(lines);
    }

    /**
     * Returns the lines of the statement with another name. The rest of the
     * lines is kept; only if the line of the name becomes too long, its
     * trailing blanks are removed.
     *
     * @param aName - new name including the quotes, if it shall be quoted
     * @return lines separated by {@link RpgLineConverter#LINE_SEPARATOR}, or
     *         <code>null</code>, if the line is longer than the columns of the
     *         source allow
     */
    public String replaceName(String aName) {

        String line = lines.get(nameLine);
        String code = line.length() > maxLength ? line.substring(0, maxLength) : line;
        String comment = line.substring(code.length());

        String converted = code.substring(0, nameStart) + aName + code.substring(nameEnd);
        if (converted.length() > maxLength) {
            converted = StringUtils.trimRight(converted);
            if (converted.length() > maxLength) {
                return null;
            }
        }

        if (comment.length() > 0) {
            converted = StringUtils.padRight(converted, maxLength) + comment;
        }

        List<String> replaced = new ArrayList<String>(lines);
        replaced.set(nameLine, converted);

        return join(replaced);
    }

    private static String join(List<String> aLines) {

        StringBuilder buffer = new StringBuilder();
        for (String line : aLines) {
            if (buffer.length() > 0) {
                buffer.append(RpgLineConverter.LINE_SEPARATOR);
            }
            buffer.append(line);
        }

        return buffer.toString();
    }

    @Override
    public String toString() {
        return getLineNumber() + ": " + text; //$NON-NLS-1$
    }
}
