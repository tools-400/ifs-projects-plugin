/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.migration;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.tools400.ifsprojects.ibmi.IfsPaths;
import de.tools400.ifsprojects.sources.ClLineConverter;

/**
 * Converts the <code>INCLUDE SRCMBR() SRCFILE()</code> commands of ILE CL
 * sources, e.g. <code>INCLUDE SRCMBR(VARS) SRCFILE(QCPYSRC)</code> to
 * <code>INCLUDE SRCSTMF('../qcpysrc/vars.clleinc')</code>.
 * <ul>
 * <li>CL searches a relative path in the directory of the including file, not
 * in the current directory, so the path is relative to the including file
 * (verified with <code>CRTBNDCL</code>).</li>
 * <li>Without <code>SRCFILE</code> or with <code>SRCFILE(*INCFILE)</code>, the
 * member is in the source file of the including member (verified).</li>
 * <li>Only an <code>INCLUDE</code> command with keyword parameters on one line
 * is converted. Commands with continuation lines or positional parameters are
 * reported as {@link Problem#NOT_SUPPORTED}.</li>
 * <li>A label and comments are kept.</li>
 * </ul>
 */
final class ClMemberIncludeConverter extends MemberIncludeConverter {

    private static final Pattern INCLUDE = Pattern.compile("^(\\s*" + ClLineConverter.LABEL + "(INCLUDE)\\s+)", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$ //$NON-NLS-2$
    private static final Pattern PARAMETER = Pattern.compile("\\G\\s*([A-Z]+)\\(\\s*([^)]*?)\\s*\\)", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$
    private static final Pattern SRCMBR = Pattern.compile("SRCMBR\\(\\s*([^)\\s]*)", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$

    private static final String SRCMBR_KEYWORD = "SRCMBR"; //$NON-NLS-1$
    private static final String SRCFILE_KEYWORD = "SRCFILE"; //$NON-NLS-1$
    private static final String SRCSTMF_KEYWORD = "SRCSTMF"; //$NON-NLS-1$
    private static final String INCLUDE_FILE = "*INCFILE"; //$NON-NLS-1$
    private static final char LIBRARY_SEPARATOR = '/';

    ClMemberIncludeConverter(String aLibrary, Map<String, String> aPaths, Set<String> aSourceFiles) {
        super(aLibrary, aPaths, aSourceFiles);
    }

    @Override
    protected LineConverter createLineConverter(final String aKey, String aContent, final Result aResult) {

        return new ClLineConverter() {

            @Override
            protected String convertInclude(int aLineNumber, String aLine, String aCode) {
                return ClMemberIncludeConverter.this.convertInclude(aKey, aLineNumber, aLine, aResult);
            }

            @Override
            protected void checkContinuedInclude(int aLineNumber, String aCommand) {
                checkUnsupported(aLineNumber, aCommand, aResult);
            }
        };
    }

    private String convertInclude(String aKey, int aLineNumber, String aLine, Result aResult) {

        Matcher include = INCLUDE.matcher(aLine);
        if (!include.lookingAt()) {
            return aLine;
        }

        String member = null;
        String sourceFile = null;
        boolean isOtherParameter = false;

        Matcher parameter = PARAMETER.matcher(aLine);
        parameter.region(include.end(), aLine.length());
        int end = include.end();
        while (parameter.find()) {
            String keyword = parameter.group(1).toUpperCase(Locale.ROOT);
            if (SRCSTMF_KEYWORD.equals(keyword)) {
                return aLine;
            } else if (SRCMBR_KEYWORD.equals(keyword)) {
                member = parameter.group(2);
            } else if (SRCFILE_KEYWORD.equals(keyword)) {
                sourceFile = parameter.group(2);
            } else {
                isOtherParameter = true;
            }
            end = parameter.end();
        }

        String rest = aLine.substring(end);
        String unparsed = ClLineConverter.removeComments(rest).trim();
        if (member == null || isOtherParameter || unparsed.length() > 0) {
            addProblem(aResult, aLineNumber, member != null ? member : unparsed, Problem.NOT_SUPPORTED);
            return aLine;
        }

        String ownSourceFile = aKey.substring(0, aKey.indexOf('('));
        String library = null;
        if (sourceFile == null || sourceFile.equalsIgnoreCase(INCLUDE_FILE)) {
            sourceFile = ownSourceFile;
        } else {
            int slash = sourceFile.indexOf(LIBRARY_SEPARATOR);
            if (slash >= 0) {
                library = sourceFile.substring(0, slash);
                sourceFile = sourceFile.substring(slash + 1);
            }
        }

        String path = resolve(aResult, aLineNumber, member, library, sourceFile, member);
        if (path == null) {
            return aLine;
        }

        String relativePath = IfsPaths.getRelativePath(getPath(aKey), path);
        addConverted(aResult, aLineNumber, member, relativePath);

        return include.group(1) + toCaseOf(SRCSTMF_KEYWORD, include.group(2)) + "('" + relativePath + "')" + rest; //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Reports an INCLUDE command, that cannot be converted, if it refers to a
     * member.
     */
    private static void checkUnsupported(int aLineNumber, String aCommand, Result aResult) {

        String command = ClLineConverter.removeComments(aCommand);
        if (!command.toUpperCase(Locale.ROOT).contains(SRCMBR_KEYWORD)) {
            return;
        }

        Matcher matcher = SRCMBR.matcher(command);
        String name = matcher.find() ? matcher.group(1) : command.trim();
        addProblem(aResult, aLineNumber, name, Problem.NOT_SUPPORTED);
    }
}
