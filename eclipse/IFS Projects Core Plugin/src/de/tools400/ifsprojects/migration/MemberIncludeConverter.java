/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.migration;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import de.tools400.ifsprojects.sources.IncludeConverter;
import de.tools400.ifsprojects.sources.SourceTypes;

/**
 * Converts the include directives of a migrated member, that refer to
 * members, so that they refer to the stream files of the migration, e.g.
 * <code>/copy QCPYSRC,CONSTS</code> to
 * <code>/copy 'qcpysrc/consts.rpgleinc'</code>. Directives, that refer to
 * stream files, are not changed. A directive is not converted, if its member
 * is in another library, in a source file, that is not migrated, or not
 * found.
 * <p>
 * The path, that {@link #convert(String, String)} expects, is the key of the
 * member, see {@link SourceMember#getKey()}.
 */
public abstract class MemberIncludeConverter extends IncludeConverter {

    private static final String LIBRARY_LIST = "*LIBL"; //$NON-NLS-1$
    private static final String CURRENT_LIBRARY = "*CURLIB"; //$NON-NLS-1$

    private final String library;
    private final Map<String, String> paths;
    private final Set<String> sourceFiles;
    private final Set<String> includedMembers = new HashSet<String>();

    /**
     * Creates a converter.
     *
     * @param aLibrary - library of the migrated source files
     * @param aPaths - paths of the stream files by the keys of the members of
     *        the migrated source files; the path is <code>null</code>, if the
     *        member is not migrated
     * @param aSourceFiles - migrated source files
     */
    MemberIncludeConverter(String aLibrary, Map<String, String> aPaths, Set<String> aSourceFiles) {
        this.library = aLibrary;
        this.paths = aPaths;
        this.sourceFiles = aSourceFiles;
    }

    /**
     * Returns the converter of a source type.
     *
     * @param aSourceType - source type of the member
     * @param aLibrary - library of the migrated source files
     * @param aPaths - paths of the stream files by the keys of the members,
     *        see {@link #MemberIncludeConverter(String, Map, Set)}
     * @param aSourceFiles - migrated source files
     * @param aStyle - style of the RPG directives
     * @return converter or <code>null</code>, if the includes of the source
     *         type are not converted
     */
    public static MemberIncludeConverter create(String aSourceType, String aLibrary, Map<String, String> aPaths, Set<String> aSourceFiles,
        IncludeStyle aStyle) {

        if (SourceTypes.RPGLE.equalsIgnoreCase(aSourceType) || SourceTypes.SQLRPGLE.equalsIgnoreCase(aSourceType)) {
            return new RpgMemberIncludeConverter(aLibrary, aPaths, aSourceFiles, aStyle);
        }

        if (SourceTypes.CLLE.equalsIgnoreCase(aSourceType)) {
            return new ClMemberIncludeConverter(aLibrary, aPaths, aSourceFiles);
        }

        return null;
    }

    /**
     * Returns the path of the stream file of a member.
     *
     * @param aKey - key of the member
     * @return path or <code>null</code>, if the member is not migrated
     */
    String getPath(String aKey) {
        return paths.get(aKey);
    }

    /**
     * Resolves the member of a directive. If it is not migrated, the problem
     * is added to the result.
     *
     * @param aResult - result
     * @param aLineNumber - line number
     * @param aName - name of the directive, for the result
     * @param aLibrary - library of the directive or <code>null</code>
     * @param aSourceFile - source file of the member
     * @param aMember - member
     * @return path of the stream file or <code>null</code>, if the member is
     *         not migrated
     */
    String resolve(Result aResult, int aLineNumber, String aName, String aLibrary, String aSourceFile, String aMember) {

        String libraryName = aLibrary == null ? null : aLibrary.toUpperCase(Locale.ROOT);
        if (libraryName != null && !libraryName.equals(LIBRARY_LIST) && !libraryName.equals(CURRENT_LIBRARY) && !libraryName.equals(library)) {
            addProblem(aResult, aLineNumber, aName, Problem.OTHER_LIBRARY);
            return null;
        }

        String sourceFile = aSourceFile.toUpperCase(Locale.ROOT);
        if (!sourceFiles.contains(sourceFile)) {
            addProblem(aResult, aLineNumber, aName, Problem.NOT_TRANSFERRED);
            return null;
        }

        String key = SourceMember.getKey(sourceFile, aMember.toUpperCase(Locale.ROOT));
        if (!paths.containsKey(key)) {
            addProblem(aResult, aLineNumber, aName, Problem.NOT_FOUND);
            return null;
        }

        String path = paths.get(key);
        if (path == null) {
            addProblem(aResult, aLineNumber, aName, Problem.NOT_TRANSFERRED);
        } else {
            includedMembers.add(key);
        }

        return path;
    }

    /**
     * Returns the keys of the members, to which the converted directives
     * refer, of all sources converted by this converter.
     */
    public Set<String> getIncludedMembers() {
        return Collections.unmodifiableSet(includedMembers);
    }
}
