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

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import de.tools400.ifsprojects.ibmi.PaseCommands;
import de.tools400.ifsprojects.terminal.ShellCommands;

public class ShellCommandsTest {

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
    public void testAddTerminalType_noCommandLine() {
        assertEquals("TERM='xterm' && export TERM\r", ShellCommands.addTerminalType("xterm", null));
    }

    @Test
    public void testAddTerminalType_commandLine() {
        assertEquals("TERM='xterm' && export TERM && cd '/home/jsmith/src' && gmake\r",
            ShellCommands.addTerminalType("xterm", ShellCommands.toCommandLine("/home/jsmith/src", "gmake")));
    }

    @Test
    public void testAddExports_commandLine() {

        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("ENVCMD", "");

        assertEquals("cd '/home/jsmith/git/dspsrc' && ENVCMD='' && export ENVCMD && makei build\r",
            ShellCommands.toCommandLine("/home/jsmith/git/dspsrc", PaseCommands.addExports(variables, "makei build")));
    }

}
