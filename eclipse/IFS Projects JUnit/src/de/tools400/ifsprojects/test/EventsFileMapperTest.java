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
import java.util.LinkedHashSet;

import org.junit.Test;

import de.tools400.ifsprojects.local.EventsFileMapper;
import de.tools400.ifsprojects.local.SourceMapping;

public class EventsFileMapperTest {

    private static final SourceMapping MAPPING = new SourceMapping("/home/jsmith/.ifsprojects/builds/ifsptest/", "ifsptest");

    @Test
    public void testToProjectLocation() {

        assertEquals("/home/jsmith/.ifsprojects/builds/ifsptest", MAPPING.getRemoteDirectory());
        assertEquals("/ifsptest/inc/consts.rpgleinc", MAPPING.toProjectLocation("/home/jsmith/.ifsprojects/builds/ifsptest/inc/consts.rpgleinc"));

        // The compilers write the path with the case of the directory.
        assertEquals("/ifsptest/hello.pgm.rpgle", MAPPING.toProjectLocation("/home/Jsmith/.ifsprojects/builds/IFSPTEST/hello.pgm.rpgle"));

        assertNull(MAPPING.toProjectLocation("/home/jsmith/.ifsprojects/builds/ifsptest2/hello.pgm.rpgle"));
        assertNull(MAPPING.toProjectLocation("/home/jsmith/.ifsprojects/builds/ifsptest/"));
        assertNull(MAPPING.toProjectLocation("/home/jsmith/.ifsprojects/builds/ifsptest"));
        assertNull(MAPPING.toProjectLocation("QTEMP/QSQLPRE(IFSPSQL)"));
        assertNull(MAPPING.toProjectLocation(null));
    }

    @Test
    public void testMap() {

        String events = "TIMESTAMP  0 20261014201350\n" //
            + "PROCESSOR  0 000 1\n" //
            + "FILEID     0 001 000000 061 /home/Jsmith/.ifsprojects/builds/ifsptest/ifsphello.pgm.rpgle 20261013213315 0\n" //
            + "FILEID     0 002 000003 038 /QIBM/include/qrpglesrc/qusec.rpgleinc 20261013212406 0\n" //
            + "ERROR      0 002 1 000003 000003 026 000003 030 RNF3430 E 20 012 Some message\n" //
            + "FILEEND    0 002 000003\n" //
            + "FILEEND    0 001 000008\n";

        EventsFileMapper.Result result = EventsFileMapper.map(events, MAPPING);

        assertEquals("TIMESTAMP  0 20261014201350\n" //
            + "PROCESSOR  0 000 1\n" //
            + "FILEID     0 001 000000 029 /ifsptest/ifsphello.pgm.rpgle 20261013213315 0\n" //
            + "FILEID     0 002 000003 038 /QIBM/include/qrpglesrc/qusec.rpgleinc 20261013212406 0\n" //
            + "ERROR      0 002 1 000003 000003 026 000003 030 RNF3430 E 20 012 Some message\n" //
            + "FILEEND    0 002 000003\n" //
            + "FILEEND    0 001 000008\n", result.getEvents());

        assertEquals(new LinkedHashSet<>(Arrays.asList("/ifsptest/ifsphello.pgm.rpgle")), result.getLocations());
    }

    @Test
    public void testMap_continuation() {

        // The path is continued in a FILEIDCONT record: 42 + 19 characters.
        String events = "FILEID     0 001 000000 061 /home/Jsmith/.ifsprojects/builds/ifsptest/\r\n" //
            + "FILEIDCONT 0 001 000000 000 ifsphello.pgm.rpgle 20261013213315 0\r\n" //
            + "FILEEND    0 001 000008\r\n";

        EventsFileMapper.Result result = EventsFileMapper.map(events, MAPPING);

        assertEquals("FILEID     0 001 000000 029 /ifsptest/ifsphello.pgm.rpgle 20261013213315 0\r\n" //
            + "FILEEND    0 001 000008\r\n", result.getEvents());
        assertTrue(result.getLocations().contains("/ifsptest/ifsphello.pgm.rpgle"));
    }

    @Test
    public void testMap_unchanged() {

        // Kept: other files, a path shorter than its length without
        // continuation and a FILEIDCONT record without FILEID record.
        String events = "FILEID     0 999 000000 022 QTEMP/QSQLPRE(IFSPSQL) 20261014201351 0\n" //
            + "FILEID     0 001 000000 099 /home/jsmith/.ifsprojects/builds/ifsptest/a.rpgle 20261013213315 0\n" //
            + "FILEEND    0 001 000008\n" //
            + "FILEIDCONT 0 001 000000 000 /home/jsmith/.ifsprojects/builds/ifsptest/a.rpgle";

        EventsFileMapper.Result result = EventsFileMapper.map(events, MAPPING);

        assertEquals(events, result.getEvents());
        assertTrue(result.getLocations().isEmpty());
    }
}
