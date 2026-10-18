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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import de.tools400.ifsprojects.export.ExportMapping;
import de.tools400.ifsprojects.export.IncludeConverter;
import de.tools400.ifsprojects.export.IncludeConverter.Include;
import de.tools400.ifsprojects.export.IncludeConverter.Problem;
import de.tools400.ifsprojects.export.IncludeConverter.Result;
import de.tools400.ifsprojects.export.IncludeResolver;
import de.tools400.ifsprojects.export.SourceFileRule;

/**
 * The search orders of the resolver have been verified on the IBM i with
 * CRTBNDRPG and CRTBNDCL.
 */
public class IncludeConverterTest {

    private static final String PROJECT = "/home/jsmith/proj";

    private static final List<String> FILES = Arrays.asList("qrpglesrc/hello.pgm.rpgle", "qrpglesrc/local.rpgleinc", "inc/consts.rpgleinc",
        "inc/proto.rpgleinc", "x.rpgleinc", "inc/x.rpgleinc", "qrpglesrc/toolongname1.rpgleinc", "qclsrc/start.pgm.clle", "qclsrc/w.clinc",
        "inc/w.clinc", "inc/v.clinc", "r.clinc", "README.md");

    @Test
    public void testCreate() {
        assertNotNull(create("RPGLE"));
        assertNotNull(create("SQLRPGLE"));
        assertNotNull(create("CLLE"));
        assertNull(create("CLP"));
        assertNull(create("DSPF"));
    }

    @Test
    public void testRpg_pathForms() {
        assertRpg("/copy 'inc/consts.rpgleinc'", "/copy inc,consts");
        assertRpg("/COPY inc/consts.rpgleinc // constants", "/COPY INC,CONSTS // constants");
        assertRpg("/include \"inc/proto.rpgleinc\";", "/include inc,proto;");
        assertRpg("  /Copy '/home/jsmith/proj/inc/consts.rpgleinc'", "  /Copy INC,CONSTS");
        assertRpg("/copy '/HOME/JSMITH/PROJ/INC/CONSTS.RPGLEINC'", "/copy inc,consts");
        assertRpg("/copy '../inc/consts.rpgleinc'", "/copy inc,consts");
    }

    @Test
    public void testRpg_searchOrder() {
        // project directory (current directory) before include directory
        assertRpg("/copy x.rpgleinc", "/copy qrpglesrc,x");
        // include directory
        assertRpg("/copy proto.rpgleinc", "/copy inc,proto");
        // directory of the source
        assertRpg("/copy local.rpgleinc", "/copy qrpglesrc,local");
        // extensions .rpgleinc and .rpgle
        assertRpg("/copy consts", "/copy inc,consts");
        assertRpg("/copy 'local'", "/copy qrpglesrc,local");
    }

    @Test
    public void testRpg_unchanged() {
        assertRpg("/COPY QSYSINC/QRPGLESRC,QUSEC", "/COPY QSYSINC/QRPGLESRC,QUSEC");
        assertRpg("/copy qcpylesrc,consts", "/copy qcpylesrc,consts");
        assertRpg("/copy unknown", "/copy unknown");
        assertRpg("// /copy 'inc/consts.rpgleinc'", "// /copy 'inc/consts.rpgleinc'");
        assertRpg("/copyright 'inc/consts.rpgleinc'", "/copyright 'inc/consts.rpgleinc'");
        assertRpg("dsply '/copy inc/consts.rpgleinc';", "dsply '/copy inc/consts.rpgleinc';");

        Result result = convert("RPGLE", "qrpglesrc/hello.pgm.rpgle", "**FREE\n/COPY QSYSINC/QRPGLESRC,QUSEC\n/copy unknown\n");
        assertTrue(result.getIncludes().isEmpty());
    }

    @Test
    public void testRpg_problems() {

        Result result = convert("RPGLE", "qrpglesrc/hello.pgm.rpgle", "**FREE\n/copy '/QIBM/include/qusec.rpgleinc'\n/copy missing.rpgleinc\n"
            + "/copy toolongname1\nexec sql include 'inc/sqlvars.rpgleinc';\nexec sql include sqlca;\n");

        assertEquals("**FREE\n/copy '/QIBM/include/qusec.rpgleinc'\n/copy missing.rpgleinc\n/copy toolongname1\n"
            + "exec sql include 'inc/sqlvars.rpgleinc';\nexec sql include sqlca;\n", result.getContent());
        assertTrue(result.hasProblems());
        assertInclude(result.getIncludes().get(0), 2, "/QIBM/include/qusec.rpgleinc", null, Problem.NOT_FOUND);
        assertInclude(result.getIncludes().get(1), 3, "missing.rpgleinc", null, Problem.NOT_FOUND);
        assertInclude(result.getIncludes().get(2), 4, "toolongname1", null, Problem.NOT_EXPORTED);
        assertInclude(result.getIncludes().get(3), 5, "inc/sqlvars.rpgleinc", null, Problem.NOT_SUPPORTED);
        assertEquals(4, result.getIncludes().size());
    }

    @Test
    public void testRpg_fixedFormat() {

        String source = "     H DFTACTGRP(*NO)\n" //
            + "      /COPY inc/consts.rpgleinc\n" //
            + "00020 /copy 'inc/proto.rpgleinc'\n" //
            + "      */copy inc/consts.rpgleinc\n" //
            + "     C/COPY inc/consts.rpgleinc\n" //
            + "        /copy 'inc/consts.rpgleinc'\n" //
            + "     C/EXEC SQL INCLUDE 'inc/sqlvars.rpgleinc'\n" //
            + "     C/END-EXEC\n";

        Result result = convert("SQLRPGLE", "qrpglesrc/hello.pgm.rpgle", source);

        assertEquals("     H DFTACTGRP(*NO)\n" //
            + "      /COPY INC,CONSTS\n" //
            + "00020 /copy inc,proto\n" //
            + "      */copy inc/consts.rpgleinc\n" //
            + "     C/COPY inc/consts.rpgleinc\n" //
            + "        /copy inc,consts\n" //
            + "     C/EXEC SQL INCLUDE 'inc/sqlvars.rpgleinc'\n" //
            + "     C/END-EXEC\n", result.getContent());
        assertEquals(4, result.getIncludes().size());
        assertInclude(result.getIncludes().get(0), 2, "inc/consts.rpgleinc", "INC,CONSTS", null);
        assertInclude(result.getIncludes().get(3), 7, "inc/sqlvars.rpgleinc", null, Problem.NOT_SUPPORTED);
    }

    @Test
    public void testRpg_fixedFormatCommentColumns() {

        String code = pad("      /COPY inc/consts.rpgleinc", 80);
        Result result = convert("RPGLE", "qrpglesrc/hello.pgm.rpgle", code + "Comment\n");
        assertEquals(pad("      /COPY INC,CONSTS", 80) + "Comment\n", result.getContent());

        String tooLong = pad("", 69) + "/copy local";
        result = convert("RPGLE", "qrpglesrc/hello.pgm.rpgle", tooLong + "\n");
        assertEquals(tooLong + "\n", result.getContent());
        assertInclude(result.getIncludes().get(0), 1, "local", null, Problem.TOO_LONG);

        String fits = pad("", 59) + "/copy local";
        result = convert("RPGLE", "qrpglesrc/hello.pgm.rpgle", fits);
        assertEquals(pad("", 59) + "/copy qrpglesrc,local", result.getContent());
    }

    @Test
    public void testRpg_compileTimeData() {

        Result result = convert("RPGLE", "qrpglesrc/hello.pgm.rpgle", "**FREE\n/copy consts\n**CTDATA names\n/copy consts\n");

        assertEquals("**FREE\n/copy inc,consts\n**CTDATA names\n/copy consts\n", result.getContent());
        assertEquals(1, result.getIncludes().size());
    }

    @Test
    public void testRpg_lineSeparators() {
        assertEquals("\uFEFF**FREE\r\n/copy inc,consts\r\ndsply 'x';", convert("RPGLE", "qrpglesrc/hello.pgm.rpgle",
            "\uFEFF**FREE\r\n/copy 'inc/consts.rpgleinc'\r\ndsply 'x';").getContent());
        assertEquals("", convert("RPGLE", "qrpglesrc/hello.pgm.rpgle", "").getContent());
        assertEquals("\n\n", convert("RPGLE", "qrpglesrc/hello.pgm.rpgle", "\n\n").getContent());
    }

    @Test
    public void testRpg_copyBook() {
        // A copy book is searched from the project directory and from its own
        // directory, not from the directory of the including source.
        Result result = convert("RPGLE", "inc/proto.rpgleinc", "**FREE\n/copy consts.rpgleinc\n/copy local.rpgleinc\n");

        assertEquals("**FREE\n/copy inc,consts\n/copy local.rpgleinc\n", result.getContent());
        assertInclude(result.getIncludes().get(1), 3, "local.rpgleinc", null, Problem.NOT_FOUND);
    }

    @Test
    public void testCl_searchOrder() {
        // directory of the source before include directory
        assertCl("INCLUDE SRCSTMF('w.clinc')", "INCLUDE SRCMBR(W) SRCFILE(QCLSRC)");
        // include directory
        assertCl("INCLUDE SRCSTMF('v.clinc')", "INCLUDE SRCMBR(V) SRCFILE(INC)");
        assertCl("INCLUDE SRCSTMF('/home/jsmith/proj/inc/w.clinc')", "INCLUDE SRCMBR(W) SRCFILE(INC)");
    }

    @Test
    public void testCl_forms() {
        assertCl("lbl: include srcstmf( 'w.clinc' ) /* variables */", "lbl: include srcmbr(w) srcfile(qclsrc) /* variables */");
        assertCl("             INCLUDE    SRCSTMF('../inc/v.clinc')", "             INCLUDE    SRCMBR(V) SRCFILE(INC)");
        assertCl("INCLUDE SRCMBR(W) SRCFILE(QCLSRC)", "INCLUDE SRCMBR(W) SRCFILE(QCLSRC)");
        assertCl("CHGVAR VAR(&X) VALUE('INCLUDE SRCSTMF(''w.clinc'')')", "CHGVAR VAR(&X) VALUE('INCLUDE SRCSTMF(''w.clinc'')')");
    }

    @Test
    public void testCl_problems() {

        Result result = convert("CLLE", "qclsrc/start.pgm.clle", "PGM\nINCLUDE SRCSTMF('r.clinc')\nINCLUDE +\n  SRCSTMF('w.clinc')\n"
            + "INCLUDE SRCMBR(W) +\n  SRCFILE(QCLSRC)\nINCLUDE SRCSTMF('it''s.clinc')\nENDPGM\n");

        assertEquals("PGM\nINCLUDE SRCSTMF('r.clinc')\nINCLUDE +\n  SRCSTMF('w.clinc')\nINCLUDE SRCMBR(W) +\n  SRCFILE(QCLSRC)\n"
            + "INCLUDE SRCSTMF('it''s.clinc')\nENDPGM\n", result.getContent());
        assertEquals(3, result.getIncludes().size());
        // The current directory is not searched.
        assertInclude(result.getIncludes().get(0), 2, "r.clinc", null, Problem.NOT_FOUND);
        assertInclude(result.getIncludes().get(1), 3, "w.clinc", null, Problem.NOT_SUPPORTED);
        assertInclude(result.getIncludes().get(2), 7, "it's.clinc", null, Problem.NOT_FOUND);
    }

    @Test
    public void testResult_noProblems() {

        Result result = convert("RPGLE", "qrpglesrc/hello.pgm.rpgle", "**FREE\n/copy consts\n");

        assertFalse(result.hasProblems());
        assertInclude(result.getIncludes().get(0), 2, "consts", "INC,CONSTS", null);
    }

    private void assertRpg(String aLine, String anExpectedLine) {
        assertEquals("**FREE\n" + anExpectedLine + "\n", convert("RPGLE", "qrpglesrc/hello.pgm.rpgle", "**FREE\n" + aLine + "\n").getContent());
    }

    private void assertCl(String aLine, String anExpectedLine) {
        assertEquals("PGM\n" + anExpectedLine + "\nENDPGM\n", convert("CLLE", "qclsrc/start.pgm.clle", "PGM\n" + aLine + "\nENDPGM\n").getContent());
    }

    private void assertInclude(Include anInclude, int aLineNumber, String aName, String aTarget, Problem aProblem) {
        assertEquals(aLineNumber, anInclude.getLineNumber());
        assertEquals(aName, anInclude.getName());
        assertEquals(aTarget, anInclude.getMember() == null ? null : anInclude.getMember().getTarget());
        assertEquals(aProblem, anInclude.getProblem());
    }

    private Result convert(String aSourceType, String aPath, String aContent) {
        return create(aSourceType).convert(aPath, aContent);
    }

    private IncludeConverter create(String aSourceType) {

        Map<String, ExportMapping.Entry> members = new HashMap<String, ExportMapping.Entry>();
        for (ExportMapping.Entry entry : new ExportMapping(SourceFileRule.DIRECTORY, null).map(FILES)) {
            if (entry.getStatus() == ExportMapping.Status.OK) {
                members.put(entry.getPath(), entry);
            }
        }

        IncludeResolver resolver = new IncludeResolver(PROJECT, Arrays.asList(PROJECT + "/inc", "/QIBM/include"), FILES);

        return IncludeConverter.create(aSourceType, resolver, members);
    }

    private static String pad(String aText, int aLength) {
        StringBuilder buffer = new StringBuilder(aText);
        while (buffer.length() < aLength) {
            buffer.append(' ');
        }
        return buffer.toString();
    }
}
