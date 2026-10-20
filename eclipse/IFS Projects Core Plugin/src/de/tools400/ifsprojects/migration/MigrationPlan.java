/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.migration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import de.tools400.ifsprojects.sources.IncludeConverter;
import de.tools400.ifsprojects.sources.MemberText;
import de.tools400.ifsprojects.sources.SourceTypes;

/**
 * Plan of a migration: the stream files of the members of source files, their
 * converted contents and the problems. The plan is computed in two passes:
 * the first pass finds the copy books, i.e. the members included by other
 * members, which get the extension of copy books, see
 * {@link MigrationMapping}; the second pass converts the include directives
 * to these paths and inserts the member text.
 * <p>
 * The contents must have the line separator <code>\n</code>, which the plan
 * uses for the lines it inserts.
 */
public final class MigrationPlan {

    private static final String CRLF = "\r\n"; //$NON-NLS-1$
    private static final String LF = "\n"; //$NON-NLS-1$

    private final String library;
    private final Set<String> sourceFiles;
    private final MigrationSettings settings;
    private final List<Item> items;

    /**
     * Computes the plan.
     *
     * @param aLibrary - library of the source files
     * @param aSourceFiles - migrated source files
     * @param aMembers - members of the source files
     * @param aContents - contents of the members by their keys, see
     *        {@link SourceMember#getKey()}
     * @param aReadErrors - errors of the members, that could not be read, by
     *        their keys
     * @param aSettings - settings
     */
    public MigrationPlan(String aLibrary, Collection<String> aSourceFiles, List<SourceMember> aMembers, Map<String, String> aContents,
        Map<String, String> aReadErrors, MigrationSettings aSettings) {

        this.library = aLibrary.toUpperCase(Locale.ROOT);
        this.sourceFiles = new HashSet<String>();
        for (String sourceFile : aSourceFiles) {
            sourceFiles.add(sourceFile.toUpperCase(Locale.ROOT));
        }
        this.settings = aSettings;

        this.items = new ArrayList<Item>();
        for (SourceMember member : aMembers) {
            String content = aContents.get(member.getKey());
            items.add(new Item(member, content == null ? null : content.replace(CRLF, LF), aReadErrors.get(member.getKey())));
        }

        compute();
    }

    /**
     * Returns the items in the order of the members.
     */
    public List<Item> getItems() {
        return Collections.unmodifiableList(items);
    }

    /**
     * Returns the items, that are migrated.
     */
    public List<Item> getMigratedItems() {

        List<Item> migrated = new ArrayList<Item>();
        for (Item item : items) {
            if (item.isMigrated()) {
                migrated.add(item);
            }
        }

        return migrated;
    }

    private void compute() {

        Set<String> copyBooks = findCopyBooks();
        Map<String, String> paths = getPaths(copyBooks);

        for (Item item : items) {
            item.path = paths.get(item.member.getKey());
            addStaticProblems(item);
            if (item.isMigrated()) {
                convert(item, paths);
            }
        }
    }

    /**
     * Converts the includes with provisional paths, in which no member is a
     * copy book, and returns the members, to which the includes refer.
     */
    private Set<String> findCopyBooks() {

        Map<String, String> paths = getPaths(Collections.<String> emptySet());

        Set<String> copyBooks = new HashSet<String>();
        for (Item item : items) {
            if (item.content != null && item.member.getSourceType() != null) {
                MemberIncludeConverter converter = MemberIncludeConverter.create(item.member.getSourceType(), library, paths, sourceFiles,
                    IncludeStyle.REPLACE);
                if (converter != null) {
                    converter.convert(item.member.getKey(), item.content);
                    copyBooks.addAll(converter.getIncludedMembers());
                }
            }
        }

        return copyBooks;
    }

    /**
     * Returns the paths of the members by their keys; the path of a member,
     * that is not migrated, is <code>null</code>.
     */
    private Map<String, String> getPaths(Set<String> aCopyBooks) {

        Map<String, String> paths = new HashMap<String, String>();
        for (Item item : items) {
            String key = item.member.getKey();
            String path = item.content == null ? null : MigrationMapping.getPath(item.member, aCopyBooks.contains(key));
            paths.put(key, path);
        }

        return paths;
    }

    private void addStaticProblems(Item anItem) {

        if (anItem.content == null) {
            anItem.problems.add(new Problem(ProblemType.NOT_READ, 0, anItem.readError, null));
            return;
        }

        String sourceType = anItem.member.getSourceType();
        if (sourceType == null) {
            anItem.problems.add(new Problem(ProblemType.NO_SOURCE_TYPE, 0, null, null));
            return;
        }

        if (!SourceTypes.isKnown(sourceType)) {
            anItem.problems.add(new Problem(ProblemType.UNKNOWN_SOURCE_TYPE, 0, sourceType, null));
        }

        if (MigrationMapping.hasMakeCharacters(anItem.member.getName())) {
            anItem.problems.add(new Problem(ProblemType.MAKE_CHARACTERS, 0, anItem.member.getName(), null));
        }
    }

    private void convert(Item anItem, Map<String, String> aPaths) {

        String content = anItem.content;

        MemberIncludeConverter converter = MemberIncludeConverter.create(anItem.member.getSourceType(), library, aPaths, sourceFiles, settings.getIncludeStyle());
        if (converter != null) {
            anItem.includes = converter.convert(anItem.member.getKey(), content);
            content = anItem.includes.getContent();
            for (String key : new TreeSet<String>(converter.getIncludedMembers())) {
                anItem.includedPaths.add(aPaths.get(key));
            }
            for (IncludeConverter.Include include : anItem.includes.getIncludes()) {
                if (include.getProblem() != null) {
                    anItem.problems.add(new Problem(ProblemType.INCLUDE, include.getLineNumber(), include.getName(), include.getProblem()));
                }
            }
        }

        if (settings.isMemberText() && anItem.member.getText() != null) {
            content = MemberText.insert(content, anItem.member.getSourceType(), anItem.member.getText());
        }

        anItem.migratedContent = content;
    }

    /**
     * Types of the problems of a member.
     */
    public enum ProblemType {

        /** The member could not be read. */
        NOT_READ(true),

        /** The member has no source type and therefore no file extension. */
        NO_SOURCE_TYPE(true),

        /** TOBi does not know the source type of the member. */
        UNKNOWN_SOURCE_TYPE(false),

        /** The name contains characters, that make interprets in Rules.mk. */
        MAKE_CHARACTERS(false),

        /** An include directive could not be converted. */
        INCLUDE(false);

        private final boolean isError;

        private ProblemType(boolean anIsError) {
            this.isError = anIsError;
        }

        /**
         * Returns <code>true</code>, if the member is not migrated because of
         * the problem; otherwise it is a warning.
         */
        public boolean isError() {
            return isError;
        }
    }

    /**
     * Problem of a member.
     */
    public static final class Problem {

        private final ProblemType type;
        private final int lineNumber;
        private final String text;
        private final IncludeConverter.Problem includeProblem;

        Problem(ProblemType aType, int aLineNumber, String aText, IncludeConverter.Problem anIncludeProblem) {
            this.type = aType;
            this.lineNumber = aLineNumber;
            this.text = aText;
            this.includeProblem = anIncludeProblem;
        }

        public ProblemType getType() {
            return type;
        }

        /**
         * Returns the line number, starting at 1, or 0, if the problem is not
         * related to a line.
         */
        public int getLineNumber() {
            return lineNumber;
        }

        /**
         * Returns the text of the problem, e.g. the name of an include
         * directive, or <code>null</code>.
         */
        public String getText() {
            return text;
        }

        /**
         * Returns the problem of the include directive of
         * {@link ProblemType#INCLUDE}, otherwise <code>null</code>.
         */
        public IncludeConverter.Problem getIncludeProblem() {
            return includeProblem;
        }

        @Override
        public String toString() {
            return type + (lineNumber > 0 ? " " + lineNumber : "") + (text == null ? "" : " " + text) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                + (includeProblem == null ? "" : " " + includeProblem); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    /**
     * Member and the stream file, into which it is migrated.
     */
    public static final class Item {

        private final SourceMember member;
        private final String content;
        private final String readError;
        private final List<Problem> problems;
        private final List<String> includedPaths;

        private String path;
        private String migratedContent;
        private IncludeConverter.Result includes;

        Item(SourceMember aMember, String aContent, String aReadError) {
            this.member = aMember;
            this.content = aContent;
            this.readError = aReadError;
            this.problems = new ArrayList<Problem>();
            this.includedPaths = new ArrayList<String>();
        }

        public SourceMember getMember() {
            return member;
        }

        /**
         * Returns the path of the stream file relative to the project
         * directory or <code>null</code>, if the member is not migrated.
         */
        public String getPath() {
            return path;
        }

        /**
         * Returns the content of the stream file: converted includes and
         * member text.
         */
        public String getContent() {
            return migratedContent;
        }

        /**
         * Returns the include directives of the member or <code>null</code>,
         * if its source type has none.
         */
        public IncludeConverter.Result getIncludes() {
            return includes;
        }

        /**
         * Returns the paths of the stream files, to which the converted include
         * directives refer, relative to the project directory, e.g.
         * <code>qcpysrc/consts.rpgleinc</code>.
         */
        public List<String> getIncludedPaths() {
            return Collections.unmodifiableList(includedPaths);
        }

        public List<Problem> getProblems() {
            return Collections.unmodifiableList(problems);
        }

        /**
         * Returns <code>true</code>, if the member is migrated: it has no
         * errors, but it may have warnings.
         */
        public boolean isMigrated() {
            for (Problem problem : problems) {
                if (problem.getType().isError()) {
                    return false;
                }
            }
            return path != null;
        }

        @Override
        public String toString() {
            return member.getKey() + " -> " + path + " " + problems; //$NON-NLS-1$ //$NON-NLS-2$
        }
    }
}
