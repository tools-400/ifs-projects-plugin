/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import de.tools400.ifsprojects.migration.IncludeStyle;
import de.tools400.ifsprojects.migration.MemberIncludeConverter;
import de.tools400.ifsprojects.sources.IncludeConverter.Include;
import de.tools400.ifsprojects.sources.IncludeConverter.Problem;
import de.tools400.ifsprojects.sources.IncludeConverter.Result;

/**
 * The converted forms have been verified on the IBM i: RPG
 * <code>/copy</code> relative to the current directory (also nested and in
 * fixed format), CL <code>INCLUDE SRCSTMF()</code> relative to the including
 * file and <code>INCLUDE SRCMBR()</code> without <code>SRCFILE</code>.
 */
public class MemberIncludeConverterTest {

    private static final String LIBRARY = "JSMITH#CL";

    /**
     * Source file of the <code>EXEC SQL INCLUDE</code> statements, which
     * {@link de.tools400.ifsprojects.migration.MigrationPlan} determines.
     */
    private static final String SQL_INCLUDE_FILE = "QCPYSRC";

    @Test
    public void testRpg_free() {

        Result result = convert("RPGLE", "QRPGLESRC(HELLO)", IncludeStyle.REPLACE, "**FREE\n" //
            + "/copy QCPYSRC,CONSTS\n" //
            + "  /include qcpysrc,consts // comment\n" //
            + "/COPY LOCAL\n" //
            + "/copy JSMITH#CL/QCPYSRC,CONSTS\n" //
            + "/copy *LIBL/QCPYSRC,CONSTS\n" //
            + "/copy OTHERLIB/QCPYSRC,CONSTS\n" //
            + "/copy QOTHER,X\n" //
            + "/copy QCPYSRC,MISSING\n" //
            + "/copy QRPGLESRC,NOTYPE\n" //
            + "/copy 'qcpysrc/consts.rpgleinc'\n" //
            + "/copy inc/consts.rpgleinc\n");

        assertEquals("**FREE\n" //
            + "/copy 'qcpysrc/consts.rpgleinc'\n" //
            + "  /include 'qcpysrc/consts.rpgleinc' // comment\n" //
            + "/COPY 'qrpglesrc/local.rpgleinc'\n" //
            + "/copy 'qcpysrc/consts.rpgleinc'\n" //
            + "/copy 'qcpysrc/consts.rpgleinc'\n" //
            + "/copy OTHERLIB/QCPYSRC,CONSTS\n" //
            + "/copy QOTHER,X\n" //
            + "/copy QCPYSRC,MISSING\n" //
            + "/copy QRPGLESRC,NOTYPE\n" //
            + "/copy 'qcpysrc/consts.rpgleinc'\n" //
            + "/copy inc/consts.rpgleinc\n", result.getContent());

        assertEquals(9, result.getIncludes().size());
        assertInclude(result.getIncludes().get(0), 2, "QCPYSRC,CONSTS", "qcpysrc/consts.rpgleinc", null);
        assertInclude(result.getIncludes().get(1), 3, "qcpysrc,consts", "qcpysrc/consts.rpgleinc", null);
        assertInclude(result.getIncludes().get(2), 4, "LOCAL", "qrpglesrc/local.rpgleinc", null);
        assertInclude(result.getIncludes().get(3), 5, "JSMITH#CL/QCPYSRC,CONSTS", "qcpysrc/consts.rpgleinc", null);
        assertInclude(result.getIncludes().get(4), 6, "*LIBL/QCPYSRC,CONSTS", "qcpysrc/consts.rpgleinc", null);
        assertInclude(result.getIncludes().get(5), 7, "OTHERLIB/QCPYSRC,CONSTS", null, Problem.OTHER_LIBRARY);
        assertInclude(result.getIncludes().get(6), 8, "QOTHER,X", null, Problem.NOT_TRANSFERRED);
        assertInclude(result.getIncludes().get(7), 9, "QCPYSRC,MISSING", null, Problem.NOT_FOUND);
        assertInclude(result.getIncludes().get(8), 10, "QRPGLESRC,NOTYPE", null, Problem.NOT_TRANSFERRED);
    }

    @Test
    public void testSqlInclude_free() {

        Result result = convert("SQLRPGLE", "QRPGLESRC(HELLO)", IncludeStyle.REPLACE, "**FREE\n" //
            + "exec sql include SQLCA;\n" //
            + "EXEC SQL INCLUDE HOSTVARS;\n" //
            + "exec sql\n" //
            + "  include hostvars; // comment\n" //
            + "exec sql include 'qcpysrc/hostvars.sqlrpgleinc';\n" //
            + "exec sql include MISSING;\n" //
            + "exec sql include NOTYPE;\n" //
            + "exec sql select count(*) into :count from qsqptabl;\n" //
            + "exec sql include UNTERMINATED\n");

        assertEquals("**FREE\n" //
            + "exec sql include SQLCA;\n" //
            + "EXEC SQL INCLUDE 'qcpysrc/hostvars.sqlrpgleinc';\n" //
            + "exec sql\n" //
            + "  include 'qcpysrc/hostvars.sqlrpgleinc'; // comment\n" //
            + "exec sql include 'qcpysrc/hostvars.sqlrpgleinc';\n" //
            + "exec sql include MISSING;\n" //
            + "exec sql include NOTYPE;\n" //
            + "exec sql select count(*) into :count from qsqptabl;\n" //
            + "exec sql include UNTERMINATED\n", result.getContent());

        assertEquals(5, result.getIncludes().size());
        assertInclude(result.getIncludes().get(0), 3, "HOSTVARS", "qcpysrc/hostvars.sqlrpgleinc", null);
        assertInclude(result.getIncludes().get(1), 5, "hostvars", "qcpysrc/hostvars.sqlrpgleinc", null);
        assertInclude(result.getIncludes().get(2), 7, "MISSING", null, Problem.NOT_FOUND);
        assertInclude(result.getIncludes().get(3), 8, "NOTYPE", null, Problem.NOT_TRANSFERRED);
        assertInclude(result.getIncludes().get(4), 10, "include UNTERMINATED", null, Problem.NOT_SUPPORTED);
    }

    @Test
    public void testSqlInclude_fixed() {

        String source = "     H DFTACTGRP(*NO)\n" //
            + "     C/EXEC SQL INCLUDE SQLCA\n" //
            + "     C/END-EXEC\n" //
            + pad("     C/EXEC SQL INCLUDE HOSTVARS", 80) + "COMMENT\n" //
            + "     C/END-EXEC\n" //
            + "     C/EXEC SQL\n" //
            + "     C+ INCLUDE\n" //
            + "     C+   HOSTVARS\n" //
            + "     C/END-EXEC\n" //
            + "      /FREE\n" //
            + "        exec sql include HOSTVARS;\n" //
            + "      /END-FREE\n";

        Result result = convert("SQLRPGLE", "QRPGLESRC(HELLO)", IncludeStyle.REPLACE, source);

        assertEquals("     H DFTACTGRP(*NO)\n" //
            + "     C/EXEC SQL INCLUDE SQLCA\n" //
            + "     C/END-EXEC\n" //
            + pad("     C/EXEC SQL INCLUDE 'qcpysrc/hostvars.sqlrpgleinc'", 80) + "COMMENT\n" //
            + "     C/END-EXEC\n" //
            + "     C/EXEC SQL\n" //
            + "     C+ INCLUDE\n" //
            + "     C+   'qcpysrc/hostvars.sqlrpgleinc'\n" //
            + "     C/END-EXEC\n" //
            + "      /FREE\n" //
            + "        exec sql include 'qcpysrc/hostvars.sqlrpgleinc';\n" //
            + "      /END-FREE\n", result.getContent());

        assertEquals(3, result.getIncludes().size());
        assertInclude(result.getIncludes().get(0), 4, "HOSTVARS", "qcpysrc/hostvars.sqlrpgleinc", null);
        assertInclude(result.getIncludes().get(1), 8, "HOSTVARS", "qcpysrc/hostvars.sqlrpgleinc", null);
        assertInclude(result.getIncludes().get(2), 11, "HOSTVARS", "qcpysrc/hostvars.sqlrpgleinc", null);
    }

    @Test
    public void testSqlInclude_conditional() {

        Result result = convert("SQLRPGLE", "QRPGLESRC(HELLO)", IncludeStyle.CONDITIONAL, "**FREE\n" //
            + "exec sql\n" //
            + "  include HOSTVARS;\n");

        assertEquals("**FREE\n" //
            + "/if defined(MBR_BUILD)\n" //
            + "exec sql\n" //
            + "  include HOSTVARS;\n" //
            + "/else\n" //
            + "exec sql\n" //
            + "  include 'qcpysrc/hostvars.sqlrpgleinc';\n" //
            + "/endif\n", result.getContent());

        result = convert("SQLRPGLE", "QRPGLESRC(HELLO)", IncludeStyle.CONDITIONAL, "     C/EXEC SQL INCLUDE HOSTVARS\n" //
            + "     C/END-EXEC\n");

        assertEquals("      /IF DEFINED(MBR_BUILD)\n" //
            + "     C/EXEC SQL INCLUDE HOSTVARS\n" //
            + "     C/END-EXEC\n" //
            + "      /ELSE\n" //
            + "     C/EXEC SQL INCLUDE 'qcpysrc/hostvars.sqlrpgleinc'\n" //
            + "     C/END-EXEC\n" //
            + "      /ENDIF\n", result.getContent());
    }

    @Test
    public void testSqlInclude_lineSeparators() {

        // The statement keeps the line separator of its last line
        Result result = convert("SQLRPGLE", "QRPGLESRC(HELLO)", IncludeStyle.REPLACE, "**FREE\r\nexec sql\r\n  include HOSTVARS;\r\n");
        assertEquals("**FREE\r\nexec sql\n  include 'qcpysrc/hostvars.sqlrpgleinc';\r\n", result.getContent());

        result = convert("SQLRPGLE", "QRPGLESRC(HELLO)", IncludeStyle.REPLACE, "**FREE\nexec sql\n  include HOSTVARS;");
        assertEquals("**FREE\nexec sql\n  include 'qcpysrc/hostvars.sqlrpgleinc';", result.getContent());
    }

    @Test
    public void testSqlInclude_problems() {

        Result result = convert("SQLRPGLE", "QRPGLESRC(HELLO)", IncludeStyle.REPLACE, null, "**FREE\nexec sql include HOSTVARS;\n");
        assertEquals(1, result.getIncludes().size());
        assertInclude(result.getIncludes().get(0), 2, "HOSTVARS", null, Problem.AMBIGUOUS);

        result = convert("SQLRPGLE", "QRPGLESRC(HELLO)", IncludeStyle.REPLACE, "OTHERLIB/QCPYSRC", "**FREE\nexec sql include HOSTVARS;\n");
        assertEquals(1, result.getIncludes().size());
        assertInclude(result.getIncludes().get(0), 2, "HOSTVARS", null, Problem.OTHER_LIBRARY);

        result = convert("SQLRPGLE", "QRPGLESRC(HELLO)", IncludeStyle.REPLACE, "QOTHER", "**FREE\nexec sql include HOSTVARS;\n");
        assertEquals(1, result.getIncludes().size());
        assertInclude(result.getIncludes().get(0), 2, "HOSTVARS", null, Problem.NOT_TRANSFERRED);

        // The path does not fit into the columns 7 to 80 of the fixed format
        result = convert("SQLRPGLE", "QRPGLESRC(HELLO)", IncludeStyle.REPLACE, "     C/EXEC SQL\n" //
            + pad("     C+", 60) + "INCLUDE HOSTVARS\n" //
            + "     C/END-EXEC\n");
        assertEquals(1, result.getIncludes().size());
        assertInclude(result.getIncludes().get(0), 2, "HOSTVARS", null, Problem.TOO_LONG);
    }

    @Test
    public void testRpg_fixed() {

        String source = "     H DFTACTGRP(*NO)\n" //
            + pad("      /COPY QCPYSRC,CONSTS", 80) + "COMMENT\n" //
            + "      * /COPY QCPYSRC,CONSTS\n" //
            + pad("", 55) + "/COPY QCPYSRC,CONSTS\n" //
            + "**CTDATA X\n" //
            + "      /COPY QCPYSRC,CONSTS\n";

        Result result = convert("RPGLE", "QRPGLESRC(HELLO)", IncludeStyle.REPLACE, source);

        assertEquals("     H DFTACTGRP(*NO)\n" //
            + pad("      /COPY 'qcpysrc/consts.rpgleinc'", 80) + "COMMENT\n" //
            + "      * /COPY QCPYSRC,CONSTS\n" //
            + pad("", 55) + "/COPY QCPYSRC,CONSTS\n" //
            + "**CTDATA X\n" //
            + "      /COPY QCPYSRC,CONSTS\n", result.getContent());

        assertEquals(2, result.getIncludes().size());
        assertInclude(result.getIncludes().get(0), 2, "QCPYSRC,CONSTS", "qcpysrc/consts.rpgleinc", null);
        assertInclude(result.getIncludes().get(1), 4, "QCPYSRC,CONSTS", null, Problem.TOO_LONG);
    }

    @Test
    public void testRpg_conditional() {

        Result result = convert("SQLRPGLE", "QRPGLESRC(HELLO)", IncludeStyle.CONDITIONAL, "**FREE\n/copy QCPYSRC,CONSTS\n");

        assertEquals("**FREE\n" //
            + "/if defined(MBR_BUILD)\n" //
            + "/copy QCPYSRC,CONSTS\n" //
            + "/else\n" //
            + "/copy 'qcpysrc/consts.rpgleinc'\n" //
            + "/endif\n", result.getContent());

        result = convert("RPGLE", "QRPGLESRC(HELLO)", IncludeStyle.CONDITIONAL, pad("      /COPY QCPYSRC,CONSTS", 80) + "COMMENT\n");

        assertEquals("      /IF DEFINED(MBR_BUILD)\n" //
            + pad("      /COPY QCPYSRC,CONSTS", 80) + "\n" //
            + "      /ELSE\n" //
            + "      /COPY 'qcpysrc/consts.rpgleinc'\n" //
            + pad("      /ENDIF", 80) + "COMMENT\n", result.getContent());
    }

    @Test
    public void testCl() {

        Result result = convert("CLLE", "QCLSRC(START)", IncludeStyle.CONDITIONAL, "             PGM\n" //
            + "             INCLUDE    SRCMBR(VARS)\n" //
            + "             INCLUDE    SRCMBR(VARS) SRCFILE(*INCFILE) /* same */\n" //
            + "             include    srcmbr(clvars) srcfile(qcpysrc)\n" //
            + " LBL:        INCLUDE    SRCFILE(JSMITH#CL/QCPYSRC) SRCMBR(CLVARS)\n" //
            + "             INCLUDE    SRCMBR(CLVARS) SRCFILE(OTHERLIB/QCPYSRC)\n" //
            + "             INCLUDE    SRCSTMF('vars.clleinc')\n" //
            + "             INCLUDE    VARS\n" //
            + "             INCLUDE    SRCMBR(VARS) +\n" //
            + "                          SRCFILE(QCLSRC)\n" //
            + "             ENDPGM\n");

        assertEquals("             PGM\n" //
            + "             INCLUDE    SRCSTMF('vars.clleinc')\n" //
            + "             INCLUDE    SRCSTMF('vars.clleinc') /* same */\n" //
            + "             include    srcstmf('../qcpysrc/clvars.clleinc')\n" //
            + " LBL:        INCLUDE    SRCSTMF('../qcpysrc/clvars.clleinc')\n" //
            + "             INCLUDE    SRCMBR(CLVARS) SRCFILE(OTHERLIB/QCPYSRC)\n" //
            + "             INCLUDE    SRCSTMF('vars.clleinc')\n" //
            + "             INCLUDE    VARS\n" //
            + "             INCLUDE    SRCMBR(VARS) +\n" //
            + "                          SRCFILE(QCLSRC)\n" //
            + "             ENDPGM\n", result.getContent());

        assertEquals(7, result.getIncludes().size());
        assertInclude(result.getIncludes().get(0), 2, "VARS", "vars.clleinc", null);
        assertInclude(result.getIncludes().get(1), 3, "VARS", "vars.clleinc", null);
        assertInclude(result.getIncludes().get(2), 4, "clvars", "../qcpysrc/clvars.clleinc", null);
        assertInclude(result.getIncludes().get(3), 5, "CLVARS", "../qcpysrc/clvars.clleinc", null);
        assertInclude(result.getIncludes().get(4), 6, "CLVARS", null, Problem.OTHER_LIBRARY);
        assertInclude(result.getIncludes().get(5), 8, "VARS", null, Problem.NOT_SUPPORTED);
        assertInclude(result.getIncludes().get(6), 9, "VARS", null, Problem.NOT_SUPPORTED);
    }

    @Test
    public void testCreate() {
        assertNull(MemberIncludeConverter.create("DSPF", LIBRARY, getPaths(), getSourceFiles(), IncludeStyle.REPLACE, SQL_INCLUDE_FILE));
        assertNull(MemberIncludeConverter.create(null, LIBRARY, getPaths(), getSourceFiles(), IncludeStyle.REPLACE, SQL_INCLUDE_FILE));
    }

    @Test
    public void testGetIncludedMembers() {

        MemberIncludeConverter converter = MemberIncludeConverter.create("RPGLE", LIBRARY, getPaths(), getSourceFiles(), IncludeStyle.REPLACE,
            SQL_INCLUDE_FILE);
        converter.convert("QRPGLESRC(HELLO)", "**FREE\n/copy QCPYSRC,CONSTS\n/copy QCPYSRC,MISSING\nexec sql include HOSTVARS;\n");
        converter.convert("QRPGLESRC(LOCAL)", "**FREE\n/copy LOCAL\n");

        assertEquals(new HashSet<String>(Arrays.asList("QCPYSRC(CONSTS)", "QCPYSRC(HOSTVARS)", "QRPGLESRC(LOCAL)")), converter.getIncludedMembers());
    }

    private static Result convert(String aSourceType, String aKey, IncludeStyle aStyle, String aContent) {
        return convert(aSourceType, aKey, aStyle, SQL_INCLUDE_FILE, aContent);
    }

    private static Result convert(String aSourceType, String aKey, IncludeStyle aStyle, String aSqlIncludeFile, String aContent) {
        return MemberIncludeConverter.create(aSourceType, LIBRARY, getPaths(), getSourceFiles(), aStyle, aSqlIncludeFile).convert(aKey, aContent);
    }

    private static Map<String, String> getPaths() {

        Map<String, String> paths = new HashMap<String, String>();
        paths.put("QRPGLESRC(HELLO)", "qrpglesrc/hello.rpgle");
        paths.put("QRPGLESRC(LOCAL)", "qrpglesrc/local.rpgleinc");
        paths.put("QRPGLESRC(NOTYPE)", null);
        paths.put("QCPYSRC(CONSTS)", "qcpysrc/consts.rpgleinc");
        paths.put("QCPYSRC(HOSTVARS)", "qcpysrc/hostvars.sqlrpgleinc");
        paths.put("QCPYSRC(NOTYPE)", null);
        paths.put("QCPYSRC(CLVARS)", "qcpysrc/clvars.clleinc");
        paths.put("QCLSRC(START)", "qclsrc/start.clle");
        paths.put("QCLSRC(VARS)", "qclsrc/vars.clleinc");

        return paths;
    }

    private static Set<String> getSourceFiles() {
        return new HashSet<String>(Arrays.asList("QRPGLESRC", "QCPYSRC", "QCLSRC"));
    }

    private static void assertInclude(Include anInclude, int aLineNumber, String aName, String aTarget, Problem aProblem) {
        assertEquals(aLineNumber, anInclude.getLineNumber());
        assertEquals(aName, anInclude.getName());
        assertEquals(aTarget, anInclude.getTarget());
        assertEquals(aProblem, anInclude.getProblem());
    }

    private static String pad(String aText, int aLength) {
        StringBuilder buffer = new StringBuilder(aText);
        while (buffer.length() < aLength) {
            buffer.append(' ');
        }
        return buffer.toString();
    }
}
