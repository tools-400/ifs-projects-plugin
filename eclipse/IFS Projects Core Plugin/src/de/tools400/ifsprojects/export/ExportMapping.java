/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.export;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.tools400.ifsprojects.ibmi.ClCommands;
import de.tools400.ifsprojects.ibmi.IfsPaths;
import de.tools400.ifsprojects.sources.SourceTypes;

/**
 * Maps the stream files of a project to the members, into which they are
 * exported. It reverses the naming of <code>makei cvtsrcpf</code> of TOBi,
 * which converts the member <code>NAME</code> of type <code>TYPE</code> to the
 * stream file <code>name.type</code>:
 * <ul>
 * <li>Member: the file name up to the first period in uppercase, e.g.
 * <code>HELLO</code> for <code>hello.pgm.rpgle</code>.</li>
 * <li>Source type: see {@link SourceTypes}, e.g. <code>RPGLE</code> for
 * <code>hello.pgm.rpgle</code> and <code>consts.rpgleinc</code>. Only files
 * with a source type are exported.</li>
 * <li>Source file: see {@link SourceFileRule}.</li>
 * </ul>
 * Files, whose member name is not valid, and files, that would be exported
 * into the same member, are not exported, see {@link Status}.
 */
public final class ExportMapping {

    private static final char SEPARATOR = '/';
    private static final char EXTENSION_SEPARATOR = '.';

    private final SourceFileRule rule;
    private final String sourceFile;

    /**
     * Creates a mapping.
     *
     * @param aRule - rule, that determines the source file
     * @param aSourceFile - source file of {@link SourceFileRule#SINGLE_FILE},
     *        ignored by the other rules
     */
    public ExportMapping(SourceFileRule aRule, String aSourceFile) {
        this.rule = aRule;
        this.sourceFile = aSourceFile == null ? null : aSourceFile.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * Returns <code>true</code>, if a file of a project is exported: it has a
     * source type, see {@link SourceTypes#getSourceType(String)}.
     *
     * @param aPath - path of the file, e.g. <code>src/hello.pgm.rpgle</code>
     * @return <code>true</code>, if the file is exported
     */
    public static boolean isExported(String aPath) {
        return SourceTypes.getSourceType(IfsPaths.getFileName(aPath)) != null;
    }

    /**
     * Returns the member name of a stream file: the file name up to the first
     * period in uppercase.
     *
     * @param aFileName - name of the stream file
     * @return member name or <code>null</code>, if it is not a valid name
     */
    public static String getMemberName(String aFileName) {

        if (aFileName == null) {
            return null;
        }

        String name = aFileName;
        int index = name.indexOf(EXTENSION_SEPARATOR);
        if (index >= 0) {
            name = name.substring(0, index);
        }

        name = name.toUpperCase(Locale.ROOT);
        if (!ClCommands.isObjectName(name)) {
            return null;
        }

        return name;
    }

    /**
     * Maps the stream files of a project to members. Files, whose source type
     * is not exported, are omitted.
     *
     * @param aPaths - paths of the files relative to the project directory,
     *        separated by slashes, e.g. <code>qrpglesrc/hello.pgm.rpgle</code>
     * @return entries in the order of the paths
     */
    public List<Entry> map(Collection<String> aPaths) {

        List<Entry> entries = new ArrayList<Entry>();
        Map<String, List<Entry>> targets = new HashMap<String, List<Entry>>();

        for (String path : aPaths) {

            String fileName = IfsPaths.getFileName(path);
            String sourceType = SourceTypes.getSourceType(fileName);
            if (sourceType == null) {
                continue;
            }

            String member = getMemberName(fileName);
            Entry entry = new Entry(path, getSourceFile(path, sourceType), member, sourceType);
            entries.add(entry);

            if (member == null) {
                entry.status = Status.INVALID_MEMBER_NAME;
                continue;
            }

            String target = entry.getTarget();
            List<Entry> sameTarget = targets.get(target);
            if (sameTarget == null) {
                sameTarget = new ArrayList<Entry>();
                targets.put(target, sameTarget);
            }
            sameTarget.add(entry);
        }

        for (List<Entry> sameTarget : targets.values()) {
            if (sameTarget.size() > 1) {
                for (Entry entry : sameTarget) {
                    entry.status = Status.DUPLICATE;
                }
            }
        }

        return entries;
    }

    private String getSourceFile(String aPath, String aSourceType) {

        switch (rule) {
        case SINGLE_FILE:
            return sourceFile;

        case DIRECTORY:
            int end = aPath.lastIndexOf(SEPARATOR);
            if (end > 0) {
                String directory = aPath.substring(aPath.lastIndexOf(SEPARATOR, end - 1) + 1, end).toUpperCase(Locale.ROOT);
                if (ClCommands.isObjectName(directory)) {
                    return directory;
                }
            }
            return SourceTypes.getDefaultSourceFile(aSourceType);

        default:
            return SourceTypes.getDefaultSourceFile(aSourceType);
        }
    }

    /**
     * Status of a stream file.
     */
    public enum Status {

        /** The file can be exported. */
        OK,

        /** The file name does not result in a valid member name. */
        INVALID_MEMBER_NAME,

        /** Other files are exported into the same member. */
        DUPLICATE;
    }

    /**
     * Stream file and the member, into which it is exported.
     */
    public static final class Entry {

        private final String path;
        private final String sourceFile;
        private final String member;
        private final String sourceType;
        private Status status;

        Entry(String aPath, String aSourceFile, String aMember, String aSourceType) {
            this.path = aPath;
            this.sourceFile = aSourceFile;
            this.member = aMember;
            this.sourceType = aSourceType;
            this.status = Status.OK;
        }

        /**
         * Returns the path of the stream file relative to the project
         * directory.
         */
        public String getPath() {
            return path;
        }

        /**
         * Returns the name of the source file.
         */
        public String getSourceFile() {
            return sourceFile;
        }

        /**
         * Returns the member name or <code>null</code>, if the file name does
         * not result in a valid member name.
         */
        public String getMember() {
            return member;
        }

        /**
         * Returns the source type of the member.
         */
        public String getSourceType() {
            return sourceType;
        }

        /**
         * Returns the status.
         */
        public Status getStatus() {
            return status;
        }

        /**
         * Returns the member as <code>FILE,MEMBER</code>, the notation of
         * <code>/COPY</code>.
         */
        public String getTarget() {
            return sourceFile + ',' + member;
        }

        @Override
        public String toString() {
            return path + " -> " + getTarget() + " " + sourceType + " " + status; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
    }
}
