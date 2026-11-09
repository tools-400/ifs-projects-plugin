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
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import de.tools400.ifsprojects.export.ExportMapping;
import de.tools400.ifsprojects.export.ExportMapping.Entry;
import de.tools400.ifsprojects.export.ExportMapping.Status;
import de.tools400.ifsprojects.export.SourceFileRule;

public class ExportMappingTest {

    @Test
    public void testGetMemberName() {
        assertEquals("HELLO", ExportMapping.getMemberName("hello.pgm.rpgle"));
        assertEquals("$UTIL_1", ExportMapping.getMemberName("$util_1.rpgleinc"));
        assertEquals("ABCDEFGHIJ", ExportMapping.getMemberName("abcdefghij.clle"));
        assertNull(ExportMapping.getMemberName("abcdefghijk.clle"));
        assertNull(ExportMapping.getMemberName("1hello.rpgle"));
        assertNull(ExportMapping.getMemberName("my-pgm.rpgle"));
        assertNull(ExportMapping.getMemberName("\u00E4nderung.rpgle"));
        assertNull(ExportMapping.getMemberName(null));
    }

    @Test
    public void testMap_directory() {

        List<Entry> entries = new ExportMapping(SourceFileRule.DIRECTORY, null).map(Arrays.asList("qrpglesrc/hello.pgm.rpgle", "inc/consts.rpgleinc",
            "src/rpg/utils.rpgle", "my-dir/start.clle", "hellod.dspf", "README.md", "iproj.json", "qrpglesrc/Rules.mk"));

        assertEquals(5, entries.size());
        assertEntry(entries.get(0), "qrpglesrc/hello.pgm.rpgle", "QRPGLESRC", "HELLO", "RPGLE", Status.OK);
        assertEntry(entries.get(1), "inc/consts.rpgleinc", "INC", "CONSTS", "RPGLE", Status.OK);
        assertEntry(entries.get(2), "src/rpg/utils.rpgle", "RPG", "UTILS", "RPGLE", Status.OK);
        assertEntry(entries.get(3), "my-dir/start.clle", "QCLSRC", "START", "CLLE", Status.OK);
        assertEntry(entries.get(4), "hellod.dspf", "QDDSSRC", "HELLOD", "DSPF", Status.OK);
    }

    @Test
    public void testMap_sourceType() {

        List<Entry> entries = new ExportMapping(SourceFileRule.SOURCE_TYPE, "IGNORED")
            .map(Arrays.asList("qprotosrc/consts.rpgleinc", "src/hello.clle"));

        assertEntry(entries.get(0), "qprotosrc/consts.rpgleinc", "QRPGLESRC", "CONSTS", "RPGLE", Status.OK);
        assertEntry(entries.get(1), "src/hello.clle", "QCLSRC", "HELLO", "CLLE", Status.OK);
    }

    @Test
    public void testMap_singleFile() {

        List<Entry> entries = new ExportMapping(SourceFileRule.SINGLE_FILE, " qsource ")
            .map(Arrays.asList("qrpglesrc/hello.pgm.rpgle", "hellod.dspf"));

        assertEntry(entries.get(0), "qrpglesrc/hello.pgm.rpgle", "QSOURCE", "HELLO", "RPGLE", Status.OK);
        assertEntry(entries.get(1), "hellod.dspf", "QSOURCE", "HELLOD", "DSPF", Status.OK);
    }

    @Test
    public void testMap_duplicates() {

        List<String> paths = Arrays.asList("qrpglesrc/utils.rpgle", "inc/utils.rpgleinc", "QCLSRC/A.clle", "qclsrc/a.CLP");

        List<Entry> byDirectory = new ExportMapping(SourceFileRule.DIRECTORY, null).map(paths);
        assertEquals(Status.OK, byDirectory.get(0).getStatus());
        assertEquals(Status.OK, byDirectory.get(1).getStatus());
        assertEquals(Status.DUPLICATE, byDirectory.get(2).getStatus());
        assertEquals(Status.DUPLICATE, byDirectory.get(3).getStatus());

        List<Entry> bySourceType = new ExportMapping(SourceFileRule.SOURCE_TYPE, null).map(paths);
        assertEquals(Status.DUPLICATE, bySourceType.get(0).getStatus());
        assertEquals(Status.DUPLICATE, bySourceType.get(1).getStatus());
    }

    @Test
    public void testMap_invalidMemberName() {

        List<Entry> entries = new ExportMapping(SourceFileRule.DIRECTORY, null)
            .map(Arrays.asList("qrpglesrc/toolongname1.rpgle", "qrpglesrc/toolongname1.rpgleinc", "qrpglesrc/hello.rpgle"));

        assertEntry(entries.get(0), "qrpglesrc/toolongname1.rpgle", "QRPGLESRC", null, "RPGLE", Status.INVALID_MEMBER_NAME);
        assertEquals(Status.INVALID_MEMBER_NAME, entries.get(1).getStatus());
        assertEquals(Status.OK, entries.get(2).getStatus());
        assertEquals("QRPGLESRC,HELLO", entries.get(2).getTarget());
    }

    private void assertEntry(Entry anEntry, String aPath, String aSourceFile, String aMember, String aSourceType, Status aStatus) {
        assertEquals(aPath, anEntry.getPath());
        assertEquals(aSourceFile, anEntry.getSourceFile());
        assertEquals(aMember, anEntry.getMember());
        assertEquals(aSourceType, anEntry.getSourceType());
        assertEquals(aStatus, anEntry.getStatus());
    }

    @Test
    public void testIsExported() {

        ExportMapping mapping = new ExportMapping(SourceFileRule.DIRECTORY, null);

        assertTrue(mapping.isExported("src/hello.pgm.rpgle"));
        assertTrue(mapping.isExported("hello.pgm.clle"));
        assertFalse(mapping.isExported("src/Rules.mk"));
        assertFalse(mapping.isExported("iproj.json"));
        assertFalse(mapping.isExported("qddssrc/table.tbl"));
    }

    /**
     * Hidden files and the files of hidden directories are not exported, e.g.
     * the Windows batch file <code>.github/release.cmd</code>, which would be
     * a command.
     */
    @Test
    public void testIsExported_hidden() {

        ExportMapping mapping = new ExportMapping(SourceFileRule.DIRECTORY, null);

        assertFalse(mapping.isExported(".github/release.cmd"));
        assertFalse(mapping.isExported("src/.hidden/hello.rpgle"));
        assertFalse(mapping.isExported("qrpglesrc/.hello.rpgle"));
        assertTrue(mapping.isExported("src.old/hello.rpgle"));

        List<Entry> entries = mapping.map(Arrays.asList(".github/release.cmd", "qcmdsrc/hello.cmd"));
        assertEquals(1, entries.size());
        assertEntry(entries.get(0), "qcmdsrc/hello.cmd", "QCMDSRC", "HELLO", "CMD", Status.OK);
    }

    @Test
    public void testIsHidden() {
        assertTrue(ExportMapping.isHidden(".github/release.cmd"));
        assertTrue(ExportMapping.isHidden("a/.b/c.rpgle"));
        assertTrue(ExportMapping.isHidden("qrpglesrc/.ibmi.json"));
        assertTrue(ExportMapping.isHidden(".gitignore"));
        assertFalse(ExportMapping.isHidden("src.old/hello.rpgle"));
        assertFalse(ExportMapping.isHidden("hello.rpgle"));
        assertFalse(ExportMapping.isHidden("a/b/c"));
    }

    @Test
    public void testGetExtension() {
        assertEquals("rpgle", ExportMapping.getExtension("hello.pgm.rpgle"));
        assertEquals("XML", ExportMapping.getExtension("BASICS1.XML"));
        assertEquals("", ExportMapping.getExtension("LICENSE"));
        assertEquals("", ExportMapping.getExtension(".gitignore"));
    }

    /**
     * The migration writes members of source types, that TOBi does not know,
     * with their source type as extension, e.g. <code>TBL</code>.
     */
    @Test
    public void testMap_furtherSourceTypes() {

        ExportMapping mapping = new ExportMapping(SourceFileRule.DIRECTORY, null, Arrays.asList("tbl", "XML"));

        assertEquals("TBL", mapping.getSourceType("qddssrc/table.tbl"));
        assertEquals("XML", mapping.getSourceType("QXML/BASICS1.XML"));
        assertEquals("RPGLE", mapping.getSourceType("qrpglesrc/hello.rpgle"));
        assertNull(mapping.getSourceType("qddssrc/table.tblinc"));
        assertNull(mapping.getSourceType("doc/download.bat"));
        assertNull(mapping.getSourceType(".hidden/table.tbl"));

        List<Entry> entries = mapping.map(Arrays.asList("qddssrc/table.tbl", "QXML/BASICS1.XML", "table.tbl", "README.md"));
        assertEquals(3, entries.size());
        assertEntry(entries.get(0), "qddssrc/table.tbl", "QDDSSRC", "TABLE", "TBL", Status.OK);
        assertEntry(entries.get(1), "QXML/BASICS1.XML", "QXML", "BASICS1", "XML", Status.OK);
        assertEntry(entries.get(2), "table.tbl", "QSRC", "TABLE", "TBL", Status.OK);
    }
}
