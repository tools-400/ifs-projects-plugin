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
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

import de.tools400.ifsprojects.build.LogLines;

public class LogLinesTest {

    @Test
    public void testAdd_chunks() throws Exception {

        LogLines logLines = new LogLines();

        assertEquals(Arrays.asList("=== Creating Bound RPG Program"), logLines.add("=== Creating Bound RPG Program\nCRTBND".getBytes("UTF-8")));
        assertTrue(logLines.add("RPG srcstmf(".getBytes("UTF-8")).isEmpty());
        assertEquals(Arrays.asList("CRTBNDRPG srcstmf('x')", ""), logLines.add("'x')\r\n\n".getBytes("UTF-8")));
        assertNull(logLines.flush());
    }

    @Test
    public void testAdd_utf8() throws Exception {

        LogLines logLines = new LogLines();

        assertEquals(Arrays.asList("\u2713 IFSTOK.PGM was created successfully!"),
            logLines.add("\u2713 IFSTOK.PGM was created successfully!\n".getBytes("UTF-8")));
    }

    @Test
    public void testAdd_iso88591() throws Exception {

        LogLines logLines = new LogLines();

        assertEquals(Arrays.asList("Ausdruck enth\u00e4lt einen Operanden"), logLines.add("Ausdruck enth\u00e4lt einen Operanden\n".getBytes("ISO-8859-1")));
    }

    @Test
    public void testFlush() throws Exception {

        LogLines logLines = new LogLines();
        logLines.add("Build Failed!".getBytes("UTF-8"));

        assertEquals("Build Failed!", logLines.flush());
        assertNull(logLines.flush());
    }
}
