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

import java.nio.charset.StandardCharsets;

import org.junit.Test;

import de.tools400.ifsprojects.sources.TextEncoding;

public class TextEncodingTest {

    private static final String XML_LATIN1 = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\r\n<!-- Datum  Änderung -->\r\n";

    @Test
    public void testGetCcsid() {
        assertEquals(1208, TextEncoding.getCcsid("UTF-8"));
        assertEquals(1208, TextEncoding.getCcsid("utf8"));
        assertEquals(819, TextEncoding.getCcsid("ISO-8859-1"));
        assertEquals(819, TextEncoding.getCcsid("latin1"));
        assertEquals(923, TextEncoding.getCcsid("ISO-8859-15"));
        assertEquals(1252, TextEncoding.getCcsid("Cp1252"));
        assertEquals(367, TextEncoding.getCcsid("US-ASCII"));
        assertEquals(0, TextEncoding.getCcsid("UTF-16"));
        assertEquals(0, TextEncoding.getCcsid("unknown"));
        assertEquals(0, TextEncoding.getCcsid(null));
    }

    @Test
    public void testGetCharset() {
        assertEquals(StandardCharsets.UTF_8, TextEncoding.getCharset(1208));
        assertEquals(StandardCharsets.ISO_8859_1, TextEncoding.getCharset(819));
        assertEquals("windows-1252", TextEncoding.getCharset(1252).name());
        assertNull(TextEncoding.getCharset(273));
        assertNull(TextEncoding.getCharset(65535));
    }

    @Test
    public void testGetXmlEncoding() {
        assertEquals("ISO-8859-1", TextEncoding.getXmlEncoding(XML_LATIN1));
        assertEquals("UTF-8", TextEncoding.getXmlEncoding("﻿<?xml version='1.0' encoding='UTF-8' standalone='yes'?>\n<a/>"));
        assertEquals("windows-1252", TextEncoding.getXmlEncoding("<?xml version=\"1.0\"\n  encoding = \"windows-1252\"?><a/>"));
        assertNull(TextEncoding.getXmlEncoding("<?xml version=\"1.0\"?>\n<a encoding=\"UTF-8\"/>"));
        assertNull(TextEncoding.getXmlEncoding("  <?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
        assertNull(TextEncoding.getXmlEncoding("**FREE\n// encoding=\"UTF-8\"\n"));
        assertNull(TextEncoding.getXmlEncoding(""));
        assertNull(TextEncoding.getXmlEncoding(null));
    }

    @Test
    public void testCanEncode() {
        assertTrue(TextEncoding.canEncode("Änderung", "ISO-8859-1"));
        assertFalse(TextEncoding.canEncode("5 €", "ISO-8859-1"));
        assertTrue(TextEncoding.canEncode("5 €", "ISO-8859-15"));
        assertFalse(TextEncoding.canEncode("x", "unknown"));
    }

    /**
     * An XML file with ISO-8859-1 in a stream file with CCSID 1208 is decoded
     * with the encoding of its declaration, like the packages of XMLGEN in the
     * round trip of Peek/400.
     */
    @Test
    public void testDecode_xmlDeclaration() {

        byte[] bytes = XML_LATIN1.getBytes(StandardCharsets.ISO_8859_1);

        assertEquals(XML_LATIN1, TextEncoding.decode(bytes, StandardCharsets.UTF_8));
        assertEquals(XML_LATIN1, TextEncoding.decode(bytes, StandardCharsets.ISO_8859_1));
    }

    /**
     * Valid UTF-8 is decoded as UTF-8, also if the XML declaration names
     * another encoding, e.g. a file of the migration before it wrote XML files
     * in their encoding.
     */
    @Test
    public void testDecode_validUtf8() {
        assertEquals(XML_LATIN1, TextEncoding.decode(XML_LATIN1.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));
    }

    /**
     * Invalid bytes are replaced without losing the next character, which the
     * reader of IBM Toolbox for Java did ("Änderung" became "derung").
     */
    @Test
    public void testDecode_invalidBytes() {

        byte[] bytes = "// Änderung\n".getBytes(StandardCharsets.ISO_8859_1);

        assertEquals("// �nderung\n", TextEncoding.decode(bytes, StandardCharsets.UTF_8));
    }
}
