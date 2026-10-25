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

import de.tools400.ifsprojects.ibmi.ClCommands;
import de.tools400.ifsprojects.sources.RpgIncludeDirective;
import de.tools400.ifsprojects.sources.RpgLineConverter;
import de.tools400.ifsprojects.sources.RpgSqlIncludeDirective;

/**
 * Converts the <code>/COPY</code> and <code>/INCLUDE</code> directives and the
 * <code>EXEC SQL INCLUDE</code> statements of RPG sources and copy books, that
 * refer to members, see {@link RpgLineConverter} for the columns.
 * <ul>
 * <li>Member forms: <code>FILE,MEMBER</code>, <code>LIB/FILE,MEMBER</code> and
 * <code>MEMBER</code>, which is a member of <code>QRPGLESRC</code>.</li>
 * <li>The converted directive refers to the stream file relative to the project
 * directory, which is the current directory of the build, e.g.
 * <code>/copy 'qcpysrc/consts.rpgleinc'</code> (verified with
 * <code>CRTBNDRPG</code> and nested copy books).</li>
 * <li><code>EXEC SQL INCLUDE</code> names only a member, whose source file is
 * the parameter <code>INCFILE</code> of the compiler; it is passed to the
 * constructor, see
 * {@link MemberIncludeConverter#create(String, String, Map, Set, IncludeStyle, String)}.
 * The converted statement refers to the stream file like the directives, e.g.
 * <code>exec sql include 'qcpysrc/hostvars.sqlrpgleinc'</code>, see
 * {@link #getSqlIncludePath(String, String)}.</li>
 * <li>With {@link IncludeStyle#CONDITIONAL}, the member directive and the
 * member statement are kept for the member build, see there.</li>
 * </ul>
 */
final class RpgMemberIncludeConverter extends MemberIncludeConverter {

    /**
     * Source file of a member without source file, e.g.
     * <code>/COPY CONSTS</code>.
     */
    private static final String DEFAULT_SOURCE_FILE = "QRPGLESRC"; //$NON-NLS-1$

    private static final String IF_DEFINED = "/IF DEFINED("; //$NON-NLS-1$
    private static final String ELSE = "/ELSE"; //$NON-NLS-1$
    private static final String ENDIF = "/ENDIF"; //$NON-NLS-1$

    private static final char MEMBER_SEPARATOR = ',';
    private static final char LIBRARY_SEPARATOR = '/';
    private static final char EXTENSION_SEPARATOR = '.';
    private static final char QUOTE = '\'';

    private final IncludeStyle style;
    private final String sqlIncludeFile;

    RpgMemberIncludeConverter(String aLibrary, Map<String, String> aPaths, Set<String> aSourceFiles, IncludeStyle aStyle, String aSqlIncludeFile) {
        super(aLibrary, aPaths, aSourceFiles);
        this.style = aStyle;
        this.sqlIncludeFile = aSqlIncludeFile;
    }

    @Override
    protected LineConverter createLineConverter(final String aKey, String aContent, final Result aResult) {

        return new RpgLineConverter(aContent) {

            @Override
            protected String convertDirective(int aLineNumber, RpgIncludeDirective aDirective) {
                return RpgMemberIncludeConverter.this.convertDirective(aLineNumber, aDirective, aResult);
            }

            @Override
            protected String convertSqlInclude(RpgSqlIncludeDirective aDirective) {
                return RpgMemberIncludeConverter.this.convertSqlInclude(aDirective, aResult);
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
            return getConditional(aDirective.getIndentation(), aDirective.getKeyword(), aDirective.getLine(), converted);
        }

        return converted;
    }

    /**
     * Converts an <code>EXEC SQL INCLUDE</code> statement of a member.
     *
     * @return converted statement or <code>null</code>, if it has not been
     *         changed
     */
    private String convertSqlInclude(RpgSqlIncludeDirective aDirective, Result aResult) {

        String name = aDirective.getName();
        if (name == null) {
            addProblem(aResult, aDirective.getFirstLineNumber(), aDirective.getText(), Problem.NOT_SUPPORTED);
            return null;
        }

        if (aDirective.isQuoted()) {
            return null; // already a stream file
        }

        int lineNumber = aDirective.getLineNumber();
        String member = name.toUpperCase(Locale.ROOT);
        if (!ClCommands.isObjectName(member)) {
            return null;
        }

        if (sqlIncludeFile == null) {
            addProblem(aResult, lineNumber, name, Problem.AMBIGUOUS);
            return null;
        }

        String library = null;
        String sourceFile = sqlIncludeFile;
        int slash = sourceFile.indexOf(LIBRARY_SEPARATOR);
        if (slash >= 0) {
            library = sourceFile.substring(0, slash);
            sourceFile = sourceFile.substring(slash + 1);
        }

        String path = resolve(aResult, lineNumber, name, library, sourceFile, member);
        if (path == null) {
            return null;
        }

        String target = getSqlIncludePath(path);
        String converted = aDirective.replaceName(QUOTE + target + QUOTE);
        if (converted == null) {
            addProblem(aResult, lineNumber, name, Problem.TOO_LONG);
            return null;
        }

        addConverted(aResult, lineNumber, name, target);

        if (style == IncludeStyle.CONDITIONAL) {
            return getConditional(aDirective.getIndentation(), aDirective.getKeyword(), aDirective.getStatement(), converted);
        }

        return converted;
    }

    /**
     * Returns the path, to which a converted <code>EXEC SQL INCLUDE</code>
     * statement refers: the path relative to the project directory, which is
     * the current directory of the build, like the <code>/COPY</code>
     * directives. This is the only place, that forms the path. If the SQL
     * precompiler searched a relative path in the directory of the including
     * stream file instead, like CL does, the path would be
     * <code>IfsPaths.getRelativePath(getPath(aKey), aPath)</code> of the
     * including member, see {@link ClMemberIncludeConverter}.
     *
     * @param aPath - path of the included stream file relative to the project
     *        directory
     * @return path of the statement
     */
    private static String getSqlIncludePath(String aPath) {
        return aPath;
    }

    /**
     * Returns the converted directive or statement, that is used, unless the
     * condition of the member build is defined.
     *
     * @param anIndentation - characters, in front of which a directive starts
     * @param aKeyword - keyword, whose case the directives follow
     * @param anOriginal - directive or statement of the member
     * @param aConverted - converted directive or statement
     * @return lines separated by {@link RpgLineConverter#LINE_SEPARATOR}
     */
    private static String getConditional(String anIndentation, String aKeyword, String anOriginal, String aConverted) {

        StringBuilder lines = new StringBuilder();
        lines.append(anIndentation).append(toCaseOf(IF_DEFINED, aKeyword)).append(IncludeStyle.MEMBER_CONDITION).append(')');
        lines.append(RpgLineConverter.LINE_SEPARATOR).append(anOriginal);
        lines.append(RpgLineConverter.LINE_SEPARATOR).append(anIndentation).append(toCaseOf(ELSE, aKeyword));
        lines.append(RpgLineConverter.LINE_SEPARATOR).append(aConverted);
        lines.append(RpgLineConverter.LINE_SEPARATOR).append(anIndentation).append(toCaseOf(ENDIF, aKeyword));

        return lines.toString();
    }
}
