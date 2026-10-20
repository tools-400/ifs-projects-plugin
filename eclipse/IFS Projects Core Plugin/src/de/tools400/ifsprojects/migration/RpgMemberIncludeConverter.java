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

import de.tools400.ifsprojects.ibmi.ClCommands;
import de.tools400.ifsprojects.sources.RpgIncludeDirective;
import de.tools400.ifsprojects.sources.RpgLineConverter;

/**
 * Converts the <code>/COPY</code> and <code>/INCLUDE</code> directives of RPG
 * sources and copy books, that refer to members, see {@link RpgLineConverter}
 * for the columns.
 * <ul>
 * <li>Member forms: <code>FILE,MEMBER</code>, <code>LIB/FILE,MEMBER</code> and
 * <code>MEMBER</code>, which is a member of <code>QRPGLESRC</code>.</li>
 * <li>The converted directive refers to the stream file relative to the
 * project directory, which is the current directory of the build, e.g.
 * <code>/copy 'qcpysrc/consts.rpgleinc'</code> (verified with
 * <code>CRTBNDRPG</code> and nested copy books).</li>
 * <li>With {@link IncludeStyle#CONDITIONAL}, the member directive is kept for
 * the member build, see there.</li>
 * <li><code>EXEC SQL INCLUDE</code> of a member is not converted yet.</li>
 * </ul>
 */
final class RpgMemberIncludeConverter extends MemberIncludeConverter {

    /** Source file of a member without source file, e.g. <code>/COPY CONSTS</code>. */
    private static final String DEFAULT_SOURCE_FILE = "QRPGLESRC"; //$NON-NLS-1$

    /** <code>EXEC SQL INCLUDE</code> of a member, except SQLCA and SQLDA. */
    private static final Pattern SQL_INCLUDE = Pattern.compile("\\bEXEC\\s+SQL\\s+INCLUDE\\s+(?!(?:SQLCA|SQLDA)\\b)([A-Z0-9$#@_]+)", //$NON-NLS-1$
        Pattern.CASE_INSENSITIVE);
    private static final String LINE_COMMENT = "//"; //$NON-NLS-1$

    private static final String IF_DEFINED = "/IF DEFINED("; //$NON-NLS-1$
    private static final String ELSE = "/ELSE"; //$NON-NLS-1$
    private static final String ENDIF = "/ENDIF"; //$NON-NLS-1$

    private static final char MEMBER_SEPARATOR = ',';
    private static final char LIBRARY_SEPARATOR = '/';
    private static final char EXTENSION_SEPARATOR = '.';
    private static final char QUOTE = '\'';

    private final IncludeStyle style;

    RpgMemberIncludeConverter(String aLibrary, Map<String, String> aPaths, Set<String> aSourceFiles, IncludeStyle aStyle) {
        super(aLibrary, aPaths, aSourceFiles);
        this.style = aStyle;
    }

    @Override
    protected LineConverter createLineConverter(final String aKey, final Result aResult) {

        return new RpgLineConverter() {

            @Override
            protected String convertDirective(int aLineNumber, RpgIncludeDirective aDirective) {
                return RpgMemberIncludeConverter.this.convertDirective(aLineNumber, aDirective, aResult);
            }

            @Override
            protected void checkCode(int aLineNumber, String aCode) {
                checkSqlInclude(aLineNumber, aCode, aResult);
            }
        };
    }

    /**
     * Converts a <code>/COPY</code> or <code>/INCLUDE</code> directive.
     *
     * @return converted line(s) or <code>null</code>, if the line has not been
     *         changed
     */
    private String convertDirective(int aLineNumber, RpgIncludeDirective aDirective, Result aResult) {

        if (aDirective.isQuoted()) {
            return null;
        }

        String name = aDirective.getName();
        String library = null;
        String sourceFile = DEFAULT_SOURCE_FILE;
        String member = name;

        int comma = name.indexOf(MEMBER_SEPARATOR);
        if (comma >= 0) {
            sourceFile = name.substring(0, comma);
            member = name.substring(comma + 1);
            int slash = sourceFile.indexOf(LIBRARY_SEPARATOR);
            if (slash >= 0) {
                library = sourceFile.substring(0, slash);
                sourceFile = sourceFile.substring(slash + 1);
            }
        } else if (name.indexOf(LIBRARY_SEPARATOR) >= 0 || name.indexOf(EXTENSION_SEPARATOR) >= 0) {
            return null;
        }

        if (!ClCommands.isObjectName(sourceFile.toUpperCase(Locale.ROOT)) || !ClCommands.isObjectName(member.toUpperCase(Locale.ROOT))) {
            return null;
        }

        String path = resolve(aResult, aLineNumber, name, library, sourceFile, member);
        if (path == null) {
            return null;
        }

        String converted = aDirective.replaceName(QUOTE + path + QUOTE);
        if (converted == null) {
            addProblem(aResult, aLineNumber, name, Problem.TOO_LONG);
            return null;
        }

        addConverted(aResult, aLineNumber, name, path);

        if (style == IncludeStyle.CONDITIONAL) {
            return getConditional(aDirective, converted);
        }

        return converted;
    }

    /**
     * Returns the converted directive, that is used, unless the condition of
     * the member build is defined.
     */
    private static String getConditional(RpgIncludeDirective aDirective, String aConverted) {

        String indentation = aDirective.getIndentation();
        String keyword = aDirective.getKeyword();

        StringBuilder lines = new StringBuilder();
        lines.append(indentation).append(toCaseOf(IF_DEFINED, keyword)).append(IncludeStyle.MEMBER_CONDITION).append(')');
        lines.append(RpgLineConverter.LINE_SEPARATOR).append(aDirective.getLine());
        lines.append(RpgLineConverter.LINE_SEPARATOR).append(indentation).append(toCaseOf(ELSE, keyword));
        lines.append(RpgLineConverter.LINE_SEPARATOR).append(aConverted);
        lines.append(RpgLineConverter.LINE_SEPARATOR).append(indentation).append(toCaseOf(ENDIF, keyword));

        return lines.toString();
    }

    private static void checkSqlInclude(int aLineNumber, String aLine, Result aResult) {

        int comment = aLine.indexOf(LINE_COMMENT);
        String code = comment < 0 ? aLine : aLine.substring(0, comment);

        Matcher matcher = SQL_INCLUDE.matcher(code);
        if (matcher.find()) {
            addProblem(aResult, aLineNumber, matcher.group(1), Problem.NOT_SUPPORTED);
        }
    }
}
