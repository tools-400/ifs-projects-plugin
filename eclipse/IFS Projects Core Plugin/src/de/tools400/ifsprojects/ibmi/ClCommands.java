/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.ibmi;

/**
 * Syntax of CL commands: character literals, object names and the paths of
 * libraries, files and members in the IFS.
 */
public final class ClCommands {

    private static final String OBJECT_NAME_PATTERN = "[A-Z$#@][A-Z0-9$#@_]{0,9}"; //$NON-NLS-1$
    private static final char QUOTE = '\'';

    private static final String QSYS_ROOT = "/QSYS.LIB/"; //$NON-NLS-1$
    private static final String LIBRARY_SUFFIX = ".LIB"; //$NON-NLS-1$
    private static final String FILE_SUFFIX = ".FILE"; //$NON-NLS-1$
    private static final String MEMBER_SUFFIX = ".MBR"; //$NON-NLS-1$
    private static final char SEPARATOR = '/';

    /** CCSID of the data of a member, that is the CCSID of its file. */
    public static final int CCSID_OF_FILE = 0;

    private ClCommands() {
        // Utility class
    }

    /**
     * Returns <code>true</code>, if a name is a valid name of an object or
     * member in uppercase: up to 10 characters, <code>A-Z</code>,
     * <code>0-9</code>, <code>$#@_</code>, not starting with a digit or an
     * underscore.
     *
     * @param aName - name or <code>null</code>
     * @return <code>true</code> for a valid name
     */
    public static boolean isObjectName(String aName) {
        return aName != null && aName.matches(OBJECT_NAME_PATTERN);
    }

    /**
     * Returns a CL character literal.
     *
     * @param aValue - value
     * @return value enclosed in apostrophes, apostrophes are doubled
     */
    public static String quote(String aValue) {

        StringBuilder buffer = new StringBuilder();
        buffer.append(QUOTE);
        for (int i = 0; i < aValue.length(); i++) {
            char c = aValue.charAt(i);
            if (c == QUOTE) {
                buffer.append(QUOTE);
            }
            buffer.append(c);
        }
        buffer.append(QUOTE);

        return buffer.toString();
    }

    /**
     * Returns the path of a library in the IFS.
     *
     * @param aLibrary - library
     * @return path, e.g. <code>/QSYS.LIB/JSMITH#CL.LIB</code>
     */
    public static String getLibraryPath(String aLibrary) {
        return QSYS_ROOT + aLibrary + LIBRARY_SUFFIX;
    }

    /**
     * Returns the path of a file in the IFS.
     *
     * @param aLibrary - library
     * @param aFile - file
     * @return path, e.g. <code>/QSYS.LIB/JSMITH#CL.LIB/QRPGLESRC.FILE</code>
     */
    public static String getFilePath(String aLibrary, String aFile) {
        return getLibraryPath(aLibrary) + SEPARATOR + aFile + FILE_SUFFIX;
    }

    /**
     * Returns the path of a member in the IFS.
     *
     * @param aLibrary - library
     * @param aFile - file
     * @param aMember - member
     * @return path, e.g.
     *         <code>/QSYS.LIB/JSMITH#CL.LIB/QRPGLESRC.FILE/HELLO.MBR</code>
     */
    public static String getMemberPath(String aLibrary, String aFile, String aMember) {
        return getFilePath(aLibrary, aFile) + SEPARATOR + aMember + MEMBER_SUFFIX;
    }

    /**
     * Returns the command that copies a member to a stream file with CCSID 1208
     * (UTF-8). An existing stream file is replaced.
     *
     * @param aMemberPath - path of the member, see {@link #getMemberPath}
     * @param aStreamFile - absolute path of the stream file
     * @return command
     */
    public static String getCopyToStreamFileCommand(String aMemberPath, String aStreamFile) {
        return getCopyToStreamFileCommand(aMemberPath, aStreamFile, CCSID_OF_FILE);
    }

    /**
     * Returns the command that copies a member to a stream file with CCSID 1208
     * (UTF-8), with the specified CCSID of the data of the member. It is needed
     * for files with CCSID 65535, which <code>CPYTOSTMF</code> does not convert
     * otherwise (CPDA08C, verified on the IBM i).
     *
     * @param aMemberPath - path of the member, see {@link #getMemberPath}
     * @param aStreamFile - absolute path of the stream file
     * @param aDataCcsid - CCSID of the data or {@link #CCSID_OF_FILE}
     * @return command
     */
    public static String getCopyToStreamFileCommand(String aMemberPath, String aStreamFile, int aDataCcsid) {

        String command = "CPYTOSTMF FROMMBR(" + quote(aMemberPath) + ") TOSTMF(" + quote(aStreamFile) + ") STMFOPT(*REPLACE) STMFCCSID(1208)"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        if (aDataCcsid == CCSID_OF_FILE) {
            return command;
        }

        return command + " DBFCCSID(" + aDataCcsid + ")"; //$NON-NLS-1$ //$NON-NLS-2$
    }
}
