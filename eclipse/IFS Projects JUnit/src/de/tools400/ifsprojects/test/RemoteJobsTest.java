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
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;

import de.tools400.ifsprojects.remote.RemoteJobs;

public class RemoteJobsTest {

    private static final long NOW = 1791972000000L;
    private static final long AGE = RemoteJobs.OBSOLETE_FILE_AGE_MILLIS;

    @Test
    public void testIsObsoleteTemporaryFile_age() throws Exception {

        assertTrue(RemoteJobs.isObsoleteTemporaryFile("ifsprojects_1791885599000_42.log", NOW - AGE - 1, NOW));
        assertFalse(RemoteJobs.isObsoleteTemporaryFile("ifsprojects_1791885600000_42.log", NOW - AGE, NOW));
        assertFalse(RemoteJobs.isObsoleteTemporaryFile("ifsprojects_1791971000000_42.rc", NOW - 1000000, NOW));
    }

    @Test
    public void testIsObsoleteTemporaryFile_otherFiles() throws Exception {

        assertFalse(RemoteJobs.isObsoleteTemporaryFile("strrsesvr.properties", NOW - 2 * AGE, NOW));
        assertFalse(RemoteJobs.isObsoleteTemporaryFile("IFSPROJECTS_1791885599000_42.log", NOW - 2 * AGE, NOW));
        assertFalse(RemoteJobs.isObsoleteTemporaryFile(null, NOW - 2 * AGE, NOW));
    }

    @Test
    public void testIsObsoleteTemporaryFile_unknownTime() throws Exception {

        assertFalse(RemoteJobs.isObsoleteTemporaryFile("ifsprojects_1791885599000_42.pid", 0, NOW));
    }

    @Test
    public void testGetDirectories() throws Exception {

        assertEquals("/home/jsmith/.ifsprojects", RemoteJobs.getRootDirectory("/home/jsmith"));
        assertEquals("/home/jsmith/.ifsprojects", RemoteJobs.getRootDirectory("/home/jsmith/"));
        assertEquals("/home/jsmith/.ifsprojects/tmp", RemoteJobs.getTemporaryDirectory("/home/jsmith"));
        assertEquals("/home/jsmith/.ifsprojects/builds", RemoteJobs.getBuildsDirectory("/home/jsmith"));
        assertEquals("/.ifsprojects/tmp", RemoteJobs.getTemporaryDirectory("/"));
    }

    @Test
    public void testGetTemporaryFile() throws Exception {

        assertEquals("/home/jsmith/.ifsprojects/tmp/ifsprojects_", RemoteJobs.getTemporaryFilePrefix("/home/jsmith/.ifsprojects/tmp"));
        assertEquals("/tmp/ifsprojects_1791972000000_42.rc", RemoteJobs.getTemporaryFile("/tmp", "1791972000000_42", ".rc"));
        assertTrue(RemoteJobs.createTemporaryFileId().matches("[0-9]+_[0-9]+"));
        assertTrue(RemoteJobs.isObsoleteTemporaryFile(new File(RemoteJobs.getTemporaryFile("/tmp", ".log")).getName(), NOW - 2 * AGE, NOW));
    }
}
