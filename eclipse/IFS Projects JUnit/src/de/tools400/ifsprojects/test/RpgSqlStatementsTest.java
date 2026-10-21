/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import de.tools400.ifsprojects.sources.RpgSqlIncludeDirective;
import de.tools400.ifsprojects.sources.RpgSqlStatements;

/**
 * The forms of the statements are those of the IBM documentation <i>Using SQL
 * statements in ILE RPG</i>: free format <code>EXEC SQL</code> ...
 * <code>;</code> and fixed format <code>/EXEC SQL</code> (positions 7 to 15),
 * continuation lines with <code>+</code> in position 7 and
 * <code>/END-EXEC</code>.
 */
public class RpgSqlStatementsTest {

    @Test
    public void testFree_oneLine() {

        List<RpgSqlIncludeDirective> includes = RpgSqlStatements.find("**FREE\n" //
            + "dcl-s x char(1);\n" //
            + "  exec sql include HOSTVARS;\n");

        assertEquals(1, includes.size());
        RpgSqlIncludeDirective include = includes.get(0);
        assertEquals("HOSTVARS", include.getName());
        assertFalse(include.isQuoted());
        assertEquals("include", include.getKeyword());
        assertEquals(3, include.getFirstLineNumber());
        assertEquals(3, include.getLineNumber());
        assertEquals(1, include.getLineCount());
        assertEquals("  ", include.getIndentation());
        assertEquals("  exec sql include HOSTVARS;", include.getStatement());
        assertEquals("  exec sql include 'qcpysrc/hostvars.sqlrpgleinc';", include.replaceName("'qcpysrc/hostvars.sqlrpgleinc'"));
    }

    @Test
    public void testFree_severalLines() {

        List<RpgSqlIncludeDirective> includes = RpgSqlStatements.find("**FREE\n" //
            + "exec sql\n" //
            + "  include\n" //
            + "    HOSTVARS;\n");

        assertEquals(1, includes.size());
        RpgSqlIncludeDirective include = includes.get(0);
        assertEquals("HOSTVARS", include.getName());
        assertEquals(2, include.getFirstLineNumber());
        assertEquals(4, include.getLineNumber());
        assertEquals(3, include.getLineCount());
        assertEquals("exec sql\n  include\n    HOSTVARS;", include.getStatement());
        assertEquals("exec sql\n  include\n    'x';", include.replaceName("'x'"));
    }

    @Test
    public void testFree_forms() {

        assertName("**FREE\nEXEC SQL INCLUDE SQLCA;\n", null);
        assertName("**FREE\nexec sql include sqlda;\n", null);
        assertName("**FREE\nexec sql select count(*) into :x from t;\n", null);
        assertName("**FREE\n// exec sql include HOSTVARS;\n", null);
        assertName("**FREE\nx = 'exec sql include HOSTVARS';\n", null);
        assertName("**FREE\nexec sql include 'qcpysrc/hostvars.sqlrpgleinc';\n", "qcpysrc/hostvars.sqlrpgleinc");
        assertName("**FREE\nexec sql include HOSTVARS; // comment\n", "HOSTVARS");
        assertName("**FREE\nexec sql include HOSTVARS; -- comment\n", "HOSTVARS");
        assertName("**FREE\nexec sql -- comment\n  include HOSTVARS;\n", "HOSTVARS");
        assertName("**FREE\nx = 1; exec sql include HOSTVARS;\n", "HOSTVARS");
        // not terminated: the name is not replaced
        assertName("**FREE\nexec sql include HOSTVARS\n", null);
    }

    @Test
    public void testFree_quoted() {

        List<RpgSqlIncludeDirective> includes = RpgSqlStatements.find("**FREE\nexec sql include 'qcpysrc/hostvars.sqlrpgleinc';\n");

        assertEquals(1, includes.size());
        assertTrue(includes.get(0).isQuoted());
        assertEquals("qcpysrc/hostvars.sqlrpgleinc", includes.get(0).getName());
        assertEquals("exec sql include HOSTVARS;", includes.get(0).replaceName("HOSTVARS"));
    }

    @Test
    public void testFixed_oneLine() {

        List<RpgSqlIncludeDirective> includes = RpgSqlStatements.find("     H DFTACTGRP(*NO)\n" //
            + "     C/EXEC SQL INCLUDE HOSTVARS\n" //
            + "     C/END-EXEC\n");

        assertEquals(1, includes.size());
        RpgSqlIncludeDirective include = includes.get(0);
        assertEquals("HOSTVARS", include.getName());
        assertEquals("INCLUDE", include.getKeyword());
        assertEquals(2, include.getFirstLineNumber());
        assertEquals(2, include.getLineNumber());
        assertEquals(2, include.getLineCount());
        assertEquals("      ", include.getIndentation());
        assertEquals("     C/EXEC SQL INCLUDE HOSTVARS\n     C/END-EXEC", include.getStatement());
        assertEquals("     C/EXEC SQL INCLUDE 'x'\n     C/END-EXEC", include.replaceName("'x'"));
    }

    @Test
    public void testFixed_continuation() {

        List<RpgSqlIncludeDirective> includes = RpgSqlStatements.find("     C/EXEC SQL\n" //
            + "     C+ INCLUDE\n" //
            + "     C+   HOSTVARS\n" //
            + "     C/END-EXEC\n");

        assertEquals(1, includes.size());
        assertEquals("HOSTVARS", includes.get(0).getName());
        assertEquals(1, includes.get(0).getFirstLineNumber());
        assertEquals(3, includes.get(0).getLineNumber());
        assertEquals(4, includes.get(0).getLineCount());
        assertEquals("     C/EXEC SQL\n     C+ INCLUDE\n     C+   'x'\n     C/END-EXEC", includes.get(0).replaceName("'x'"));
    }

    @Test
    public void testFixed_commentColumns() {

        List<RpgSqlIncludeDirective> includes = RpgSqlStatements.find(pad("     C/EXEC SQL INCLUDE HOSTVARS", 80) + "COMMENT\n" //
            + "     C/END-EXEC\n");

        assertEquals(1, includes.size());
        assertEquals(pad("     C/EXEC SQL INCLUDE 'x'", 80) + "COMMENT\n     C/END-EXEC", includes.get(0).replaceName("'x'"));
        // The code must not reach beyond position 80
        assertNull(includes.get(0).replaceName("'" + pad("", 60) + "'"));
    }

    @Test
    public void testFixed_forms() {

        // comment line inside the statement
        assertName("     C/EXEC SQL\n      * comment\n     C+ INCLUDE HOSTVARS\n     C/END-EXEC\n", "HOSTVARS");
        // no continuation line: the statement is not terminated
        assertName("     C/EXEC SQL\n     C  INCLUDE HOSTVARS\n     C/END-EXEC\n", null);
        // free format statements need a /FREE block
        assertName("     H DFTACTGRP(*NO)\n        exec sql include HOSTVARS;\n", null);
        assertName("     H DFTACTGRP(*NO)\n      /FREE\n        exec sql include HOSTVARS;\n      /END-FREE\n", "HOSTVARS");
        assertName("      /FREE\n      /END-FREE\n        exec sql include HOSTVARS;\n", null);
        // comment line
        assertName("      *C/EXEC SQL INCLUDE HOSTVARS\n     C/END-EXEC\n", null);
        // compile-time data
        assertName("**CTDATA names\n     C/EXEC SQL INCLUDE HOSTVARS\n     C/END-EXEC\n", null);
    }

    @Test
    public void testSeveralStatements() {

        List<RpgSqlIncludeDirective> includes = RpgSqlStatements.find("**FREE\n" //
            + "exec sql include ONE;\n" //
            + "exec sql set option commit = *none;\n" //
            + "exec sql include TWO;\n");

        assertEquals(2, includes.size());
        assertEquals("ONE", includes.get(0).getName());
        assertEquals(2, includes.get(0).getLineNumber());
        assertEquals("TWO", includes.get(1).getName());
        assertEquals(4, includes.get(1).getLineNumber());
    }

    @Test
    public void testEmpty() {
        assertTrue(RpgSqlStatements.find("").isEmpty());
        assertTrue(RpgSqlStatements.find("**FREE\n").isEmpty());
    }

    /**
     * Asserts the name of the only statement of a source, or that the source
     * has no statement with a name.
     */
    private static void assertName(String aContent, String anExpectedName) {

        List<RpgSqlIncludeDirective> includes = RpgSqlStatements.find(aContent);
        if (anExpectedName == null) {
            assertTrue(includes.isEmpty() || includes.get(0).getName() == null);
        } else {
            assertEquals(1, includes.size());
            assertEquals(anExpectedName, includes.get(0).getName());
        }
    }

    private static String pad(String aText, int aLength) {

        StringBuilder buffer = new StringBuilder(aText);
        while (buffer.length() < aLength) {
            buffer.append(' ');
        }

        return buffer.toString();
    }
}
