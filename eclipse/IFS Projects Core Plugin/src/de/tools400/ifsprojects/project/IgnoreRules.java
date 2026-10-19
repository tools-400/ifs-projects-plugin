/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.project;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Rules of the <code>.deployignore</code> or <code>.gitignore</code> file of
 * the project directory, that exclude files from the upload of local IFS
 * projects and from the export to a library, see {@link ProjectFiles}.
 * <p>
 * The syntax of git is supported for a single file in the project directory:
 * comments (<code>#</code>), negation (<code>!</code>), directories (trailing
 * <code>/</code>), patterns relative to the project directory (with a
 * <code>/</code> at the beginning or in the middle) or matching a name at any
 * level, and the wildcards <code>*</code>, <code>?</code>,
 * <code>[...]</code> and <code>**</code>. The last matching rule wins. Like
 * git, a file cannot be included again, if a parent directory is excluded,
 * because the upload does not descend into excluded directories.
 * <p>
 * Ignore files in subdirectories are not supported.
 */
public final class IgnoreRules {

    /** Ignore file of the plug-in, preferred to the ignore file of git. */
    public static final String DEPLOY_IGNORE_FILE = ".deployignore"; //$NON-NLS-1$

    /** Ignore file of git, used if there is no deploy ignore file. */
    public static final String GIT_IGNORE_FILE = ".gitignore"; //$NON-NLS-1$

    private static final String LINE_SEPARATOR_PATTERN = "\\r?\\n"; //$NON-NLS-1$
    private static final String ANY_DIRECTORIES = "(?:.*/)?"; //$NON-NLS-1$
    private static final String ANYTHING = ".*"; //$NON-NLS-1$
    private static final String ANY_CHARACTERS = "[^/]*"; //$NON-NLS-1$
    private static final String ANY_CHARACTER = "[^/]"; //$NON-NLS-1$
    private static final String REGEX_SPECIAL_CHARACTERS = "\\.[]{}()*+-?^$|&"; //$NON-NLS-1$

    private final List<Rule> rules;

    private IgnoreRules(List<Rule> aRules) {
        this.rules = Collections.unmodifiableList(aRules);
    }

    /**
     * Returns rules, that do not exclude any file.
     */
    public static IgnoreRules empty() {
        return new IgnoreRules(new ArrayList<Rule>());
    }

    /**
     * Parses the content of an ignore file. Invalid patterns are skipped.
     *
     * @param aContent - content of the file
     * @return rules
     */
    public static IgnoreRules parse(String aContent) {

        List<Rule> rules = new ArrayList<Rule>();
        if (aContent != null) {
            for (String line : aContent.split(LINE_SEPARATOR_PATTERN)) {
                Rule rule = parseLine(line);
                if (rule != null) {
                    rules.add(rule);
                }
            }
        }

        return new IgnoreRules(rules);
    }

    /**
     * Returns <code>true</code>, if the rules exclude a file or directory.
     *
     * @param aRelativePath - path relative to the project directory, e.g.
     *        <code>src/hello.rpgle</code>
     * @param anIsDirectory - <code>true</code>, if the path is a directory
     * @return <code>true</code>, if the file or directory is not uploaded
     */
    public boolean isIgnored(String aRelativePath, boolean anIsDirectory) {

        boolean isIgnored = false;
        for (Rule rule : rules) {
            if (rule.isDirectoryOnly && !anIsDirectory) {
                continue;
            }
            if (rule.pattern.matcher(aRelativePath).matches()) {
                isIgnored = !rule.isNegated;
            }
        }

        return isIgnored;
    }

    private static Rule parseLine(String aLine) {

        String line = stripTrailingBlanks(aLine);
        if (line.length() == 0 || line.charAt(0) == '#') {
            return null;
        }

        boolean isNegated = false;
        if (line.charAt(0) == '!') {
            isNegated = true;
            line = line.substring(1);
        } else if (line.startsWith("\\!") || line.startsWith("\\#")) { //$NON-NLS-1$ //$NON-NLS-2$
            line = line.substring(1);
        }

        boolean isDirectoryOnly = false;
        if (line.endsWith("/")) { //$NON-NLS-1$
            isDirectoryOnly = true;
            line = line.substring(0, line.length() - 1);
        }

        // A pattern with a slash is relative to the project directory.
        boolean isAnchored = line.indexOf('/') >= 0;
        if (line.startsWith("/")) { //$NON-NLS-1$
            line = line.substring(1);
        }

        if (line.length() == 0) {
            return null;
        }

        try {
            Pattern pattern = Pattern.compile((isAnchored ? "" : ANY_DIRECTORIES) + toRegex(line)); //$NON-NLS-1$
            return new Rule(pattern, isNegated, isDirectoryOnly);
        } catch (PatternSyntaxException e) {
            return null;
        }
    }

    /**
     * Converts a pattern of git to a regular expression, that matches a path
     * relative to the project directory.
     */
    private static String toRegex(String aPattern) {

        StringBuilder regex = new StringBuilder();
        int length = aPattern.length();

        int index = 0;
        while (index < length) {
            char character = aPattern.charAt(index);

            if (character == '*' && index + 1 < length && aPattern.charAt(index + 1) == '*') {
                boolean isAtStart = index == 0 || aPattern.charAt(index - 1) == '/';
                boolean isAtEnd = index + 2 == length || aPattern.charAt(index + 2) == '/';
                if (isAtStart && index + 2 == length) {
                    // "**" at the end: everything inside
                    regex.append(ANYTHING);
                    index += 2;
                } else if (isAtStart && isAtEnd) {
                    // "**/": zero or more directories
                    regex.append(ANY_DIRECTORIES);
                    index += 3;
                } else {
                    regex.append(ANY_CHARACTERS);
                    index += 2;
                }
            } else if (character == '*') {
                regex.append(ANY_CHARACTERS);
                index++;
            } else if (character == '?') {
                regex.append(ANY_CHARACTER);
                index++;
            } else if (character == '[' && aPattern.indexOf(']', index + 2) > 0) {
                int end = aPattern.indexOf(']', index + 2);
                String characters = aPattern.substring(index + 1, end);
                regex.append('[');
                if (characters.charAt(0) == '!') {
                    regex.append('^');
                    characters = characters.substring(1);
                }
                for (char member : characters.toCharArray()) {
                    if (member == '\\' || member == '[' || member == ']' || member == '&') {
                        regex.append('\\');
                    }
                    regex.append(member);
                }
                regex.append(']');
                index = end + 1;
            } else if (character == '\\' && index + 1 < length) {
                appendLiteral(regex, aPattern.charAt(index + 1));
                index += 2;
            } else {
                appendLiteral(regex, character);
                index++;
            }
        }

        return regex.toString();
    }

    private static void appendLiteral(StringBuilder aRegex, char aCharacter) {

        if (REGEX_SPECIAL_CHARACTERS.indexOf(aCharacter) >= 0) {
            aRegex.append('\\');
        }
        aRegex.append(aCharacter);
    }

    /**
     * Removes trailing blanks, unless they are escaped with a backslash.
     */
    private static String stripTrailingBlanks(String aLine) {

        int end = aLine.length();
        while (end > 0 && aLine.charAt(end - 1) == ' ' && !(end > 1 && aLine.charAt(end - 2) == '\\')) {
            end--;
        }

        return aLine.substring(0, end);
    }

    private static final class Rule {

        private final Pattern pattern;
        private final boolean isNegated;
        private final boolean isDirectoryOnly;

        private Rule(Pattern aPattern, boolean anIsNegated, boolean anIsDirectoryOnly) {
            this.pattern = aPattern;
            this.isNegated = anIsNegated;
            this.isDirectoryOnly = anIsDirectoryOnly;
        }
    }
}
