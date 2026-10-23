/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.strpreprc.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.tools400.ifsprojects.sources.headers.SourceHeader;
import de.tools400.ifsprojects.sources.headers.SourceHeaderParsers;
import de.tools400.ifsprojects.strpreprc.StrPrePrcHeaderParser;

/**
 * The <code>STRPREPRC</code> headers of the source types, that the migration
 * knows. The headers are read with the registry, as the migration does it.
 */
public class StrPrePrcHeaderParserTest {

    private final SourceHeaderParsers parsers = new SourceHeaderParsers(new StrPrePrcHeaderParser());

    @Test
    public void testFreeFormRpg() {

        SourceHeader header = header("**FREE\n" //
            + "// >>PRE-COMPILER<<\n" //
            + "// >>CRTCMD<< CRTSQLRPGI OBJ(&LI/&OB) SRCSTMF('&STMF') +\n" //
            + "//            INCFILE(&SL/QCPYSRC) RPGPPOPT(*LVL2);\n" //
            + "// >>END-PRE-COMPILER<<\n" //
            + "dsply 'Hello';\n");

        assertEquals("CRTSQLRPGI", header.getCreateCommand().getName());
        assertEquals("'&STMF'", header.getCreateCommand().getParameter("SRCSTMF"));
        assertEquals("*LVL2", header.getCreateCommand().getParameter("RPGPPOPT"));
        assertEquals("QCPYSRC", header.getIncludeFile());
        assertFalse(header.isModule());
    }

    @Test
    public void testFixedFormRpg() {

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
    public void testCl() {

        SourceHeader header = header("/* >>PRE-COMPILER<<                                     */\n" //
            + "/*   >>CRTCMD<< CRTCLMOD MODULE(&LI/&OB) +               */\n" //
            + "/*              SRCSTMF('&STMF');                       */\n" //
            + "/* >>END-PRE-COMPILER<<                                 */\n" //
            + "PGM\nENDPGM\n");

        assertEquals("CRTCLMOD", header.getCreateCommand().getName());
        assertEquals("'&STMF'", header.getCreateCommand().getParameter("SRCSTMF"));
        assertTrue(header.isModule());
    }

    @Test
    public void testClOnOneLine() {

        SourceHeader header = header("/* >>CRTCMD<< CRTCLMOD MODULE(&LI/&OB); */\nPGM\nENDPGM\n");

        assertEquals("CRTCLMOD", header.getCreateCommand().getName());
        assertTrue(header.isModule());
    }

    @Test
    public void testObjectTypeOfTheCommand() {

        assertTrue(header("**FREE\n// >>CRTCMD<< CRTSQLRPGI OBJ(&LI/&OB) OBJTYPE(*MODULE);\n").isModule());
        assertFalse(header("**FREE\n// >>CRTCMD<< CRTBNDRPG PGM(&LI/&OB);\n").isModule());
    }

    @Test
    public void testSeveralCommands() {

        // The modules of a program are created before it is bound.
        SourceHeader header = header("// >>CRTCMD<< CRTRPGMOD MODULE(&LI/&OB);\n" //
            + "// >>CRTCMD<< CRTPGM PGM(&LI/&OB) MODULE(&OB);\n");

        assertEquals(2, header.getCommands().size());
        assertEquals("CRTPGM", header.getCreateCommand().getName());
        assertFalse(header.isModule());
    }

    @Test
    public void testEndsAtTheSource() {

        // The command is not terminated, the source must not become part of it.
        SourceHeader header = header("// >>CRTCMD<< CRTBNDRPG PGM(&LI/&OB) +\n" //
            + "**FREE\n" //
            + "dsply 'INCFILE(QOTHER)';\n");

        assertEquals("CRTBNDRPG", header.getCreateCommand().getName());
        assertNull(header.getIncludeFile());
    }

    @Test
    public void testEndsAtTheNextTag() {

        SourceHeader header = header("// >>CRTCMD<< CRTBNDRPG PGM(&LI/&OB) +\n" //
            + "// >>END-PRE-COMPILER<<\n");

        assertEquals("CRTBNDRPG", header.getCreateCommand().getName());
        assertEquals("&LI/&OB", header.getCreateCommand().getParameter("PGM"));
    }

    @Test
    public void testIncludeFile() {

        assertEquals("QCPYSRC", header("// >>CRTCMD<< CRTSQLRPGI OBJ(&LI/&OB) INCFILE(&SL/QCPYSRC);\n").getIncludeFile());
        assertEquals("QCPYSRC", header("// >>CRTCMD<< CRTSQLRPGI OBJ(&LI/&OB) INCFILE(qcpysrc);\n").getIncludeFile());
        assertEquals("*LIBL/QCPYSRC", header("// >>CRTCMD<< CRTSQLRPGI OBJ(&LI/&OB) INCFILE(*LIBL/QCPYSRC);\n").getIncludeFile());
        assertNull(header("// >>CRTCMD<< CRTSQLRPGI OBJ(&LI/&OB) INCFILE(*SRCFILE);\n").getIncludeFile());
        assertNull(header("// >>CRTCMD<< CRTSQLRPGI OBJ(&LI/&OB);\n").getIncludeFile());
    }

    @Test
    public void testNoHeader() {

        assertTrue(header("**FREE\ndsply 'Hello';\n").isEmpty());
        assertTrue(header("").isEmpty());
    }

    @Test
    public void testBehindTheHeaderLines() {

        StringBuilder content = new StringBuilder("**FREE\n");
        for (int i = 0; i < 120; i++) {
            content.append("// line ").append(i).append('\n');
        }
        content.append("// >>CRTCMD<< CRTRPGMOD MODULE(&LI/&OB);\n");

        assertTrue(header(content.toString()).isEmpty());
    }

    private SourceHeader header(String aContent) {
        return parsers.parse(aContent, "RPGLE");
    }
}
