/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.test;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import de.tools400.ifsprojects.migration.IncludeStyle;
import de.tools400.ifsprojects.migration.MigrationPlan;
import de.tools400.ifsprojects.migration.MigrationSettings;
import de.tools400.ifsprojects.migration.SourceFile;
import de.tools400.ifsprojects.migration.SourceMember;
import de.tools400.ifsprojects.migration.TobiBuildFiles;

public class TobiBuildFilesTest {

    private static final String HEADER = "# Draft of the rules of TOBi for the members of JSMITH#CL/%s, created by the migration\n"
        + "# of IFS Projects. Check the object types and set the parameters of the installer or of the\n"
        + "# STRPREPRC headers, because the defaults of TOBi differ from the CL commands, e.g.:\n" //
        + "#   %%.MODULE: private DBGVIEW := *LIST\n" //
        + "#   HELLO.PGM: private ACTGRP := *NEW\n";

    private final List<SourceMember> members = new ArrayList<SourceMember>();
    private final Map<String, String> contents = new HashMap<String, String>();

    @Test
    public void testGetIbmiJson() {
        assertEquals("{\n    \"build\": {\n        \"tgtCcsid\": \"273\"\n    }\n}\n", TobiBuildFiles.getIbmiJson(sourceFile("QRPGLESRC", 273), 1141));
        assertEquals("{\n    \"build\": {\n        \"tgtCcsid\": \"1141\"\n    }\n}\n", TobiBuildFiles.getIbmiJson(sourceFile("QRPGLESRC", 65535), 1141));
    }

    @Test
    public void testGetProjectRules() {
        assertEquals("# Rules of TOBi (The Object Builder for i), created by the migration of IFS Projects.\n"
            + "# The objects are defined in the Rules.mk files of the directories.\n\n" //
            + "SUBDIRS = qclsrc qcpysrc qrpglesrc\n", TobiBuildFiles.getProjectRules(Arrays.asList("qrpglesrc", "qcpysrc", "qclsrc")));
    }

    @Test
    public void testGetDirectoryRules() {

        add("QRPGLESRC", "HELLO", "RPGLE", "**FREE\n/copy QCPYSRC,TYPES\n/copy QCPYSRC,CONSTS\n/copy QCPYSRC,MISSING\n");
        add("QRPGLESRC", "UTILS", "RPGLE", "      * >>PRE-COMPILER<<\n      *   >>CRTCMD<<  CRTRPGMOD MODULE(&LI/&OB) +\n"
            + "      *                           SRCSTMF('&STMF');\n      * >>END-PRE-COMPILER<<\n     H NOMAIN\n");
        add("QRPGLESRC", "SQLMOD", "SQLRPGLE", "// >>CRTCMD<< CRTSQLRPGI OBJ(&LI/&OB) OBJTYPE(*MODULE);\n");
        add("QCPYSRC", "CONSTS", "RPGLE", "**FREE\n");
        add("QCPYSRC", "TYPES", "RPGLE", "**FREE\n");
        add("QCLSRC", "START", "CLLE", "PGM\nINCLUDE SRCMBR(VARS)\nENDPGM\n");
        add("QCLSRC", "VARS", "CLLE", "DCL VAR(&A) TYPE(*CHAR)\n");
        add("QCLSRC", "STARTM", "CLLE", "/* >>CRTCMD<< CRTCLMOD MODULE(&LI/&OB); */\nPGM\nENDPGM\n");
        add("QDDSSRC", "HELLOD", "DSPF", "     A          R FMT1\n");
        add("QCMDSRC", "HELLO", "CMD", "CMD PROMPT('Hello')\n");
        add("QPNLSRC", "HELLOP", "PNLGRP", ":PNLGRP.\n:EPNLGRP.\n");
        add("QSQLSRC", "CUST", "TABLE", "create table cust (id int);\n");

        MigrationPlan plan = new MigrationPlan("JSMITH#CL", Arrays.asList("QRPGLESRC", "QCPYSRC", "QCLSRC", "QDDSSRC", "QCMDSRC", "QPNLSRC",
            "QSQLSRC"), members, contents, new HashMap<String, String>(), new MigrationSettings(IncludeStyle.REPLACE, false, 1141));

        assertEquals(String.format(HEADER, "QRPGLESRC") //
            + "\nHELLO.PGM: hello.rpgle qcpysrc/consts.rpgleinc qcpysrc/types.rpgleinc\n" //
            + "\nUTILS.MODULE: utils.rpgle\n" //
            + "\nSQLMOD.MODULE: sqlmod.sqlrpgle\n", rules(plan, "QRPGLESRC"));
        assertEquals(String.format(HEADER, "QCPYSRC"), rules(plan, "QCPYSRC"));
        assertEquals(String.format(HEADER, "QCLSRC") //
            + "\nSTART.PGM: private INCDIR := 'qclsrc'\nSTART.PGM: start.clle qclsrc/vars.clleinc\n\t$(PGM.CLLE_TO_PGM_RECIPE)\n" //
            + "\nSTARTM.MODULE: private INCDIR := 'qclsrc'\nSTARTM.MODULE: startm.clle\n", rules(plan, "QCLSRC"));
        assertEquals(String.format(HEADER, "QDDSSRC") + "\nHELLOD.FILE: hellod.dspf\n", rules(plan, "QDDSSRC"));
        assertEquals(String.format(HEADER, "QCMDSRC") + "\nHELLO.CMD: hello.cmd\n", rules(plan, "QCMDSRC"));
        assertEquals(String.format(HEADER, "QPNLSRC") + "\nHELLOP.PNLGRP: hellop.pnlgrp\n\t$(PNLGRPSRC_TO_PNLGRP_RECIPE)\n", rules(plan,
            "QPNLSRC"));
        assertEquals(String.format(HEADER, "QSQLSRC") + "\n# No rule created for:\n#   cust.table\n", rules(plan, "QSQLSRC"));
    }

    private static String rules(MigrationPlan aPlan, String aSourceFile) {

        List<MigrationPlan.Item> items = new ArrayList<MigrationPlan.Item>();
        for (MigrationPlan.Item item : aPlan.getMigratedItems()) {
            if (item.getMember().getSourceFile().equals(aSourceFile)) {
                items.add(item);
            }
        }

        return TobiBuildFiles.getDirectoryRules(sourceFile(aSourceFile, 273), items);
    }

    private void add(String aSourceFile, String aName, String aSourceType, String aContent) {
        SourceMember member = new SourceMember(aSourceFile, aName, aSourceType, null);
        members.add(member);
        contents.put(member.getKey(), aContent);
    }

    private static SourceFile sourceFile(String aName, int aCcsid) {
        return new SourceFile("JSMITH#CL", aName, null, aCcsid, 100, 1);
    }
}
