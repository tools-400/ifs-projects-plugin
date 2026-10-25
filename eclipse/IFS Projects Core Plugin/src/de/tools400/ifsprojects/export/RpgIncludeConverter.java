/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.export;

import java.util.Map;

import de.tools400.ifsprojects.sources.RpgIncludeDirective;
import de.tools400.ifsprojects.sources.RpgLineConverter;
import de.tools400.ifsprojects.sources.RpgSqlIncludeDirective;

/**
 * Converts the <code>/COPY</code> and <code>/INCLUDE</code> directives and the
 * <code>EXEC SQL INCLUDE</code> statements of RPG sources and copy books, see
 * {@link RpgLineConverter} for the columns.
 * <ul>
 * <li>A quoted name (<code>'...'</code>, <code>"..."</code>) is a stream file,
 * an unquoted name with a comma a member (unchanged). An unquoted name without
 * comma is converted, if it is a file of the project; otherwise it is kept as
 * member name, unless it contains a slash or a period.</li>
 * <li>The converted directive refers to the member without library, e.g.
 * <code>/COPY INC,CONSTS</code>, in the case of the directive.</li>
 * <li><code>EXEC SQL INCLUDE</code> names only a member, e.g.
 * <code>EXEC SQL INCLUDE HOSTVARS</code>. The compiler searches it in the
 * source file of the parameter <code>INCFILE</code>, by default the source file
 * of the compiled member. Therefore a statement, whose member is exported into
 * another source file than the including member, is converted and reported as
 * {@link Problem#INCLUDE_FILE}.</li>
 * </ul>
 */
final class RpgIncludeConverter extends ExportIncludeConverter {

    private static final char MEMBER_SEPARATOR = ',';
    private static final char SEPARATOR = '/';
    private static final char EXTENSION_SEPARATOR = '.';

    RpgIncludeConverter(IncludeResolver aResolver, Map<String, ExportMapping.Entry> aMembers) {
        super(aResolver, aMembers);
    }

    @Override
    protected LineConverter createLineConverter(final String aPath, String aContent, final Result aResult) {

        return new RpgLineConverter(aContent) {

            @Override
            protected String convertDirective(int aLineNumber, RpgIncludeDirective aDirective) {
                return RpgIncludeConverter.this.convertDirective(aPath, aLineNumber, aDirective, aResult);
            }

            @Override
            protected String convertSqlInclude(RpgSqlIncludeDirective aDirective) {
                return RpgIncludeConverter.this.convertSqlInclude(aPath, aDirective, aResult);
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

    /**
     * Converts an <code>EXEC SQL INCLUDE</code> statement of a stream file to
     * the member name.
     *
     * @return converted statement or <code>null</code>, if it has not been
     *         changed
     */
    private String convertSqlInclude(String aPath, RpgSqlIncludeDirective aDirective, Result aResult) {

        String name = aDirective.getName();
        if (name == null) {
            addProblem(aResult, aDirective.getFirstLineNumber(), aDirective.getText(), Problem.NOT_SUPPORTED);
            return null;
        }

        if (!aDirective.isQuoted()) {
            return null; // already a member
        }

        int lineNumber = aDirective.getLineNumber();

        String file = getResolver().resolveSqlInclude(aPath, name);
        if (file == null) {
            addProblem(aResult, lineNumber, name, Problem.NOT_FOUND);
            return null;
        }

        ExportMapping.Entry member = getMember(file);
        if (member == null) {
            addProblem(aResult, lineNumber, name, Problem.NOT_TRANSFERRED);
            return null;
        }

        String converted = aDirective.replaceName(toCaseOf(member.getMember(), aDirective.getKeyword()));
        if (converted == null) {
            addProblem(aResult, lineNumber, name, Problem.TOO_LONG);
            return null;
        }

        ExportMapping.Entry including = getMember(aPath);
        if (including != null && !including.getSourceFile().equals(member.getSourceFile())) {
            addConverted(aResult, lineNumber, name, member.getTarget(), Problem.INCLUDE_FILE);
        } else {
            addConverted(aResult, lineNumber, name, member.getTarget());
        }

        return converted;
    }
}
