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

import de.tools400.ifsprojects.build.TerminalBuildCommands;

public class TerminalBuildCommandsTest {

    private static final String SCRIPT_FILE = "/home/jsmith/.ifsprojects/tmp/ifsprojects.sh";

    @Test
    public void testGetScriptFile() {
        assertEquals(SCRIPT_FILE, TerminalBuildCommands.getScriptFile("/home/jsmith/.ifsprojects/tmp"));
    }

    @Test
    public void testGetScript() {
        assertEquals(
            "# IFS Projects: writes the marker files of a build or compile command in the terminal.\n"
                + "# Sourced by: ( _ifsp=<id>; . <this script>; <command>; _ifsp_end $? )\n" + "_ifsp_f='/home/it'\\''s/ifsprojects_'\"$_ifsp\"\n"
                + "_ifsp_end() { echo $1 > \"$_ifsp_f.rc\"; exit $1; }\n" + "trap '_ifsp_end 129' 1\n" + "trap '_ifsp_end 130' 2\n"
                + "trap '_ifsp_end 143' 15\n" + "/QOpenSys/usr/bin/sh -c 'echo $PPID' > \"$_ifsp_f.start\"\n",
            TerminalBuildCommands.getScript("/home/it's/ifsprojects_"));
    }

    @Test
    public void testWrap() {
        assertEquals("( _ifsp=1791972000000_42; . '/home/jsmith/.ifsprojects/tmp/ifsprojects.sh'; makei build; _ifsp_end $? )",
            TerminalBuildCommands.wrap(" makei build ", "1791972000000_42", SCRIPT_FILE));
        assertEquals("( _ifsp=1_2; . '" + SCRIPT_FILE + "'; makei compile -f 'a b.rpgle' && echo ok; _ifsp_end $? )",
            TerminalBuildCommands.wrap("makei compile -f 'a b.rpgle' && echo ok", "1_2", SCRIPT_FILE));
        assertEquals("( _ifsp=1_2; . '/p/it'\\''s.sh'; gmake LIB=JSMITH#CL; _ifsp_end $? )",
            TerminalBuildCommands.wrap("gmake LIB=JSMITH#CL ; ;", "1_2", "/p/it's.sh"));
    }

    @Test
    public void testWrap_notPossible() {
        assertNull(TerminalBuildCommands.wrap(null, "1_2", SCRIPT_FILE));
        assertNull(TerminalBuildCommands.wrap(" ; ", "1_2", SCRIPT_FILE));
        assertNull(TerminalBuildCommands.wrap("makei build # all", "1_2", SCRIPT_FILE));
        assertNull(TerminalBuildCommands.wrap("#makei build", "1_2", SCRIPT_FILE));
        assertNull(TerminalBuildCommands.wrap("makei build &", "1_2", SCRIPT_FILE));
        assertNull(TerminalBuildCommands.wrap("makei build &&", "1_2", SCRIPT_FILE));
        assertNull(TerminalBuildCommands.wrap("makei build |", "1_2", SCRIPT_FILE));
        assertNull(TerminalBuildCommands.wrap("makei build \\", "1_2", SCRIPT_FILE));
    }

    @Test
    public void testGetCheckScript() {
        assertEquals(
            "exec < /dev/null > /dev/null 2>&1; [ -n \"$(/QOpenSys/usr/bin/ps -p $$ -o pid=)\" ]"
                + " && [ -z \"$(/QOpenSys/usr/bin/ps -p 54394 -o pid=)\" ] && echo gone > '/t/ifsprojects_1_2.gone'; exit 0",
            TerminalBuildCommands.getCheckScript(54394, "/t/ifsprojects_1_2.gone"));
    }

    @Test
    public void testParseProcessId() {
        assertEquals(Integer.valueOf(54394), TerminalBuildCommands.parseProcessId("54394\n"));
        assertEquals(Integer.valueOf(7), TerminalBuildCommands.parseProcessId(" 7 "));
        assertNull(TerminalBuildCommands.parseProcessId(""));
        assertNull(TerminalBuildCommands.parseProcessId("0"));
        assertNull(TerminalBuildCommands.parseProcessId("-1"));
        assertNull(TerminalBuildCommands.parseProcessId("1234567890"));
        assertNull(TerminalBuildCommands.parseProcessId("x"));
        assertNull(TerminalBuildCommands.parseProcessId(null));
    }
}
