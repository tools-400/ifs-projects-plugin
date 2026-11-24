/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.iagentbridge.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import de.tools400.ifsprojects.iagentbridge.CompileReport;
import de.tools400.ifsprojects.iagentbridge.EventsFileMessages;

/**
 * Tests the reading of events files with the parser of RDi. The test data are
 * events files, that TOBi has written on an IBM i, with the paths of the user
 * replaced by <code>/home/jsmith/demo</code>.
 */
public class EventsFileMessagesTest {

    private static final int CCSID = 37;

    private static final String HELLO = "/home/jsmith/demo/ifsphello.pgm.rpgle";
    private static final String CONSTS = "/home/jsmith/demo/inc/consts.rpgleinc";
    private static final String SQL = "/home/jsmith/demo/ifspsql.pgm.sqlrpgle";
    private static final String UTILS = "/home/jsmith/demo/ifsputils.rpgle";

    @Test
    public void testParse_null() throws Exception {
        assertTrue(EventsFileMessages.parse(null, CCSID, noLocalFiles()).isEmpty());
    }

    @Test
    public void testParse_rpgleWithCopyBook() throws Exception {

        List<CompileReport.Message> messages = EventsFileMessages.parse(read("ifsphello.pgm.evfevent"), CCSID, noLocalFiles());

        assertEquals(5, messages.size());
        assertEquals(CONSTS + ":3:26 RNF3430 20 Initial value 'abc' does not match data type of field BADINC. Initial value is ignored.",
            messages.get(0).toString());
        assertEquals(CONSTS + ":3:7 RNF7031 0 The name or indicator BADINC is not referenced.", messages.get(1).toString());
        assertEquals(HELLO + ":6:1 RNF7030 30 The name or indicator UNDEFINEDVAR is not defined.", messages.get(2).toString());
        assertEquals(HELLO + ":6:1 RNF7503 30 Expression contains an operand that is not defined.", messages.get(3).toString());
        assertEquals(HELLO + ":0:0 RNS9308 50 Compilation stopped. Severity 30 errors found in program.", messages.get(4).toString());
    }

    @Test
    public void testParse_localFilesReplaceThePathsOfTheEvents() throws Exception {

        Map<String, String> localFiles = new HashMap<>();
        localFiles.put(HELLO, "C:/git/demo/ifsphello.pgm.rpgle");

        List<CompileReport.Message> messages = EventsFileMessages.parse(read("ifsphello.pgm.evfevent"), CCSID, localFiles);

        assertTrue(messages.get(0).toString().startsWith(CONSTS + ":"));
        assertTrue(messages.get(2).toString().startsWith("C:/git/demo/ifsphello.pgm.rpgle:6:1 RNF7030"));
    }

    @Test
    public void testParse_sqlrpgleLinesOfTheSource() throws Exception {

        List<CompileReport.Message> messages = EventsFileMessages.parse(read("ifspsql.pgm.evfevent"), CCSID, noLocalFiles());

        // The RPG compiler reports line 91 of the member of QTEMP, that the SQL
        // precompiler has created; the events map it back to line 5 of the
        // source.
        CompileReport.Message error = find(messages, "RNF7416");
        assertEquals(SQL + ":5:12 RNF7416 30 The types of the right and left hand side do not match in the EVAL operation.", error.toString());
        for (CompileReport.Message message : messages) {
            assertTrue(message.toString(), message.toString().startsWith(SQL + ":"));
        }
    }

    @Test
    public void testParse_severalEventsFilesOneAfterTheOther() throws Exception {

        String events = read("ifsphello.pgm.evfevent") + "\n" + read("ifspsql.pgm.evfevent") + "\n" + read("ifsputils.evfevent");

        List<CompileReport.Message> messages = EventsFileMessages.parse(events, CCSID, noLocalFiles());

        assertNotNull(find(messages, "RNF7030"));
        assertEquals(SQL, file(find(messages, "RNF7416")));
        boolean hasUtils = false;
        for (CompileReport.Message message : messages) {
            hasUtils |= message.toString().startsWith(UTILS + ":");
        }
        assertTrue(hasUtils);
    }

    private static CompileReport.Message find(List<CompileReport.Message> aMessages, String anId) {

        for (CompileReport.Message message : aMessages) {
            if (message.toString().contains(" " + anId + " ")) {
                return message;
            }
        }

        return null;
    }

    private static String file(CompileReport.Message aMessage) {
        return aMessage.toString().substring(0, aMessage.toString().indexOf(':'));
    }

    private static Map<String, String> noLocalFiles() {
        return Collections.emptyMap();
    }

    private String read(String aName) throws Exception {

        InputStream inputStream = getClass().getResourceAsStream("data/" + aName);
        assertNotNull("Resource not found: " + aName, inputStream);
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] bytes = new byte[4096];
            int count;
            while ((count = inputStream.read(bytes)) >= 0) {
                buffer.write(bytes, 0, count);
            }
            return new String(buffer.toByteArray(), "UTF-8");
        } finally {
            inputStream.close();
        }
    }
}
