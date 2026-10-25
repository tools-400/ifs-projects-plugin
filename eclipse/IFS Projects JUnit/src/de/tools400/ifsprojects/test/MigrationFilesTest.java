/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import de.tools400.ifsprojects.migration.IncludeStyle;
import de.tools400.ifsprojects.migration.MigrationFiles;
import de.tools400.ifsprojects.migration.MigrationPlan;
import de.tools400.ifsprojects.migration.MigrationSettings;
import de.tools400.ifsprojects.migration.SourceFile;
import de.tools400.ifsprojects.migration.SourceMember;
import de.tools400.ifsprojects.sources.headers.SourceHeaderParsers;

public class MigrationFilesTest {

    private static final List<SourceFile> SOURCE_FILES = Arrays.asList(new SourceFile("JSMITH#CL", "QRPGLESRC", null, 273, 100, 2),
        new SourceFile("JSMITH#CL", "QCPYSRC", null, 65535, 100, 1), new SourceFile("JSMITH#CL", "QEMPTY", null, 273, 100, 0));

    @Test
    public void testGetFiles_tobi() {

        Map<String, String> files = MigrationFiles.getFiles(createPlan(), SOURCE_FILES, true);

        assertEquals(Arrays.asList("qrpglesrc/hello.rpgle", "qcpysrc/consts.rpgleinc", "qrpglesrc/.ibmi.json", "qrpglesrc/Rules.mk",
            "qcpysrc/.ibmi.json", "qcpysrc/Rules.mk", "Rules.mk"), new ArrayList<>(files.keySet()));
        assertEquals("**FREE\n/copy 'qcpysrc/consts.rpgleinc'\n", files.get("qrpglesrc/hello.rpgle"));
        assertTrue(files.get("qcpysrc/.ibmi.json").contains("\"tgtCcsid\": \"1141\""));
        assertTrue(files.get("Rules.mk").endsWith("SUBDIRS = qcpysrc qrpglesrc\n"));
    }

    @Test
    public void testGetFiles_gmake() {

        Map<String, String> files = MigrationFiles.getFiles(createPlan(), SOURCE_FILES, false);

        assertEquals(Arrays.asList("qrpglesrc/hello.rpgle", "qcpysrc/consts.rpgleinc"), new ArrayList<>(files.keySet()));
    }

    private static MigrationPlan createPlan() {

        List<SourceMember> members = new ArrayList<>();
        Map<String, String> contents = new HashMap<>();
        add(members, contents, "QRPGLESRC", "HELLO", "RPGLE", "**FREE\n/copy QCPYSRC,CONSTS\n");
        add(members, contents, "QRPGLESRC", "NOTYPE", null, "x\n");
        add(members, contents, "QCPYSRC", "CONSTS", "RPGLE", "**FREE\n");

        return new MigrationPlan("JSMITH#CL", Arrays.asList("QRPGLESRC", "QCPYSRC", "QEMPTY"), members, contents, new HashMap<String, String>(),
            new MigrationSettings(IncludeStyle.REPLACE, false, 1141), new SourceHeaderParsers(new TestHeaderParser()));
    }

    private static void add(List<SourceMember> aMembers, Map<String, String> aContents, String aSourceFile, String aName, String aSourceType,
        String aContent) {
        SourceMember member = new SourceMember(aSourceFile, aName, aSourceType, null);
        aMembers.add(member);
        aContents.put(member.getKey(), aContent);
    }
}
