/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.cplcas.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.tools400.ifsprojects.cplcas.CplCasHeaderParser;
import de.tools400.ifsprojects.sources.headers.SourceHeader;
import de.tools400.ifsprojects.sources.headers.SourceHeaderParsers;

/**
 * The header of the <code>CPLCAS</code> command of Denis Robitaille. The
 * examples are taken from its documentation.
 */
public class CplCasHeaderParserTest {

    private final SourceHeaderParsers parsers = new SourceHeaderParsers(new CplCasHeaderParser());

    @Test
    public void testRpgProgram() {

        SourceHeader header = header("      *%%%* Instructions for the CP-option - CRTRPGPGM\n" //
            + "      *%%% CRTRPGPGM\n" //
            + "      *%%%   PGM(%LIBO/%OBJ)\n" //
            + "      *%%%   SRCFILE(%LIBS/%FIC)\n" //
            + "      *%%%   SRCMBR(%MBR)\n" //
            + "      *%%%&\n" //
            + "     H DATEDIT(*YMD)\n");

        assertEquals(1, header.getCommands().size());
        assertEquals("CRTRPGPGM", header.getCreateCommand().getName());
        assertEquals("&LIBO/&OBJ", header.getCreateCommand().getParameter("PGM"));
        assertEquals("&LIBS/&FIC", header.getCreateCommand().getParameter("SRCFILE"));
        assertEquals("&MBR", header.getCreateCommand().getParameter("SRCMBR"));
        assertFalse(header.isModule());
    }

    @Test
    public void testOverride() {

        SourceHeader header = header("      *%%%* Instructions for the CP-option - CRTRPGPGM with override\n" //
            + "      *%%% OVRDBF FILE(TESTFILP1A)\n" //
            + "      *%%%        TOFILE(*LIBL/TESTFILP1)\n" //
            + "      *%%%#\n" //
            + "      *%%% CRTRPGPGM\n" //
            + "      *%%%   PGM(%LIBO/%OBJ)\n" //
            + "      *%%%&\n");

        assertEquals(2, header.getCommands().size());
        assertEquals("OVRDBF", header.getCommands().get(0).getName());
        assertEquals("*LIBL/TESTFILP1", header.getCommands().get(0).getParameter("TOFILE"));
        assertEquals("CRTRPGPGM", header.getCreateCommand().getName());
    }

    @Test
    public void testModuleAndProgram() {

        // A parameter, that is switched off with '**', is a comment.
        SourceHeader header = header(" *%%%* Instruction pour compilation -------------------+\n" //
            + " *%%% CRTRPGMOD\n" //
            + " *%%%   MODULE(%LIBO/%OBJ)\n" //
            + " *%%%   OPTIMIZE(*FULL)\n" //
            + " *%%%** DBGVIEW(*ALL)\n" //
            + " *%%%#\n" //
            + " *%%% CRTPGM\n" //
            + " *%%%   PGM(%LIBO/%OBJ)\n" //
            + " *%%%   ACTGRP(*CALLER)\n" //
            + " *%%%&\n");

        assertEquals(2, header.getCommands().size());
        assertEquals("*FULL", header.getCommands().get(0).getParameter("OPTIMIZE"));
        assertNull(header.getCommands().get(0).getParameter("DBGVIEW"));
        assertEquals("CRTPGM", header.getCreateCommand().getName());
        assertFalse(header.isModule());
    }

    @Test
    public void testModule() {

        assertTrue(header("      *%%% CRTRPGMOD\n      *%%%   MODULE(%LIBO/%OBJ)\n      *%%%&\n").isModule());
        assertTrue(header("      *%%% CRTSQLRPGI OBJ(%LIBO/%OBJ) OBJTYPE(*MODULE)\n      *%%%&\n").isModule());
    }

    @Test
    public void testFramedDds() {

        // The frame of the comment block stands behind column 61.
        SourceHeader header = header("     A****<INSTRUCTIONS DE COMPILATION>**************************\n" //
            + "     A*%%%* ------------------------------------------------+  *\n" //
            + "     A*%%% DLTF                                                *\n" //
            + "     A*%%%   FILE(%LIBO/%OBJ)                                  *\n" //
            + "     A*%%%#                                                    *\n" //
            + "     A*%%% CRTPF                                               *\n" //
            + "     A*%%%   FILE(%LIBO/%OBJ)                                  *\n" //
            + "     A*%%%   SRCFILE(%LIBS/%FIC)                               *\n" //
            + "     A*%%%   SRCMBR(%MBR)                                      *\n" //
            + "     A*%%%   REUSEDLT(*YES)                                    *\n" //
            + "     A*%%%&                                                    *\n" //
            + "     A***********************************************************\n");

        assertEquals(2, header.getCommands().size());
        assertEquals("CRTPF", header.getCreateCommand().getName());
        assertEquals("*YES", header.getCreateCommand().getParameter("REUSEDLT"));
        assertEquals("&LIBS/&FIC", header.getCreateCommand().getParameter("SRCFILE"));
    }

    @Test
    public void testMultipleExecution() {

        SourceHeader header = header("     A*%%%* Instruction pour compilation -------------------+\n" //
            + "     A*%%% CRTDSPF\n" //
            + "     A*%%%   FILE(%LIBO/%OBJ)\n" //
            + "     A*%%%@\n" //
            + "     A*%%%&\n");

        assertEquals(1, header.getCommands().size());
        assertEquals("CRTDSPF", header.getCreateCommand().getName());
        assertEquals("&LIBO/&OBJ", header.getCreateCommand().getParameter("FILE"));
    }

    @Test
    public void testPanelGroup() {

        SourceHeader header = header(".* *%%%* Instruction for the CP-option - CRTPNLGRP\n" //
            + ".* *%%% CRTPNLGRP\n" //
            + ".* *%%%   PNLGRP(%LIBO/%OBJ)\n" //
            + ".* *%%%   REPLACE(*YES)\n" //
            + ".* *%%%&\n" //
            + ":PNLGRP.\n");

        assertEquals("CRTPNLGRP", header.getCreateCommand().getName());
        assertEquals("&LIBO/&OBJ", header.getCreateCommand().getParameter("PNLGRP"));
    }

    @Test
    public void testEndOfHeader() {

        // Behind '&', no command is read.
        SourceHeader header = header("      *%%% CRTBNDRPG PGM(%LIBO/%OBJ)\n" //
            + "      *%%%&\n" //
            + "      *%%% CRTRPGMOD MODULE(%LIBO/%OBJ)\n" //
            + "      *%%%&\n");

        assertEquals(1, header.getCommands().size());
        assertEquals("CRTBNDRPG", header.getCreateCommand().getName());
    }

    @Test
    public void testIncludeFile() {

        assertEquals("QCPYSRC", header("      *%%% CRTSQLRPGI OBJ(%LIBO/%OBJ) INCFILE(%LIBS/QCPYSRC)\n      *%%%&\n")
            .getIncludeFile());
        assertNull(header("      *%%% CRTSQLRPGI OBJ(%LIBO/%OBJ) INCFILE(*SRCFILE)\n      *%%%&\n").getIncludeFile());
    }

    @Test
    public void testSubstitutionVariables() {

        SourceHeader header = header("      *%%% CRTBNDRPG PGM(%libo/%obj) TEXT('50% of %OBJECTS')\n      *%%%&\n");

        assertEquals("&libo/&obj", header.getCreateCommand().getParameter("PGM"));
        assertEquals("'50% of %OBJECTS'", header.getCreateCommand().getParameter("TEXT"));
    }

    @Test
    public void testEndsAtTheSource() {

        // The command is not terminated, the source must not become part of it.
        SourceHeader header = header("      *%%% CRTBNDRPG PGM(%LIBO/%OBJ)\n" //
            + "     H DFTACTGRP(*NO)\n" //
            + "      *%%% CRTRPGMOD MODULE(%LIBO/%OBJ)\n");

        assertEquals(2, header.getCommands().size());
        assertEquals("&LIBO/&OBJ", header.getCommands().get(0).getParameter("PGM"));
        assertNull(header.getCommands().get(0).getParameter("DFTACTGRP"));
    }

    @Test
    public void testNoHeader() {

        assertTrue(header("**FREE\ndsply 'Hello';\n").isEmpty());
        assertTrue(header("      *_> CRTBNDRPG PGM(@5/@4)\n").isEmpty());
        assertTrue(header("      *%%%* Instructions for the CP-option\n      *%%%&\n").isEmpty());
        assertTrue(header("").isEmpty());
    }

    @Test
    public void testBehindTheHeaderLines() {

        StringBuilder content = new StringBuilder("**FREE\n");
        for (int i = 0; i < 120; i++) {
            content.append("// line ").append(i).append('\n');
        }
        content.append("      *%%% CRTBNDRPG PGM(%LIBO/%OBJ)\n      *%%%&\n");

        assertTrue(header(content.toString()).isEmpty());
    }

    private SourceHeader header(String aContent) {
        return parsers.parse(aContent, "RPGLE");
    }
}
