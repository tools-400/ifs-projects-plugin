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

import de.tools400.ifsprojects.local.Deployment;
import de.tools400.ifsprojects.local.LocalProject;

public class LocalProjectTest {

    @Test
    public void testGetDefaultDeployDirectory() {

        assertEquals("/home/jsmith/.ifsprojects/builds/ifsptest", LocalProject.getDefaultDeployDirectory("/home/jsmith", "ifsptest"));
        assertEquals("/home/jsmith/.ifsprojects/builds/my_project_1.0-a",
            LocalProject.getDefaultDeployDirectory("/home/jsmith/", "my project#1.0-a"));
    }

    @Test
    public void testNormalizeDeployDirectory() {

        assertEquals("", LocalProject.normalizeDeployDirectory(null));
        assertEquals("", LocalProject.normalizeDeployDirectory("  "));
        assertEquals("/home/jsmith/builds/demo", LocalProject.normalizeDeployDirectory(" /home/jsmith/builds/demo/ "));
        assertNull(LocalProject.normalizeDeployDirectory("/"));
        assertNull(LocalProject.normalizeDeployDirectory("builds/demo"));
        assertNull(LocalProject.normalizeDeployDirectory("/home/jsmith/*"));
    }

    @Test
    public void testGetCcsid() {

        assertEquals(1208, Deployment.getCcsid("UTF-8"));
        assertEquals(1252, Deployment.getCcsid("Cp1252"));
        assertEquals(1252, Deployment.getCcsid("windows-1252"));
        assertEquals(819, Deployment.getCcsid("ISO-8859-1"));
        assertEquals(923, Deployment.getCcsid("ISO-8859-15"));
        assertEquals(367, Deployment.getCcsid("US-ASCII"));
        assertEquals(1208, Deployment.getCcsid("UTF-16"));
        assertEquals(1208, Deployment.getCcsid("unknown"));
        assertEquals(1208, Deployment.getCcsid(null));
    }
}
