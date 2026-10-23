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
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.tools400.ifsprojects.sources.headers.CreateCommand;
import de.tools400.ifsprojects.sources.headers.HeaderLines;
import de.tools400.ifsprojects.sources.headers.ISourceHeaderParser;
import de.tools400.ifsprojects.sources.headers.QuotedStrings;

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
 * with a semicolon outside of a quoted string, which can span several lines.
 * A line, that ends with <code>+</code> or <code>-</code>, is continued by the
 * next line of the header, from which the comment characters of the source
 * are removed: <code>//</code> of free form RPG, <code>*</code> of fixed form
 * RPG, also in column 7 behind a form type, e.g.
 * <code>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;H*</code>, the start of a comment of CL
 * and <code>--</code> of SQL. A line, that is no comment or that contains
 * another tag, ends the command, so that a header without a semicolon does not
 * swallow the source.
 * <p>
 * Only the create commands are read. The commands of
 * <code>&gt;&gt;EXECUTE&lt;&lt;</code> do not create the object of the source.
 */
public final class StrPrePrcHeaderParser implements ISourceHeaderParser {

    /** Tag of a create command. */
    private static final String CREATE_COMMAND_TAG = ">>CRTCMD<<"; //$NON-NLS-1$

    /**
     * Tag of a create command in any case. The tag is searched in the line
     * itself, because upper case can change the length of a line, e.g.
     * <code>&szlig;</code> becomes <code>SS</code>.
     */
    private static final Pattern CREATE_COMMAND_TAG_PATTERN = Pattern.compile(Pattern.quote(CREATE_COMMAND_TAG), Pattern.CASE_INSENSITIVE);

    /**
     * A tag of the header, e.g. <code>&gt;&gt;END-PRE-COMPILER&lt;&lt;</code>.
     * Other text with <code>&gt;&gt;</code>, e.g.
     * <code>TEXT('a &gt;&gt; b')</code>, is no tag.
     */
    private static final Pattern TAG_PATTERN = Pattern.compile(">>[A-Z0-9_-]+<<", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$

    private static final String RPG_COMMENT = "//"; //$NON-NLS-1$
    private static final String CL_COMMENT = "/*"; //$NON-NLS-1$
    private static final String SQL_COMMENT = "--"; //$NON-NLS-1$
    private static final String CL_COMMENT_END = "*/"; //$NON-NLS-1$

    /** Start of a comment of free form RPG, of CL and of SQL. */
    private static final String[] COMMENT_STARTS = { RPG_COMMENT, CL_COMMENT, SQL_COMMENT };
    private static final char FIXED_FORM_COMMENT = '*';

    /** Index of column 6 of fixed form RPG, the form type, e.g. H or C. */
    private static final int FORM_TYPE_INDEX = 5;

    /** Index of column 7 of fixed form RPG, the asterisk of a comment. */
    private static final int FIXED_FORM_COMMENT_INDEX = 6;

    /** Start of **FREE and of the compile time data, which are no comments. */
    private static final String DOUBLE_ASTERISK = "**"; //$NON-NLS-1$

    /** Continuation, that separates the lines with a blank. */
    private static final char CONTINUATION_WITH_BLANK = '+';

    /** Continuation, that joins the lines without a blank. */
    private static final char CONTINUATION = '-';

    private static final char END_OF_COMMAND = ';';
    private static final char BLANK = ' ';

    @Override
    public List<CreateCommand> parse(String aContent, String aSourceType) {

        List<CreateCommand> commands = new ArrayList<CreateCommand>();

        ListIterator<String> lines = HeaderLines.read(aContent).listIterator();
        while (lines.hasNext()) {

            String line = lines.next();
            int tag = indexOfTag(line);
            if (tag < 0) {
                continue;
            }

            CreateCommand command = CreateCommand.parse(readCommand(line.substring(tag + CREATE_COMMAND_TAG.length()), lines));
            if (command != null) {
                commands.add(command);
            }
        }

        return commands;
    }

    /**
     * Returns the text of a command up to its semicolon. A continued line is
     * joined with the next line, if that is a comment without another tag.
     *
     * @param aFirstLine - text behind the tag
     * @param aLines - lines behind the line of the tag
     * @return text of the command
     */
    private static String readCommand(String aFirstLine, ListIterator<String> aLines) {

        StringBuilder text = new StringBuilder();

        String line = aFirstLine;
        while (line != null) {

            line = removeCommentEnd(line);

            int end = indexOfEndOfCommand(text, line);
            if (end >= 0) {
                return text.append(line.substring(0, end)).toString();
            }

            Continuation continuation = Continuation.of(line);
            if (continuation == Continuation.NONE) {
                return text.append(line).toString();
            }

            text.append(line.substring(0, line.length() - 1));

            line = readContinuationLine(aLines);
            if (line != null && continuation == Continuation.WITH_BLANK) {
                text.append(BLANK);
            }
        }

        return text.toString();
    }

    /**
     * Returns the next line without its comment characters, if it continues a
     * command, i.e. if it is a comment without another tag. Otherwise the line
     * is not read, so that it is searched for the next tag, and
     * <code>null</code> is returned.
     */
    private static String readContinuationLine(ListIterator<String> aLines) {

        if (!aLines.hasNext()) {
            return null;
        }

        String line = removeCommentCharacters(aLines.next());
        if (line == null || TAG_PATTERN.matcher(line).find()) {
            aLines.previous();
            return null;
        }

        return line;
    }

    /**
     * Returns the index of the tag of a create command or <code>-1</code>.
     */
    private static int indexOfTag(String aLine) {

        Matcher matcher = CREATE_COMMAND_TAG_PATTERN.matcher(aLine);

        return matcher.find() ? matcher.start() : -1;
    }

    /**
     * Removes the end of a CL comment, which stands behind the command, and
     * trailing blanks.
     */
    private static String removeCommentEnd(String aLine) {

        String line = HeaderLines.trimRight(aLine);
        if (line.endsWith(CL_COMMENT_END)) {
            line = HeaderLines.trimRight(line.substring(0, line.length() - CL_COMMENT_END.length()));
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

        for (String commentStart : COMMENT_STARTS) {
            if (aLine.startsWith(commentStart, i)) {
                return aLine.substring(i + commentStart.length());
            }
        }

        if (isFixedFormComment(aLine)) {
            return aLine.substring(FIXED_FORM_COMMENT_INDEX + 1);
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
     * Returns <code>true</code>, if the line is a comment of fixed form RPG: an
     * asterisk in column 7 behind a form type or a blank in column 6, e.g.
     * <code>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;H*</code>. Columns 1 - 5 may contain
     * a sequence number or a change mark.
     */
    private static boolean isFixedFormComment(String aLine) {

        if (aLine.length() <= FIXED_FORM_COMMENT_INDEX || aLine.charAt(FIXED_FORM_COMMENT_INDEX) != FIXED_FORM_COMMENT) {
            return false;
        }

        char formType = aLine.charAt(FORM_TYPE_INDEX);

        return formType == BLANK || Character.isLetter(formType);
    }

    /**
     * Returns the index of the semicolon in a line, that ends the command, or
     * <code>-1</code>. A semicolon in a quoted string is part of the command;
     * the string can start on a previous line of the command.
     *
     * @param aPreviousLines - text of the previous lines of the command
     * @param aLine - line of the command
     */
    private static int indexOfEndOfCommand(CharSequence aPreviousLines, String aLine) {

        String text = aPreviousLines + aLine;
        int i = 0;
        while (i < text.length()) {
            char character = text.charAt(i);
            if (character == QuotedStrings.QUOTE) {
                i = QuotedStrings.skip(text, i);
                continue;
            }
            if (character == END_OF_COMMAND && i >= aPreviousLines.length()) {
                return i - aPreviousLines.length();
            }
            i++;
        }

        return -1;
    }

    /**
     * Continuation of a line of a command by the next line.
     */
    private enum Continuation {

        /** The line is not continued. */
        NONE,

        /** <code>+</code>: the lines are separated by a blank. */
        WITH_BLANK,

        /** <code>-</code>: the lines are joined without a blank. */
        WITHOUT_BLANK;

        /**
         * Returns the continuation by the last character of a line.
         */
        static Continuation of(String aLine) {

            if (aLine.isEmpty()) {
                return NONE;
            }

            switch (aLine.charAt(aLine.length() - 1)) {
            case CONTINUATION_WITH_BLANK:
                return WITH_BLANK;
            case CONTINUATION:
                return WITHOUT_BLANK;
            default:
                return NONE;
            }
        }
    }
}
