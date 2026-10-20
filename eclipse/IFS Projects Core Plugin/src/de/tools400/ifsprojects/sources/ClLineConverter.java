/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.sources;

import java.util.regex.Pattern;

/**
 * Finds the <code>INCLUDE</code> commands in the lines of an ILE CL source and
 * lets the subclass convert them. Comments are ignored, when a command is
 * searched. A command with continuation lines (<code>+</code>,
 * <code>-</code>) is not converted, but passed to the subclass for a check.
 */
public abstract class ClLineConverter implements IncludeConverter.LineConverter {

    /** Regular expression of the optional label of a command, e.g. <code>START:</code>. */
    public static final String LABEL = "(?:[A-Z$#@][A-Z0-9$#@_.]*\\s*:\\s*)?"; //$NON-NLS-1$

    private static final Pattern INCLUDE = Pattern.compile("^\\s*" + LABEL + "INCLUDE(?=\\s|$)", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$ //$NON-NLS-2$
    private static final Pattern COMMENT = Pattern.compile("/\\*.*?(\\*/|$)"); //$NON-NLS-1$
    private static final String EMPTY = ""; //$NON-NLS-1$

    /** First line of an INCLUDE command with continuation lines. */
    private int continuedLineNumber;

    /** Text of an INCLUDE command with continuation lines. */
    private StringBuilder continuedCommand;

    public final String convert(int aLineNumber, String aLine) {

        String code = removeComments(aLine);

        if (continuedCommand != null) {
            continuedCommand.append(' ').append(code);
            if (!isContinued(code)) {
                checkContinuedInclude(continuedLineNumber, continuedCommand.toString());
                continuedCommand = null;
            }
            return aLine;
        }

        if (!INCLUDE.matcher(code).find()) {
            return aLine;
        }

        if (isContinued(code)) {
            continuedLineNumber = aLineNumber;
            continuedCommand = new StringBuilder(code);
            return aLine;
        }

        return convertInclude(aLineNumber, aLine, code);
    }

    /**
     * Converts an <code>INCLUDE</code> command on one line.
     *
     * @param aLineNumber - line number, starting at 1
     * @param aLine - line
     * @param aCode - line without comments
     * @return converted or unchanged line
     */
    protected abstract String convertInclude(int aLineNumber, String aLine, String aCode);

    /**
     * Checks an <code>INCLUDE</code> command with continuation lines, which
     * is not converted.
     *
     * @param aLineNumber - number of the first line of the command
     * @param aCommand - lines of the command without comments, joined with
     *        blanks
     */
    protected abstract void checkContinuedInclude(int aLineNumber, String aCommand);

    /**
     * Removes the comments of a line.
     *
     * @param aLine - line
     * @return line without comments
     */
    public static String removeComments(String aLine) {
        return COMMENT.matcher(aLine).replaceAll(EMPTY);
    }

    private static boolean isContinued(String aCode) {

        String code = aCode.trim();
        if (code.length() == 0) {
            return false;
        }

        char last = code.charAt(code.length() - 1);

        return last == '+' || last == '-';
    }
}
