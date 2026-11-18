/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.project;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Entries of the <code>.gitignore</code> file of a project directory for the
 * files, that do not belong into the repository: the output of TOBi, the
 * environment variables of Code for IBM i, the log directory, that IBM i
 * Project Explorer ignores, too, and the project file of RDi. The settings of
 * RDi (<code>.settings</code>) belong into the repository, because they hold
 * the encoding of the project, which determines the CCSID of the uploaded
 * files.
 * <p>
 * The entries are written like IBM i Project Explorer writes them, without a
 * trailing slash, because its action <i>Create iproj.json</i> compares the
 * lines of an existing file literally and would add <code>.logs</code> again
 * after <code>.logs/</code>. A new file gets a comment per {@link Group}. An
 * existing file keeps its content and only gets the entries, that its rules do
 * not ignore yet (see {@link IgnoreRules}), without comments.
 * <p>
 * The wizards of local IFS projects add the project file of RDi, the migration
 * all groups.
 */
public final class GitIgnoreEntries {

    /**
     * Groups of entries with the comment, that precedes them in a new file.
     */
    public enum Group {

        /** Output of <code>makei build</code>. */
        TOBI_OUTPUT ("Output of TOBi (makei build)", ".logs", ".evfevent", ".deps", ".Rules.mk.build"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

        /** Environment variables of Code for IBM i. */
        ENVIRONMENT ("Environment variables of Code for IBM i, e.g. the library of the build", ".env"), //$NON-NLS-1$ //$NON-NLS-2$

        /** Log directory, that IBM i Project Explorer ignores. */
        TEST_LOGS ("Log directory, that IBM i Project Explorer ignores, too", ".itest"), //$NON-NLS-1$ //$NON-NLS-2$

        /**
         * Project file, that RDi creates in the project directory. Its name is
         * chosen by every user on the import.
         */
        RDI_PROJECT_FILE ("Project file of RDi (Import as Local IFS Project)", ".project"); //$NON-NLS-1$ //$NON-NLS-2$

        private final String comment;
        private final List<String> entries;

        private Group(String aComment, String... anEntries) {
            this.comment = aComment;
            this.entries = Collections.unmodifiableList(Arrays.asList(anEntries));
        }

        /**
         * Returns the comment, that precedes the entries in a new file.
         */
        public String getComment() {
            return comment;
        }

        /**
         * Returns the entries, e.g. <code>.logs</code>.
         */
        public List<String> getEntries() {
            return entries;
        }
    }

    /** Entries, that are directories. */
    private static final Set<String> DIRECTORIES = new HashSet<>(Arrays.asList(".logs", ".evfevent", ".deps", ".itest")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

    private static final String GIT_DIRECTORY = ".git"; //$NON-NLS-1$
    private static final String DIRECTORY_SUFFIX = "/"; //$NON-NLS-1$
    private static final String LINE_FEED = "\n"; //$NON-NLS-1$
    private static final String CRLF = "\r\n"; //$NON-NLS-1$
    private static final String LINE_SEPARATOR_PATTERN = "\\r?\\n"; //$NON-NLS-1$
    private static final String COMMENT = "# "; //$NON-NLS-1$
    private static final char COMMENT_CHARACTER = '#';

    private GitIgnoreEntries() {
        // Utility class
    }

    /**
     * Returns the groups, that a migration writes into the
     * <code>.gitignore</code> file of the new project.
     *
     * @param anIsTobi - <code>true</code>, if the build tool of the project is
     *        TOBi
     * @return all groups; without the output of TOBi, if the build tool is not
     *         TOBi
     */
    public static List<Group> getMigrationGroups(boolean anIsTobi) {

        List<Group> groups = new ArrayList<>();
        if (anIsTobi) {
            groups.add(Group.TOBI_OUTPUT);
        }
        groups.add(Group.ENVIRONMENT);
        groups.add(Group.TEST_LOGS);
        groups.add(Group.RDI_PROJECT_FILE);

        return groups;
    }

    /**
     * Returns the groups, that the wizards of local IFS projects write into the
     * <code>.gitignore</code> file of the project directory.
     *
     * @return the project file of RDi
     */
    public static List<Group> getLocalProjectGroups() {
        return Collections.singletonList(Group.RDI_PROJECT_FILE);
    }

    /**
     * Adds the entries of groups to the content of a <code>.gitignore</code>
     * file. Without content, the groups are written with their comments,
     * separated by blank lines. Otherwise only the entries, that the content
     * does not ignore yet, are added at its end, with the line separator of the
     * content.
     *
     * @param aContent - content of the existing file or <code>null</code>, if
     *        there is no file
     * @param aGroups - groups
     * @return new content or <code>null</code>, if the content already ignores
     *         all entries
     */
    public static String addEntries(String aContent, List<Group> aGroups) {

        if (aContent == null || aContent.trim().length() == 0) {
            return createContent(aGroups);
        }

        List<String> entries = new ArrayList<>();
        for (Group group : aGroups) {
            entries.addAll(group.getEntries());
        }

        return addMissingEntries(aContent, entries);
    }

    /**
     * Adds the entries of a new <code>.gitignore</code> file, e.g. of a
     * migration, that the content of an existing file does not ignore yet, at
     * the end of the existing content. Comments and blank lines of the new file
     * are not added.
     *
     * @param aContent - content of the existing file
     * @param aNewContent - content of the new file
     * @return new content or <code>null</code>, if the existing content already
     *         ignores all entries
     */
    public static String merge(String aContent, String aNewContent) {

        List<String> entries = new ArrayList<>();
        for (String line : aNewContent.split(LINE_SEPARATOR_PATTERN)) {
            String entry = line.trim();
            if (entry.length() > 0 && entry.charAt(0) != COMMENT_CHARACTER) {
                entries.add(entry);
            }
        }

        return addMissingEntries(aContent, entries);
    }

    /**
     * Returns <code>true</code>, if a directory is in a Git working copy: the
     * directory or one of its parent directories contains <code>.git</code>, a
     * directory or, in a linked working tree or a submodule, a file. The
     * directory does not need to exist.
     *
     * @param aDirectory - directory
     * @return <code>true</code>, if the directory is in a Git working copy
     */
    public static boolean isInGitWorkingCopy(File aDirectory) {

        for (File directory = aDirectory.getAbsoluteFile(); directory != null; directory = directory.getParentFile()) {
            if (new File(directory, GIT_DIRECTORY).exists()) {
                return true;
            }
        }

        return false;
    }

    private static String createContent(List<Group> aGroups) {

        StringBuilder content = new StringBuilder();
        for (Group group : aGroups) {
            if (content.length() > 0) {
                content.append(LINE_FEED);
            }
            content.append(COMMENT).append(group.getComment()).append(LINE_FEED);
            for (String entry : group.getEntries()) {
                content.append(entry).append(LINE_FEED);
            }
        }

        return content.toString();
    }

    private static String addMissingEntries(String aContent, List<String> anEntries) {

        String lineSeparator = aContent.contains(CRLF) ? CRLF : LINE_FEED;
        IgnoreRules rules = IgnoreRules.parse(aContent);

        StringBuilder addedLines = new StringBuilder();
        for (String entry : anEntries) {
            if (!isIgnored(rules, entry)) {
                addedLines.append(entry).append(lineSeparator);
            }
        }

        if (addedLines.length() == 0) {
            return null;
        }

        String content = aContent;
        if (content.length() > 0 && !content.endsWith(LINE_FEED)) {
            content += lineSeparator;
        }

        return content + addedLines;
    }

    private static boolean isIgnored(IgnoreRules aRules, String anEntry) {

        if (anEntry.endsWith(DIRECTORY_SUFFIX)) {
            return aRules.isIgnored(anEntry.substring(0, anEntry.length() - DIRECTORY_SUFFIX.length()), true);
        }

        return aRules.isIgnored(anEntry, DIRECTORIES.contains(anEntry));
    }
}
