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

import org.eclipse.ui.IMemento;
import org.eclipse.ui.XMLMemento;
import org.junit.Test;

import de.tools400.ifsprojects.terminal.TerminalRequest;
import de.tools400.ifsprojects.terminal.tm.TerminalRequestMemento;

/**
 * The mementos are not written to XML and parsed again, because the XML parser
 * of the class path of the tests may not support the properties, that
 * XMLMemento.createReadRoot() sets. An incomplete memento is not tested,
 * because restore() logs a warning, which loads the plug-in class.
 */
public class TerminalRequestMementoTest {

    @Test
    public void testSaveAndRestore() {

        TerminalRequest request = TerminalRequest.builder().connectionName("MYIBMI").hostName("ibmi.example.com").port(2222).userId("jsmith")
            .password("secret").encoding("ISO-8859-1").timeout(10).keepAlive(60).directory("/home/jsmith/src").command("gmake")
            .reuseKey("build:MYIBMI:/home/jsmith/src").build();

        IMemento memento = save(request);
        TerminalRequest restored = TerminalRequestMemento.restore(memento);

        assertEquals("MYIBMI", restored.getConnectionName());
        assertEquals("ibmi.example.com", restored.getHostName());
        assertEquals(2222, restored.getPort());
        assertEquals("jsmith", restored.getUserId());
        assertEquals("ISO-8859-1", restored.getEncoding());
        assertEquals(10, restored.getTimeout());
        assertEquals(60, restored.getKeepAlive());
        assertEquals("/home/jsmith/src", restored.getDirectory());
        assertEquals("build:MYIBMI:/home/jsmith/src", restored.getReuseKey());
    }

    @Test
    public void testPasswordAndCommandAreNotSaved() {

        TerminalRequest request = TerminalRequest.builder().hostName("ibmi.example.com").userId("jsmith").password("secret").command("gmake").build();

        IMemento memento = save(request);
        TerminalRequest restored = TerminalRequestMemento.restore(memento);

        assertNull(restored.getPassword());
        assertNull(restored.getCommand());
        for (String key : memento.getAttributeKeys()) {
            assertFalse(key, "secret".equals(memento.getString(key)));
            assertFalse(key, "gmake".equals(memento.getString(key)));
        }
    }

    @Test
    public void testRestore_oldMemento() {

        // Memento of a version without encoding, timeout, keep alive and
        // reuse key.
        IMemento memento = XMLMemento.createWriteRoot("session");
        memento.putString("connectionName", "MYIBMI");
        memento.putString("hostName", "ibmi.example.com");
        memento.putInteger("port", 22);
        memento.putString("userId", "jsmith");

        TerminalRequest restored = TerminalRequestMemento.restore(memento);

        assertEquals("MYIBMI", restored.getConnectionName());
        assertEquals("jsmith", restored.getUserId());
        assertEquals(TerminalRequest.DEFAULT_ENCODING, restored.getEncoding());
        assertEquals(TerminalRequest.DEFAULT_TIMEOUT, restored.getTimeout());
        assertEquals(TerminalRequest.DEFAULT_KEEP_ALIVE, restored.getKeepAlive());
        assertNull(restored.getDirectory());
        assertNull(restored.getReuseKey());
    }

    private IMemento save(TerminalRequest aRequest) {

        IMemento memento = XMLMemento.createWriteRoot("session");
        TerminalRequestMemento.save(memento, aRequest);

        return memento;
    }
}
