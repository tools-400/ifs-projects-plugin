/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400JDBCDriver;

import de.tools400.ifsprojects.utils.StringUtils;

/**
 * Reads the source files of a library and their members from the SQL catalog.
 * The CCSID and the data length of a source file are those of its column
 * <code>SRCDTA</code>.
 */
public final class SourceFileCatalog {

    private static final String SOURCE_FILES_QUERY = "select t.system_table_name, t.table_text, c.ccsid, c.length," //$NON-NLS-1$
        + " (select count(*) from qsys2.syspartitionstat p" //$NON-NLS-1$
        + " where p.system_table_schema = t.system_table_schema and p.system_table_name = t.system_table_name)" //$NON-NLS-1$
        + " from qsys2.systables t" //$NON-NLS-1$
        + " join qsys2.syscolumns c on c.system_table_schema = t.system_table_schema and c.system_table_name = t.system_table_name" //$NON-NLS-1$
        + " and c.column_name = 'SRCDTA'" //$NON-NLS-1$
        + " where t.system_table_schema = ? and t.file_type = 'S'"; //$NON-NLS-1$
    private static final String FILE_CONDITION = " and t.system_table_name = ?"; //$NON-NLS-1$
    private static final String SOURCE_FILES_ORDER = " order by t.system_table_name"; //$NON-NLS-1$

    private static final String MEMBERS_QUERY = "select system_table_member, source_type, partition_text from qsys2.syspartitionstat" //$NON-NLS-1$
        + " where system_table_schema = ? and system_table_name = ? order by system_table_member"; //$NON-NLS-1$

    private SourceFileCatalog() {
        // Utility class
    }

    /**
     * Returns the source files of a library.
     *
     * @param aSystem - system object
     * @param aLibrary - library
     * @param aFile - source file or <code>null</code> for all source files
     * @return source files in the order of their names
     * @throws Exception if the catalog could not be read
     */
    public static List<SourceFile> getSourceFiles(AS400 aSystem, String aLibrary, String aFile) throws Exception {

        String query = SOURCE_FILES_QUERY + (aFile == null ? "" : FILE_CONDITION) + SOURCE_FILES_ORDER; //$NON-NLS-1$

        List<SourceFile> sourceFiles = new ArrayList<SourceFile>();
        try (Connection connection = new AS400JDBCDriver().connect(aSystem); PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, aLibrary);
            if (aFile != null) {
                statement.setString(2, aFile);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    sourceFiles.add(new SourceFile(aLibrary, resultSet.getString(1).trim(), StringUtils.trimToNull(resultSet.getString(2)), resultSet
                        .getInt(3), resultSet.getInt(4), resultSet.getInt(5)));
                }
            }
        }

        return sourceFiles;
    }

    /**
     * Returns the members of a source file.
     *
     * @param aSystem - system object
     * @param aSourceFile - source file
     * @return members in the order of their names
     * @throws Exception if the catalog could not be read
     */
    public static List<SourceMember> getMembers(AS400 aSystem, SourceFile aSourceFile) throws Exception {

        List<SourceMember> members = new ArrayList<SourceMember>();
        try (Connection connection = new AS400JDBCDriver().connect(aSystem); PreparedStatement statement = connection.prepareStatement(MEMBERS_QUERY)) {
            statement.setString(1, aSourceFile.getLibrary());
            statement.setString(2, aSourceFile.getName());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    members.add(new SourceMember(aSourceFile.getName(), resultSet.getString(1).trim(), StringUtils.trimToNull(resultSet.getString(2)),
                        StringUtils.trimToNull(resultSet.getString(3))));
                }
            }
        }

        return members;
    }
}
