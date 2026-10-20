/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.export;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.tools400.ifsprojects.sources.RpgIncludeDirective;
import de.tools400.ifsprojects.sources.RpgLineConverter;

/**
 * Converts the <code>/COPY</code> and <code>/INCLUDE</code> directives of RPG
 * sources and copy books, see {@link RpgLineConverter} for the columns.
 * <ul>
 * <li>A quoted name (<code>'...'</code>, <code>"..."</code>) is a stream file,
 * an unquoted name with a comma a member (unchanged). An unquoted name without
 * comma is converted, if it is a file of the project; otherwise it is kept as
 * member name, unless it contains a slash or a period.</li>
 * <li>The converted directive refers to the member without library, e.g.
 * <code>/COPY INC,CONSTS</code>, in the case of the directive.</li>
 * <li><code>EXEC SQL INCLUDE</code> of a stream file cannot be converted,
 * because a member can only be specified with the parameter
 * <code>INCFILE</code> of the compiler.</li>
 * </ul>
 */
final class RpgIncludeConverter extends ExportIncludeConverter {

    private static final Pattern SQL_INCLUDE = Pattern.compile("\\bEXEC\\s+SQL\\s+INCLUDE\\s+(['\"])(.*?)(\\1|$)", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$
    private static final String LINE_COMMENT = "//"; //$NON-NLS-1$

    private static final char MEMBER_SEPARATOR = ',';
    private static final char SEPARATOR = '/';
    private static final char EXTENSION_SEPARATOR = '.';

    RpgIncludeConverter(IncludeResolver aResolver, Map<String, ExportMapping.Entry> aMembers) {
        super(aResolver, aMembers);
    }

    @Override
    protected LineConverter createLineConverter(final String aPath, final Result aResult) {

        return new RpgLineConverter() {

            @Override
            protected String convertDirective(int aLineNumber, RpgIncludeDirective aDirective) {
                return RpgIncludeConverter.this.convertDirective(aPath, aLineNumber, aDirective, aResult);
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
     * @return converted line or <code>null</code>, if the line has not been
     *         changed
     */
    private String convertDirective(String aPath, int aLineNumber, RpgIncludeDirective aDirective, Result aResult) {

        String name = aDirective.getName();
        boolean isPath = aDirective.isQuoted();
        if (!isPath) {
            if (name.indexOf(MEMBER_SEPARATOR) >= 0) {
                return null;
            }
            isPath = name.indexOf(SEPARATOR) >= 0 || name.indexOf(EXTENSION_SEPARATOR) >= 0;
        }

        String file = getResolver().resolveRpgInclude(aPath, name);
        if (file == null) {
            if (isPath) {
                addProblem(aResult, aLineNumber, name, Problem.NOT_FOUND);
            }
            return null;
        }

        ExportMapping.Entry member = getMember(file);
        if (member == null) {
            addProblem(aResult, aLineNumber, name, Problem.NOT_TRANSFERRED);
            return null;
        }

        String converted = aDirective.replaceName(toCaseOf(member.getTarget(), aDirective.getKeyword()));
        if (converted == null) {
            addProblem(aResult, aLineNumber, name, Problem.TOO_LONG);
            return null;
        }

        addConverted(aResult, aLineNumber, name, member.getTarget());

        return converted;
    }

    private static void checkSqlInclude(int aLineNumber, String aLine, Result aResult) {

        int comment = aLine.indexOf(LINE_COMMENT);
        String code = comment < 0 ? aLine : aLine.substring(0, comment);

        Matcher matcher = SQL_INCLUDE.matcher(code);
        if (matcher.find()) {
            addProblem(aResult, aLineNumber, matcher.group(2), Problem.NOT_SUPPORTED);
        }
    }
}
