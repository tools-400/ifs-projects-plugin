/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.export;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.ibm.as400.access.CharConverter;

import de.tools400.ifsprojects.ibmi.IfsPaths;
import de.tools400.ifsprojects.sources.IncludeConverter;
import de.tools400.ifsprojects.sources.MemberText;

/**
 * Plan of an export: the files of a project, the members, into which they are
 * exported, their converted content and the problems, that have been found
 * before the export.
 * <ul>
 * <li>Errors: the member name is not valid, several files are exported into the
 * same member, the file could not be read, the source file exists, but is not a
 * source file, or a line is longer than the record length of an existing source
 * file. Such files are not exported.</li>
 * <li>Warnings: characters, that do not exist in the CCSID of the source file,
 * and include directives, that could not be converted.</li>
 * </ul>
 * New source files get the record length of the settings or, if a line is
 * longer, the length of the longest line plus sequence number and date.
 */
public final class ExportPlan {

    private final ExportSettings settings;
    private final IncludeResolver resolver;
    private final LibraryInfo library;
    private final List<Item> items;
    private final Map<String, Integer> otherFileTypes;
    private final List<SourceTypeProposals.Proposal> sourceTypeProposals;
    private final Map<String, Integer> newSourceFiles;

    /**
     * Creates the plan.
     *
     * @param aSettings - settings of the export
     * @param aFiles - paths of all files of the project relative to the project
     *        directory
     * @param aContents - contents of the files by path, at least of the
     *        exported files, see {@link ExportMapping#isExported(String)}, and
     *        of {@link SourceTypeProposals#CODE_FOR_IBM_I_ACTIONS}, if it
     *        exists
     * @param aReadErrors - messages of the files, that could not be read, by
     *        path
     * @param aResolver - resolver of the include directives
     * @param aLibrary - files and members of the library
     */
    public ExportPlan(ExportSettings aSettings, List<String> aFiles, Map<String, String> aContents, Map<String, String> aReadErrors,
        IncludeResolver aResolver, LibraryInfo aLibrary) {

        this.settings = aSettings;
        this.resolver = aResolver;
        this.library = aLibrary;
        this.items = new ArrayList<>();
        this.newSourceFiles = new LinkedHashMap<>();

        ExportMapping mapping = ExportMapping.create(aSettings);
        Set<String> exportedPaths = new HashSet<>();
        for (ExportMapping.Entry entry : mapping.map(aFiles)) {
            items.add(new Item(entry, aContents.get(entry.getPath()), aReadErrors.get(entry.getPath())));
            exportedPaths.add(entry.getPath());
        }
        this.otherFileTypes = countOtherFileTypes(aFiles, exportedPaths);
        this.sourceTypeProposals = Collections.unmodifiableList(SourceTypeProposals.create(aFiles, exportedPaths,
            aSettings.getFurtherSourceTypes(), aContents.get(SourceTypeProposals.CODE_FOR_IBM_I_ACTIONS)));

        compute();
    }

    /**
     * Counts the files, that are not exported, by their extensions. Hidden
     * files are skipped like the other excluded files, e.g. <code>.git</code>.
     *
     * @return numbers of files by extension in lowercase, the most frequent
     *         extension first
     */
    private static Map<String, Integer> countOtherFileTypes(List<String> aFiles, Set<String> anExportedPaths) {

        final Map<String, Integer> counts = new HashMap<>();
        for (String path : aFiles) {
            if (!anExportedPaths.contains(path) && !ExportMapping.isHidden(path)) {
                String extension = ExportMapping.getExtension(IfsPaths.getFileName(path)).toLowerCase(Locale.ROOT);
                Integer count = counts.get(extension);
                counts.put(extension, Integer.valueOf(count == null ? 1 : count.intValue() + 1));
            }
        }

        List<String> extensions = new ArrayList<>(counts.keySet());
        Collections.sort(extensions, new Comparator<String>() {
            public int compare(String anExtension, String anotherExtension) {
                int result = counts.get(anotherExtension).compareTo(counts.get(anExtension));
                return result != 0 ? result : anExtension.compareTo(anotherExtension);
            }
        });

        Map<String, Integer> sortedCounts = new LinkedHashMap<>();
        for (String extension : extensions) {
            sortedCounts.put(extension, counts.get(extension));
        }

        return Collections.unmodifiableMap(sortedCounts);
    }

    /**
     * Returns the files with an exported source type in the order of the paths.
     */
    public List<Item> getItems() {
        return Collections.unmodifiableList(items);
    }

    /**
     * Returns the files, that are selected and have no errors.
     */
    public List<Item> getExportedItems() {

        List<Item> exportedItems = new ArrayList<>();
        for (Item item : items) {
            if (item.isExported()) {
                exportedItems.add(item);
            }
        }

        return exportedItems;
    }

    /**
     * Returns the number of files, that have no exported source type, e.g.
     * <code>iproj.json</code>, except of hidden files.
     */
    public int getOtherFileCount() {

        int count = 0;
        for (Integer value : otherFileTypes.values()) {
            count += value.intValue();
        }

        return count;
    }

    /**
     * Returns the extensions of the files, that have no exported source type,
     * except of hidden files, e.g. <code>json</code>, <code>mk</code> or
     * <code>tbl</code>, which can be added to the further source types of the
     * settings.
     *
     * @return numbers of files by extension in lowercase, an empty string for
     *         files without extension, the most frequent extension first
     */
    public Map<String, Integer> getOtherFileTypes() {
        return otherFileTypes;
    }

    /**
     * Returns the extensions, that can be added to the further source types
     * of the settings, and those, that are selected already, see
     * {@link SourceTypeProposals}.
     */
    public List<SourceTypeProposals.Proposal> getSourceTypeProposals() {
        return sourceTypeProposals;
    }

    /**
     * Returns the extensions, that are probably source types, but are not
     * exported, because they are no further source types of the settings.
     *
     * @return extensions in lowercase, e.g. <code>tbl</code>
     */
    public List<String> getUnselectedProposals() {

        List<String> extensions = new ArrayList<>();
        for (SourceTypeProposals.Proposal proposal : sourceTypeProposals) {
            if (proposal.getReason() != null && !proposal.isSelected()) {
                extensions.add(proposal.getExtension());
            }
        }

        return extensions;
    }

    /**
     * Returns <code>true</code>, if the library does not exist and is created
     * by the export.
     */
    public boolean isCreateLibrary() {
        return !library.exists();
    }

    /**
     * Returns the source files, that are created for the exported files.
     *
     * @return record lengths by source file, in the order of the files
     */
    public Map<String, Integer> getNewSourceFiles() {
        return Collections.unmodifiableMap(newSourceFiles);
    }

    /**
     * Returns the settings of the export.
     */
    public ExportSettings getSettings() {
        return settings;
    }

    /**
     * Selects or deselects a file and computes the plan again.
     *
     * @param anItem - file
     * @param anIsSelected - <code>true</code> to export the file
     */
    public void setSelected(Item anItem, boolean anIsSelected) {
        anItem.isSelected = anIsSelected;
        compute();
    }

    /**
     * Selects the specified files, deselects the others and computes the plan
     * again, once for all files.
     *
     * @param aSelectedItems - files to export
     */
    public void setSelectedItems(Collection<Item> aSelectedItems) {
        for (Item item : items) {
            item.isSelected = aSelectedItems.contains(item);
        }
        compute();
    }

    /**
     * Computes contents, problems and new source files. A file, whose lines are
     * too long, is not exported, so that the include directives, that refer to
     * it, cannot be converted. Therefore the includes are converted again,
     * until no further file fails.
     */
    private void compute() {

        Set<String> failedPaths = new HashSet<>();
        for (Item item : items) {
            item.reset();
            addStaticProblems(item);
            if (item.hasErrors()) {
                failedPaths.add(item.getPath());
            }
        }

        boolean isChanged = true;
        while (isChanged) {
            isChanged = false;
            Map<String, ExportMapping.Entry> members = getMembers(failedPaths);
            for (Item item : items) {
                if (members.containsKey(item.getPath())) {
                    convert(item, members);
                    LibraryInfo.File file = library.getFile(item.getSourceFile());
                    if (file != null && item.longestLine.getLength() > file.getDataLength()) {
                        failedPaths.add(item.getPath());
                        isChanged = true;
                    }
                }
            }
        }

        Map<Integer, CharConverter> converters = new HashMap<>();
        newSourceFiles.clear();
        for (Item item : items) {
            if (item.isSelected && item.content != null && !item.hasErrors()) {
                addDynamicProblems(item, converters);
            }
        }

        for (Item item : items) {
            if (item.isExported() && library.getFile(item.getSourceFile()) == null) {
                int recordLength = Math.max(settings.getRecordLength(), item.longestLine.getLength() + ExportSettings.SEQUENCE_AND_DATE_LENGTH);
                Integer previous = newSourceFiles.get(item.getSourceFile());
                if (previous == null || previous.intValue() < recordLength) {
                    newSourceFiles.put(item.getSourceFile(), Integer.valueOf(recordLength));
                }
            }
        }
    }

    /**
     * Returns the members of the selected files, that have no errors yet.
     */
    private Map<String, ExportMapping.Entry> getMembers(Set<String> aFailedPaths) {

        Map<String, ExportMapping.Entry> members = new HashMap<>();
        for (Item item : items) {
            if (item.isSelected && !aFailedPaths.contains(item.getPath())) {
                members.put(item.getPath(), item.entry);
            }
        }

        return members;
    }

    private void convert(Item anItem, Map<String, ExportMapping.Entry> aMembers) {

        String content = SourceCheck.removeByteOrderMark(anItem.rawContent);

        anItem.includes = null;
        if (settings.isConvertIncludes()) {
            ExportIncludeConverter converter = ExportIncludeConverter.create(anItem.getSourceType(), resolver, aMembers);
            if (converter != null) {
                anItem.includes = converter.convert(anItem.getPath(), content);
                content = anItem.includes.getContent();
            }
        }

        // The text becomes the text of the member, so its comment block, e.g.
        // of the migration, is not exported.
        anItem.text = null;
        if (settings.isMemberText()) {
            anItem.text = MemberText.parse(content);
            if (anItem.text != null) {
                content = MemberText.remove(content);
            }
        }

        anItem.content = content;
        anItem.longestLine = SourceCheck.getLongestLine(content);
    }

    private void addStaticProblems(Item anItem) {

        switch (anItem.entry.getStatus()) {
        case INVALID_MEMBER_NAME:
            anItem.problems.add(new Problem(ProblemType.INVALID_MEMBER_NAME, 0, null, 0, 0));
            return;
        case DUPLICATE:
            anItem.problems.add(new Problem(ProblemType.DUPLICATE, 0, null, 0, 0));
            return;
        default:
            break;
        }

        if (anItem.rawContent == null) {
            anItem.problems.add(new Problem(ProblemType.NOT_READ, 0, anItem.readError, 0, 0));
            return;
        }

        LibraryInfo.File file = library.getFile(anItem.getSourceFile());
        if (file != null && !file.isSourceFile()) {
            anItem.problems.add(new Problem(ProblemType.NOT_A_SOURCE_FILE, 0, anItem.getSourceFile(), 0, 0));
        }
    }

    private void addDynamicProblems(Item anItem, Map<Integer, CharConverter> aConverters) {

        LibraryInfo.File file = library.getFile(anItem.getSourceFile());
        if (file != null && anItem.longestLine.getLength() > file.getDataLength()) {
            anItem.problems.add(
                new Problem(ProblemType.LINE_TOO_LONG, anItem.longestLine.getNumber(), null, anItem.longestLine.getLength(), file.getDataLength()));
            return;
        }

        // Invalid bytes are the cause of the characters, that do not exist in
        // the CCSID, so they are reported instead.
        int ccsid = file == null ? settings.getCcsid() : file.getCcsid();
        int invalidLine = SourceCheck.findInvalidLine(anItem.content);
        CharConverter converter = getConverter(ccsid, aConverters);
        if (invalidLine > 0) {
            anItem.problems.add(new Problem(ProblemType.INVALID_BYTES, invalidLine, null, 0, 0));
        } else if (converter != null) {
            int lineNumber = SourceCheck.findUnmappableLine(anItem.content, converter);
            if (lineNumber > 0) {
                anItem.problems.add(new Problem(ProblemType.UNMAPPABLE_CHARACTER, lineNumber, null, 0, ccsid));
            }
        }

        if (anItem.includes != null) {
            for (IncludeConverter.Include include : anItem.includes.getIncludes()) {
                if (include.getProblem() != null) {
                    // A converted directive is named by its member, e.g. the
                    // source file of INCLUDE_FILE.
                    String text = include.getTarget() == null ? include.getName() : include.getTarget();
                    anItem.problems.add(new Problem(ProblemType.INCLUDE, include.getLineNumber(), text, 0, 0, include.getProblem()));
                }
            }
        }

        anItem.isExistingMember = library.hasMember(anItem.getSourceFile(), anItem.getMember());
    }

    /**
     * Returns the converter of a CCSID or <code>null</code>, if IBM Toolbox for
     * Java does not support the CCSID.
     */
    private static CharConverter getConverter(int aCcsid, Map<Integer, CharConverter> aConverters) {

        Integer key = Integer.valueOf(aCcsid);
        if (aConverters.containsKey(key)) {
            return aConverters.get(key);
        }

        CharConverter converter;
        try {
            converter = new CharConverter(aCcsid);
        } catch (UnsupportedEncodingException e) {
            converter = null;
        }
        aConverters.put(key, converter);

        return converter;
    }

    /**
     * Types of the problems of a file.
     */
    public enum ProblemType {

        /** The file name is not a valid member name. */
        INVALID_MEMBER_NAME (true),

        /** Several files are exported into the same member. */
        DUPLICATE (true),

        /** The file could not be read, see {@link Problem#getText()}. */
        NOT_READ (true),

        /**
         * The file of the member is not a source file, see
         * {@link Problem#getText()}.
         */
        NOT_A_SOURCE_FILE (true),

        /**
         * A line is longer than the record length of the existing source file.
         */
        LINE_TOO_LONG (true),

        /** A line contains characters, that do not exist in the CCSID. */
        UNMAPPABLE_CHARACTER (false),

        /**
         * A line contains bytes, that are not valid in the encoding of the
         * file, e.g. ISO-8859-1 in a stream file with CCSID 1208.
         */
        INVALID_BYTES (false),

        /** An include directive has not been converted. */
        INCLUDE (false);

        private final boolean isError;

        private ProblemType(boolean anIsError) {
            this.isError = anIsError;
        }

        /**
         * Returns <code>true</code>, if the file is not exported.
         */
        public boolean isError() {
            return isError;
        }
    }

    /**
     * Problem of a file.
     */
    public static final class Problem {

        private final ProblemType type;
        private final int lineNumber;
        private final String text;
        private final int length;
        private final int limit;
        private final IncludeConverter.Problem includeProblem;

        Problem(ProblemType aType, int aLineNumber, String aText, int aLength, int aLimit) {
            this(aType, aLineNumber, aText, aLength, aLimit, null);
        }

        Problem(ProblemType aType, int aLineNumber, String aText, int aLength, int aLimit, IncludeConverter.Problem anIncludeProblem) {
            this.type = aType;
            this.lineNumber = aLineNumber;
            this.text = aText;
            this.length = aLength;
            this.limit = aLimit;
            this.includeProblem = anIncludeProblem;
        }

        public ProblemType getType() {
            return type;
        }

        /**
         * Returns <code>true</code>, if the file is not exported.
         */
        public boolean isError() {
            return type.isError();
        }

        /**
         * Returns the line number or 0.
         */
        public int getLineNumber() {
            return lineNumber;
        }

        /**
         * Returns the text of the problem: the message of
         * {@link ProblemType#NOT_READ}, the file of
         * {@link ProblemType#NOT_A_SOURCE_FILE} or the name of the include
         * directive of {@link ProblemType#INCLUDE}.
         */
        public String getText() {
            return text;
        }

        /**
         * Returns the length of the line of {@link ProblemType#LINE_TOO_LONG}.
         */
        public int getLength() {
            return length;
        }

        /**
         * Returns the allowed length of {@link ProblemType#LINE_TOO_LONG} or
         * the CCSID of {@link ProblemType#UNMAPPABLE_CHARACTER}.
         */
        public int getLimit() {
            return limit;
        }

        /**
         * Returns the problem of {@link ProblemType#INCLUDE}.
         */
        public IncludeConverter.Problem getIncludeProblem() {
            return includeProblem;
        }
    }

    /**
     * File of the project.
     */
    public static final class Item {

        private final ExportMapping.Entry entry;
        private final String rawContent;
        private final String readError;
        private final List<Problem> problems;

        private boolean isSelected;
        private String content;
        private SourceCheck.Line longestLine;
        private IncludeConverter.Result includes;
        private String text;
        private boolean isExistingMember;

        Item(ExportMapping.Entry anEntry, String aRawContent, String aReadError) {
            this.entry = anEntry;
            this.rawContent = aRawContent;
            this.readError = aReadError;
            this.problems = new ArrayList<>();
            this.isSelected = true;
        }

        private void reset() {
            problems.clear();
            content = null;
            longestLine = null;
            includes = null;
            text = null;
            isExistingMember = false;
        }

        public String getPath() {
            return entry.getPath();
        }

        public String getSourceFile() {
            return entry.getSourceFile();
        }

        /**
         * Returns the member or <code>null</code>, if the file name is not a
         * valid member name.
         */
        public String getMember() {
            return entry.getMember();
        }

        public String getSourceType() {
            return entry.getSourceType();
        }

        /**
         * Returns <code>true</code>, if the user has selected the file.
         */
        public boolean isSelected() {
            return isSelected;
        }

        /**
         * Returns <code>true</code>, if the file is exported: it is selected
         * and has no errors.
         */
        public boolean isExported() {
            return isSelected && content != null && !hasErrors();
        }

        /**
         * Returns <code>true</code>, if the file has errors and cannot be
         * exported.
         */
        public boolean hasErrors() {
            for (Problem problem : problems) {
                if (problem.isError()) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Returns the problems, errors first. The problems of files, that are
         * not selected, are not checked, except for the errors of the name.
         */
        public List<Problem> getProblems() {

            List<Problem> sortedProblems = new ArrayList<>();
            for (Problem problem : problems) {
                if (problem.isError()) {
                    sortedProblems.add(problem);
                }
            }
            for (Problem problem : problems) {
                if (!problem.isError()) {
                    sortedProblems.add(problem);
                }
            }

            return sortedProblems;
        }

        /**
         * Returns the content, that is exported: the content of the file with
         * converted include directives, without byte order mark and without
         * the comment block of the member text, if it becomes the text of the
         * member, see {@link MemberText}.
         */
        public String getContent() {
            return content;
        }

        /**
         * Returns the content of the file, as it has been read, or
         * <code>null</code>, if it could not be read.
         */
        public String getOriginalContent() {
            return rawContent;
        }

        /**
         * Returns the include directives of the file or <code>null</code>, if
         * they are not converted.
         */
        public IncludeConverter.Result getIncludes() {
            return includes;
        }

        /**
         * Returns the member text or <code>null</code>.
         */
        public String getText() {
            return text;
        }

        /**
         * Returns <code>true</code>, if the member exists and is replaced.
         */
        public boolean isExistingMember() {
            return isExistingMember;
        }
    }
}
