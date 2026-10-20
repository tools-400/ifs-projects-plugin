/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.sources;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Source types of stream files and members, as TOBi uses them:
 * <ul>
 * <li>The extension of a stream file is the source type of the member in
 * lowercase, e.g. <code>hello.pgm.rpgle</code> is of type <code>RPGLE</code>.
 * Only the extensions, that TOBi knows, are source types.</li>
 * <li>A copy book has the extension of its source type followed by
 * <code>inc</code>, e.g. <code>consts.rpgleinc</code> is of type
 * <code>RPGLE</code>. The extension <code>clinc</code> of Code for IBM i is
 * of type <code>CLLE</code>.</li>
 * </ul>
 */
public final class SourceTypes {

    /** Source type of ILE RPG sources. */
    public static final String RPGLE = "RPGLE"; //$NON-NLS-1$

    /** Source type of ILE RPG sources with embedded SQL. */
    public static final String SQLRPGLE = "SQLRPGLE"; //$NON-NLS-1$

    /** Source type of ILE CL sources. */
    public static final String CLLE = "CLLE"; //$NON-NLS-1$

    /** Source file of source types without default source file. */
    public static final String OTHER_SOURCE_FILE = "QSRC"; //$NON-NLS-1$

    private static final String COPY_BOOK_SUFFIX = "inc"; //$NON-NLS-1$
    private static final char EXTENSION_SEPARATOR = '.';

    /** Source types by extension (lowercase). */
    private static final Map<String, String> SOURCE_TYPES = new HashMap<String, String>();

    /** Default source files by source type. */
    private static final Map<String, String> SOURCE_FILES = new HashMap<String, String>();

    static {
        // Extensions of TOBi (FILE_TARGET_MAPPING of makei/const.py)
        addSourceTypes("QRPGLESRC", RPGLE, SQLRPGLE); //$NON-NLS-1$
        addSourceTypes("QRPGSRC", "RPG", "SQLRPG"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        addSourceTypes("QCLSRC", CLLE, "CLP"); //$NON-NLS-1$ //$NON-NLS-2$
        addSourceTypes("QCBLLESRC", "CBLLE", "SQLCBLLE"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        addSourceTypes("QCBLSRC", "CBL"); //$NON-NLS-1$ //$NON-NLS-2$
        addSourceTypes("QCSRC", "C", "SQLC"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        addSourceTypes("QCPPSRC", "CPP", "SQLCPP"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        addSourceTypes("QDDSSRC", "DSPF", "PRTF", "PF", "LF"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
        addSourceTypes("QMNUSRC", "MENU", "MENUSRC", "MNUDDS", "MNUCMD"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
        addSourceTypes("QCMDSRC", "CMD", "CMDSRC"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        addSourceTypes("QPNLSRC", "PNLGRP", "PNLGRPSRC"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        addSourceTypes("QSRVSRC", "BND", "ILESRVPGM"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        addSourceTypes("QSQLSRC", "SQL", "TABLE", "PFSQL", "VIEW", "INDEX", "SQLPRC", "SQLUDF", "SQLUDT", "SQLTRG", "SQLSEQ", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$
            "SQLALIAS", "SQLVAR"); //$NON-NLS-1$ //$NON-NLS-2$
        addSourceTypes("QQMQRYSRC", "QMQRY"); //$NON-NLS-1$ //$NON-NLS-2$
        addSourceTypes("QTXTSRC", "TXT"); //$NON-NLS-1$ //$NON-NLS-2$
        addSourceTypes(OTHER_SOURCE_FILE, "MODULE", "ILEPGM", "FILE", "BNDDIR", "DTAARA", "DTAQ", "SYSTRG", "MSGF", "WSCSTSRC"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$

        // Copy books of CL in Code for IBM i
        SOURCE_TYPES.put("clinc", CLLE); //$NON-NLS-1$
    }

    private SourceTypes() {
        // Utility class
    }

    /**
     * Returns the source type of a stream file.
     *
     * @param aFileName - name of the stream file
     * @return source type, e.g. <code>RPGLE</code> for
     *         <code>hello.pgm.rpgle</code> and <code>consts.rpgleinc</code>, or
     *         <code>null</code>, if the extension is not a source type
     */
    public static String getSourceType(String aFileName) {

        if (aFileName == null) {
            return null;
        }

        int index = aFileName.lastIndexOf(EXTENSION_SEPARATOR);
        if (index <= 0) {
            return null;
        }

        String extension = aFileName.substring(index + 1).toLowerCase(Locale.ROOT);
        String sourceType = SOURCE_TYPES.get(extension);
        if (sourceType == null && extension.endsWith(COPY_BOOK_SUFFIX)) {
            sourceType = SOURCE_TYPES.get(extension.substring(0, extension.length() - COPY_BOOK_SUFFIX.length()));
        }

        return sourceType;
    }

    /**
     * Returns <code>true</code>, if TOBi knows a source type. A copy book of
     * a known source type has the extension of the source type followed by
     * <code>inc</code>.
     *
     * @param aSourceType - source type, e.g. <code>RPGLE</code>
     * @return <code>true</code>, if the source type is known
     */
    public static boolean isKnown(String aSourceType) {
        return aSourceType != null && aSourceType.equalsIgnoreCase(SOURCE_TYPES.get(aSourceType.toLowerCase(Locale.ROOT)));
    }

    /**
     * Returns the default source file of a source type, which is the default
     * of the IBM commands that compile the source, e.g. <code>QRPGLESRC</code>
     * for <code>RPGLE</code>.
     *
     * @param aSourceType - source type, see {@link #getSourceType(String)}
     * @return source file, {@link #OTHER_SOURCE_FILE} for unknown types
     */
    public static String getDefaultSourceFile(String aSourceType) {

        String file = aSourceType == null ? null : SOURCE_FILES.get(aSourceType.toUpperCase(Locale.ROOT));
        if (file == null) {
            return OTHER_SOURCE_FILE;
        }

        return file;
    }

    private static void addSourceTypes(String aSourceFile, String... aSourceTypes) {
        for (String sourceType : aSourceTypes) {
            SOURCE_TYPES.put(sourceType.toLowerCase(Locale.ROOT), sourceType);
            SOURCE_FILES.put(sourceType, aSourceFile);
        }
    }
}
