/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.compile;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Pattern;

import de.tools400.ifsprojects.sources.headers.CreateCommand;
import de.tools400.ifsprojects.sources.headers.HeaderLines;
import de.tools400.ifsprojects.sources.headers.ISourceHeaderParser;

/**
 * Reads the compile commands of the <code>COMPILE</code> command of Alan Campin
 * in the first lines of a source, e.g.
 *
 * <pre>
 *       *_&gt; CNLLSTSPLF SRCFILE(&#64;2/&#64;1) SRCMBR(&#64;3)
 *       *_&gt; DLTMOD MODULE(&#64;5/&#64;4)
 *       *_&gt; CRTSQLRPGI OBJ(&#64;5/&#64;4) +
 *       *_&gt;   SRCFILE(&#64;2/&#64;1) SRCMBR(&#64;3) +
 *       *_&gt;   OBJTYPE(*MODULE) OPTION(*EVENTF) COMMIT(*NONE)
 * </pre>
 *
 * A command starts behind <code>*_&gt;</code>, which may stand anywhere in a
 * line, and ends at the end of the line. In CL and SQL sources, the header is a
 * comment: the <code>*&#47;</code> at the end of a line is removed. A line,
 * that ends with <code>+</code> or <code>-</code>, is continued by the next
 * line, which repeats <code>*_&gt;</code>; like in CL, <code>+</code> skips the
 * leading blanks of the next line, <code>-</code> keeps them. A line without
 * the marker ends the command, so that an unterminated one does not swallow the
 * source.
 * <p>
 * The substitution variables of the build tool are <code>&#64;1</code> to
 * <code>&#64;15</code>, e.g. <code>&#64;2</code> for the library of the source
 * and <code>&#64;5</code> for the library of the object. They are returned with
 * <code>&amp;</code>, e.g. <code>&amp;2</code>, so that a library, that is a
 * variable, is recognized as such.
 * <p>
 * No other header contains the marker, so the priority of the parser does not
 * matter. It is the lowest of the parsers of this repository.
 */
public final class CompileHeaderParser implements ISourceHeaderParser {

    /** Start of a command. */
    private static final String TAG = "*_>"; //$NON-NLS-1$

    /** End of a CL comment, which encloses a line of the header. */
    private static final String CL_COMMENT_END = "*/"; //$NON-NLS-1$

    /** Continuation, that skips the leading blanks of the next line. */
    private static final char CONTINUATION_SKIPPING_BLANKS = '+';

    /** Continuation, that keeps the leading blanks of the next line. */
    private static final char CONTINUATION = '-';

    /** Start of a substitution variable of the build tool, e.g. @2. */
    private static final Pattern SUBSTITUTION_VARIABLE = Pattern.compile("@(?=[0-9])"); //$NON-NLS-1$

    /** Start of a substitution variable, that the core recognizes. */
    private static final String VARIABLE = "&"; //$NON-NLS-1$

    @Override
    public List<CreateCommand> parse(String aContent, String aSourceType) {

        List<CreateCommand> commands = new ArrayList<>();

        ListIterator<String> lines = HeaderLines.read(aContent).listIterator();
        while (lines.hasNext()) {

            String line = lines.next();
            int tag = line.indexOf(TAG);
            if (tag < 0) {
                continue;
            }

            String text = readCommand(line.substring(tag + TAG.length()), lines);
            CreateCommand command = CreateCommand.parse(replaceSubstitutionVariables(text));
            if (command != null) {
                commands.add(command);
            }
        }

        return commands;
    }

    /**
     * Returns the text of a command. A continued line is joined with the next
     * line, if that repeats the tag.
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
            if (!isContinued(line)) {
                return text.append(line).toString();
            }

            char continuation = line.charAt(line.length() - 1);
            text.append(line.substring(0, line.length() - 1));

            line = readContinuationLine(aLines);
            if (line != null && continuation == CONTINUATION_SKIPPING_BLANKS) {
                line = line.trim();
            }
        }

        return text.toString();
    }

    /**
     * Returns the text behind the tag of the next line, if it continues a
     * command, i.e. if it repeats the tag. Otherwise the line is not read and
     * <code>null</code> is returned.
     */
    private static String readContinuationLine(ListIterator<String> aLines) {

        if (!aLines.hasNext()) {
            return null;
        }

        String line = aLines.next();
        int tag = line.indexOf(TAG);
        if (tag < 0) {
            aLines.previous();
            return null;
        }

        return line.substring(tag + TAG.length());
    }

    /**
     * Removes the end of a CL comment, which encloses the line, and trailing
     * blanks.
     */
    private static String removeCommentEnd(String aLine) {

        String line = HeaderLines.trimRight(aLine);
        if (line.endsWith(CL_COMMENT_END)) {
            line = HeaderLines.trimRight(line.substring(0, line.length() - CL_COMMENT_END.length()));
        }

        return line;
    }

    /**
     * Returns <code>true</code>, if the next line continues the command.
     */
    private static boolean isContinued(String aLine) {

        if (aLine.isEmpty()) {
            return false;
        }

        char lastCharacter = aLine.charAt(aLine.length() - 1);

        return lastCharacter == CONTINUATION_SKIPPING_BLANKS || lastCharacter == CONTINUATION;
    }

    /**
     * Returns the command with the substitution variables of the core, e.g.
     * <code>&amp;2/&amp;1</code> instead of <code>&#64;2/&#64;1</code>. An
     * <code>&#64;</code>, that is not followed by a digit, is kept, e.g. in
     * <code>TEXT('a&#64;b.com')</code>.
     */
    private static String replaceSubstitutionVariables(String aText) {
        return SUBSTITUTION_VARIABLE.matcher(aText).replaceAll(VARIABLE);
    }
}
