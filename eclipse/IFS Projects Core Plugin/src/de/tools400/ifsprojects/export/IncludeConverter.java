/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.export;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Converts the include directives of a source, that refer to stream files, so
 * that they refer to the members of the export, e.g.
 * <code>/copy 'inc/consts.rpgleinc'</code> to <code>/copy inc,consts</code>.
 * Directives, that refer to members, are not changed. The rest of the source
 * is kept unchanged, including the line separators.
 */
public abstract class IncludeConverter {

    private static final String RPGLE = "RPGLE"; //$NON-NLS-1$
    private static final String SQLRPGLE = "SQLRPGLE"; //$NON-NLS-1$
    private static final String CLLE = "CLLE"; //$NON-NLS-1$

    private final IncludeResolver resolver;
    private final Map<String, ExportMapping.Entry> members;

    /**
     * Creates a converter.
     *
     * @param aResolver - resolver, that finds the files of the directives
     * @param aMembers - exported files by their path relative to the project
     *        directory
     */
    protected IncludeConverter(IncludeResolver aResolver, Map<String, ExportMapping.Entry> aMembers) {
        this.resolver = aResolver;
        this.members = aMembers;
    }

    /**
     * Returns the converter of a source type.
     *
     * @param aSourceType - source type, see {@link ExportMapping#getSourceType}
     * @param aResolver - resolver, that finds the files of the directives
     * @param aMembers - exported files by their path relative to the project
     *        directory
     * @return converter or <code>null</code>, if the includes of the source
     *         type are not converted
     */
    public static IncludeConverter create(String aSourceType, IncludeResolver aResolver, Map<String, ExportMapping.Entry> aMembers) {

        if (RPGLE.equals(aSourceType) || SQLRPGLE.equals(aSourceType)) {
            return new RpgIncludeConverter(aResolver, aMembers);
        }

        if (CLLE.equals(aSourceType)) {
            return new ClIncludeConverter(aResolver, aMembers);
        }

        return null;
    }

    /**
     * Converts the include directives of a source.
     *
     * @param aPath - path of the source relative to the project directory
     * @param aContent - content of the source
     * @return result
     */
    public Result convert(String aPath, String aContent) {

        Result result = new Result();
        StringBuilder content = new StringBuilder(aContent.length());

        List<String[]> lines = splitLines(aContent);
        LineConverter lineConverter = createLineConverter(aPath, result);
        for (int i = 0; i < lines.size(); i++) {
            content.append(lineConverter.convert(i + 1, lines.get(i)[0])).append(lines.get(i)[1]);
        }

        result.content = content.toString();

        return result;
    }

    /**
     * Returns the converter of the lines of a source.
     *
     * @param aPath - path of the source relative to the project directory
     * @param aResult - result, to which the includes are added
     * @return converter
     */
    protected abstract LineConverter createLineConverter(String aPath, Result aResult);

    /**
     * Returns the resolver of the include directives.
     */
    protected IncludeResolver getResolver() {
        return resolver;
    }

    /**
     * Returns the member, into which a file is exported.
     *
     * @param aPath - path relative to the project directory
     * @return member or <code>null</code>, if the file is not exported
     */
    protected ExportMapping.Entry getMember(String aPath) {
        return members.get(aPath);
    }

    /**
     * Returns a text in the case of a keyword of the source: lowercase, if the
     * keyword is in lowercase, otherwise uppercase.
     *
     * @param aText - text, e.g. <code>INC,CONSTS</code>
     * @param aKeyword - keyword of the directive, e.g. <code>/copy</code>
     * @return text in the case of the keyword
     */
    protected static String toCaseOf(String aText, String aKeyword) {

        if (aKeyword.equals(aKeyword.toLowerCase(Locale.ROOT))) {
            return aText.toLowerCase(Locale.ROOT);
        }

        return aText.toUpperCase(Locale.ROOT);
    }

    /**
     * Splits a text into lines.
     *
     * @return lines, each an array with the text of the line and its line
     *         separator, which is empty for the last line
     */
    private static List<String[]> splitLines(String aText) {

        List<String[]> lines = new ArrayList<String[]>();

        int start = 0;
        int i = 0;
        while (i < aText.length()) {
            char c = aText.charAt(i);
            if (c == '\n' || c == '\r') {
                int end = i;
                if (c == '\r' && i + 1 < aText.length() && aText.charAt(i + 1) == '\n') {
                    i++;
                }
                lines.add(new String[] { aText.substring(start, end), aText.substring(end, i + 1) });
                start = i + 1;
            }
            i++;
        }

        if (start < aText.length()) {
            lines.add(new String[] { aText.substring(start), "" }); //$NON-NLS-1$
        }

        return lines;
    }

    /**
     * Converts the lines of one source.
     */
    protected interface LineConverter {

        /**
         * Converts a line.
         *
         * @param aLineNumber - line number, starting at 1
         * @param aLine - line without line separator
         * @return converted or unchanged line
         */
        String convert(int aLineNumber, String aLine);
    }

    /**
     * Problems, because of which an include directive is not converted.
     */
    public enum Problem {

        /** The file is not a file of the project. */
        NOT_FOUND,

        /** The file is a file of the project, but it is not exported. */
        NOT_EXPORTED,

        /** The directive cannot be converted, e.g. <code>EXEC SQL INCLUDE</code>. */
        NOT_SUPPORTED,

        /** The converted directive does not fit into the columns of the source. */
        TOO_LONG;
    }

    /**
     * Include directive of a source.
     */
    public static final class Include {

        private final int lineNumber;
        private final String name;
        private final ExportMapping.Entry member;
        private final Problem problem;

        Include(int aLineNumber, String aName, ExportMapping.Entry aMember, Problem aProblem) {
            this.lineNumber = aLineNumber;
            this.name = aName;
            this.member = aMember;
            this.problem = aProblem;
        }

        /**
         * Returns the line number, starting at 1.
         */
        public int getLineNumber() {
            return lineNumber;
        }

        /**
         * Returns the name of the directive, e.g. the path of the stream file.
         */
        public String getName() {
            return name;
        }

        /**
         * Returns the member, to which the directive refers after the
         * conversion, or <code>null</code>, if it has not been converted.
         */
        public ExportMapping.Entry getMember() {
            return member;
        }

        /**
         * Returns the problem, because of which the directive has not been
         * converted, or <code>null</code>, if it has been converted.
         */
        public Problem getProblem() {
            return problem;
        }

        @Override
        public String toString() {
            return lineNumber + ": " + name + " -> " + (problem == null ? member.getTarget() : problem.toString()); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    /**
     * Result of the conversion of a source.
     */
    public static final class Result {

        private final List<Include> includes = new ArrayList<Include>();
        private String content;

        /**
         * Returns the converted content.
         */
        public String getContent() {
            return content;
        }

        /**
         * Returns the include directives, that refer to stream files, in the
         * order of the source.
         */
        public List<Include> getIncludes() {
            return Collections.unmodifiableList(includes);
        }

        /**
         * Returns <code>true</code>, if at least one directive has not been
         * converted.
         */
        public boolean hasProblems() {
            for (Include include : includes) {
                if (include.getProblem() != null) {
                    return true;
                }
            }
            return false;
        }

        void addConverted(int aLineNumber, String aName, ExportMapping.Entry aMember) {
            includes.add(new Include(aLineNumber, aName, aMember, null));
        }

        void addProblem(int aLineNumber, String aName, Problem aProblem) {
            includes.add(new Include(aLineNumber, aName, null, aProblem));
        }
    }
}
