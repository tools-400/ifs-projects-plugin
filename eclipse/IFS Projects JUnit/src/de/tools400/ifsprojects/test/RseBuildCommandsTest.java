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

import de.tools400.ifsprojects.build.BuildMode;
import de.tools400.ifsprojects.build.RseBuildCommands;
import de.tools400.ifsprojects.project.IProjConfig;

public class RseBuildCommandsTest {

    @Test
    public void testGetScript_bash() {
        assertEquals("exec < /dev/null >> '/home/jsmith/.ifsprojects/tmp/b.log' 2>&1; echo $$ > '/home/jsmith/.ifsprojects/tmp/b.pid';"
            + " export HOME='/home/jsmith';"
            + " [ -f /QOpenSys/etc/profile ] && . /QOpenSys/etc/profile;"
            + " if [ -f \"$HOME/.bash_profile\" ]; then . \"$HOME/.bash_profile\"; elif [ -f \"$HOME/.bash_login\" ]; then . \"$HOME/.bash_login\";"
            + " elif [ -f \"$HOME/.profile\" ]; then . \"$HOME/.profile\"; fi;"
            + " cd '/home/jsmith/git/dspsrc' && makei build; echo $? > '/home/jsmith/.ifsprojects/tmp/b.rc'",
            RseBuildCommands.getScript(RseBuildCommands.BASH, "/home/jsmith", "/home/jsmith/.ifsprojects/tmp/b.log",
                "/home/jsmith/.ifsprojects/tmp/b.rc", "/home/jsmith/.ifsprojects/tmp/b.pid", "/home/jsmith/git/dspsrc", " makei build "));
    }

    @Test
    public void testGetScript_sh() {
        assertEquals("exec < /dev/null >> '/t/b.log' 2>&1; echo $$ > '/t/b.pid'; export HOME='/home/jsmith';"
            + " [ -f /QOpenSys/etc/profile ] && . /QOpenSys/etc/profile; [ -f \"$HOME/.profile\" ] && . \"$HOME/.profile\";"
            + " cd '/p/it'\\''s' && gmake; echo $? > '/t/b.rc'",
            RseBuildCommands.getScript(RseBuildCommands.SH, "/home/jsmith", "/t/b.log", "/t/b.rc", "/t/b.pid", "/p/it's", "gmake"));
    }

    @Test
    public void testGetStopScript() {
        assertEquals("exec < /dev/null > /dev/null 2>&1; pid=$(cat '/t/b.pid'); [ -n \"$pid\" ] || exit 0;"
            + " tree() { echo $1; for child in $(ps -e -o pid= -o ppid= | awk -v p=$1 '$2 == p { print $1 }'); do tree $child; done; };"
            + " kill -TERM $(tree $pid); exit 0", RseBuildCommands.getStopScript("/t/b.pid"));
    }

    @Test
    public void testGetCallCommand() {
        assertEquals("CALL PGM(QP2SHELL2) PARM('/QOpenSys/pkgs/bin/bash' '-c' 'cd ''/p'' && ENV=''x'' && export ENV && makei build')",
            RseBuildCommands.getCallCommand(RseBuildCommands.BASH, "cd '/p' && ENV='x' && export ENV && makei build"));
    }

    @Test
    public void testGetObjectLibrary() throws Exception {

        Map<String, String> variables = new LinkedHashMap<String, String>();
        variables.put("LIB", "dspsrc");

        assertEquals("JSMITH#CL", RseBuildCommands.getObjectLibrary(IProjConfig.parse("{\"objlib\": \"JSMITH#CL\", \"curlib\": \"QGPL\"}"), variables));
        assertEquals("QGPL", RseBuildCommands.getObjectLibrary(IProjConfig.parse("{\"curlib\": \"QGPL\"}"), variables));
        assertEquals("DSPSRC", RseBuildCommands.getObjectLibrary(IProjConfig.parse("{\"objlib\": \"&LIB\"}"), variables));
        assertNull(RseBuildCommands.getObjectLibrary(IProjConfig.parse("{\"objlib\": \"*CURLIB\"}"), variables));
        assertNull(RseBuildCommands.getObjectLibrary(IProjConfig.parse("{\"objlib\": \"&UNDEFINED\"}"), variables));
        assertNull(RseBuildCommands.getObjectLibrary(IProjConfig.parse("{}"), variables));
    }

    @Test
    public void testParseExitCode() {
        assertEquals(Integer.valueOf(0), RseBuildCommands.parseExitCode("0\n"));
        assertEquals(Integer.valueOf(2), RseBuildCommands.parseExitCode(" 2 "));
        assertNull(RseBuildCommands.parseExitCode(""));
        assertNull(RseBuildCommands.parseExitCode("x"));
        assertNull(RseBuildCommands.parseExitCode(null));
    }

    @Test
    public void testBuildMode() {
        assertEquals(BuildMode.RSE, BuildMode.fromValue("rse"));
        assertEquals(BuildMode.TERMINAL, BuildMode.fromValue("terminal"));
        assertEquals(BuildMode.TERMINAL, BuildMode.fromValue("unknown"));
        assertEquals(BuildMode.TERMINAL, BuildMode.fromValue(null));
    }
}
