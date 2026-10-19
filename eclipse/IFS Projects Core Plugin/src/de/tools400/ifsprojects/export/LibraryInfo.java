/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.export;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400JDBCDriver;
import com.ibm.as400.access.IFSFile;

import de.tools400.ifsprojects.ibmi.ClCommands;

/**
 * Files and source members of the library, into which a project is exported.
 * The record length and the CCSID of a source file are the length and the
 * CCSID of its column <code>SRCDTA</code>.
 * <p>
 * The files are listed with <code>OBJECT_STATISTICS</code>, because the SQL
 * catalog does not contain program-described files, e.g.
 * <code>EVFEVENT</code>. Verified on the IBM i.
 */
public final class LibraryInfo {

    private static final String SOURCE_FILE_TYPE = "S"; //$NON-NLS-1$

    private static final String FILES_QUERY = "select o.objname, t.file_type, c.length, c.ccsid" //$NON-NLS-1$
        + " from table(qsys2.object_statistics(?, '*FILE', '*ALLSIMPLE')) o" //$NON-NLS-1$
        + " left join qsys2.systables t on t.system_table_schema = ? and t.system_table_name = o.objname" //$NON-NLS-1$
        + " left join qsys2.syscolumns c on c.system_table_schema = ? and c.system_table_name = o.objname and c.column_name = 'SRCDTA'"; //$NON-NLS-1$
    private static final String MEMBERS_QUERY = "select system_table_name, system_table_member from qsys2.syspartitionstat" //$NON-NLS-1$
        + " where system_table_schema = ?"; //$NON-NLS-1$

    private final boolean exists;
    private final Map<String, File> files;
    private final Set<String> members;

    /**
     * Creates the information of a library without files.
     *
     * @param anExists - <code>true</code>, if the library exists
     */
    public LibraryInfo(boolean anExists) {
        this.exists = anExists;
        this.files = new HashMap<String, File>();
        this.members = new HashSet<String>();
    }

    /**
     * Reads the files and members of a library.
     *
     * @param aSystem - system object
     * @param aLibrary - library
     * @return information
     * @throws Exception if the catalog could not be read
     */
    public static LibraryInfo read(AS400 aSystem, String aLibrary) throws Exception {

        LibraryInfo info = new LibraryInfo(new IFSFile(aSystem, ClCommands.getLibraryPath(aLibrary)).exists());
        if (!info.exists) {
            return info;
        }

        Connection connection = new AS400JDBCDriver().connect(aSystem);
        try {
            PreparedStatement statement = connection.prepareStatement(FILES_QUERY);
            try {
                statement.setString(1, aLibrary);
                statement.setString(2, aLibrary);
                statement.setString(3, aLibrary);
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    boolean isSourceFile = SOURCE_FILE_TYPE.equals(trim(resultSet.getString(2)));
                    info.addFile(trim(resultSet.getString(1)), isSourceFile, resultSet.getInt(3), resultSet.getInt(4));
                }
            } finally {
                statement.close();
            }

            statement = connection.prepareStatement(MEMBERS_QUERY);
            try {
                statement.setString(1, aLibrary);
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    info.addMember(trim(resultSet.getString(1)), trim(resultSet.getString(2)));
                }
            } finally {
                statement.close();
            }
        } finally {
            connection.close();
        }

        return info;
    }

    /**
     * Returns <code>true</code>, if the library exists.
     */
    public boolean exists() {
        return exists;
    }

    /**
     * Adds a file of the library.
     *
     * @param aName - name of the file
     * @param anIsSourceFile - <code>true</code> for a source file
     * @param aDataLength - length of <code>SRCDTA</code> of a source file
     * @param aCcsid - CCSID of <code>SRCDTA</code> of a source file
     */
    public void addFile(String aName, boolean anIsSourceFile, int aDataLength, int aCcsid) {
        files.put(aName, new File(anIsSourceFile, aDataLength, aCcsid));
    }

    /**
     * Adds a member of a file of the library.
     */
    public void addMember(String aFile, String aMember) {
        members.add(aFile + ',' + aMember);
    }

    /**
     * Returns a file of the library.
     *
     * @param aName - name of the file
     * @return file or <code>null</code>, if it does not exist
     */
    public File getFile(String aName) {
        return files.get(aName);
    }

    /**
     * Returns <code>true</code>, if a member exists.
     */
    public boolean hasMember(String aFile, String aMember) {
        return members.contains(aFile + ',' + aMember);
    }

    private static String trim(String aValue) {
        return aValue == null ? null : aValue.trim();
    }

    /**
     * Existing file of the library.
     */
    public static final class File {

        private final boolean isSourceFile;
        private final int dataLength;
        private final int ccsid;

        File(boolean anIsSourceFile, int aDataLength, int aCcsid) {
            this.isSourceFile = anIsSourceFile;
            this.dataLength = aDataLength;
            this.ccsid = aCcsid;
        }

        /**
         * Returns <code>true</code>, if the file is a source file.
         */
        public boolean isSourceFile() {
            return isSourceFile;
        }

        /**
         * Returns the number of characters of a line, i.e. the record length
         * without sequence number and date.
         */
        public int getDataLength() {
            return dataLength;
        }

        /**
         * Returns the CCSID of the source lines.
         */
        public int getCcsid() {
            return ccsid;
        }
    }
}
