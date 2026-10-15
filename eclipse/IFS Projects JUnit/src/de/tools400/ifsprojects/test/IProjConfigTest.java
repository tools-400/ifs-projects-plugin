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
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import de.tools400.ifsprojects.project.IProjConfig;
import de.tools400.ifsprojects.project.IProjParseException;

public class IProjConfigTest {

    private static final String BYTE_ORDER_MARK = String.valueOf((char)0xFEFF);

    @Test
    public void testParse_empty() throws Exception {
        assertEquals("{}\n", IProjConfig.parse(null).toJson());
        assertEquals("{}\n", IProjConfig.parse("").toJson());
        assertEquals("{}\n", IProjConfig.parse("  \n ").toJson());
    }

    @Test
    public void testParse_byteOrderMark() throws Exception {
        IProjConfig config = IProjConfig.parse(BYTE_ORDER_MARK + "{\"description\":\"Utilities\"}");
        assertEquals("Utilities", config.getString(IProjConfig.DESCRIPTION));
    }

    @Test(expected = IProjParseException.class)
    public void testParse_invalid() throws Exception {
        IProjConfig.parse("{\"description\": ");
    }

    @Test(expected = IProjParseException.class)
    public void testParse_rootIsArray() throws Exception {
        IProjConfig.parse("[1, 2]");
    }

    @Test
    public void testUnknownAttributesAndOrderArePreserved() throws Exception {

        IProjConfig config = IProjConfig.parse("{\"objlib\":\"UTILS\",\"description\":\"old\",\"foo\":{\"bar\":1},\"tags\":[1,2]}");
        config.setString(IProjConfig.DESCRIPTION, "new");
        config.setString(IProjConfig.BUILD_COMMAND, "gmake");

        String json = config.toJson();

        assertTrue(json.contains("\"foo\": {"));
        assertTrue(json.contains("\"bar\": 1"));
        assertTrue(json.contains("\"new\""));
        assertFalse(json.contains("\"old\""));
        assertTrue(json.indexOf("\"objlib\"") < json.indexOf("\"description\""));
        assertTrue(json.indexOf("\"description\"") < json.indexOf("\"foo\""));
        assertTrue(json.indexOf("\"tags\"") < json.indexOf("\"buildCommand\""));
        assertTrue(json.endsWith("}\n"));
    }

    @Test
    public void testSetString_emptyRemovesAttribute() throws Exception {

        IProjConfig config = IProjConfig.parse("{\"description\":\"x\",\"repository\":\"y\"}");
        config.setString(IProjConfig.DESCRIPTION, "  ");
        config.setString(IProjConfig.REPOSITORY, null);

        assertEquals("{}\n", config.toJson());
    }

    @Test
    public void testNoHtmlEscaping() throws Exception {

        IProjConfig config = new IProjConfig();
        config.setString(IProjConfig.BUILD_COMMAND, "gmake 2>&1 | tee build.log");

        assertTrue(config.toJson().contains("gmake 2>&1 | tee build.log"));
        assertEquals("gmake 2>&1 | tee build.log", IProjConfig.parse(config.toJson()).getBuildCommand());
    }

    @Test
    public void testStringList() throws Exception {

        IProjConfig config = IProjConfig.parse("{\"includePath\":[\"src/include\",\"/QIBM/include\"]}");
        assertEquals(Arrays.asList("src/include", "/QIBM/include"), config.getStringList(IProjConfig.INCLUDE_PATH));
        assertEquals(Collections.<String> emptyList(), config.getStringList(IProjConfig.PRE_USRLIBL));

        config.setStringList(IProjConfig.PRE_USRLIBL, Arrays.asList("LIB1", " ", "LIB2 "));
        assertEquals(Arrays.asList("LIB1", "LIB2"), IProjConfig.parse(config.toJson()).getStringList(IProjConfig.PRE_USRLIBL));

        config.setStringList(IProjConfig.INCLUDE_PATH, Collections.<String> emptyList());
        assertFalse(config.toJson().contains(IProjConfig.INCLUDE_PATH));
    }

    @Test
    public void testNotEditable() throws Exception {

        IProjConfig config = IProjConfig.parse("{\"description\":{\"de\":\"x\"},\"includePath\":[{\"a\":1}],\"curlib\":\"LIB\"}");

        assertTrue(config.isNotEditable(IProjConfig.DESCRIPTION, false));
        assertNull(config.getString(IProjConfig.DESCRIPTION));
        assertEquals("{\"de\":\"x\"}", config.getJsonText(IProjConfig.DESCRIPTION));

        assertTrue(config.isNotEditable(IProjConfig.INCLUDE_PATH, true));
        assertNull(config.getStringList(IProjConfig.INCLUDE_PATH));

        assertFalse(config.isNotEditable(IProjConfig.CURLIB, false));
        assertFalse(config.isNotEditable(IProjConfig.OBJLIB, false));
    }

    @Test
    public void testBuildCommand() throws Exception {
        assertNull(IProjConfig.parse("{}").getBuildCommand());
        assertNull(IProjConfig.parse("{\"buildCommand\":\"  \"}").getBuildCommand());
        assertEquals("makei build", IProjConfig.parse("{\"buildCommand\":\" makei build \"}").getBuildCommand());
    }

    @Test
    public void testUmlauts() throws Exception {

        String text = "Werkzeuge f" + (char)0xFC + "r IFS";

        IProjConfig config = new IProjConfig();
        config.setString(IProjConfig.DESCRIPTION, text);

        List<String> nothing = config.getStringList(IProjConfig.INCLUDE_PATH);
        assertTrue(nothing.isEmpty());
        assertEquals(text, IProjConfig.parse(config.toJson()).getString(IProjConfig.DESCRIPTION));
    }
}
