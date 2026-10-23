/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.sources.headers;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * A CL command of the header of a source, e.g.
 * <code>CRTSQLRPGI OBJ(&amp;LI/&amp;OB) OBJTYPE(*MODULE) INCFILE(&amp;SL/QCPYSRC)</code>.
 * <p>
 * The command is passed as one line: the comment characters of the build tool
 * are removed and the continuation lines are joined, see
 * {@link ISourceHeaderParser}. Parameter values are returned as they are
 * written, including the substitution variables of the build tool, which start
 * with <code>&amp;</code>, e.g. <code>&amp;SL/QCPYSRC</code>.
 */
public final class CreateCommand {

    /** Prefix of the substitution variables of the build tools. */
    public static final char VARIABLE = '&';

    /** Prefix of the commands, that create an object. */
    private static final String CREATE = "CRT"; //$NON-NLS-1$

    private static final char LIBRARY_SEPARATOR = '/';
    private static final char LEFT = '(';
    private static final char RIGHT = ')';

    private final String text;
    private final String name;
    private final Map<String, String> parameters;

    private CreateCommand(String aText, String aName, Map<String, String> aParameters) {
        this.text = aText;
        this.name = aName;
        this.parameters = Collections.unmodifiableMap(aParameters);
    }

    /**
     * Parses a command.
     *
     * @param aText - command, e.g.
     *        <code>CRTRPGMOD MODULE(&amp;LI/&amp;OB) SRCSTMF('&amp;STMF')</code>
     * @return command or <code>null</code>, if the text is blank
     */
    public static CreateCommand parse(String aText) {

        if (aText == null) {
            return null;
        }

        String text = aText.trim();
        if (text.isEmpty()) {
            return null;
        }

        int end = 0;
        while (end < text.length() && !Character.isWhitespace(text.charAt(end)) && text.charAt(end) != LEFT) {
            end++;
        }

        String command = text.substring(0, end);
        int slash = command.lastIndexOf(LIBRARY_SEPARATOR);
        String name = (slash < 0 ? command : command.substring(slash + 1)).toUpperCase(Locale.ROOT);

        return new CreateCommand(text, name, getParameters(text, end));
    }

    /**
     * Returns <code>true</code>, if a parameter value is a substitution
     * variable of the build tool, e.g. <code>&amp;OB</code>.
     */
    public static boolean isVariable(String aValue) {
        return aValue != null && !aValue.isEmpty() && aValue.charAt(0) == VARIABLE;
    }

    /**
     * Returns the name of the command in upper case, without its library, e.g.
     * <code>CRTSQLRPGI</code>.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns <code>true</code>, if the command creates an object, i.e. its
     * name starts with <code>CRT</code>.
     */
    public boolean isCreateCommand() {
        return name.startsWith(CREATE);
    }

    /**
     * Returns the value of a parameter without its parentheses, e.g.
     * <code>&amp;SL/QCPYSRC</code> for <code>INCFILE</code>.
     *
     * @param aName - name of the parameter, not case-sensitive
     * @return value or <code>null</code>, if the command has no such parameter
     */
    public String getParameter(String aName) {
        return parameters.get(aName.toUpperCase(Locale.ROOT));
    }

    /**
     * Returns the parameters by their names in upper case, in the order of the
     * command.
     */
    public Map<String, String> getParameters() {
        return parameters;
    }

    /**
     * Returns the command, as it has been parsed.
     */
    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        return text;
    }

    /**
     * Returns the keyword parameters of a command. Positional parameters are
     * skipped, because the header of a source names the parameters, that the
     * build tool has to pass to the compiler.
     */
    private static Map<String, String> getParameters(String aText, int aStart) {

        Map<String, String> parameters = new LinkedHashMap<String, String>();

        int i = aStart;
        while (i < aText.length()) {

            char character = aText.charAt(i);
            if (Character.isWhitespace(character)) {
                i++;
                continue;
            }

            if (character == QuotedStrings.QUOTE) {
                i = QuotedStrings.skip(aText, i);
                continue;
            }

            if (character == LEFT) {
                i = skipValue(aText, i);
                continue;
            }

            int end = i;
            while (end < aText.length() && isNameCharacter(aText.charAt(end))) {
                end++;
            }

            if (end == i) {
                i++;
                continue;
            }

            if (end < aText.length() && aText.charAt(end) == LEFT) {
                int valueEnd = indexOfValueEnd(aText, end);
                String value = aText.substring(end + 1, valueEnd).trim();
                parameters.put(aText.substring(i, end).toUpperCase(Locale.ROOT), value);
                i = Math.min(valueEnd + 1, aText.length());
            } else {
                i = end;
            }
        }

        return parameters;
    }

    /**
     * Returns the index behind the value, that starts with the parenthesis at
     * the given index.
     */
    private static int skipValue(String aText, int anIndex) {
        return Math.min(indexOfValueEnd(aText, anIndex) + 1, aText.length());
    }

    /**
     * Returns the index of the parenthesis, that closes the value, that starts
     * with the parenthesis at the given index, or the length of the text, if
     * the value is not closed. Nested parentheses, e.g.
     * <code>BNDDIR((*LIBL/MAIN))</code>, and parentheses in quoted strings are
     * skipped.
     */
    private static int indexOfValueEnd(String aText, int anIndex) {

        int level = 0;
        int i = anIndex;
        while (i < aText.length()) {
            char character = aText.charAt(i);
            if (character == QuotedStrings.QUOTE) {
                i = QuotedStrings.skip(aText, i);
                continue;
            }
            if (character == LEFT) {
                level++;
            } else if (character == RIGHT) {
                level--;
                if (level == 0) {
                    return i;
                }
            }
            i++;
        }

        return aText.length();
    }

    private static boolean isNameCharacter(char aCharacter) {
        return Character.isLetterOrDigit(aCharacter) || aCharacter == '#' || aCharacter == '$' || aCharacter == '@' || aCharacter == '_';
    }
}
