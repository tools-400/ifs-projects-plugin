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
    public void testPlan_noMemberText() {

        add("QRPGLESRC", "HELLO", "RPGLE", "Hello world", "**FREE\n");

        assertEquals("**FREE\n", createPlan(false).getItems().get(0).getContent());
    }

    private MigrationPlan createPlan(boolean anIsMemberText) {
        return new MigrationPlan("jsmith#cl", Arrays.asList("QRPGLESRC", "QCPYSRC", "QCLSRC"), members, contents, readErrors,
            new MigrationSettings(IncludeStyle.REPLACE, anIsMemberText, 1141));
    }

    private void add(String aSourceFile, String aName, String aSourceType, String aText, String aContent) {

        SourceMember member = new SourceMember(aSourceFile, aName, aSourceType, aText);
        members.add(member);
        if (aContent != null) {
            contents.put(member.getKey(), aContent);
        }
    }
}
