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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import de.tools400.ifsprojects.syntaxcheck.EventsFiles;

public class EventsFilesTest {

    /**
     * Events file of CRTSQLRPGI OBJTYPE(*MODULE) of a program source (IBM i
     * 7.5).
     */
    private static final String EVENTS = "TIMESTAMP  0 20261013191128\r\n" //
        + "PROCESSOR  0 999 1\r\n" //
        + "FILEID     0 999 000000 022 QTEMP/QSQLTEMP1(EVFT2) 20261013191128 0\r\n" //
        + "FILEID     0 001 000000 031 /tmp/evftest/src/evft2.sqlrpgle 20261013190933 0\r\n" //
        + "EXPANSION  0 001 000000 000000 999 000005 000078\r\n" //
        + "FILEEND    0 001 000008\r\n" //
        + "FILEEND    0 999 000090\r\n" //
        + "PROCESSOR  0 000 1\r\n" //
        + "FILEID     0 001 000000 044 /QSYS.LIB/QTEMP.LIB/QSQLTEMP1.FILE/EVFT2.MBR 20261013191128 0\r\n" //
        + "ERROR      0 001 1 000002 000002 009 000002 017 RNF1324 E 20 054 Keywords DFTACTGRP, ACTGRP, or USRPRF are not allowed.\r\n" //
        + "ERROR      0 001 1 000089 000089 001 000089 013 RNF7030 S 30 051 The name or indicator UNDEFINEDVAR2 is not defined.\r\n" //
        + "FILEEND    0 001 000090\r\n";

    @Test
    public void testRemoveMessages() {

        String expected = "TIMESTAMP  0 20261013191128\n" //
            + "PROCESSOR  0 999 1\n" //
            + "FILEID     0 999 000000 022 QTEMP/QSQLTEMP1(EVFT2) 20261013191128 0\n" //
            + "FILEID     0 001 000000 031 /tmp/evftest/src/evft2.sqlrpgle 20261013190933 0\n" //
            + "EXPANSION  0 001 000000 000000 999 000005 000078\n" //
            + "FILEEND    0 001 000008\n" //
            + "FILEEND    0 999 000090\n" //
            + "PROCESSOR  0 000 1\n" //
            + "FILEID     0 001 000000 044 /QSYS.LIB/QTEMP.LIB/QSQLTEMP1.FILE/EVFT2.MBR 20261013191128 0\n" //
            + "ERROR      0 001 1 000089 000089 001 000089 013 RNF7030 S 30 051 The name or indicator UNDEFINEDVAR2 is not defined.\n" //
            + "FILEEND    0 001 000090\n";

        assertEquals(expected, EventsFiles.removeMessages(EVENTS, Collections.singleton("RNF1324")));
    }

    @Test
    public void testRemoveMessages_continuationLines() {

        String events = "ERROR      0 001 1 000001 000001 001 000001 001 RNF1324 E 20 300 A long message\n" //
            + "that continues on the next line.\n" //
            + "ERROR      0 001 1 000002 000002 001 000002 001 RNF7030 S 30 300 Another long message\n" //
            + "that continues as well.\n" //
            + "FILEEND    0 001 000002\n";

        assertEquals("ERROR      0 001 1 000002 000002 001 000002 001 RNF7030 S 30 300 Another long message\n" //
            + "that continues as well.\n" //
            + "FILEEND    0 001 000002\n", EventsFiles.removeMessages(events, Collections.singleton("RNF1324")));
    }

    @Test
    public void testRemoveMessages_nothingToRemove() {
        assertEquals(EVENTS, EventsFiles.removeMessages(EVENTS, new HashSet<String>()));
        assertEquals(EVENTS, EventsFiles.removeMessages(EVENTS, (Set<String>)null));
        assertNull(EventsFiles.removeMessages(null, Collections.singleton("RNF1324")));
    }
}
