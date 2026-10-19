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

import org.junit.Test;

import de.tools400.ifsprojects.rse.RseTerminalRequests;
import de.tools400.ifsprojects.terminal.TerminalRequest;

public class TerminalRequestTest {

    private TerminalRequest.Builder newBuilder() {
        return TerminalRequest.builder().connectionName("MYIBMI").hostName("ibmi.example.com").userId("jsmith");
    }

    @Test
    public void testHomeDirectory() {

        TerminalRequest request = newBuilder().build();

        assertEquals("MYIBMI: ~", request.getTitle());
        assertEquals("jsmith@ibmi.example.com:~", request.getToolTip());
        assertNull(request.getCommandLine());
        assertEquals(TerminalRequest.DEFAULT_PORT, request.getPort());
        assertEquals(TerminalRequest.DEFAULT_ENCODING, request.getEncoding());
    }

    @Test
    public void testDirectory() {

        TerminalRequest request = newBuilder().directory("/home/jsmith/src/").build();

        assertEquals("MYIBMI: src", request.getTitle());
        assertEquals("jsmith@ibmi.example.com:/home/jsmith/src/", request.getToolTip());
        assertEquals("cd '/home/jsmith/src/'\r", request.getCommandLine());
    }

    @Test
    public void testRootDirectory() {
        assertEquals("MYIBMI: /", newBuilder().directory("/").build().getTitle());
    }

    @Test
    public void testBuildCommand() {

        TerminalRequest request = newBuilder().directory("/home/jsmith/src").command("gmake").reuseKey("build:MYIBMI:/home/jsmith/src").build();

        assertEquals("cd '/home/jsmith/src' && gmake\r", request.getCommandLine());
        assertEquals("build:MYIBMI:/home/jsmith/src", request.getReuseKey());
    }

    @Test
    public void testToBuilder() {

        TerminalRequest request = newBuilder().port(2222).password("secret").directory("/tmp").command("ls").build();
        TerminalRequest copy = request.toBuilder().command(null).build();

        assertEquals(2222, copy.getPort());
        assertEquals("secret", copy.getPassword());
        assertEquals("/tmp", copy.getDirectory());
        assertNull(copy.getCommand());
    }

    @Test
    public void testSessionKey() {

        String home = newBuilder().build().getSessionKey();
        String src = newBuilder().directory("/home/jsmith/src").build().getSessionKey();

        assertEquals(src, newBuilder().directory("/home/jsmith/src/").build().getSessionKey());
        assertEquals(src, newBuilder().directory("/home/jsmith/src").command("gmake").build().getSessionKey());
        assertNotEquals(home, src);
        assertNotEquals(src, newBuilder().directory("/home/jsmith/doc").build().getSessionKey());
        assertNotEquals(src, newBuilder().connectionName("MYIBMI2").directory("/home/jsmith/src").build().getSessionKey());
        assertNotEquals(src, newBuilder().port(2222).directory("/home/jsmith/src").build().getSessionKey());
        assertNotEquals(src, newBuilder().userId("qsecofr").directory("/home/jsmith/src").build().getSessionKey());
        assertEquals("MYIBMI|ibmi.example.com|22|jsmith|/", newBuilder().directory("/").build().getSessionKey());
    }

    @Test
    public void testForceNew() {

        TerminalRequest request = newBuilder().build();
        assertFalse(request.isForceNew());

        TerminalRequest newRequest = request.toBuilder().forceNew(true).build();
        assertTrue(newRequest.isForceNew());
        assertTrue(newRequest.toBuilder().build().isForceNew());
        assertEquals(request.getSessionKey(), newRequest.getSessionKey());
    }

    @Test
    public void testConnectionNameDefaultsToHostName() {
        assertEquals("ibmi.example.com: ~", TerminalRequest.builder().hostName("ibmi.example.com").userId("jsmith").build().getTitle());
    }

    @Test(expected = IllegalStateException.class)
    public void testMissingUserId() {
        TerminalRequest.builder().hostName("ibmi.example.com").build();
    }

    @Test(expected = IllegalStateException.class)
    public void testMissingHostName() {
        TerminalRequest.builder().userId("jsmith").build();
    }

    @Test
    public void testSshUserId() {
        assertEquals("jsmith", RseTerminalRequests.toSshUserId("JSMITH", true));
        assertEquals("JSMITH", RseTerminalRequests.toSshUserId(" JSMITH ", false));
        assertNull(RseTerminalRequests.toSshUserId(null, true));
    }
}
