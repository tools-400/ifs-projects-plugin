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

import de.tools400.ifsprojects.sources.ClLineConverter;
import de.tools400.ifsprojects.utils.StringUtils;

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
final class ClIncludeConverter extends ExportIncludeConverter {

    private static final Pattern INCLUDE_SRCSTMF = Pattern.compile("^(\\s*" + ClLineConverter.LABEL //$NON-NLS-1$
        + "(INCLUDE)\\s+)SRCSTMF\\(\\s*'((?:[^']|'')*)'\\s*\\)(.*)$", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$
    private static final Pattern SRCSTMF = Pattern.compile("SRCSTMF\\(\\s*'((?:[^']|'')*)'", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$

    private static final String SRCSTMF_KEYWORD = "SRCSTMF"; //$NON-NLS-1$
    private static final String QUOTE = "'"; //$NON-NLS-1$
    private static final String DOUBLED_QUOTE = "''"; //$NON-NLS-1$

    ClIncludeConverter(IncludeResolver aResolver, Map<String, ExportMapping.Entry> aMembers) {
        super(aResolver, aMembers);
    }

    @Override
    protected LineConverter createLineConverter(final String aPath, String aContent, final Result aResult) {

        return new ClLineConverter() {

            @Override
            protected String convertInclude(int aLineNumber, String aLine, String aCode) {

                Matcher matcher = INCLUDE_SRCSTMF.matcher(aLine);
                if (!matcher.matches() || !StringUtils.isBlank(removeComments(matcher.group(4)))) {
                    checkUnsupported(aLineNumber, aCode, aResult);
                    return aLine;
                }

                return ClIncludeConverter.this.convertInclude(aPath, aLineNumber, aLine, matcher, aResult);
            }

            @Override
            protected void checkContinuedInclude(int aLineNumber, String aCommand) {
                checkUnsupported(aLineNumber, aCommand, aResult);
            }
        };
    }

    private String convertInclude(String aPath, int aLineNumber, String aLine, Matcher aMatcher, Result aResult) {

        String name = aMatcher.group(3).replace(DOUBLED_QUOTE, QUOTE);

        String file = getResolver().resolveClInclude(aPath, name);
        if (file == null) {
            addProblem(aResult, aLineNumber, name, Problem.NOT_FOUND);
            return aLine;
        }

        ExportMapping.Entry member = getMember(file);
        if (member == null) {
            addProblem(aResult, aLineNumber, name, Problem.NOT_TRANSFERRED);
            return aLine;
        }

        addConverted(aResult, aLineNumber, name, member.getTarget());

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
        addProblem(aResult, aLineNumber, name, Problem.NOT_SUPPORTED);
    }
}
