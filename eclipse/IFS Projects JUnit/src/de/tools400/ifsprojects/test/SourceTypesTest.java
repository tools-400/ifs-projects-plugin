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

import org.junit.Test;

import de.tools400.ifsprojects.sources.SourceTypes;

public class SourceTypesTest {

    @Test
    public void testGetSourceType() {
        assertEquals("RPGLE", SourceTypes.getSourceType("hello.pgm.rpgle"));
        assertEquals("SQLRPGLE", SourceTypes.getSourceType("SQLPGM.SQLRPGLE"));
        assertEquals("CLLE", SourceTypes.getSourceType("hello.pgm.clle"));
        assertEquals("DSPF", SourceTypes.getSourceType("hellod.dspf"));
        assertEquals("TABLE", SourceTypes.getSourceType("customer.table"));
        assertEquals("BND", SourceTypes.getSourceType("utils.bnd"));
        assertNull(SourceTypes.getSourceType("README.md"));
        assertNull(SourceTypes.getSourceType("iproj.json"));
        assertNull(SourceTypes.getSourceType("Rules.mk"));
        assertNull(SourceTypes.getSourceType("makefile"));
        assertNull(SourceTypes.getSourceType(".rpgle"));
        assertNull(SourceTypes.getSourceType(null));
    }

    @Test
    public void testGetSourceType_copyBooks() {
        assertEquals("RPGLE", SourceTypes.getSourceType("consts.rpgleinc"));
        assertEquals("SQLRPGLE", SourceTypes.getSourceType("hostvars.sqlrpgleinc"));
        assertEquals("CLLE", SourceTypes.getSourceType("vars.clleinc"));
        assertEquals("CLLE", SourceTypes.getSourceType("vars.clinc"));
        assertEquals("RPG", SourceTypes.getSourceType("old.RPGINC"));
        assertEquals("CBLLE", SourceTypes.getSourceType("record.cblleinc"));
        assertNull(SourceTypes.getSourceType("consts.inc"));
        assertNull(SourceTypes.getSourceType("consts.mdinc"));
    }

    @Test
    public void testGetDefaultSourceFile() {
        assertEquals("QRPGLESRC", SourceTypes.getDefaultSourceFile("RPGLE"));
        assertEquals("QRPGLESRC", SourceTypes.getDefaultSourceFile("SQLRPGLE"));
        assertEquals("QCLSRC", SourceTypes.getDefaultSourceFile("CLP"));
        assertEquals("QDDSSRC", SourceTypes.getDefaultSourceFile("PRTF"));
        assertEquals("QCMDSRC", SourceTypes.getDefaultSourceFile("CMD"));
        assertEquals("QSQLSRC", SourceTypes.getDefaultSourceFile("SQLPRC"));
        assertEquals("QSRVSRC", SourceTypes.getDefaultSourceFile("BND"));
        assertEquals("QSRC", SourceTypes.getDefaultSourceFile("BNDDIR"));
        assertEquals("QSRC", SourceTypes.getDefaultSourceFile("UNKNOWN"));
        assertEquals("QSRC", SourceTypes.getDefaultSourceFile(null));
    }
}
