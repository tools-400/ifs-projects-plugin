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
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import de.tools400.ifsprojects.project.EnvironmentVariables;
import de.tools400.ifsprojects.project.IProjConfig;
import java.util.HashMap;

public class EnvironmentVariablesTest {

    @Test
    public void testIsValidName() {
        assertTrue(EnvironmentVariables.isValidName("ENVCMD"));
        assertTrue(EnvironmentVariables.isValidName("_lib1"));
        assertFalse(EnvironmentVariables.isValidName("1LIB"));
        assertFalse(EnvironmentVariables.isValidName("MY-LIB"));
        assertFalse(EnvironmentVariables.isValidName(""));
        assertFalse(EnvironmentVariables.isValidName(null));
    }

    @Test
    public void testParse() {

        Map<String, String> variables = EnvironmentVariables
            .parse("ENVCMD=CHGSYSLIBL LIB(UBMSYS) OPTION(*REMOVE)\r\n\r\n# comment\n  LIB = DSPSRC\nEMPTY=\nURL=a=b");

        assertEquals(Arrays.asList("ENVCMD", "LIB", "EMPTY", "URL"), Arrays.asList(variables.keySet().toArray()));
        assertEquals("CHGSYSLIBL LIB(UBMSYS) OPTION(*REMOVE)", variables.get("ENVCMD"));
        assertEquals(" DSPSRC", variables.get("LIB"));
        assertEquals("", variables.get("EMPTY"));
        assertEquals("a=b", variables.get("URL"));
    }

    @Test
    public void testParse_empty() {
        assertTrue(EnvironmentVariables.parse("").isEmpty());
        assertTrue(EnvironmentVariables.parse(null).isEmpty());
    }

    @Test
    public void testFindInvalidLine() {
        assertEquals(0, EnvironmentVariables.findInvalidLine("A=1\nB=2"));
        assertEquals(2, EnvironmentVariables.findInvalidLine("A=1\nB"));
        assertEquals(2, EnvironmentVariables.findInvalidLine("A=1\n1B=2"));
        assertEquals(3, EnvironmentVariables.findInvalidLine("A=1\n\nA=2"));
        assertEquals(1, EnvironmentVariables.findInvalidLine("=1"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParse_invalid() {
        EnvironmentVariables.parse("A=1\nB C=2");
    }

    @Test
    public void testFormat() {

        Map<String, String> variables = new LinkedHashMap<String, String>();
        variables.put("ENVCMD", "CHGSYSLIBL LIB(UBMSYS) OPTION(*REMOVE)");
        variables.put("EMPTY", "");

        String text = EnvironmentVariables.format(variables);

        assertEquals("ENVCMD=CHGSYSLIBL LIB(UBMSYS) OPTION(*REMOVE)\nEMPTY=", text);
        assertEquals(variables, EnvironmentVariables.parse(text));
    }

    @Test
    public void testMerge() {

        Map<String, String> defaults = new LinkedHashMap<String, String>();
        defaults.put("ENVCMD", "CHGSYSLIBL LIB(UBMSYS) OPTION(*REMOVE)");
        defaults.put("LIB", "DSPSRC");
        defaults.put("TRACE", "1");

        Map<String, String> variables = new LinkedHashMap<String, String>();
        variables.put("LIB", "QTEMP");
        variables.put("TRACE", "");
        variables.put("LANG", "ENG");

        Map<String, String> merged = EnvironmentVariables.merge(defaults, variables);

        assertEquals(Arrays.asList("ENVCMD", "LIB", "TRACE", "LANG"), Arrays.asList(merged.keySet().toArray()));
        assertEquals("CHGSYSLIBL LIB(UBMSYS) OPTION(*REMOVE)", merged.get("ENVCMD"));
        assertEquals("QTEMP", merged.get("LIB"));
        assertEquals("", merged.get("TRACE"));
        assertEquals("ENG", merged.get("LANG"));
        assertEquals("DSPSRC", defaults.get("LIB"));
    }

    @Test
    public void testMerge_null() {
        assertTrue(EnvironmentVariables.merge(null, null).isEmpty());
        assertEquals("1", EnvironmentVariables.merge(EnvironmentVariables.parse("A=1"), null).get("A"));
        assertEquals("2", EnvironmentVariables.merge(null, EnvironmentVariables.parse("A=2")).get("A"));
    }

    @Test
    public void testFindReferences() throws Exception {

        IProjConfig config = IProjConfig.parse("{\"description\": \"R&D tools &NOVAR\", \"objlib\": \"&LIB\", "
            + "\"preUsrlibl\": [\"&LIB\", \"QGPL\"], \"setIBMiEnvCmd\": [\"&ENVCMD\", \"CALL &SETUPLIB/SETUP\", \"ADDLIBLE LIB(&NOTOKEN)\"]}");

        assertEquals(Arrays.asList("LIB", "ENVCMD", "SETUPLIB"), Arrays.asList(EnvironmentVariables.findReferences(config).toArray()));
    }

    @Test
    public void testFindReferences_stringInsteadOfList() throws Exception {

        IProjConfig config = IProjConfig.parse("{\"preUsrlibl\": \"&ACCOUNTING ACMEUTIL\"}");

        assertEquals(Arrays.asList("ACCOUNTING"), Arrays.asList(EnvironmentVariables.findReferences(config).toArray()));
    }

    @Test
    public void testFindReferences_none() throws Exception {
        assertTrue(EnvironmentVariables.findReferences(IProjConfig.parse("{\"curlib\": \"DSPSRC\"}")).isEmpty());
    }

    @Test
    public void testResolveReferences() {

        Map<String, String> variables = new LinkedHashMap<String, String>();
        variables.put("ENVCMD", "CHGSYSLIBL LIB(UBMSYS) OPTION(*REMOVE)");
        variables.put("LIB", "DSPSRC");
        variables.put("EMPTY", "");

        assertEquals("CHGSYSLIBL LIB(UBMSYS) OPTION(*REMOVE)", EnvironmentVariables.resolveReferences("&ENVCMD", variables));
        assertEquals("CALL DSPSRC/SETUP", EnvironmentVariables.resolveReferences("CALL &LIB/SETUP", variables));
        assertEquals("/home/DSPSRC/inc", EnvironmentVariables.resolveReferences("/home/&LIB/inc", variables));
        assertEquals("ADDLIBLE LIB(&LIB)", EnvironmentVariables.resolveReferences("ADDLIBLE LIB(&LIB)", variables));
        assertEquals("&UNDEFINED QGPL", EnvironmentVariables.resolveReferences("&UNDEFINED QGPL", variables));
        assertEquals(" QGPL", EnvironmentVariables.resolveReferences("&EMPTY QGPL", variables));
        assertEquals("R&D &", EnvironmentVariables.resolveReferences("R&D &", variables));
    }

    @Test
    public void testResolveReferences_noVariables() {
        assertEquals("CALL &LIB/SETUP", EnvironmentVariables.resolveReferences("CALL &LIB/SETUP", null));
        assertEquals("CALL &LIB/SETUP", EnvironmentVariables.resolveReferences("CALL &LIB/SETUP", new LinkedHashMap<String, String>()));
        assertNull(EnvironmentVariables.resolveReferences(null, new LinkedHashMap<String, String>()));
    }

    @Test
    public void testAddExports() {

        Map<String, String> variables = new LinkedHashMap<String, String>();
        variables.put("ENVCMD", "CHGSYSLIBL LIB(UBMSYS) OPTION(*REMOVE)");
        variables.put("EMPTY", "");

        assertEquals("ENVCMD='CHGSYSLIBL LIB(UBMSYS) OPTION(*REMOVE)' EMPTY='' && export ENVCMD EMPTY && makei build",
            EnvironmentVariables.addExports(variables, "makei build"));
        assertEquals("ENVCMD='CHGSYSLIBL LIB(UBMSYS) OPTION(*REMOVE)' EMPTY='' && export ENVCMD EMPTY",
            EnvironmentVariables.addExports(variables, null));
    }

    @Test
    public void testAddExports_quote() {

        Map<String, String> variables = new LinkedHashMap<String, String>();
        variables.put("TEXT", "it's $HOME");

        assertEquals("TEXT='it'\\''s $HOME' && export TEXT && gmake", EnvironmentVariables.addExports(variables, "gmake"));
    }

    @Test
    public void testAddExports_noVariables() {
        assertEquals("makei build", EnvironmentVariables.addExports(new HashMap<String, String>(), " makei build "));
        assertEquals("makei build", EnvironmentVariables.addExports(null, "makei build"));
        assertNull(EnvironmentVariables.addExports(null, null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddExports_invalidName() {
        Map<String, String> variables = new LinkedHashMap<String, String>();
        variables.put("MY-LIB", "x");
        EnvironmentVariables.addExports(variables, "gmake");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddExports_lineBreak() {
        Map<String, String> variables = new LinkedHashMap<String, String>();
        variables.put("CMD", "a\nrm -rf /");
        EnvironmentVariables.addExports(variables, "gmake");
    }
}
