/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.export;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts the <code>INCLUDE SRCSTMF('...')</code> commands of ILE CL sources,
 * e.g. to <code>INCLUDE SRCMBR(CONSTS) SRCFILE(INC)</code>.
 * <ul>
 * <li>Only an <code>INCLUDE</code> command with the parameter
 * <code>SRCSTMF</code> on one line is converted. Commands with continuation
 * lines or further parameters are reported as {@link Problem#NOT_SUPPORTED}.</li>
 * <li>The source file is specified without library, so that it is searched in
 * the library list.</li>
 * <li>A label and comments are kept.</li>
 * </ul>
 */
final class ClIncludeConverter extends IncludeConverter {

    private static final String LABEL = "(?:[A-Z$#@][A-Z0-9$#@_.]*\\s*:\\s*)?"; //$NON-NLS-1$
    private static final Pattern INCLUDE_SRCSTMF = Pattern.compile("^(\\s*" + LABEL + "(INCLUDE)\\s+)SRCSTMF\\(\\s*'((?:[^']|'')*)'\\s*\\)(.*)$", //$NON-NLS-1$ //$NON-NLS-2$
        Pattern.CASE_INSENSITIVE);
    private static final Pattern INCLUDE = Pattern.compile("^\\s*" + LABEL + "INCLUDE(?=\\s|$)", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$ //$NON-NLS-2$
    private static final Pattern SRCSTMF = Pattern.compile("SRCSTMF\\(\\s*'((?:[^']|'')*)'", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$
    private static final Pattern COMMENT = Pattern.compile("/\\*.*?(\\*/|$)"); //$NON-NLS-1$

    private static final String SRCSTMF_KEYWORD = "SRCSTMF"; //$NON-NLS-1$
    private static final String EMPTY = ""; //$NON-NLS-1$
    private static final String QUOTE = "'"; //$NON-NLS-1$
    private static final String DOUBLED_QUOTE = "''"; //$NON-NLS-1$

    ClIncludeConverter(IncludeResolver aResolver, Map<String, ExportMapping.Entry> aMembers) {
        super(aResolver, aMembers);
    }

    @Override
    protected LineConverter createLineConverter(final String aPath, final Result aResult) {

        return new LineConverter() {

            /** First line of an INCLUDE command with continuation lines. */
            private int continuedLineNumber;

            /** Text of an INCLUDE command with continuation lines. */
            private StringBuilder continuedCommand;

            public String convert(int aLineNumber, String aLine) {

                String code = removeComments(aLine);

                if (continuedCommand != null) {
                    continuedCommand.append(' ').append(code);
                    if (!isContinued(code)) {
                        checkUnsupported(continuedLineNumber, continuedCommand.toString(), aResult);
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

                Matcher matcher = INCLUDE_SRCSTMF.matcher(aLine);
                if (!matcher.matches() || removeComments(matcher.group(4)).trim().length() > 0) {
                    checkUnsupported(aLineNumber, code, aResult);
                    return aLine;
                }

                return convertInclude(aPath, aLineNumber, aLine, matcher, aResult);
            }
        };
    }

    private String convertInclude(String aPath, int aLineNumber, String aLine, Matcher aMatcher, Result aResult) {

        String name = aMatcher.group(3).replace(DOUBLED_QUOTE, QUOTE);

        String file = getResolver().resolveClInclude(aPath, name);
        if (file == null) {
            aResult.addProblem(aLineNumber, name, Problem.NOT_FOUND);
            return aLine;
        }

        ExportMapping.Entry member = getMember(file);
        if (member == null) {
            aResult.addProblem(aLineNumber, name, Problem.NOT_EXPORTED);
            return aLine;
        }

        aResult.addConverted(aLineNumber, name, member);

        String parameters = "SRCMBR(" + member.getMember() + ") SRCFILE(" + member.getSourceFile() + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        return aMatcher.group(1) + toCaseOf(parameters, aMatcher.group(2)) + aMatcher.group(4);
    }

    /**
     * Reports an INCLUDE command, that cannot be converted, if it refers to a
     * stream file.
     */
    private static void checkUnsupported(int aLineNumber, String aCommand, Result aResult) {

        if (!aCommand.toUpperCase(Locale.ROOT).contains(SRCSTMF_KEYWORD)) {
            return;
        }

        Matcher matcher = SRCSTMF.matcher(aCommand);
        String name = matcher.find() ? matcher.group(1).replace(DOUBLED_QUOTE, QUOTE) : aCommand.trim();

        aResult.addProblem(aLineNumber, name, Problem.NOT_SUPPORTED);
    }

    private static boolean isContinued(String aCode) {

        String code = aCode.trim();
        if (code.length() == 0) {
            return false;
        }

        char last = code.charAt(code.length() - 1);
        return last == '+' || last == '-';
    }

    private static String removeComments(String aLine) {
        return COMMENT.matcher(aLine).replaceAll(EMPTY);
    }
}
