/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.tools400.ifsprojects.export.ExportCommands;

public class ExportCommandsTest {

    @Test
    public void testGetCreateCommands() {
        assertEquals("CRTLIB LIB(JSMITH#CL)", ExportCommands.getCreateLibraryCommand("JSMITH#CL"));
        assertEquals("CRTSRCPF FILE(JSMITH#CL/QRPGLESRC) RCDLEN(112) CCSID(1141)",
            ExportCommands.getCreateSourceFileCommand("JSMITH#CL", "QRPGLESRC", 112, 1141));
    }

    @Test
    public void testGetAddMemberCommand() {
        assertEquals("ADDPFM FILE(JSMITH#CL/QRPGLESRC) MBR(HELLO) SRCTYPE(RPGLE) TEXT('It''s a test')",
            ExportCommands.getAddMemberCommand("JSMITH#CL", "QRPGLESRC", "HELLO", "RPGLE", "It's a test"));
        assertEquals("ADDPFM FILE(JSMITH#CL/INC) MBR(CONSTS) SRCTYPE(RPGLE) TEXT(*BLANK)",
            ExportCommands.getAddMemberCommand("JSMITH#CL", "INC", "CONSTS", "RPGLE", null));
        assertEquals("ADDPFM FILE(JSMITH#CL/INC) MBR(CONSTS) SRCTYPE(RPGLE) TEXT(*BLANK)",
            ExportCommands.getAddMemberCommand("JSMITH#CL", "INC", "CONSTS", "RPGLE", "  "));
    }

    @Test
    public void testGetCopyCommand() {
        assertEquals(
            "CPYFRMSTMF FROMSTMF('/home/jsmith/.ifsprojects/tmp/ifsprojects_1_2.export')"
                + " TOMBR('/QSYS.LIB/JSMITH#CL.LIB/QCLSRC.FILE/START.MBR') MBROPT(*REPLACE)",
            ExportCommands.getCopyCommand("/home/jsmith/.ifsprojects/tmp/ifsprojects_1_2.export", "JSMITH#CL", "QCLSRC", "START"));
        assertEquals("CPYFRMSTMF FROMSTMF('/home/jsmith/it''s/a.clle') TOMBR('/QSYS.LIB/JSMITH#CL.LIB/QCLSRC.FILE/A.MBR') MBROPT(*REPLACE)",
            ExportCommands.getCopyCommand("/home/jsmith/it's/a.clle", "JSMITH#CL", "QCLSRC", "A"));
    }

    @Test
    public void testGetChangeMemberCommand() {
        assertEquals("CHGPFM FILE(JSMITH#CL/QRPGLESRC) MBR(HELLO) SRCTYPE(SQLRPGLE) TEXT('Hello')",
            ExportCommands.getChangeMemberCommand("JSMITH#CL", "QRPGLESRC", "HELLO", "SQLRPGLE", "Hello"));
        assertEquals("CHGPFM FILE(JSMITH#CL/QRPGLESRC) MBR(HELLO) SRCTYPE(RPGLE)",
            ExportCommands.getChangeMemberCommand("JSMITH#CL", "QRPGLESRC", "HELLO", "RPGLE", null));
    }

    @Test
    public void testGetRemoveMemberCommand() {
        assertEquals("RMVM FILE(JSMITH#CL/QRPGLESRC) MBR(HELLO)", ExportCommands.getRemoveMemberCommand("JSMITH#CL", "QRPGLESRC", "HELLO"));
    }
}
