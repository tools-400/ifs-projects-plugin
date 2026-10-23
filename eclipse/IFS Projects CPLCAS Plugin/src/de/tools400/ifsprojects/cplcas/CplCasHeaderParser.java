/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.cplcas;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import de.tools400.ifsprojects.sources.headers.CreateCommand;
import de.tools400.ifsprojects.sources.headers.HeaderLines;
import de.tools400.ifsprojects.sources.headers.ISourceHeaderParser;

/**
 * Reads the compile commands of the <code>CPLCAS</code> command of Denis
 * Robitaille (pre-compiler of Cascades) in the first lines of a source, e.g.
 *
 * <pre>
 *      A*%%%* Instruction for the CP-option - CRTPF
 *      A*%%% DLTF
 *      A*%%%   FILE(%LIBO/%OBJ)
 *      A*%%%#
 *      A*%%% CRTPF
 *      A*%%%   FILE(%LIBO/%OBJ)
 *      A*%%%   SRCFILE(%LIBS/%FIC)
 *      A*%%%   SRCMBR(%MBR)
 *      A*%%%&amp;
 * </pre>
 *
 * The marker <code>*%%%</code> stands in columns 7 - 10, but is searched
 * anywhere in a line, like the markers of the other build tools. The
 * character behind it is a flag:
 * <ul>
 * <li>a blank: the next 50 characters are a part of the command; the parts of
 * a command are joined with a blank,</li>
 * <li><code>#</code>: end of a command,</li>
 * <li><code>&amp;</code>: end of the last command and of the header,</li>
 * <li><code>*</code>: comment, e.g. a parameter that is switched off with
 * <code>*%%%**</code>,</li>
 * <li><code>@</code>: the commands are run again for further libraries, which
 * does not matter for the object, that the commands create.</li>
 * </ul>
 * A line without the marker ends the command, so that an unterminated one does
 * not swallow the source.
 * <p>
 * The substitution variables of the build tool are <code>%LIBO</code>,
 * <code>%LIBS</code>, <code>%FIC</code>, <code>%MBR</code> and
 * <code>%OBJ</code>. They are returned with <code>&amp;</code>, e.g.
 * <code>&amp;LIBS</code>, so that a library, that is a variable, is recognized
 * as such.
 * <p>
 * No other header contains the marker, so the priority of the parser does not
 * matter. It is the lowest of the parsers of this repository.
 */
public final class CplCasHeaderParser implements ISourceHeaderParser {

    /** Start of a line of the header. */
    private static final String TAG = "*%%%"; //$NON-NLS-1$

    /** Maximum length of the text of a line behind the flag. */
    private static final int MAX_TEXT_LENGTH = 50;

    /** Flag of a line, whose text is a part of a command. */
    private static final char TEXT = ' ';

    /** Flag of the end of a command. */
    private static final char END_OF_COMMAND = '#';

    /** Flag of the end of the last command and of the header. */
    private static final char END_OF_HEADER = '&';

    /** Substitution variables of the build tool, e.g. %LIBS. */
    private static final Pattern SUBSTITUTION_VARIABLE = Pattern.compile("%(?=(LIBO|LIBS|FIC|MBR|OBJ)\\b)", //$NON-NLS-1$
        Pattern.CASE_INSENSITIVE);

    /** Start of a substitution variable, that the core recognizes. */
    private static final String VARIABLE = "&"; //$NON-NLS-1$

    private static final char BLANK = ' ';

    @Override
    public List<CreateCommand> parse(String aContent, String aSourceType) {

        List<CreateCommand> commands = new ArrayList<CreateCommand>();
        StringBuilder text = new StringBuilder();

        for (String line : HeaderLines.read(aContent)) {

            int tag = line.indexOf(TAG);
            if (tag < 0) {
                addCommand(commands, text);
                continue;
            }

            int flag = tag + TAG.length();
            if (flag >= line.length()) {
                continue;
            }

            switch (line.charAt(flag)) {
            case TEXT:
                appendText(text, line.substring(flag + 1, Math.min(line.length(), flag + 1 + MAX_TEXT_LENGTH)));
                break;
            case END_OF_COMMAND:
                addCommand(commands, text);
                break;
            case END_OF_HEADER:
                addCommand(commands, text);
                return commands;
            default:
                // A comment or the flag of the multiple execution.
                break;
            }
        }

        addCommand(commands, text);

        return commands;
    }

    /**
     * Appends the text of a line to the text of the command.
     */
    private static void appendText(StringBuilder aCommand, String aText) {

        String text = aText.trim();
        if (text.isEmpty()) {
            return;
        }

        if (aCommand.length() > 0) {
            aCommand.append(BLANK);
        }
        aCommand.append(text);
    }

    /**
     * Adds the command, whose text has been read, and clears the text for the
     * next command.
     */
    private static void addCommand(List<CreateCommand> aCommands, StringBuilder aText) {

        if (aText.length() == 0) {
            return;
        }

        CreateCommand command = CreateCommand.parse(replaceSubstitutionVariables(aText.toString()));
        if (command != null) {
            aCommands.add(command);
        }

        aText.setLength(0);
    }

    /**
     * Returns the command with the substitution variables of the core, e.g.
     * <code>&amp;LIBS/&amp;FIC</code> instead of <code>%LIBS/%FIC</code>.
     * Another <code>%</code> is kept, e.g. in <code>TEXT('50%')</code>.
     */
    private static String replaceSubstitutionVariables(String aText) {
        return SUBSTITUTION_VARIABLE.matcher(aText).replaceAll(VARIABLE);
    }
}
