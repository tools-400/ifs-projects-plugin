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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import de.tools400.ifsprojects.migration.IncludeStyle;
import de.tools400.ifsprojects.migration.MigrationPlan;
import de.tools400.ifsprojects.migration.MigrationPlan.Item;
import de.tools400.ifsprojects.migration.MigrationPlan.ProblemType;
import de.tools400.ifsprojects.migration.MigrationSettings;
import de.tools400.ifsprojects.migration.SourceMember;
import de.tools400.ifsprojects.sources.IncludeConverter;
import de.tools400.ifsprojects.sources.MemberText;
import de.tools400.ifsprojects.sources.headers.SourceHeaderParsers;

public class MigrationPlanTest {

    private final List<SourceMember> members = new ArrayList<SourceMember>();
    private final Map<String, String> contents = new HashMap<String, String>();
    private final Map<String, String> readErrors = new HashMap<String, String>();

    @Test
    public void testPlan() {

        add("QRPGLESRC", "HELLO", "RPGLE", "Hello world", "**FREE\r\n/copy QCPYSRC,CONSTS\r\ndsply 'x';\r\n");
        add("QCPYSRC", "CONSTS", "RPGLE", null, "**FREE\ndcl-c X 1;\n");
        add("QCLSRC", "START", "CLLE", null, "PGM\nINCLUDE SRCMBR(VARS)\nENDPGM\n");
        add("QCLSRC", "VARS", "CLLE", null, "DCL VAR(&A) TYPE(*CHAR)\n");
        add("QRPGLESRC", "NOTYPE", null, null, "x\n");
        add("QRPGLESRC", "BROKEN", "RPGLE", null, null);
        readErrors.put("QRPGLESRC(BROKEN)", "CPFA097");
        add("QRPGLESRC", "$UTIL", "RPGLE", null, "**FREE\n");
        add("QRPGLESRC", "OLD", "RPG38", null, "x\n");

        MigrationPlan plan = createPlan(true);
        List<Item> items = plan.getItems();
        assertEquals(8, items.size());

        Item hello = items.get(0);
        assertEquals("qrpglesrc/hello.rpgle", hello.getPath());
        assertEquals(MemberText.insert("**FREE\n/copy 'qcpysrc/consts.rpgleinc'\ndsply 'x';\n", "RPGLE", "Hello world"), hello.getContent());
        assertTrue(hello.getContent().contains("%TEXT Hello world"));
        assertTrue(hello.getProblems().isEmpty());
        assertTrue(hello.isMigrated());

        assertEquals("qcpysrc/consts.rpgleinc", items.get(1).getPath());
        assertEquals("**FREE\ndcl-c X 1;\n", items.get(1).getContent());

        assertEquals("qclsrc/start.clle", items.get(2).getPath());
        assertEquals("PGM\nINCLUDE SRCSTMF('vars.clleinc')\nENDPGM\n", items.get(2).getContent());
        assertEquals("qclsrc/vars.clleinc", items.get(3).getPath());

        Item noType = items.get(4);
        assertNull(noType.getPath());
        assertFalse(noType.isMigrated());
        assertEquals(ProblemType.NO_SOURCE_TYPE, noType.getProblems().get(0).getType());

        Item broken = items.get(5);
        assertFalse(broken.isMigrated());
        assertEquals(ProblemType.NOT_READ, broken.getProblems().get(0).getType());
        assertEquals("CPFA097", broken.getProblems().get(0).getText());

        Item util = items.get(6);
        assertTrue(util.isMigrated());
        assertEquals(ProblemType.MAKE_CHARACTERS, util.getProblems().get(0).getType());

        Item old = items.get(7);
        assertTrue(old.isMigrated());
        assertEquals("qrpglesrc/old.rpg38", old.getPath());
        assertEquals(ProblemType.UNKNOWN_SOURCE_TYPE, old.getProblems().get(0).getType());

        assertEquals(6, plan.getMigratedItems().size());
    }

    @Test
    public void testPlan_includeProblems() {

        add("QRPGLESRC", "HELLO", "RPGLE", null, "**FREE\n/copy QRPGLESRC,NOTYPE\n/copy QOTHER,X\n");
        add("QRPGLESRC", "NOTYPE", null, null, "x\n");

        Item hello = createPlan(false).getItems().get(0);

        assertTrue(hello.isMigrated());
        assertEquals("qrpglesrc/hello.rpgle", hello.getPath());
        assertEquals(2, hello.getProblems().size());
        assertEquals(ProblemType.INCLUDE, hello.getProblems().get(0).getType());
        assertEquals(2, hello.getProblems().get(0).getLineNumber());
        assertEquals(IncludeConverter.Problem.NOT_TRANSFERRED, hello.getProblems().get(0).getIncludeProblem());
        assertEquals(IncludeConverter.Problem.NOT_TRANSFERRED, hello.getProblems().get(1).getIncludeProblem());
    }

    @Test
    public void testPlan_sqlIncludeFile_ownSourceFile() {

        add("QRPGLESRC", "HELLO", "SQLRPGLE", null, "**FREE\nexec sql include HOSTVARS;\n");
        add("QRPGLESRC", "HOSTVARS", "SQLRPGLE", null, "**FREE\ndcl-s x char(1);\n");

        List<Item> items = createPlan(false).getItems();

        assertEquals("**FREE\nexec sql include 'qrpglesrc/hostvars.sqlrpgleinc';\n", items.get(0).getContent());
        assertTrue(items.get(0).getProblems().isEmpty());
        assertEquals(Arrays.asList("qrpglesrc/hostvars.sqlrpgleinc"), items.get(0).getIncludedPaths());
        // The included member is a copy book
        assertEquals("qrpglesrc/hostvars.sqlrpgleinc", items.get(1).getPath());
    }

    @Test
    public void testPlan_sqlIncludeFile_otherSourceFile() {

        // The members of all statements have to be in one source file, so
        // QCPYSRC is found, although HOSTVARS is in QRPGLESRC, too.
        add("QRPGLESRC", "HELLO", "SQLRPGLE", null, "**FREE\nexec sql include HOSTVARS;\nexec sql include CURSORS;\n");
        add("QRPGLESRC", "HOSTVARS", "SQLRPGLE", null, "**FREE\n");
        add("QCPYSRC", "HOSTVARS", "SQLRPGLE", null, "**FREE\n");
        add("QCPYSRC", "CURSORS", "SQLRPGLE", null, "**FREE\n");

        List<Item> items = createPlan(false).getItems();

        assertEquals("**FREE\nexec sql include 'qcpysrc/hostvars.sqlrpgleinc';\nexec sql include 'qcpysrc/cursors.sqlrpgleinc';\n",
            items.get(0).getContent());
        assertEquals("qrpglesrc/hostvars.sqlrpgle", items.get(1).getPath());
        assertEquals("qcpysrc/hostvars.sqlrpgleinc", items.get(2).getPath());
    }

    @Test
    public void testPlan_sqlIncludeFile_header() {

        // INCFILE of the header of the build tool wins over the own source file.
        add("QRPGLESRC", "HELLO", "SQLRPGLE", null, "**FREE\n" //
            + "// BUILD: CRTSQLRPGI OBJ(&LI/&OB) SRCSTMF('&STMF') INCFILE(&SL/QCPYSRC) RPGPPOPT(*LVL2)\n" //
            + "exec sql include HOSTVARS;\n");
        add("QRPGLESRC", "HOSTVARS", "SQLRPGLE", null, "**FREE\n");
        add("QCPYSRC", "HOSTVARS", "SQLRPGLE", null, "**FREE\n");

        List<Item> items = createPlan(false).getItems();

        assertTrue(items.get(0).getContent().contains("exec sql include 'qcpysrc/hostvars.sqlrpgleinc';"));
        assertEquals("qcpysrc/hostvars.sqlrpgleinc", items.get(2).getPath());
    }

    @Test
    public void testPlan_sqlIncludeFile_unknown() {

        // HOSTVARS is in two source files, MISSING in none
        add("QRPGLESRC", "HELLO", "SQLRPGLE", null, "**FREE\nexec sql include HOSTVARS;\nexec sql include MISSING;\n");
        add("QCPYSRC", "HOSTVARS", "SQLRPGLE", null, "**FREE\n");
        add("QCLSRC", "HOSTVARS", "SQLRPGLE", null, "**FREE\n");

        Item hello = createPlan(false).getItems().get(0);

        assertEquals("**FREE\nexec sql include HOSTVARS;\nexec sql include MISSING;\n", hello.getContent());
        assertTrue(hello.isMigrated());
        assertEquals(2, hello.getProblems().size());
        assertEquals(ProblemType.INCLUDE, hello.getProblems().get(0).getType());
        assertEquals(IncludeConverter.Problem.AMBIGUOUS, hello.getProblems().get(0).getIncludeProblem());
        assertEquals("HOSTVARS", hello.getProblems().get(0).getText());
        assertEquals(IncludeConverter.Problem.AMBIGUOUS, hello.getProblems().get(1).getIncludeProblem());
    }

    @Test
    public void testPlan_noMemberText() {

        add("QRPGLESRC", "HELLO", "RPGLE", "Hello world", "**FREE\n");

        assertEquals("**FREE\n", createPlan(false).getItems().get(0).getContent());
    }

    private MigrationPlan createPlan(boolean anIsMemberText) {
        return new MigrationPlan("jsmith#cl", Arrays.asList("QRPGLESRC", "QCPYSRC", "QCLSRC"), members, contents, readErrors,
            new MigrationSettings(IncludeStyle.REPLACE, anIsMemberText, 1141), new SourceHeaderParsers(new TestHeaderParser()));
    }

    private void add(String aSourceFile, String aName, String aSourceType, String aText, String aContent) {

        SourceMember member = new SourceMember(aSourceFile, aName, aSourceType, aText);
        members.add(member);
        if (aContent != null) {
            contents.put(member.getKey(), aContent);
        }
    }
}
