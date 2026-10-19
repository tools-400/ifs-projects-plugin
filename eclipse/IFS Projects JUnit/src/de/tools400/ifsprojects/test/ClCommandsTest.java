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

import org.junit.Test;

import de.tools400.ifsprojects.ibmi.ClCommands;

public class ClCommandsTest {

    @Test
    public void testIsObjectName() {
        assertTrue(ClCommands.isObjectName("JSMITH#CL"));
        assertTrue(ClCommands.isObjectName("$A_1"));
        assertFalse(ClCommands.isObjectName("jsmith"));
        assertFalse(ClCommands.isObjectName("1ABC"));
        assertFalse(ClCommands.isObjectName("_ABC"));
        assertFalse(ClCommands.isObjectName("ABCDEFGHIJK"));
        assertFalse(ClCommands.isObjectName(null));
    }

    @Test
    public void testQuote() {
        assertEquals("'/home/jsmith'", ClCommands.quote("/home/jsmith"));
        assertEquals("'it''s'", ClCommands.quote("it's"));
        assertEquals("''", ClCommands.quote(""));
    }

    @Test
    public void testGetMemberPath() {
        assertEquals("/QSYS.LIB/JSMITH#CL.LIB", ClCommands.getLibraryPath("JSMITH#CL"));
        assertEquals("/QSYS.LIB/JSMITH#CL.LIB/QRPGLESRC.FILE", ClCommands.getFilePath("JSMITH#CL", "QRPGLESRC"));
        assertEquals("/QSYS.LIB/JSMITH#CL.LIB/QRPGLESRC.FILE/HELLO.MBR", ClCommands.getMemberPath("JSMITH#CL", "QRPGLESRC", "HELLO"));
    }
}
