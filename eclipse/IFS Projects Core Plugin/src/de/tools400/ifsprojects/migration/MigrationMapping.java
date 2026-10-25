/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.migration;

import java.util.Locale;

import de.tools400.ifsprojects.sources.SourceTypes;

/**
 * Maps the members of source files to stream files of a project, like
 * <code>makei cvtsrcpf --tolower</code> of TOBi: the member <code>NAME</code>
 * of type <code>TYPE</code> in the source file <code>QXXXSRC</code> becomes
 * <code>qxxxsrc/name.type</code>. A copy book, i.e. a member included by other
 * members, gets the extension <code>typeinc</code>, which TOBi recommends, e.g.
 * <code>qcpysrc/consts.rpgleinc</code>, if TOBi knows its source type.
 */
public final class MigrationMapping {

    private static final char SEPARATOR = '/';
    private static final char EXTENSION_SEPARATOR = '.';
    private static final String COPY_BOOK_SUFFIX = "inc"; //$NON-NLS-1$

    private MigrationMapping() {
        // Utility class
    }

    /**
     * Returns the directory of the members of a source file.
     *
     * @param aSourceFile - source file, e.g. <code>QRPGLESRC</code>
     * @return directory relative to the project directory, e.g.
     *         <code>qrpglesrc</code>
     */
    public static String getDirectory(String aSourceFile) {
        return aSourceFile.toLowerCase(Locale.ROOT);
    }

    /**
     * Returns the path of the stream file of a member.
     *
     * @param aMember - member
     * @param anIsCopyBook - <code>true</code>, if the member is included by
     *        other members
     * @return path relative to the project directory, e.g.
     *         <code>qrpglesrc/hello.rpgle</code>, or <code>null</code>, if the
     *         member has no source type
     */
    public static String getPath(SourceMember aMember, boolean anIsCopyBook) {

        String sourceType = aMember.getSourceType();
        if (sourceType == null) {
            return null;
        }

        String extension = sourceType.toLowerCase(Locale.ROOT);
        if (anIsCopyBook && SourceTypes.isKnown(sourceType)) {
            extension += COPY_BOOK_SUFFIX;
        }

        return getDirectory(aMember.getSourceFile()) + SEPARATOR + aMember.getName().toLowerCase(Locale.ROOT) + EXTENSION_SEPARATOR + extension;
    }
}
