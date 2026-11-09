/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.export;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import de.tools400.ifsprojects.ibmi.ClCommands;
import de.tools400.ifsprojects.ibmi.IfsPaths;
import de.tools400.ifsprojects.sources.SourceTypes;

/**
 * Extensions of a project, that can be added to the further source types of an
 * export, see {@link ExportSettings#getFurtherSourceTypes()}: the extensions of
 * the files, that are not exported, and the further source types, that are
 * selected already. An extension is proposed, i.e. it is probably a source
 * type, if
 * <ul>
 * <li>it is a source type of IBM, that TOBi does not know, e.g.
 * <code>TBL</code>,</li>
 * <li>an action of Code for IBM i in <code>.vscode/actions.json</code> applies
 * to it, or</li>
 * <li>a file with the extension is in a source directory: a directory with
 * <code>.ibmi.json</code>, which TOBi and the migration write into every
 * directory of a source file, or with files of the source types of TOBi.</li>
 * </ul>
 * Extensions of documents, scripts, settings and binary files, e.g.
 * <code>md</code>, <code>mk</code> or <code>pdf</code>, are never proposed, but
 * offered. Hidden files and extensions, that are no valid source types, are not
 * offered at all. The contents of the files are not read.
 */
public final class SourceTypeProposals {

    /** Actions of Code for IBM i, whose extensions are proposed. */
    public static final String CODE_FOR_IBM_I_ACTIONS = ".vscode/actions.json"; //$NON-NLS-1$

    private static final String IBMI_JSON = ".ibmi.json"; //$NON-NLS-1$
    private static final String EXTENSIONS = "extensions"; //$NON-NLS-1$
    private static final String GLOBAL = "GLOBAL"; //$NON-NLS-1$
    private static final char SEPARATOR = '/';

    /** Source types of IBM i, that TOBi does not know. */
    private static final Set<String> IBM_SOURCE_TYPES = new HashSet<>(Arrays.asList("BAS", "BASP", "CICSC", "CICSCBL", "CICSCBLLE", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
        "CICSMAP", "CICSSQLCBL", "CL", "CLD", "DFU", "FTP", "ICFF", "MNU", "PAS", "PLI", "QRY", "REXX", "RPG36", "RPG38", "RPGT", "SPADCT", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$ //$NON-NLS-13$ //$NON-NLS-14$ //$NON-NLS-15$ //$NON-NLS-16$
        "SQLCBL", "SQLPLI", "TBL")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

    /** Extensions of files, that are no sources of the IBM i. */
    private static final Set<String> OTHER_EXTENSIONS = new HashSet<>(Arrays.asList("7Z", "BASH", "BMP", "CFG", "CLASS", "CONF", "CSS", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
        "CSV", "DLL", "DOC", "DOCX", "EXE", "GIF", "GZ", "HTM", "HTML", "ICO", "INI", "JAR", "JAVA", "JPEG", "JPG", "JS", "JSON", "JSONC", "KSH", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$ //$NON-NLS-13$ //$NON-NLS-14$ //$NON-NLS-15$ //$NON-NLS-16$ //$NON-NLS-17$ //$NON-NLS-18$ //$NON-NLS-19$
        "LOCK", "LOG", "MARKDOWN", "MD", "MK", "ODS", "ODT", "PDF", "PNG", "PPT", "PPTX", "PROPERTIES", "PS1", "PY", "RAR", "RTF", "SAVF", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$ //$NON-NLS-13$ //$NON-NLS-14$ //$NON-NLS-15$ //$NON-NLS-16$ //$NON-NLS-17$
        "SCSS", "SH", "SO", "SVG", "TAR", "TGZ", "TOML", "TS", "XLS", "XLSX", "YAML", "YML", "ZIP")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$ //$NON-NLS-13$

    private SourceTypeProposals() {
        // Utility class
    }

    /**
     * Returns the extensions, that can be added to the further source types.
     *
     * @param aFiles - all files of the project relative to the project
     *        directory
     * @param anExportedFiles - files, that are exported, see
     *        {@link ExportMapping#isExported(String)}
     * @param aFurtherSourceTypes - further source types, that are selected
     *        already
     * @param aCodeForIbmiActions - content of {@link #CODE_FOR_IBM_I_ACTIONS}
     *        or <code>null</code>
     * @return extensions: the selected and the proposed ones first, each group
     *         by number of files and name
     */
    public static List<Proposal> create(Collection<String> aFiles, Collection<String> anExportedFiles, Collection<String> aFurtherSourceTypes,
        String aCodeForIbmiActions) {

        Set<String> selected = new HashSet<>();
        for (String sourceType : aFurtherSourceTypes) {
            selected.add(sourceType.toUpperCase(Locale.ROOT));
        }

        Set<String> exported = new HashSet<>(anExportedFiles);
        Set<String> sourceDirectories = getSourceDirectories(aFiles);
        Set<String> actionExtensions = getActionExtensions(aCodeForIbmiActions);

        final Map<String, Proposal> proposals = new LinkedHashMap<>();
        for (String path : aFiles) {

            if (ExportMapping.isHidden(path)) {
                continue;
            }

            String sourceType = ExportMapping.getExtension(IfsPaths.getFileName(path)).toUpperCase(Locale.ROOT);
            boolean isSelected = selected.contains(sourceType);
            if (!isSelected && (exported.contains(path) || !ClCommands.isObjectName(sourceType))) {
                continue;
            }

            Proposal proposal = proposals.get(sourceType);
            if (proposal == null) {
                proposal = new Proposal(sourceType, path, isSelected);
                proposals.put(sourceType, proposal);
            }
            proposal.fileCount++;

            if (proposal.reason == null && !OTHER_EXTENSIONS.contains(sourceType)) {
                if (IBM_SOURCE_TYPES.contains(sourceType)) {
                    proposal.reason = Reason.IBM_SOURCE_TYPE;
                } else if (actionExtensions.contains(sourceType)) {
                    proposal.reason = Reason.CODE_FOR_IBM_I;
                } else if (sourceDirectories.contains(getDirectory(path))) {
                    proposal.reason = Reason.SOURCE_DIRECTORY;
                    proposal.directory = getDirectory(path);
                }
            }
        }

        List<Proposal> sorted = new ArrayList<>(proposals.values());
        Collections.sort(sorted, new Comparator<Proposal>() {
            public int compare(Proposal aProposal, Proposal anotherProposal) {
                if (aProposal.isRecommended() != anotherProposal.isRecommended()) {
                    return aProposal.isRecommended() ? -1 : 1;
                }
                if (aProposal.fileCount != anotherProposal.fileCount) {
                    return anotherProposal.fileCount - aProposal.fileCount;
                }
                return aProposal.sourceType.compareTo(anotherProposal.sourceType);
            }
        });

        return sorted;
    }

    /**
     * Returns the source directories: the directories with
     * <code>.ibmi.json</code> or with files of the source types of TOBi.
     */
    private static Set<String> getSourceDirectories(Collection<String> aFiles) {

        Set<String> directories = new HashSet<>();
        for (String path : aFiles) {
            String fileName = IfsPaths.getFileName(path);
            if (IBMI_JSON.equals(fileName) || (!ExportMapping.isHidden(path) && SourceTypes.getSourceType(fileName) != null)) {
                directories.add(getDirectory(path));
            }
        }

        return directories;
    }

    /**
     * Returns the extensions of the actions of Code for IBM i, e.g.
     * <code>[{"name": "Create", "extensions": ["TBL"], ...}]</code>, without
     * <code>GLOBAL</code>, which applies to all extensions. Comments and other
     * relaxed syntax are accepted, invalid content is ignored.
     */
    private static Set<String> getActionExtensions(String aContent) {

        Set<String> extensions = new HashSet<>();
        if (aContent == null) {
            return extensions;
        }

        try {
            // JsonParser reads leniently, i.e. it accepts comments.
            JsonElement actions = new JsonParser().parse(aContent);
            if (!actions.isJsonArray()) {
                return extensions;
            }
            for (JsonElement action : actions.getAsJsonArray()) {
                JsonElement values = action.isJsonObject() ? action.getAsJsonObject().get(EXTENSIONS) : null;
                if (values != null && values.isJsonArray()) {
                    for (JsonElement value : (JsonArray)values) {
                        if (value.isJsonPrimitive()) {
                            String extension = value.getAsString().trim().toUpperCase(Locale.ROOT);
                            if (!GLOBAL.equals(extension)) {
                                extensions.add(extension);
                            }
                        }
                    }
                }
            }
        } catch (RuntimeException e) {
            // Not a valid file of actions, which proposes nothing
        }

        return extensions;
    }

    private static String getDirectory(String aPath) {
        int index = aPath.lastIndexOf(SEPARATOR);
        return index < 0 ? "" : aPath.substring(0, index); //$NON-NLS-1$
    }

    /**
     * Reason, why an extension is probably a source type.
     */
    public enum Reason {

        /** It is a source type of IBM, that TOBi does not know. */
        IBM_SOURCE_TYPE,

        /** An action of Code for IBM i applies to the extension. */
        CODE_FOR_IBM_I,

        /** A file with the extension is in a source directory. */
        SOURCE_DIRECTORY;
    }

    /**
     * Extension of the files of a project.
     */
    public static final class Proposal {

        private final String sourceType;
        private final String example;
        private final boolean isSelected;
        private int fileCount;
        private Reason reason;
        private String directory;

        Proposal(String aSourceType, String anExample, boolean anIsSelected) {
            this.sourceType = aSourceType;
            this.example = anExample;
            this.isSelected = anIsSelected;
        }

        /**
         * Returns the source type: the extension in uppercase, e.g.
         * <code>TBL</code>.
         */
        public String getSourceType() {
            return sourceType;
        }

        /**
         * Returns the extension in lowercase, e.g. <code>tbl</code>.
         */
        public String getExtension() {
            return sourceType.toLowerCase(Locale.ROOT);
        }

        /**
         * Returns the number of files with the extension.
         */
        public int getFileCount() {
            return fileCount;
        }

        /**
         * Returns the path of the first file with the extension.
         */
        public String getExample() {
            return example;
        }

        /**
         * Returns <code>true</code>, if the extension is a further source type
         * of the settings already.
         */
        public boolean isSelected() {
            return isSelected;
        }

        /**
         * Returns the reason, why the extension is probably a source type, or
         * <code>null</code>, if it is only offered.
         */
        public Reason getReason() {
            return reason;
        }

        /**
         * Returns the source directory of {@link Reason#SOURCE_DIRECTORY},
         * e.g. <code>qddssrc</code>, an empty string for the project
         * directory.
         */
        public String getDirectory() {
            return directory;
        }

        /**
         * Returns <code>true</code>, if the extension is selected already or
         * proposed.
         */
        public boolean isRecommended() {
            return isSelected || reason != null;
        }

        @Override
        public String toString() {
            return sourceType + " " + fileCount + " " + reason + (isSelected ? " selected" : ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
    }
}
