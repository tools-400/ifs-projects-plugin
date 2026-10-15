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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import de.tools400.ifsprojects.local.DeployManifest;
import de.tools400.ifsprojects.local.Deployment;

public class DeployManifestTest {

    @Test
    public void testWriteAndRead() throws Exception {

        DeployManifest manifest = new DeployManifest();
        manifest.setToken("8d1f6a2e-0c4b-4e57-9a3e-2f1d6c7b8a90");
        manifest.putEntry("src/hello.rpgle", new DeployManifest.Entry(120, 1791972000000L, 1208, "0cc175b9c0f1b6a831c399e269772661"));
        manifest.putEntry("inc/umlaut \u00e4\u00f6\u00fc=x.rpgleinc", new DeployManifest.Entry(7, 1791972000001L, 1252, "92eb5ffee6ae2fec3ad71c777531578f"));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        manifest.write(outputStream);
        DeployManifest copy = DeployManifest.read(new ByteArrayInputStream(outputStream.toByteArray()));

        assertEquals("8d1f6a2e-0c4b-4e57-9a3e-2f1d6c7b8a90", copy.getToken());
        assertEquals(Arrays.asList("inc/umlaut \u00e4\u00f6\u00fc=x.rpgleinc", "src/hello.rpgle"), copy.getPaths());

        DeployManifest.Entry entry = copy.getEntry("inc/umlaut \u00e4\u00f6\u00fc=x.rpgleinc");
        assertEquals(7, entry.getSize());
        assertEquals(1791972000001L, entry.getModified());
        assertEquals(1252, entry.getCcsid());
        assertEquals("92eb5ffee6ae2fec3ad71c777531578f", entry.getHash());
    }

    @Test
    public void testWriteAndRead_remoteFile() throws Exception {

        DeployManifest manifest = new DeployManifest();
        manifest.putEntry("a", new DeployManifest.Entry(1, 2, 1208, "x").withRemoteFile(new DeployManifest.RemoteFile(3, 4)));
        manifest.putEntry("b", new DeployManifest.Entry(1, 2, 1208, "y"));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        manifest.write(outputStream);
        DeployManifest copy = DeployManifest.read(new ByteArrayInputStream(outputStream.toByteArray()));

        assertTrue(copy.getEntry("a").hasRemoteFile());
        assertEquals(3, copy.getEntry("a").getRemoteSize());
        assertEquals(4, copy.getEntry("a").getRemoteModified());
        assertFalse(copy.getEntry("b").hasRemoteFile());
    }

    @Test
    public void testRead_firstVersion() throws Exception {

        // Entries of the first version have no size and time of the stream file.
        String properties = "token=t\nfile.src/hello.rpgle=120,1791972000000,1208,0cc175b9c0f1b6a831c399e269772661\n";
        DeployManifest manifest = DeployManifest.read(new ByteArrayInputStream(properties.getBytes("ISO-8859-1")));

        DeployManifest.Entry entry = manifest.getEntry("src/hello.rpgle");
        assertEquals(120, entry.getSize());
        assertFalse(entry.hasRemoteFile());
    }

    @Test
    public void testRemoteFiles() {

        DeployManifest manifest = new DeployManifest();
        manifest.putEntry("unchanged", new DeployManifest.Entry(1, 2, 1208, "a").withRemoteFile(new DeployManifest.RemoteFile(10, 100)));
        manifest.putEntry("size", new DeployManifest.Entry(1, 2, 1208, "b").withRemoteFile(new DeployManifest.RemoteFile(10, 100)));
        manifest.putEntry("time", new DeployManifest.Entry(1, 2, 1208, "c").withRemoteFile(new DeployManifest.RemoteFile(10, 100)));
        manifest.putEntry("missing", new DeployManifest.Entry(1, 2, 1208, "d").withRemoteFile(new DeployManifest.RemoteFile(10, 100)));
        manifest.putEntry("unknown", new DeployManifest.Entry(1, 2, 1208, "e"));

        Map<String, DeployManifest.RemoteFile> remoteFiles = new HashMap<String, DeployManifest.RemoteFile>();
        remoteFiles.put("unchanged", new DeployManifest.RemoteFile(10, 100));
        remoteFiles.put("size", new DeployManifest.RemoteFile(11, 100));
        remoteFiles.put("time", new DeployManifest.RemoteFile(10, 101));
        remoteFiles.put("unknown", new DeployManifest.RemoteFile(10, 100));
        remoteFiles.put("other", new DeployManifest.RemoteFile(10, 100));

        assertEquals(Arrays.asList("size", "time"), manifest.getChangedRemoteFiles(remoteFiles));
        assertEquals(Arrays.asList("missing"), manifest.getMissingRemoteFiles(remoteFiles));

        manifest.setRemoteFiles(remoteFiles);
        assertTrue(manifest.getChangedRemoteFiles(remoteFiles).isEmpty());
        assertEquals(10, manifest.getEntry("unknown").getRemoteSize());
        assertEquals("e", manifest.getEntry("unknown").getHash());
        assertEquals(100, manifest.getEntry("missing").getRemoteModified());
    }

    @Test
    public void testRead_invalidEntries() throws Exception {

        String properties = "file.a=1,2,1208,abc\nfile.b=1,2,1208\nfile.c=x,2,1208,abc\nfile.=1,2,1208,abc\nother=1\n";
        DeployManifest manifest = DeployManifest.read(new ByteArrayInputStream(properties.getBytes("ISO-8859-1")));

        assertNull(manifest.getToken());
        assertEquals(Arrays.asList("a"), manifest.getPaths());
    }

    @Test
    public void testIsUnchanged() {

        DeployManifest.Entry entry = new DeployManifest.Entry(120, 1791972000000L, 1208, "abc");

        assertTrue(entry.isUnchanged(120, 1791972000000L, 1208));
        assertFalse(entry.isUnchanged(121, 1791972000000L, 1208));
        assertFalse(entry.isUnchanged(120, 1791972000001L, 1208));
        assertFalse(entry.isUnchanged(120, 1791972000000L, 1252));
    }

    @Test
    public void testRemove() {

        DeployManifest manifest = new DeployManifest();
        manifest.putEntry("a", new DeployManifest.Entry(1, 2, 1208, "x"));
        manifest.putEntry("b", new DeployManifest.Entry(1, 2, 1208, "y"));

        manifest.removeEntry("a");
        assertEquals(Arrays.asList("b"), manifest.getPaths());

        manifest.removeAllEntries();
        assertTrue(manifest.getPaths().isEmpty());
    }

    @Test
    public void testGetManifestName() {

        String name = Deployment.getManifestName("MYIBMI", "/home/jsmith/.ifsprojects/builds/ifstest", "ifstest");

        assertTrue(name.matches("[0-9a-f]{40}\\.manifest"));
        assertEquals(name, Deployment.getManifestName("MYIBMI", "/home/jsmith/.ifsprojects/builds/ifstest", "ifstest"));
        assertNotEquals(name, Deployment.getManifestName("MYIBMI2", "/home/jsmith/.ifsprojects/builds/ifstest", "ifstest"));
        assertNotEquals(name, Deployment.getManifestName("MYIBMI", "/home/jsmith/builds/ifstest", "ifstest"));
        assertNotEquals(name, Deployment.getManifestName("MYIBMI", "/home/jsmith/.ifsprojects/builds/ifstest", "ifstest2"));
    }
}
