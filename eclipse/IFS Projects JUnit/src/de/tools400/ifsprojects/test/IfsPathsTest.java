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

import de.tools400.ifsprojects.ibmi.IfsPaths;

public class IfsPathsTest {

    @Test
    public void testNormalizeDirectory() {
        assertEquals("/home/jsmith", IfsPaths.normalizeDirectory("/home/jsmith/"));
        assertEquals("/", IfsPaths.normalizeDirectory("/"));
        assertEquals("/", IfsPaths.normalizeDirectory("///"));
        assertNull(IfsPaths.normalizeDirectory("/home/*"));
        assertNull(IfsPaths.normalizeDirectory("/home/jsmith?"));
        assertNull(IfsPaths.normalizeDirectory("home"));
        assertNull(IfsPaths.normalizeDirectory("  "));
        assertNull(IfsPaths.normalizeDirectory(null));
    }

    @Test
    public void testGetLastSegment() {
        assertEquals("src", IfsPaths.getLastSegment("/home/jsmith/src"));
        assertEquals("src", IfsPaths.getLastSegment("/home/jsmith/src/"));
        assertEquals("home", IfsPaths.getLastSegment("/home"));
        assertEquals("/", IfsPaths.getLastSegment("/"));
        assertNull(IfsPaths.getLastSegment("src"));
    }

    @Test
    public void testGetParent() {
        assertEquals("/home/jsmith", IfsPaths.getParent("/home/jsmith/src"));
        assertEquals("/home/jsmith", IfsPaths.getParent("/home/jsmith/src/"));
        assertEquals("/", IfsPaths.getParent("/home"));
        assertNull(IfsPaths.getParent("/"));
        assertNull(IfsPaths.getParent("src"));
        assertNull(IfsPaths.getParent(null));
    }

    @Test
    public void testGetFileName() {
        assertEquals("hello.rpgle", IfsPaths.getFileName("/home/jsmith/src/hello.rpgle"));
        assertEquals("hello.rpgle", IfsPaths.getFileName("src/hello.rpgle"));
        assertEquals("hello.rpgle", IfsPaths.getFileName("hello.rpgle"));
    }

    @Test
    public void testAppend() {
        assertEquals("/home/jsmith/src/iproj.json", IfsPaths.append("/home/jsmith/src", "iproj.json"));
        assertEquals("/home/jsmith/src/iproj.json", IfsPaths.append("/home/jsmith/src/", "iproj.json"));
        assertEquals("/iproj.json", IfsPaths.append("/", "iproj.json"));
    }
}
