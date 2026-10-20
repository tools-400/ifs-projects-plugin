/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.sources;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Converts the include directives of a source line by line, e.g. from stream
 * files to members (<code>/copy 'inc/consts.rpgleinc'</code> to
 * <code>/copy inc,consts</code>). The subclasses decide, to what a directive
 * refers after the conversion, see {@link RpgLineConverter} and
 * {@link ClLineConverter}. The rest of the source is kept unchanged, including
 * the line separators.
 */
public abstract class IncludeConverter {

    /**
     * Converts the include directives of a source.
     *
     * @param aPath - path of the source, that identifies it for the subclass,
     *        e.g. relative to the project directory
     * @param aContent - content of the source
     * @return result
     */
    public final Result convert(String aPath, String aContent) {

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
     * @param aPath - path of the source, see {@link #convert(String, String)}
     * @param aResult - result, to which the includes are added
     * @return converter
     */
    protected abstract LineConverter createLineConverter(String aPath, Result aResult);

    /**
     * Adds a converted directive to a result.
     *
     * @param aResult - result
     * @param aLineNumber - line number, starting at 1
     * @param aName - name of the directive before the conversion
     * @param aTarget - name of the directive after the conversion
     */
    protected static void addConverted(Result aResult, int aLineNumber, String aName, String aTarget) {
        aResult.includes.add(new Include(aLineNumber, aName, aTarget, null));
    }

    /**
     * Adds a directive, that has not been converted, to a result.
     *
     * @param aResult - result
     * @param aLineNumber - line number, starting at 1
     * @param aName - name of the directive
     * @param aProblem - reason, why it has not been converted
     */
    protected static void addProblem(Result aResult, int aLineNumber, String aName, Problem aProblem) {
        aResult.includes.add(new Include(aLineNumber, aName, null, aProblem));
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
     * Converter of the lines of a source. It may keep a state, e.g. for
     * continuation lines.
     */
    public interface LineConverter {

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

        /** The file or member of the directive is not found. */
        NOT_FOUND,

        /**
         * The file or member of the directive is found, but it is not
         * transferred itself, e.g. not exported.
         */
        NOT_TRANSFERRED,

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
        private final String target;
        private final Problem problem;

        Include(int aLineNumber, String aName, String aTarget, Problem aProblem) {
            this.lineNumber = aLineNumber;
            this.name = aName;
            this.target = aTarget;
            this.problem = aProblem;
        }

        /**
         * Returns the line number, starting at 1.
         */
        public int getLineNumber() {
            return lineNumber;
        }

        /**
         * Returns the name of the directive before the conversion, e.g. the
         * path of the stream file.
         */
        public String getName() {
            return name;
        }

        /**
         * Returns the name of the directive after the conversion, e.g.
         * <code>INC,CONSTS</code>, or <code>null</code>, if it has not been
         * converted.
         */
        public String getTarget() {
            return target;
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
            return lineNumber + ": " + name + " -> " + (problem == null ? target : problem.toString()); //$NON-NLS-1$ //$NON-NLS-2$
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
         * Returns the include directives, that have been converted or could
         * not be converted, in the order of the source.
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
    }
}
