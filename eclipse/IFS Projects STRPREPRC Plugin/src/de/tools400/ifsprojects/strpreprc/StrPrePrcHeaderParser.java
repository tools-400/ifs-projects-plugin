/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.strpreprc;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.tools400.ifsprojects.sources.headers.CreateCommand;
import de.tools400.ifsprojects.sources.headers.ISourceHeaderParser;

/**
 * Reads the create commands of the <code>STRPREPRC</code> header in the first
 * lines of a source, e.g.
 *
 * <pre>
 *  //&gt;&gt;PRE-COMPILER&lt;&lt;
 *  //&gt;&gt;CRTCMD&lt;&lt; CRTSQLRPGI OBJ(&amp;LI/&amp;OB) SRCSTMF('&amp;STMF') +
 *  //            INCFILE(&amp;SL/QCPYSRC) RPGPPOPT(*LVL2);
 *  //&gt;&gt;END-PRE-COMPILER&lt;&lt;
 * </pre>
 *
 * A command starts behind the tag <code>&gt;&gt;CRTCMD&lt;&lt;</code> and ends
 * with a semicolon. A line, that ends with <code>+</code> or <code>-</code>, is
 * continued by the next line of the header, from which the comment characters
 * of the source are removed: <code>//</code> of free form RPG, <code>*</code>
 * of fixed form RPG, the start of a comment of CL and <code>--</code> of SQL. A
 * line, that is no comment or that contains another tag, ends the command, so
 * that a header without a semicolon does not swallow the source.
 * <p>
 * Only the create commands are read. The commands of
 * <code>&gt;&gt;EXECUTE&lt;&lt;</code> do not create the object of the source.
 */
public final class StrPrePrcHeaderParser implements ISourceHeaderParser {

    /** Tag of a create command. */
    private static final String CREATE_COMMAND_TAG = ">>CRTCMD<<"; //$NON-NLS-1$

    /** Start of the tags of the header, e.g. of >>END-PRE-COMPILER<<. */
    private static final String TAG = ">>"; //$NON-NLS-1$

    /** Number of lines, in which the header is searched. */
    private static final int HEADER_LINES = 100;

    private static final String RPG_COMMENT = "//"; //$NON-NLS-1$
    private static final String CL_COMMENT = "/*"; //$NON-NLS-1$
    private static final String SQL_COMMENT = "--"; //$NON-NLS-1$
    private static final String CL_COMMENT_END = "*/"; //$NON-NLS-1$
    private static final char FIXED_FORM_COMMENT = '*';

    /** Start of **FREE and of the compile time data, which are no comments. */
    private static final String DOUBLE_ASTERISK = "**"; //$NON-NLS-1$

    /** Continuation, that separates the lines with a blank. */
    private static final char CONTINUATION_WITH_BLANK = '+';

    /** Continuation, that joins the lines without a blank. */
    private static final char CONTINUATION = '-';

    private static final char END_OF_COMMAND = ';';
    private static final char QUOTE = '\'';
    private static final char LF = '\n';
    private static final char CR = '\r';
    private static final char TABULATOR = '\t';
    private static final char BLANK = ' ';

    public List<CreateCommand> parse(String aContent, String aSourceType) {

        List<String> lines = getHeaderLines(aContent);

        List<CreateCommand> commands = new ArrayList<CreateCommand>();
        for (int i = 0; i < lines.size(); i++) {

            int tag = indexOfTag(lines.get(i));
            if (tag < 0) {
                continue;
            }

            StringBuilder text = new StringBuilder();
            String line = lines.get(i).substring(tag + CREATE_COMMAND_TAG.length());

            while (true) {

                line = removeCommentEnd(line);

                int end = indexOfEndOfCommand(line);
                if (end >= 0) {
                    text.append(line.substring(0, end));
                    break;
                }

                char continuation = getContinuation(line);
                if (continuation == 0) {
                    text.append(line);
                    break;
                }

                text.append(line.substring(0, line.length() - 1));

                String next = i + 1 < lines.size() ? removeCommentCharacters(lines.get(i + 1)) : null;
                if (next == null || next.contains(TAG)) {
                    break;
                }

                if (continuation == CONTINUATION_WITH_BLANK) {
                    text.append(BLANK);
                }

                i++;
                line = next;
            }

            CreateCommand command = CreateCommand.parse(text.toString());
            if (command != null) {
                commands.add(command);
            }
        }

        return commands;
    }

    /**
     * Returns the first lines of a source, in which the header stands.
     */
    private static List<String> getHeaderLines(String aContent) {

        List<String> lines = new ArrayList<String>();

        int start = 0;
        while (lines.size() < HEADER_LINES && start <= aContent.length()) {
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
     * Returns the index of the tag of a create command or <code>-1</code>.
     */
    private static int indexOfTag(String aLine) {
        return aLine.toUpperCase(Locale.ROOT).indexOf(CREATE_COMMAND_TAG);
    }

    /**
     * Removes the end of a CL comment, which stands behind the command, and
     * trailing blanks.
     */
    private static String removeCommentEnd(String aLine) {

        String line = trimRight(aLine);
        if (line.endsWith(CL_COMMENT_END)) {
            line = trimRight(line.substring(0, line.length() - CL_COMMENT_END.length()));
        }

        return line;
    }

    /**
     * Returns the line without its comment characters or <code>null</code>, if
     * the line is empty or no comment and therefore ends the command.
     */
    private static String removeCommentCharacters(String aLine) {

        int i = 0;
        while (i < aLine.length() && Character.isWhitespace(aLine.charAt(i))) {
            i++;
        }

        if (i >= aLine.length()) {
            return null;
        }

        if (aLine.startsWith(RPG_COMMENT, i) || aLine.startsWith(CL_COMMENT, i) || aLine.startsWith(SQL_COMMENT, i)) {
            return aLine.substring(i + 2);
        }

        if (aLine.startsWith(DOUBLE_ASTERISK, i)) {
            return null;
        }

        if (aLine.charAt(i) == FIXED_FORM_COMMENT) {
            return aLine.substring(i + 1);
        }

        return null;
    }

    /**
     * Returns the continuation character at the end of a line or <code>0</code>,
     * if the line is not continued.
     */
    private static char getContinuation(String aLine) {

        if (aLine.isEmpty()) {
            return 0;
        }

        char last = aLine.charAt(aLine.length() - 1);

        return last == CONTINUATION_WITH_BLANK || last == CONTINUATION ? last : 0;
    }

    /**
     * Returns the index of the semicolon, that ends the command, or
     * <code>-1</code>. A semicolon in a quoted string is part of the command.
     */
    private static int indexOfEndOfCommand(String aLine) {

        int i = 0;
        while (i < aLine.length()) {
            char character = aLine.charAt(i);
            if (character == QUOTE) {
                i = skipString(aLine, i);
                continue;
            }
            if (character == END_OF_COMMAND) {
                return i;
            }
            i++;
        }

        return -1;
    }

    /**
     * Returns the index behind the quoted string, that starts at the given
     * index. Two quotes are one quote of the string.
     */
    private static int skipString(String aText, int anIndex) {

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

    private static String trimRight(String aText) {

        int end = aText.length();
        while (end > 0 && (aText.charAt(end - 1) == BLANK || aText.charAt(end - 1) == TABULATOR || aText.charAt(end - 1) == CR)) {
            end--;
        }

        return aText.substring(0, end);
    }
}
