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

import org.junit.Test;

import de.tools400.ifsprojects.project.BuildTool;

public class BuildToolTest {

    @Test
    public void testCommands() {
        assertEquals("gmake", BuildTool.GMAKE.getLabel());
        assertEquals("TOBi (makei)", BuildTool.TOBI.getLabel());
        assertEquals("gmake", BuildTool.GMAKE.getBuildCommand());
        assertEquals("gmake {filename}", BuildTool.GMAKE.getCompileCommand());
        assertEquals("makei build", BuildTool.TOBI.getBuildCommand());
        assertEquals("makei compile -f {filename}", BuildTool.TOBI.getCompileCommand());
    }

    @Test
    public void testFind() {
        assertEquals(BuildTool.GMAKE, BuildTool.find("gmake", "gmake {filename}"));
        assertEquals(BuildTool.TOBI, BuildTool.find(" makei build ", "makei compile -f {filename}\t"));
    }

    @Test
    public void testFind_noMatch() {
        assertNull(BuildTool.find("gmake", "makei compile -f {filename}"));
        assertNull(BuildTool.find("makei build", ""));
        assertNull(BuildTool.find("gmake all", "gmake {filename}"));
        assertNull(BuildTool.find(null, "gmake {filename}"));
        assertNull(BuildTool.find("gmake", null));
    }

    @Test
    public void testFindExisting() {
        assertEquals(BuildTool.GMAKE, BuildTool.findExisting("gmake", null));
        assertEquals(BuildTool.GMAKE, BuildTool.findExisting(" gmake ", ""));
        assertEquals(BuildTool.GMAKE, BuildTool.findExisting("gmake all", null));
        assertEquals(BuildTool.GMAKE, BuildTool.findExisting("/QOpenSys/pkgs/bin/gmake -f Makefile", null));
        assertEquals(BuildTool.TOBI, BuildTool.findExisting("makei build", " "));
        assertEquals(BuildTool.TOBI, BuildTool.findExisting("makei b", null));
        assertEquals(BuildTool.TOBI, BuildTool.findExisting("cd src && makei build", null));
    }

    @Test
    public void testFindExisting_buildCommandDecides() {
        assertEquals(BuildTool.TOBI, BuildTool.findExisting("makei build", "makei compile -f {filename}"));
        assertEquals(BuildTool.GMAKE, BuildTool.findExisting("gmake", "makei compile -f {filename}"));
        assertEquals(BuildTool.TOBI, BuildTool.findExisting(null, "makei c -f {filename}"));
        assertEquals(BuildTool.GMAKE, BuildTool.findExisting(" ", "gmake {filename}"));
    }

    @Test
    public void testFindExisting_noMatch() {
        assertNull(BuildTool.findExisting("make", null));
        assertNull(BuildTool.findExisting("make -f makefile", null));
        assertNull(BuildTool.findExisting("xgmake", null));
        assertNull(BuildTool.findExisting("gmake-4.3", null));
        assertNull(BuildTool.findExisting("makeit", null));
        assertNull(BuildTool.findExisting("makei build && gmake docs", null));
        assertNull(BuildTool.findExisting(null, null));
        assertNull(BuildTool.findExisting("", ""));
    }
}
