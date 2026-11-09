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
import de.tools400.ifsprojects.sources.headers.SourceHeaderParsers;

public class TobiBuildFilesTest {

    private static final String HEADER = "# Draft of the rules of TOBi for the members of JSMITH#CL/%s, created by the migration\n"
        + "# of IFS Projects. Check the object types and set the parameters of the installer or of the\n"
        + "# STRPREPRC headers, because the defaults of TOBi differ from the CL commands, e.g.:\n" //
        + "#   %%.MODULE: private DBGVIEW := *LIST\n" //
        + "#   HELLO.PGM: private ACTGRP := *NEW\n";

    private final List<SourceMember> members = new ArrayList<>();
    private final Map<String, String> contents = new HashMap<>();

    @Test
    public void testGetIbmiJson() {
        assertEquals("{\n    \"version\": \"0.0.1\",\n    \"build\": {\n        \"tgtCcsid\": \"273\"\n    }\n}\n",
            TobiBuildFiles.getIbmiJson(sourceFile("QRPGLESRC", 273), 1141));
        assertEquals("{\n    \"version\": \"0.0.1\",\n    \"build\": {\n        \"tgtCcsid\": \"1141\"\n    }\n}\n",
            TobiBuildFiles.getIbmiJson(sourceFile("QRPGLESRC", 65535), 1141));
    }

    /**
     * A binding source becomes a service program, an RPG or CL member with its
     * name the module of the service program.
     */
    @Test
    public void testGetDirectoryRules_serviceProgram() {

        add("QRPGLESRC", "UTILS", "RPGLE", "**FREE\nctl-opt nomain;\n");
        add("QRPGLESRC", "HELLO", "RPGLE", "**FREE\n");
        add("QSRVSRC", "UTILS", "BND", "STRPGMEXP PGMLVL(*CURRENT)\nENDPGMEXP\n");
        add("QSRVSRC", "OTHER", "BND", "STRPGMEXP PGMLVL(*CURRENT)\nENDPGMEXP\n");

        MigrationPlan plan = createPlan("QRPGLESRC", "QSRVSRC");

        assertEquals(String.format(HEADER, "QRPGLESRC") + "\nUTILS.MODULE: utils.rpgle\n" + "\nHELLO.PGM: hello.rpgle\n", rules(plan, "QRPGLESRC"));
        assertEquals(String.format(HEADER, "QSRVSRC") + "\nUTILS.SRVPGM: utils.bnd UTILS.MODULE\n"
            + "\n# Add the modules of the service program, e.g. MODULE1.MODULE MODULE2.MODULE.\nOTHER.SRVPGM: other.bnd\n", rules(plan, "QSRVSRC"));
    }

    /**
     * TOBi 3.4 and later accept a <code>#</code> in the name of a target or
     * of its source, if it is escaped, otherwise make reads the rule as a
     * comment, and a <code>$</code> as it is. Because TOBi 3.5 does not build
     * the targets with a recipe, if a target with <code>$</code> or
     * <code>#</code> has none, the rules of such members are commented out.
     * Copy books with <code>$</code> or <code>#</code> cannot be prerequisites
     * (verified with makei 3.5).
     */
    @Test
    public void testGetDirectoryRules_hash() {

        add("QRPGLESRC", "#INIT", "RPGLE", "**FREE\n/copy QRPGLESRC,#CONSTS\n/copy QRPGLESRC,TYPES\n/copy QRPGLESRC,$PROTOS\n");
        add("QRPGLESRC", "#CONSTS", "RPGLE", "**FREE\n");
        add("QRPGLESRC", "TYPES", "RPGLE", "**FREE\n");
        add("QRPGLESRC", "$PROTOS", "RPGLE", "**FREE\n");
        add("QRPGLESRC", "$UTIL", "RPGLE", "**FREE\n");
        add("QRPGLESRC", "MAIN", "RPGLE", "**FREE\n");
        add("QRPGLESRC", "HELLO", "RPGLE", "**FREE\n");
        add("QCLSRC", "#START", "CLLE", "PGM\nENDPGM\n");

        MigrationPlan plan = createPlan("QRPGLESRC", "QCLSRC");

        String comment = "\n# Rules of members with $ or # in the name, which make interprets. TOBi 3.4 and later accept\n"
            + "# them in names (# written as \\#), but TOBi 3.5 does not build the targets with a recipe, if a\n"
            + "# target with $ or # has none. Check the rules, before you remove the comment characters.\n";

        assertEquals(String.format(HEADER, "QRPGLESRC") //
            + "\nMAIN.PGM: main.rpgle\n" + "\nHELLO.PGM: hello.rpgle\n" + comment //
            + "\n# Copy books with $ or # cannot be prerequisites in TOBi: qrpglesrc/#consts.rpgleinc qrpglesrc/$protos.rpgleinc\n"
            + "# \\#INIT.PGM: \\#init.rpgle qrpglesrc/types.rpgleinc\n" //
            + "\n# $UTIL.PGM: $util.rpgle\n", rules(plan, "QRPGLESRC"));
        assertEquals(String.format(HEADER, "QCLSRC") + comment
            + "\n# \\#START.PGM: private INCDIR := 'qclsrc'\n# \\#START.PGM: \\#start.clle\n# \t$(PGM.CLLE_TO_PGM_RECIPE)\n", rules(plan, "QCLSRC"));
    }

    /**
     * A member, that includes a copy book with <code>$</code> or
     * <code>#</code>, gets an active rule without it.
     */
    @Test
    public void testGetDirectoryRules_copyBookWithHash() {

        add("QRPGLESRC", "MAIN", "RPGLE", "**FREE\n/copy QRPGLESRC,#CONSTS\n");
        add("QRPGLESRC", "#CONSTS", "RPGLE", "**FREE\n");
        add("QRPGLESRC", "HELLO", "RPGLE", "**FREE\n");

        MigrationPlan plan = createPlan("QRPGLESRC");

        assertEquals(String.format(HEADER, "QRPGLESRC") //
            + "\n# Copy books with $ or # cannot be prerequisites in TOBi: qrpglesrc/#consts.rpgleinc\nMAIN.PGM: main.rpgle\n" //
            + "\nHELLO.PGM: hello.rpgle\n", rules(plan, "QRPGLESRC"));
    }

    private MigrationPlan createPlan(String... aSourceFiles) {
        return new MigrationPlan("JSMITH#CL", Arrays.asList(aSourceFiles), members, contents, new HashMap<String, String>(),
            new MigrationSettings(IncludeStyle.REPLACE, false, 1141), new SourceHeaderParsers(new TestHeaderParser()));
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
        add("QRPGLESRC", "UTILS", "RPGLE", "      * BUILD: CRTRPGMOD MODULE(&LI/&OB) SRCSTMF('&STMF')\n     H NOMAIN\n");
        add("QRPGLESRC", "SQLMOD", "SQLRPGLE", "**FREE\n// BUILD: CRTSQLRPGI OBJ(&LI/&OB) OBJTYPE(*MODULE)\n" + "exec sql include HOSTVARS;\n");
        add("QCPYSRC", "HOSTVARS", "SQLRPGLE", "**FREE\n");
        add("QCPYSRC", "CONSTS", "RPGLE", "**FREE\n");
        add("QCPYSRC", "TYPES", "RPGLE", "**FREE\n");
        add("QCLSRC", "START", "CLLE", "PGM\nINCLUDE SRCMBR(VARS)\nENDPGM\n");
        add("QCLSRC", "VARS", "CLLE", "DCL VAR(&A) TYPE(*CHAR)\n");
        add("QCLSRC", "STARTM", "CLLE", "/* BUILD: CRTCLMOD MODULE(&LI/&OB) */\nPGM\nENDPGM\n");
        add("QDDSSRC", "HELLOD", "DSPF", "     A          R FMT1\n");
        add("QCMDSRC", "HELLO", "CMD", "CMD PROMPT('Hello')\n");
        add("QPNLSRC", "HELLOP", "PNLGRP", ":PNLGRP.\n:EPNLGRP.\n");
        add("QSQLSRC", "CUST", "TABLE", "create table cust (id int);\n");

        MigrationPlan plan = new MigrationPlan("JSMITH#CL",
            Arrays.asList("QRPGLESRC", "QCPYSRC", "QCLSRC", "QDDSSRC", "QCMDSRC", "QPNLSRC", "QSQLSRC"), members, contents,
            new HashMap<String, String>(), new MigrationSettings(IncludeStyle.REPLACE, false, 1141), new SourceHeaderParsers(new TestHeaderParser()));

        assertEquals(String.format(HEADER, "QRPGLESRC") //
            + "\nHELLO.PGM: hello.rpgle qcpysrc/consts.rpgleinc qcpysrc/types.rpgleinc\n" //
            + "\nUTILS.MODULE: utils.rpgle\n" //
            + "\n# DBGVIEW(*SOURCE) fails for EXEC SQL INCLUDE of a stream file (SQL8009).\n" //
            + "SQLMOD.MODULE: private DBGVIEW := *LIST\n" //
            + "SQLMOD.MODULE: sqlmod.sqlrpgle qcpysrc/hostvars.sqlrpgleinc\n", rules(plan, "QRPGLESRC"));
        assertEquals(String.format(HEADER, "QCPYSRC"), rules(plan, "QCPYSRC"));
        assertEquals(String.format(HEADER, "QCLSRC") //
            + "\nSTART.PGM: private INCDIR := 'qclsrc'\nSTART.PGM: start.clle qclsrc/vars.clleinc\n\t$(PGM.CLLE_TO_PGM_RECIPE)\n" //
            + "\nSTARTM.MODULE: private INCDIR := 'qclsrc'\nSTARTM.MODULE: startm.clle\n", rules(plan, "QCLSRC"));
        assertEquals(String.format(HEADER, "QDDSSRC") + "\nHELLOD.FILE: hellod.dspf\n", rules(plan, "QDDSSRC"));
        assertEquals(String.format(HEADER, "QCMDSRC") + "\nHELLO.CMD: hello.cmd\n", rules(plan, "QCMDSRC"));
        assertEquals(String.format(HEADER, "QPNLSRC") + "\nHELLOP.PNLGRP: hellop.pnlgrp\n\t$(PNLGRPSRC_TO_PNLGRP_RECIPE)\n", rules(plan, "QPNLSRC"));
        assertEquals(String.format(HEADER, "QSQLSRC") + "\n# No rule created for:\n#   cust.table\n", rules(plan, "QSQLSRC"));
    }

    private static String rules(MigrationPlan aPlan, String aSourceFile) {

        List<MigrationPlan.Item> items = new ArrayList<>();
        for (MigrationPlan.Item item : aPlan.getMigratedItems()) {
            if (item.getMember().getSourceFile().equals(aSourceFile)) {
                items.add(item);
            }
        }

        return TobiBuildFiles.getDirectoryRules(sourceFile(aSourceFile, 273), items, aPlan.getMigratedItems());
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
