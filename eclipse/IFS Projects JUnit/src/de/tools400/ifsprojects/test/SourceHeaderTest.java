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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import de.tools400.ifsprojects.sources.headers.CreateCommand;
import de.tools400.ifsprojects.sources.headers.ISourceHeaderParser;
import de.tools400.ifsprojects.sources.headers.SourceHeader;
import de.tools400.ifsprojects.sources.headers.SourceHeaderParsers;
import de.tools400.ifsprojects.strpreprc.StrPrePrcHeaderParser;

public class SourceHeaderTest {

    private final SourceHeaderParsers parsers = new SourceHeaderParsers(new StrPrePrcHeaderParser());

    @Test
    public void testCommand_name() {

        assertEquals("CRTSQLRPGI", CreateCommand.parse("CRTSQLRPGI OBJ(&LI/&OB)").getName());
        assertEquals("CRTRPGMOD", CreateCommand.parse("crtrpgmod module(&li/&ob)").getName());
        assertEquals("CRTBNDRPG", CreateCommand.parse("MYLIB/CRTBNDRPG PGM(&LI/&OB)").getName());
        assertEquals("CRTBNDRPG", CreateCommand.parse("CRTBNDRPG").getName());

        assertNull(CreateCommand.parse("   "));
        assertNull(CreateCommand.parse(null));
    }

    @Test
    public void testCommand_isCreateCommand() {

        assertTrue(CreateCommand.parse("CRTCLMOD MODULE(&LI/&OB)").isCreateCommand());
        assertFalse(CreateCommand.parse("CHGOBJOWN OBJ(&LI/&OB) OBJTYPE(*PGM)").isCreateCommand());
    }

    @Test
    public void testCommand_parameters() {

        CreateCommand command = CreateCommand.parse("CRTSQLRPGI OBJ(&LI/&OB) OBJTYPE( *MODULE ) INCFILE(&SL/QCPYSRC) DBGVIEW(*LIST)");

        assertEquals("&LI/&OB", command.getParameter("OBJ"));
        assertEquals("*MODULE", command.getParameter("OBJTYPE"));
        assertEquals("&SL/QCPYSRC", command.getParameter("incfile"));
        assertEquals("*LIST", command.getParameter("DBGVIEW"));
        assertNull(command.getParameter("SRCSTMF"));

        assertEquals("[OBJ, OBJTYPE, INCFILE, DBGVIEW]", command.getParameters().keySet().toString());
    }

    @Test
    public void testCommand_nestedParenthesesAndQuotes() {

        CreateCommand command = CreateCommand.parse("CRTPGM PGM(&LI/&OB) BNDDIR((*LIBL/MAIN) (QC2LE)) TEXT('Order (new)') ACTGRP(*NEW)");

        assertEquals("(*LIBL/MAIN) (QC2LE)", command.getParameter("BNDDIR"));
        assertEquals("'Order (new)'", command.getParameter("TEXT"));
        assertEquals("*NEW", command.getParameter("ACTGRP"));
    }

    @Test
    public void testHeader_isModule() {

        assertTrue(header("// >>CRTCMD<< CRTRPGMOD MODULE(&LI/&OB);\n").isModule());
        assertTrue(header("// >>CRTCMD<< CRTSQLRPGI OBJ(&LI/&OB) OBJTYPE(*MODULE);\n").isModule());
        assertFalse(header("// >>CRTCMD<< CRTBNDRPG PGM(&LI/&OB);\n").isModule());
        assertFalse(header("**FREE\ndsply 'Hello';\n").isModule());
    }

    @Test
    public void testHeader_getIncludeFile() {

        assertEquals("QCPYSRC", header("// >>CRTCMD<< CRTSQLRPGI OBJ(&LI/&OB) INCFILE(&SL/QCPYSRC);\n").getIncludeFile());
        assertEquals("QCPYSRC", header("// >>CRTCMD<< CRTSQLRPGI OBJ(&LI/&OB) INCFILE(qcpysrc);\n").getIncludeFile());
        assertEquals("*LIBL/QCPYSRC", header("// >>CRTCMD<< CRTSQLRPGI OBJ(&LI/&OB) INCFILE(*LIBL/QCPYSRC);\n").getIncludeFile());
        assertNull(header("// >>CRTCMD<< CRTSQLRPGI OBJ(&LI/&OB) INCFILE(*SRCFILE);\n").getIncludeFile());
        assertNull(header("// >>CRTCMD<< CRTSQLRPGI OBJ(&LI/&OB);\n").getIncludeFile());
    }

    @Test
    public void testHeader_createCommandIsTheLastOne() {

        // The modules of a program are created before it is bound.
        SourceHeader header = header("// >>CRTCMD<< CRTRPGMOD MODULE(&LI/&OB);\n" //
            + "// >>CRTCMD<< CRTPGM PGM(&LI/&OB) MODULE(&OB);\n");

        assertEquals(2, header.getCommands().size());
        assertEquals("CRTPGM", header.getCreateCommand().getName());
        assertFalse(header.isModule());
    }

    @Test
    public void testParser_freeFormRpg() {

        SourceHeader header = header("**FREE\n" //
            + "// >>PRE-COMPILER<<\n" //
            + "// >>CRTCMD<< CRTSQLRPGI OBJ(&LI/&OB) SRCSTMF('&STMF') +\n" //
            + "//            INCFILE(&SL/QCPYSRC) RPGPPOPT(*LVL2);\n" //
            + "// >>END-PRE-COMPILER<<\n" //
            + "dsply 'Hello';\n");

        assertEquals("CRTSQLRPGI", header.getCreateCommand().getName());
        assertEquals("'&STMF'", header.getCreateCommand().getParameter("SRCSTMF"));
        assertEquals("QCPYSRC", header.getIncludeFile());
    }

    @Test
    public void testParser_fixedFormRpg() {

        SourceHeader header = header("      * >>PRE-COMPILER<<\n" //
            + "      *   >>CRTCMD<<  CRTRPGMOD MODULE(&LI/&OB) +\n" //
            + "      *                         SRCSTMF('&STMF');\n" //
            + "      * >>END-PRE-COMPILER<<\n" //
            + "     H NOMAIN\n");

        assertEquals("CRTRPGMOD", header.getCreateCommand().getName());
        assertEquals("'&STMF'", header.getCreateCommand().getParameter("SRCSTMF"));
        assertTrue(header.isModule());
    }

    @Test
    public void testParser_cl() {

        SourceHeader header = header("/* >>PRE-COMPILER<<                                     */\n" //
            + "/*   >>CRTCMD<< CRTCLMOD MODULE(&LI/&OB) +               */\n" //
            + "/*              SRCSTMF('&STMF');                       */\n" //
            + "/* >>END-PRE-COMPILER<<                                 */\n" //
            + "PGM\nENDPGM\n");

        assertEquals("CRTCLMOD", header.getCreateCommand().getName());
        assertEquals("'&STMF'", header.getCreateCommand().getParameter("SRCSTMF"));
    }

    @Test
    public void testParser_endsAtTheSource() {

        // The command is not terminated, the source must not become part of it.
        SourceHeader header = header("// >>CRTCMD<< CRTBNDRPG PGM(&LI/&OB) +\n" //
            + "**FREE\n" //
            + "dsply 'INCFILE(QOTHER)';\n");

        assertEquals("CRTBNDRPG", header.getCreateCommand().getName());
        assertNull(header.getIncludeFile());
    }

    @Test
    public void testParser_endsAtTheNextTag() {

        SourceHeader header = header("// >>CRTCMD<< CRTBNDRPG PGM(&LI/&OB) +\n" //
            + "// >>END-PRE-COMPILER<<\n");

        assertEquals("CRTBNDRPG", header.getCreateCommand().getName());
        assertEquals("&LI/&OB", header.getCreateCommand().getParameter("PGM"));
    }

    @Test
    public void testParser_noHeader() {

        assertTrue(header("**FREE\ndsply 'Hello';\n").isEmpty());
        assertTrue(header("").isEmpty());
    }

    @Test
    public void testParser_behindTheHeaderLines() {

        StringBuilder content = new StringBuilder("**FREE\n");
        for (int i = 0; i < 120; i++) {
            content.append("// line ").append(i).append('\n');
        }
        content.append("// >>CRTCMD<< CRTRPGMOD MODULE(&LI/&OB);\n");

        assertTrue(header(content.toString()).isEmpty());
    }

    @Test
    public void testParsers_withoutAnyParser() {

        SourceHeader header = new SourceHeaderParsers().parse("// >>CRTCMD<< CRTRPGMOD MODULE(&LI/&OB);\n", "RPGLE");

        assertSame(SourceHeader.NONE, header);
        assertFalse(header.isModule());
        assertNull(header.getIncludeFile());
    }

    @Test
    public void testParsers_withoutContent() {
        assertSame(SourceHeader.NONE, parsers.parse(null, "RPGLE"));
    }

    @Test
    public void testParsers_eachToolHasItsOwnParser() {

        SourceHeaderParsers parsers = new SourceHeaderParsers(new StrPrePrcHeaderParser(), new OtherToolParser());

        assertEquals("CRTRPGMOD", parsers.parse("// >>CRTCMD<< CRTRPGMOD MODULE(&LI/&OB);\n", "RPGLE").getCreateCommand().getName());
        assertEquals("CRTBNDRPG", parsers.parse("     * BUILD: CRTBNDRPG PGM(&LI/&OB)\n", "RPGLE").getCreateCommand().getName());
    }

    private SourceHeader header(String aContent) {
        return parsers.parse(aContent, "RPGLE");
    }

    /**
     * Parser of another build tool, which shows, how a plug-in contributes one.
     */
    private static final class OtherToolParser implements ISourceHeaderParser {

        public List<CreateCommand> parse(String aContent, String aSourceType) {

            List<CreateCommand> commands = new ArrayList<CreateCommand>();
            for (String line : aContent.split("\n")) {
                int tag = line.indexOf("BUILD:");
                if (tag >= 0) {
                    commands.add(CreateCommand.parse(line.substring(tag + "BUILD:".length())));
                }
            }

            return commands;
        }
    }
}
