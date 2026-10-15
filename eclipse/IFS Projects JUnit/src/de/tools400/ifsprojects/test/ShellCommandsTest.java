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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import de.tools400.ifsprojects.terminal.ShellCommands;

public class ShellCommandsTest {

    @Test
    public void testQuote() {
        assertEquals("'/home/jsmith'", ShellCommands.quote("/home/jsmith"));
        assertEquals("'/home/jsmith/my project'", ShellCommands.quote("/home/jsmith/my project"));
        assertEquals("'/home/jsmith/it'\\''s a test'", ShellCommands.quote("/home/jsmith/it's a test"));
        assertEquals("'$HOME'", ShellCommands.quote("$HOME"));
    }

    @Test
    public void testCommandLine_nothing() {
        assertNull(ShellCommands.toCommandLine(null, null));
        assertNull(ShellCommands.toCommandLine("", "  "));
    }

    @Test
    public void testCommandLine_directoryOnly() {
        assertEquals("cd '/home/jsmith/src'\r", ShellCommands.toCommandLine("/home/jsmith/src", null));
        assertEquals("cd '/'\r", ShellCommands.toCommandLine("/", null));
        assertEquals("cd '/home/jsmith/it'\\''s a test'\r", ShellCommands.toCommandLine("/home/jsmith/it's a test", null));
    }

    @Test
    public void testCommandLine_directoryAndCommand() {
        assertEquals("cd '/home/jsmith/src' && gmake\r", ShellCommands.toCommandLine("/home/jsmith/src", "gmake"));
        assertEquals("cd '/home/jsmith/src' && makei build\r", ShellCommands.toCommandLine("/home/jsmith/src", "  makei build  "));
    }

    @Test
    public void testCommandLine_commandOnly() {
        assertEquals("gmake\r", ShellCommands.toCommandLine(null, "gmake"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCommandLine_directoryWithLineFeed() {
        ShellCommands.toCommandLine("/home/jsmith\nrm -rf /", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCommandLine_commandWithCarriageReturn() {
        ShellCommands.toCommandLine("/home/jsmith", "gmake\rrm -rf /");
    }

    @Test
    public void testReplacePlaceholders() {

        Map<String, String> values = new HashMap<String, String>();
        values.put("filename", "WHOAMICPP.RPGLE");
        values.put("files", "WHOAMICPP.RPGLE");
        values.put("usrprf", "JSMITH");

        assertEquals("gmake WHOAMICPP.RPGLE", ShellCommands.replacePlaceholders("gmake {filename}", values));
        assertEquals("makei c -f WHOAMICPP.RPGLE", ShellCommands.replacePlaceholders("makei c -f {files}", values));
        assertEquals("gmake WHOAMICPP.RPGLE BIN_LIB=JSMITH", ShellCommands.replacePlaceholders("gmake {filename} BIN_LIB={usrprf}", values));
        assertEquals("gmake WHOAMICPP.RPGLE WHOAMICPP.RPGLE", ShellCommands.replacePlaceholders("gmake {filename} {filename}", values));
    }

    @Test
    public void testReplacePlaceholders_unknownPlaceholderIsKept() {

        Map<String, String> values = new HashMap<String, String>();
        values.put("filename", "WHOAMI.PNLGRP");

        assertEquals("gmake WHOAMI.PNLGRP {unknown}", ShellCommands.replacePlaceholders("gmake {filename} {unknown}", values));
        assertEquals("gmake", ShellCommands.replacePlaceholders("gmake", values));
    }

    @Test
    public void testReplacePlaceholders_nullValues() {

        Map<String, String> values = new HashMap<String, String>();
        values.put("host", null);

        assertEquals("ssh ", ShellCommands.replacePlaceholders("ssh {host}", values));
        assertNull(ShellCommands.replacePlaceholders(null, values));
    }

    @Test
    public void testReplacePlaceholders_quotedFileName() {

        Map<String, String> values = new HashMap<String, String>();
        values.put("filename", ShellCommands.quote("my file.rpgle"));

        assertEquals("gmake 'my file.rpgle'", ShellCommands.replacePlaceholders("gmake {filename}", values));
    }

    @Test
    public void testAddExports() {

        Map<String, String> variables = new LinkedHashMap<String, String>();
        variables.put("ENVCMD", "CHGSYSLIBL LIB(UBMSYS) OPTION(*REMOVE)");
        variables.put("EMPTY", "");

        assertEquals("ENVCMD='CHGSYSLIBL LIB(UBMSYS) OPTION(*REMOVE)' EMPTY='' && export ENVCMD EMPTY && makei build",
            ShellCommands.addExports(variables, "makei build"));
        assertEquals("ENVCMD='CHGSYSLIBL LIB(UBMSYS) OPTION(*REMOVE)' EMPTY='' && export ENVCMD EMPTY",
            ShellCommands.addExports(variables, null));
    }

    @Test
    public void testAddExports_quote() {

        Map<String, String> variables = new LinkedHashMap<String, String>();
        variables.put("TEXT", "it's $HOME");

        assertEquals("TEXT='it'\\''s $HOME' && export TEXT && gmake", ShellCommands.addExports(variables, "gmake"));
    }

    @Test
    public void testAddExports_noVariables() {
        assertEquals("makei build", ShellCommands.addExports(new HashMap<String, String>(), " makei build "));
        assertEquals("makei build", ShellCommands.addExports(null, "makei build"));
        assertNull(ShellCommands.addExports(null, null));
    }

    @Test
    public void testAddExports_commandLine() {

        Map<String, String> variables = new LinkedHashMap<String, String>();
        variables.put("ENVCMD", "");

        assertEquals("cd '/home/jsmith/git/dspsrc' && ENVCMD='' && export ENVCMD && makei build\r",
            ShellCommands.toCommandLine("/home/jsmith/git/dspsrc", ShellCommands.addExports(variables, "makei build")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddExports_invalidName() {
        Map<String, String> variables = new LinkedHashMap<String, String>();
        variables.put("MY-LIB", "x");
        ShellCommands.addExports(variables, "gmake");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddExports_lineBreak() {
        Map<String, String> variables = new LinkedHashMap<String, String>();
        variables.put("CMD", "a\nrm -rf /");
        ShellCommands.addExports(variables, "gmake");
    }
}
