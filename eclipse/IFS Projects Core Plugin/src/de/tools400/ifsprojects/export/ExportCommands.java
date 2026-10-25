/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.export;

import de.tools400.ifsprojects.ibmi.ClCommands;
import de.tools400.ifsprojects.utils.StringUtils;

/**
 * CL commands of the export of stream files into members.
 * <p>
 * <code>CPYFRMSTMF</code> creates a missing member, but without source type and
 * text. Therefore a new member is added with <code>ADDPFM</code> before.
 * <code>MBROPT(*REPLACE)</code> keeps source type and text of an existing
 * member; they are changed with <code>CHGPFM</code> afterwards. Verified on the
 * IBM i.
 */
public final class ExportCommands {

    private static final char SEPARATOR = '/';

    private ExportCommands() {
        // Utility class
    }

    /**
     * Returns the command that creates a library.
     *
     * @param aLibrary - library
     * @return command
     */
    public static String getCreateLibraryCommand(String aLibrary) {
        return "CRTLIB LIB(" + aLibrary + ")"; //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Returns the command that creates a source file.
     *
     * @param aLibrary - library
     * @param aSourceFile - source file
     * @param aRecordLength - record length including sequence number and date
     *        (12 characters)
     * @param aCcsid - CCSID
     * @return command
     */
    public static String getCreateSourceFileCommand(String aLibrary, String aSourceFile, int aRecordLength, int aCcsid) {
        return "CRTSRCPF FILE(" + aLibrary + SEPARATOR + aSourceFile + ") RCDLEN(" + aRecordLength + ") CCSID(" + aCcsid + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    }

    /**
     * Returns the command that adds a member to a source file.
     *
     * @param aLibrary - library
     * @param aSourceFile - source file
     * @param aMember - member
     * @param aSourceType - source type
     * @param aText - text or <code>null</code>
     * @return command
     */
    public static String getAddMemberCommand(String aLibrary, String aSourceFile, String aMember, String aSourceType, String aText) {
        return "ADDPFM FILE(" + aLibrary + SEPARATOR + aSourceFile + ") MBR(" + aMember + ") SRCTYPE(" + aSourceType + ") TEXT(" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            + getTextParameter(aText) + ")"; //$NON-NLS-1$
    }

    /**
     * Returns the command that copies a stream file into a member. The text is
     * converted from the CCSID of the stream file to the CCSID of the source
     * file.
     *
     * @param aStreamFile - absolute path of the stream file
     * @param aLibrary - library
     * @param aSourceFile - source file
     * @param aMember - member
     * @return command
     */
    public static String getCopyCommand(String aStreamFile, String aLibrary, String aSourceFile, String aMember) {
        return "CPYFRMSTMF FROMSTMF(" + ClCommands.quote(aStreamFile) + ") TOMBR(" //$NON-NLS-1$ //$NON-NLS-2$
            + ClCommands.quote(ClCommands.getMemberPath(aLibrary, aSourceFile, aMember)) + ") MBROPT(*REPLACE)"; //$NON-NLS-1$
    }

    /**
     * Returns the command that changes source type and text of a member.
     *
     * @param aLibrary - library
     * @param aSourceFile - source file
     * @param aMember - member
     * @param aSourceType - source type
     * @param aText - text or <code>null</code> to keep the text
     * @return command
     */
    public static String getChangeMemberCommand(String aLibrary, String aSourceFile, String aMember, String aSourceType, String aText) {

        StringBuilder command = new StringBuilder();
        command.append("CHGPFM FILE(").append(aLibrary).append(SEPARATOR).append(aSourceFile).append(") MBR(").append(aMember); //$NON-NLS-1$ //$NON-NLS-2$
        command.append(") SRCTYPE(").append(aSourceType).append(')'); //$NON-NLS-1$
        if (aText != null) {
            command.append(" TEXT(").append(getTextParameter(aText)).append(')'); //$NON-NLS-1$
        }

        return command.toString();
    }

    /**
     * Returns the command that removes a member.
     *
     * @param aLibrary - library
     * @param aSourceFile - source file
     * @param aMember - member
     * @return command
     */
    public static String getRemoveMemberCommand(String aLibrary, String aSourceFile, String aMember) {
        return "RMVM FILE(" + aLibrary + SEPARATOR + aSourceFile + ") MBR(" + aMember + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    private static String getTextParameter(String aText) {

        if (StringUtils.isBlank(aText)) {
            return "*BLANK"; //$NON-NLS-1$
        }

        return ClCommands.quote(aText);
    }
}
