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

import org.junit.Test;

import de.tools400.ifsprojects.migration.MigrationMapping;
import de.tools400.ifsprojects.migration.SourceMember;

public class MigrationMappingTest {

    @Test
    public void testGetPath() {
        assertEquals("qrpglesrc/hello.rpgle", MigrationMapping.getPath(member("QRPGLESRC", "HELLO", "RPGLE"), false));
        assertEquals("qclsrc/start.clle", MigrationMapping.getPath(member("QCLSRC", "START", "CLLE"), false));
        assertEquals("qddssrc/hellod.dspf", MigrationMapping.getPath(member("QDDSSRC", "HELLOD", "DSPF"), false));
        assertEquals("qsrc/$util#1.rpgle", MigrationMapping.getPath(member("QSRC", "$UTIL#1", "RPGLE"), false));
        assertNull(MigrationMapping.getPath(member("QRPGLESRC", "NOTYPE", null), false));
    }

    @Test
    public void testGetPath_copyBook() {
        assertEquals("qcpysrc/consts.rpgleinc", MigrationMapping.getPath(member("QCPYSRC", "CONSTS", "RPGLE"), true));
        assertEquals("qcpysrc/hostvars.sqlrpgleinc", MigrationMapping.getPath(member("QCPYSRC", "HOSTVARS", "SQLRPGLE"), true));
        assertEquals("qclsrc/vars.clleinc", MigrationMapping.getPath(member("QCLSRC", "VARS", "CLLE"), true));
        assertEquals("qcpysrc/old.rpg38", MigrationMapping.getPath(member("QCPYSRC", "OLD", "RPG38"), true));
    }

    @Test
    public void testGetDirectory() {
        assertEquals("qrpglesrc", MigrationMapping.getDirectory("QRPGLESRC"));
    }

    @Test
    public void testHasMakeCharacters() {
        assertTrue(MigrationMapping.hasMakeCharacters("$UTIL"));
        assertTrue(MigrationMapping.hasMakeCharacters("UTIL#1"));
        assertFalse(MigrationMapping.hasMakeCharacters("UTIL@1"));
        assertFalse(MigrationMapping.hasMakeCharacters("HELLO_1"));
    }

    private static SourceMember member(String aSourceFile, String aName, String aSourceType) {
        return new SourceMember(aSourceFile, aName, aSourceType, null);
    }
}
