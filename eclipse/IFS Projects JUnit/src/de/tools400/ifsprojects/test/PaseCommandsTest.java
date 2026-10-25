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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import de.tools400.ifsprojects.ibmi.PaseCommands;

public class PaseCommandsTest {

    @Test
    public void testQuote() {
        assertEquals("'/home/jsmith'", PaseCommands.quote("/home/jsmith"));
        assertEquals("'/home/jsmith/my project'", PaseCommands.quote("/home/jsmith/my project"));
        assertEquals("'/home/jsmith/it'\\''s a test'", PaseCommands.quote("/home/jsmith/it's a test"));
        assertEquals("'$HOME'", PaseCommands.quote("$HOME"));
    }

    @Test
    public void testIsVariableName() {
        assertTrue(PaseCommands.isVariableName("ENVCMD"));
        assertTrue(PaseCommands.isVariableName("_lib1"));
        assertFalse(PaseCommands.isVariableName("1LIB"));
        assertFalse(PaseCommands.isVariableName("MY-LIB"));
        assertFalse(PaseCommands.isVariableName(""));
        assertFalse(PaseCommands.isVariableName(null));
    }

    @Test
    public void testAddExports() {

        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("ENVCMD", "CHGSYSLIBL LIB(UBMSYS) OPTION(*REMOVE)");
        variables.put("EMPTY", "");

        assertEquals("ENVCMD='CHGSYSLIBL LIB(UBMSYS) OPTION(*REMOVE)' EMPTY='' && export ENVCMD EMPTY && makei build",
            PaseCommands.addExports(variables, "makei build"));
        assertEquals("ENVCMD='CHGSYSLIBL LIB(UBMSYS) OPTION(*REMOVE)' EMPTY='' && export ENVCMD EMPTY", PaseCommands.addExports(variables, null));
    }

    @Test
    public void testAddExports_quote() {

        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("TEXT", "it's $HOME");

        assertEquals("TEXT='it'\\''s $HOME' && export TEXT && gmake", PaseCommands.addExports(variables, "gmake"));
    }

    @Test
    public void testAddExports_noVariables() {
        assertEquals("makei build", PaseCommands.addExports(new HashMap<String, String>(), " makei build "));
        assertEquals("makei build", PaseCommands.addExports(null, "makei build"));
        assertNull(PaseCommands.addExports(null, null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddExports_invalidName() {
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("MY-LIB", "x");
        PaseCommands.addExports(variables, "gmake");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddExports_lineBreak() {
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("CMD", "a\nrm -rf /");
        PaseCommands.addExports(variables, "gmake");
    }

    @Test
    public void testReplacePlaceholders() {

        Map<String, String> values = new HashMap<>();
        values.put("filename", "WHOAMICPP.RPGLE");
        values.put("files", "WHOAMICPP.RPGLE");
        values.put("usrprf", "JSMITH");

        assertEquals("gmake WHOAMICPP.RPGLE", PaseCommands.replacePlaceholders("gmake {filename}", values));
        assertEquals("makei c -f WHOAMICPP.RPGLE", PaseCommands.replacePlaceholders("makei c -f {files}", values));
        assertEquals("gmake WHOAMICPP.RPGLE BIN_LIB=JSMITH", PaseCommands.replacePlaceholders("gmake {filename} BIN_LIB={usrprf}", values));
        assertEquals("gmake WHOAMICPP.RPGLE WHOAMICPP.RPGLE", PaseCommands.replacePlaceholders("gmake {filename} {filename}", values));
    }

    @Test
    public void testReplacePlaceholders_unknownPlaceholderIsKept() {

        Map<String, String> values = new HashMap<>();
        values.put("filename", "WHOAMI.PNLGRP");

        assertEquals("gmake WHOAMI.PNLGRP {unknown}", PaseCommands.replacePlaceholders("gmake {filename} {unknown}", values));
        assertEquals("gmake", PaseCommands.replacePlaceholders("gmake", values));
    }

    @Test
    public void testReplacePlaceholders_nullValues() {

        Map<String, String> values = new HashMap<>();
        values.put("host", null);

        assertEquals("ssh ", PaseCommands.replacePlaceholders("ssh {host}", values));
        assertNull(PaseCommands.replacePlaceholders(null, values));
    }

    @Test
    public void testReplacePlaceholders_quotedFileName() {

        Map<String, String> values = new HashMap<>();
        values.put("filename", PaseCommands.quote("my file.rpgle"));

        assertEquals("gmake 'my file.rpgle'", PaseCommands.replacePlaceholders("gmake {filename}", values));
    }

    @Test
    public void testGetCallCommand() {
        assertEquals("CALL PGM(QP2SHELL2) PARM('/QOpenSys/pkgs/bin/bash' '-c' 'cd ''/p'' && ENV=''x'' && export ENV && makei build')",
            PaseCommands.getCallCommand(PaseCommands.BASH, "cd '/p' && ENV='x' && export ENV && makei build"));
    }
}
