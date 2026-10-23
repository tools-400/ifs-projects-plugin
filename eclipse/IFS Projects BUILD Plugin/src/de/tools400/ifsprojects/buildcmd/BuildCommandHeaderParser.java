/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.buildcmd;

import java.util.ArrayList;
import java.util.List;

import de.tools400.ifsprojects.sources.headers.CreateCommand;
import de.tools400.ifsprojects.sources.headers.HeaderLines;
import de.tools400.ifsprojects.sources.headers.ISourceHeaderParser;
import de.tools400.ifsprojects.sources.headers.QuotedStrings;

/**
 * Reads the compile commands of the <code>BUILD</code> command of Scott
 * Klement (https://www.scottklement.com/build/) in the first lines of a
 * source, e.g.
 *
 * <pre>
 *       * To compile:
 *       *&gt;  ign: DLTPGM PYUEMPLR4
 *       *&gt;  CRTBNDRPG PYUEMPLR4 SRCFILE(&amp;L/QRPGLESRC) DBGVIEW(*LIST) -
 *       *&gt;            INCFILE(&amp;L/QCPYSRC)
 *       *&gt;  CHGOBJOWN OBJ(PYUEMPLR4) OBJTYPE(*PGM) NEWOWN(KLOWN)
 * </pre>
 *
 * A command starts behind <code>*&gt;</code>, which may stand anywhere in a
 * line, and ends at the optional <code>&lt;*</code> outside of a quoted string
 * or at the end of the line.
 * A line, that ends with <code>-</code>, is continued by the next line, which
 * repeats <code>*&gt;</code>; a line without the marker ends the command, so
 * that an unterminated one does not swallow the source. The prefix
 * <code>IGN:</code>, with which the build tool ignores a failing command, is
 * removed.
 * <p>
 * The substitution variables of the build tool start with <code>&amp;</code>,
 * e.g. <code>&amp;L</code> for the library of the source and <code>&amp;ON</code>
 * for the name of the object, so that a library, that is a variable, is
 * recognized as such.
 * <p>
 * The marker is two characters only, so a source could contain it by chance.
 * The parser therefore has a lower priority than the one of
 * <code>STRPREPRC</code>, whose header is recognized by a tag of its own: a
 * fixed form header written as <code>*&gt;&gt;CRTCMD&lt;&lt;</code> contains
 * <code>*&gt;</code>, too, and is read by its own parser first.
 */
public final class BuildCommandHeaderParser implements ISourceHeaderParser {

    /** Start of a command. */
    private static final String START_TAG = "*>"; //$NON-NLS-1$

    /** Optional end of a command. */
    private static final String END_TAG = "<*"; //$NON-NLS-1$

    /** Prefix of a command, whose failure the build tool ignores. */
    private static final String IGNORE_PREFIX = "IGN:"; //$NON-NLS-1$

    private static final char CONTINUATION = '-';
    private static final char BLANK = ' ';

    public List<CreateCommand> parse(String aContent, String aSourceType) {

        List<String> lines = HeaderLines.read(aContent);

        List<CreateCommand> commands = new ArrayList<CreateCommand>();
        for (int i = 0; i < lines.size(); i++) {

            int tag = lines.get(i).indexOf(START_TAG);
            if (tag < 0) {
                continue;
            }

            StringBuilder text = new StringBuilder();
            String line = lines.get(i).substring(tag + START_TAG.length());

            while (true) {

                line = removeEndTag(text, line);

                if (!isContinued(line)) {
                    text.append(line);
                    break;
                }

                text.append(line.substring(0, line.length() - 1)).append(BLANK);

                int next = i + 1 < lines.size() ? lines.get(i + 1).indexOf(START_TAG) : -1;
                if (next < 0) {
                    break;
                }

                i++;
                line = lines.get(i).substring(next + START_TAG.length());
            }

            CreateCommand command = CreateCommand.parse(removeIgnorePrefix(text.toString()));
            if (command != null) {
                commands.add(command);
            }
        }

        return commands;
    }

    /**
     * Returns the text of a line before the end of the command and without
     * trailing blanks. An end tag in a quoted string is part of the command,
     * e.g. in <code>TEXT('a &lt;* b')</code>; the string can start on a
     * previous line of the command.
     *
     * @param aPreviousLines - text of the previous lines of the command
     * @param aLine - line behind the start tag
     */
    private static String removeEndTag(CharSequence aPreviousLines, String aLine) {

        String text = aPreviousLines + aLine;
        int i = 0;
        while (i < text.length()) {
            if (text.charAt(i) == QuotedStrings.QUOTE) {
                i = QuotedStrings.skip(text, i);
                continue;
            }
            if (i >= aPreviousLines.length() && text.startsWith(END_TAG, i)) {
                return HeaderLines.trimRight(aLine.substring(0, i - aPreviousLines.length()));
            }
            i++;
        }

        return HeaderLines.trimRight(aLine);
    }

    /**
     * Returns <code>true</code>, if the next line continues the command.
     */
    private static boolean isContinued(String aLine) {
        return !aLine.isEmpty() && aLine.charAt(aLine.length() - 1) == CONTINUATION;
    }

    /**
     * Returns the command without the prefix, with which the build tool
     * ignores its failure.
     */
    private static String removeIgnorePrefix(String aText) {

        // Not toUpperCase().startsWith(), which can change the length.
        String text = aText.trim();
        if (text.regionMatches(true, 0, IGNORE_PREFIX, 0, IGNORE_PREFIX.length())) {
            return text.substring(IGNORE_PREFIX.length());
        }

        return text;
    }
}
