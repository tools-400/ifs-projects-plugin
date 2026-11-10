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

import de.tools400.ifsprojects.build.BuildProgress;

public class BuildProgressTest {

    @Test
    public void testAdd_step() {

        BuildProgress progress = new BuildProgress();
        assertNull(progress.getStep());

        assertFalse(progress.add(">> /QOpenSys/pkgs/bin/make -k BUILDVARSMKPATH=\"/tmp/tmpbue5gtyj\" -f Makefile all"));
        assertTrue(progress.add("=== Creating program [PEEK05] from modules [PEEK05] and service programs [PEEK51 PEEK53]"));
        assertEquals("Creating program [PEEK05] from modules [PEEK05] and service programs [PEEK51 PEEK53]", progress.getStep());

        assertFalse(progress.add("=== Creating program [PEEK05] from modules [PEEK05] and service programs [PEEK51 PEEK53]"));
        assertFalse(progress.add("crtpgm pgm(CLAUDEP6/PEEK05) module(PEEK05) ACTGRP(PEEK)"));
        assertEquals(0, progress.getCreatedCount());
        assertEquals(0, progress.getFailedCount());
    }

    @Test
    public void testAdd_created() {

        BuildProgress progress = new BuildProgress();

        assertTrue(progress.add("✓ PEEK05.MODULE was created successfully!"));
        assertTrue(progress.add("[SUCCESS] PEEK05.PGM was created successfully!"));
        assertTrue(progress.add("✓ PEEK.BNDDIR was registered successfully!"));
        assertEquals(3, progress.getCreatedCount());
    }

    @Test
    public void testAdd_customRecipeCountedOnce() {

        BuildProgress progress = new BuildProgress();

        assertTrue(progress.add("=== Creating [QCOPYLE.FILE] from custom recipe"));
        assertTrue(progress.add("=== Creating source file [QCOPYLE] in CLAUDEP6"));
        assertEquals("Creating source file [QCOPYLE] in CLAUDEP6", progress.getStep());
        assertTrue(progress.add("✓ QCOPYLE.FILE was created successfully!"));
        assertFalse(progress.add("✓ End of creating QCOPYLE.FILE!"));
        assertTrue(progress.add("✓ End of creating PEEKDST.CMD!"));
        assertEquals(2, progress.getCreatedCount());
    }

    @Test
    public void testAdd_failed() {

        BuildProgress progress = new BuildProgress();

        assertTrue(progress.add("✕ Failed to create PEEK05.MODULE!"));
        assertTrue(progress.add("[FAIL] Failed to create 'PEEK06.MODULE'!"));
        assertFalse(progress.add("✕ Failed to create PEEK05.MODULE!"));
        assertEquals(2, progress.getFailedCount());
        assertEquals(0, progress.getCreatedCount());
    }

    @Test
    public void testAdd_colored() {

        BuildProgress progress = new BuildProgress();

        assertTrue(progress.add("\u001B[32;49;1m✓ PEEK05.MODULE was created successfully!\u001B[0m"));
        assertTrue(progress.add("\u001B[31;49;1m✕ Failed to create PEEK06.MODULE!\u001B[0m"));
        assertTrue(progress.add("\u001B[0m=== Creating DSPF ['peek05fm.dspf'] in CLAUDEP6\u001B[0m"));
        assertEquals("Creating DSPF ['peek05fm.dspf'] in CLAUDEP6", progress.getStep());
        assertEquals(1, progress.getCreatedCount());
        assertEquals(1, progress.getFailedCount());
    }

    @Test
    public void testAdd_otherLines() {

        BuildProgress progress = new BuildProgress();

        assertFalse(progress.add(""));
        assertFalse(progress.add("✓"));
        assertFalse(progress.add("Objects:             0 failed 263 succeed 263 total"));
        assertFalse(progress.add("Build Successful!"));
        assertEquals(0, progress.getCreatedCount());
        assertEquals(0, progress.getFailedCount());
    }
}
