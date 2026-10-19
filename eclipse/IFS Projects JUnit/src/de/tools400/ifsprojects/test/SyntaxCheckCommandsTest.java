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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import de.tools400.ifsprojects.project.IProjConfig;
import de.tools400.ifsprojects.syntaxcheck.SourceType;
import de.tools400.ifsprojects.syntaxcheck.SyntaxCheckCommands;

public class SyntaxCheckCommandsTest {

    @Test
    public void testSourceType() {
        assertEquals(SourceType.RPGLE, SourceType.fromFileName("hello.pgm.rpgle"));
        assertEquals(SourceType.RPGLE, SourceType.fromFileName("DSPSRCR1.RPGLE"));
        assertEquals(SourceType.SQLRPGLE, SourceType.fromFileName("sqlpgm.pgm.sqlrpgle"));
        assertNull(SourceType.fromFileName("constants.rpgleinc"));
        assertNull(SourceType.fromFileName("hello.clle"));
        assertNull(SourceType.fromFileName("rpgle"));
        assertNull(SourceType.fromFileName(null));
    }

    @Test
    public void testGetObjectName() {
        assertEquals("HELLO", SyntaxCheckCommands.getObjectName("hello.pgm.rpgle"));
        assertEquals("DSPSRCR1", SyntaxCheckCommands.getObjectName("DSPSRCR1.RPGLE"));
        assertEquals("$UTIL_1", SyntaxCheckCommands.getObjectName("$util_1.sqlrpgle"));
        assertEquals("IFSCHECK", SyntaxCheckCommands.getObjectName("toolongname1.rpgle"));
        assertEquals("IFSCHECK", SyntaxCheckCommands.getObjectName("1hello.rpgle"));
        assertEquals("IFSCHECK", SyntaxCheckCommands.getObjectName("my-pgm.rpgle"));
        assertEquals("IFSCHECK", SyntaxCheckCommands.getObjectName("\u00e4nderung.rpgle"));
        assertEquals("IFSCHECK", SyntaxCheckCommands.getObjectName(".rpgle"));
        assertEquals("IFSCHECK", SyntaxCheckCommands.getObjectName(null));
    }

    @Test
    public void testIsNoMain_free() {
        assertTrue(SyntaxCheckCommands.isNoMain("**free\nctl-opt nomain;\ndcl-proc p export;\nend-proc;"));
        assertTrue(SyntaxCheckCommands.isNoMain("**FREE\nCTL-OPT OPTION(*SRCSTMT)\n  NOMAIN;\n"));
        assertTrue(SyntaxCheckCommands.isNoMain("**free\r\nctl-opt\r\n  nomain\r\n  option(*nodebugio);\r\n"));
        assertFalse(SyntaxCheckCommands.isNoMain("**free\nctl-opt dftactgrp(*no);\ndsply 'nomain';\n"));
        assertFalse(SyntaxCheckCommands.isNoMain("**free\n// ctl-opt nomain;\nctl-opt main(main);\n"));
        assertFalse(SyntaxCheckCommands.isNoMain("**free\nctl-opt dftactgrp(*no); // nomain\n"));
        assertFalse(SyntaxCheckCommands.isNoMain("**free\ndcl-s noMainFlag ind;\n"));
        assertFalse(SyntaxCheckCommands.isNoMain(""));
        assertFalse(SyntaxCheckCommands.isNoMain(null));
    }

    @Test
    public void testIsNoMain_fixed() {
        assertTrue(SyntaxCheckCommands.isNoMain("     H NOMAIN\n     D x               S             10A\n"));
        assertTrue(SyntaxCheckCommands.isNoMain("     h option(*srcstmt) nomain\n"));
        assertTrue(SyntaxCheckCommands.isNoMain("       ctl-opt nomain;\n"));
        assertFalse(SyntaxCheckCommands.isNoMain("     H*NOMAIN\n     H DFTACTGRP(*NO)\n"));
        assertFalse(SyntaxCheckCommands.isNoMain("      * H NOMAIN\n     H DFTACTGRP(*NO)\n"));
        assertFalse(SyntaxCheckCommands.isNoMain("     C                   EVAL      NOMAIN = 1\n"));
    }

    @Test
    public void testGetCompileCommand_rpgleProgram() {
        assertEquals("CRTBNDRPG PGM(QTEMP/HELLO) SRCSTMF('/home/jsmith/proj/qrpglesrc/hello.pgm.rpgle') OPTION(*NOGEN *EVENTF) OUTPUT(*NONE)"
            + " DFTACTGRP(*NO) TGTCCSID(*JOB) INCDIR('/home/jsmith/proj/qprotsrc')",
            SyntaxCheckCommands.getCompileCommand(SourceType.RPGLE, "HELLO", "/home/jsmith/proj/qrpglesrc/hello.pgm.rpgle",
                Arrays.asList("/home/jsmith/proj/qprotsrc"), false));
    }

    @Test
    public void testGetCompileCommand_rpgleModule() {
        assertEquals("CRTRPGMOD MODULE(QTEMP/UTILS) SRCSTMF('/p/utils.rpgle') OPTION(*NOGEN *EVENTF) OUTPUT(*NONE) TGTCCSID(*JOB)",
            SyntaxCheckCommands.getCompileCommand(SourceType.RPGLE, "UTILS", "/p/utils.rpgle", new ArrayList<String>(), true));
    }

    @Test
    public void testGetCompileCommand_sqlrpgle() {
        assertEquals("CRTSQLRPGI OBJ(QTEMP/SQLPGM) SRCSTMF('/p/sqlpgm.pgm.sqlrpgle') OBJTYPE(*MODULE) OPTION(*EVENTF) OUTPUT(*NONE)"
            + " CVTCCSID(*JOB) RPGPPOPT(*LVL2) COMPILEOPT('OUTPUT(*NONE) TGTCCSID(*JOB) INCDIR(''/p/inc'' ''/p/it''''s'')')",
            SyntaxCheckCommands.getCompileCommand(SourceType.SQLRPGLE, "SQLPGM", "/p/sqlpgm.pgm.sqlrpgle", Arrays.asList("/p/inc", "/p/it's"),
                false));
        assertEquals("CRTSQLRPGI OBJ(QTEMP/SQLPGM) SRCSTMF('/p/sqlpgm.sqlrpgle') OBJTYPE(*MODULE) OPTION(*EVENTF) OUTPUT(*NONE)"
            + " CVTCCSID(*JOB) RPGPPOPT(*LVL2) COMPILEOPT('OUTPUT(*NONE) TGTCCSID(*JOB)')",
            SyntaxCheckCommands.getCompileCommand(SourceType.SQLRPGLE, "SQLPGM", "/p/sqlpgm.sqlrpgle", null, true));
    }

    @Test
    public void testGetIgnoredMessageIds() {
        assertEquals(Collections.singleton("RNF1324"), SyntaxCheckCommands.getIgnoredMessageIds(SourceType.SQLRPGLE, false));
        assertTrue(SyntaxCheckCommands.getIgnoredMessageIds(SourceType.SQLRPGLE, true).isEmpty());
        assertTrue(SyntaxCheckCommands.getIgnoredMessageIds(SourceType.RPGLE, false).isEmpty());
        assertTrue(SyntaxCheckCommands.getIgnoredMessageIds(SourceType.RPGLE, true).isEmpty());
    }

    @Test
    public void testGetSetupCommands() throws Exception {

        IProjConfig config = IProjConfig.parse("{\"curlib\": \"&LIB\", \"preUsrlibl\": [\"LIB1\", \"LIB2\"], \"postUsrlibl\": [\"QGPL\"],"
            + " \"setIBMiEnvCmd\": [\"&ENVCMD\", \"CALL &LIB/SETUP\"]}");

        Map<String, String> variables = new LinkedHashMap<String, String>();
        variables.put("LIB", "DSPSRC");
        variables.put("ENVCMD", "CHGSYSLIBL LIB(UBMSYS) OPTION(*REMOVE)");

        assertEquals(Arrays.asList("CHGCURDIR DIR('/home/jsmith/git/dspsrc')", "CHGSYSLIBL LIB(UBMSYS) OPTION(*REMOVE)", "CALL DSPSRC/SETUP",
            "CHGCURLIB CURLIB(DSPSRC)", "ADDLIBLE LIB(LIB2) POSITION(*FIRST)", "ADDLIBLE LIB(LIB1) POSITION(*FIRST)",
            "ADDLIBLE LIB(QGPL) POSITION(*LAST)"), SyntaxCheckCommands.getSetupCommands("/home/jsmith/git/dspsrc", config, variables));
    }

    @Test
    public void testGetSetupCommands_emptyConfig() throws Exception {
        assertEquals(Arrays.asList("CHGCURDIR DIR('/home/jsmith/it''s')"),
            SyntaxCheckCommands.getSetupCommands("/home/jsmith/it's", IProjConfig.parse("{}"), new LinkedHashMap<String, String>()));
    }

    @Test
    public void testGetSetupCommands_librariesAsString() throws Exception {

        IProjConfig config = IProjConfig.parse("{\"preUsrlibl\": \"&ACCOUNTING ACMEUTIL\", \"postUsrlibl\": [\"QGPL\", \"qgpl\", \"  \"]}");

        Map<String, String> variables = new LinkedHashMap<String, String>();
        variables.put("ACCOUNTING", "ACCLIB1 ACCLIB2");

        assertEquals(Arrays.asList("CHGCURDIR DIR('/p')", "ADDLIBLE LIB(ACMEUTIL) POSITION(*FIRST)", "ADDLIBLE LIB(ACCLIB2) POSITION(*FIRST)",
            "ADDLIBLE LIB(ACCLIB1) POSITION(*FIRST)", "ADDLIBLE LIB(QGPL) POSITION(*LAST)"),
            SyntaxCheckCommands.getSetupCommands("/p", config, variables));
    }


    @Test
    public void testEventsMemberCommands() {
        assertEquals("RMVM FILE(QTEMP/EVFEVENT) MBR(HELLO)", SyntaxCheckCommands.getRemoveEventsMemberCommand("HELLO"));
        assertEquals("CPYTOSTMF FROMMBR('/QSYS.LIB/QTEMP.LIB/EVFEVENT.FILE/HELLO.MBR') TOSTMF('/tmp/x.evfevent') STMFOPT(*REPLACE) STMFCCSID(1208)",
            SyntaxCheckCommands.getCopyEventsMemberCommand("HELLO", "/tmp/x.evfevent"));
        assertEquals(
            "CPYTOSTMF FROMMBR('/QSYS.LIB/JSMITH#CL.LIB/EVFEVENT.FILE/HELLO.MBR') TOSTMF('/home/jsmith/.ifsprojects/tmp/x.evfevent') STMFOPT(*REPLACE) STMFCCSID(1208)",
            SyntaxCheckCommands.getCopyEventsMemberCommand("JSMITH#CL", "HELLO", "/home/jsmith/.ifsprojects/tmp/x.evfevent"));
    }

}
