/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.compile.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.tools400.ifsprojects.compile.CompileHeaderParser;
import de.tools400.ifsprojects.sources.headers.SourceHeader;
import de.tools400.ifsprojects.sources.headers.SourceHeaderParsers;

/**
 * The header of the <code>COMPILE</code> command of Alan Campin. The examples
 * are taken from its documentation.
 */
public class CompileHeaderParserTest {

    private final SourceHeaderParsers parsers = new SourceHeaderParsers(new CompileHeaderParser());

    @Test
    public void testRpgModule() {

        SourceHeader header = header("      *_> CNLLSTSPLF SRCFILE(@2/@1) SRCMBR(@3)\n" //
            + "      *_> DLTMOD MODULE(@5/@4)\n" //
            + "      *_> CRTRPGMOD MODULE(@5/@4) SRCFILE(@2/@1) SRCMBR(@3) +\n" //
            + "      *_>   OPTION(*EVENTF) DBGVIEW(@9) OPTIMIZE(@8)\n" //
            + "     H NOMAIN\n");

        assertEquals(3, header.getCommands().size());
        assertEquals("CRTRPGMOD", header.getCreateCommand().getName());
        assertEquals("&5/&4", header.getCreateCommand().getParameter("MODULE"));
        assertEquals("&2/&1", header.getCreateCommand().getParameter("SRCFILE"));
        assertEquals("&9", header.getCreateCommand().getParameter("DBGVIEW"));
        assertTrue(header.isModule());
    }

    @Test
    public void testSqlRpgModule() {

        SourceHeader header = header("      *_> CNLLSTSPLF SRCFILE(@2/@1) SRCMBR(@3)\n" //
            + "      *_> DLTMOD MODULE(@5/@4)\n" //
            + "      *_> CRTSQLRPGI OBJ(@5/@4) +\n" //
            + "      *_>   SRCFILE(@2/@1) SRCMBR(@3) +\n" //
            + "      *_>   OBJTYPE(*MODULE) OPTION(*EVENTF) COMMIT(*NONE) +\n" //
            + "      *_>   DBGVIEW(*SOURCE) DATFMT(*ISO) OUTPUT(*PRINT)\n");

        assertEquals("CRTSQLRPGI", header.getCreateCommand().getName());
        assertEquals("*NONE", header.getCreateCommand().getParameter("COMMIT"));
        assertEquals("*PRINT", header.getCreateCommand().getParameter("OUTPUT"));
        assertTrue(header.isModule());
    }

    @Test
    public void testClModule() {

        // In CL, each line of the header is a comment of its own.
        SourceHeader header = header("/* *_> CNLLSTSPLF SRCFILE(@2/@1) SRCMBR(@3)                     */\n" //
            + "/* *_> DLTMOD MODULE(@5/@4)                                        */\n" //
            + "/* *_> CRTCLMOD MODULE(@5/@4) SRCFILE(@2/@1) SRCMBR(@3) +           */\n" //
            + "/* *_>   DBGVIEW(@9) OPTIMIZE(@8)                                   */\n" //
            + "PGM\nENDPGM\n");

        assertEquals("CRTCLMOD", header.getCreateCommand().getName());
        assertEquals("&2/&1", header.getCreateCommand().getParameter("SRCFILE"));
        assertEquals("&8", header.getCreateCommand().getParameter("OPTIMIZE"));
        assertTrue(header.isModule());
    }

    @Test
    public void testProgramOfModules() {

        // CHKOBJECT creates the modules first; CRTPGM creates the object.
        SourceHeader header = header("      *_> DLTPGM PGM(@5/@4)\n" //
            + "      *_> CHKOBJECT OBJNAME(@5/TG0001_D01) OBJTYPE(*FILE) +\n" //
            + "      *_>   SRCFILE(@2/@1) SRCMBR(TG0001_D01)\n" //
            + "      *_> CHKOBJECT OBJNAME(@5/TG0001_M01) OBJTYPE(*MODULE) +\n" //
            + "      *_>   SRCFILE(@2/@1) SRCMBR(TG0001_M01)\n" //
            + "      *_> CRTPGM PGM(@5/@4) MODULE(TG0001_M01 TG0002_M01 +\n" //
            + "      *_>   TG0002_M02) +\n" //
            + "      *_>   ENTMOD(@5/@4) +\n" //
            + "      *_>   BNDSRVPGM(XVMSGF XVUSPC) ACTGRP(QILE)\n");

        assertEquals(4, header.getCommands().size());
        assertEquals("*MODULE", header.getCommands().get(2).getParameter("OBJTYPE"));
        assertEquals("CRTPGM", header.getCreateCommand().getName());
        assertEquals("TG0001_M01 TG0002_M01 TG0002_M02", header.getCreateCommand().getParameter("MODULE"));
        assertEquals("QILE", header.getCreateCommand().getParameter("ACTGRP"));
        assertFalse(header.isModule());
    }

    @Test
    public void testServiceProgram() {

        SourceHeader header = header("      *_> DLTSRVPGM SRVPGM(@5/@4)\n" //
            + "      *_> CRTSRVPGM SRVPGM(@5/@4) MODULE(XVIFSX_M01) +\n" //
            + "      *_>   SRCFILE(@2/@1) SRCMBR(XVIFSX_B) BNDSRVPGM(XVERRH) +\n" //
            + "      *_>   TEXT('Integrated File System-Common Functions.') +\n" //
            + "      *_>   ACTGRP(QILE) BNDDIR(QC2LE)\n");

        assertEquals("CRTSRVPGM", header.getCreateCommand().getName());
        assertEquals("'Integrated File System-Common Functions.'", header.getCreateCommand().getParameter("TEXT"));
        assertEquals("QC2LE", header.getCreateCommand().getParameter("BNDDIR"));
        assertFalse(header.isModule());
    }

    @Test
    public void testSqlScript() {

        // RUNSQLSTM creates no object of the source.
        SourceHeader header = header("/* *_> CNLLSTSPLF SRCFILE(@2/@1) SRCMBR(@3)                     */\n" //
            + "/* *_> RUNSQLSTM SRCFILE(@2/@1) SRCMBR(@3) +                        */\n" //
            + "/* *_>   DFTRDBCOL(@5) CLOSQLCSR(*ENDMOD) ERRLVL(20)                 */\n" //
            + "DROP TABLE OI_MACRO_PROGRAMS;\n");

        assertFalse(header.isEmpty());
        assertEquals("&5", header.getCommands().get(1).getParameter("DFTRDBCOL"));
        assertNull(header.getCreateCommand());
    }

    @Test
    public void testContinuationKeepingBlanks() {

        SourceHeader header = header("      *_> CRTPF FILE(@5/@4) SRCFILE(@2/@1) -\n" //
            + "      *_> SRCMBR(@3) MBR(*FILE) SIZE(10000 1000 999)\n");

        assertEquals("&2/&1", header.getCreateCommand().getParameter("SRCFILE"));
        assertEquals("&3", header.getCreateCommand().getParameter("SRCMBR"));
        assertEquals("10000 1000 999", header.getCreateCommand().getParameter("SIZE"));
    }

    @Test
    public void testContinuationSkippingBlanks() {

        // Like in CL, '+' joins a quoted string without the leading blanks.
        SourceHeader header = header("      *_> CRTPGM PGM(@5/@4) TEXT('Order +\n" //
            + "      *_>        entry')\n");

        assertEquals("'Order entry'", header.getCreateCommand().getParameter("TEXT"));
    }

    @Test
    public void testSubstitutionVariables() {

        SourceHeader header = header("      *_> CRTBNDRPG PGM(@15/@4) TEXT('mail@example.com')\n");

        assertEquals("&15/&4", header.getCreateCommand().getParameter("PGM"));
        assertEquals("'mail@example.com'", header.getCreateCommand().getParameter("TEXT"));
    }

    @Test
    public void testIncludeFile() {

        assertEquals("QCPYSRC", header("      *_> CRTSQLRPGI OBJ(@5/@4) INCFILE(@2/QCPYSRC)\n").getIncludeFile());
        assertEquals("MYLIB/QCPYSRC", header("      *_> CRTSQLRPGI OBJ(@5/@4) INCFILE(MYLIB/QCPYSRC)\n").getIncludeFile());
        assertNull(header("      *_> CRTSQLRPGI OBJ(@5/@4) INCFILE(*SRCFILE)\n").getIncludeFile());
        assertNull(header("      *_> CRTSQLRPGI OBJ(@5/@4)\n").getIncludeFile());
    }

    @Test
    public void testEndsAtTheSource() {

        // The command is not terminated, the source must not become part of it.
        SourceHeader header = header("      *_> CRTBNDRPG PGM(@5/@4) SRCFILE(@2/@1) +\n" //
            + "     H DFTACTGRP(*NO)\n" //
            + "      * INCFILE(QOTHER)\n");

        assertEquals("CRTBNDRPG", header.getCreateCommand().getName());
        assertNull(header.getIncludeFile());
    }

    @Test
    public void testNotContinuedBeforeTheNextCommand() {

        // A line without the marker ends the command; the next one is found.
        SourceHeader header = header("      *_> CRTRPGMOD MODULE(@5/@4) +\n" //
            + "     H NOMAIN\n" //
            + "      *_> CRTPGM PGM(@5/@4) MODULE(@4)\n");

        assertEquals(2, header.getCommands().size());
        assertEquals("&5/&4", header.getCommands().get(0).getParameter("MODULE"));
        assertEquals("CRTPGM", header.getCreateCommand().getName());
    }

    @Test
    public void testNoHeader() {

        assertTrue(header("**FREE\ndsply 'Hello';\n").isEmpty());
        assertTrue(header("      *> CRTBNDRPG PGM(&O/&ON)\n").isEmpty());
        assertTrue(header("      * To compile: CRTBNDRPG PGM(@5/@4)\n").isEmpty());
        assertTrue(header("").isEmpty());
    }

    @Test
    public void testBehindTheHeaderLines() {

        StringBuilder content = new StringBuilder("**FREE\n");
        for (int i = 0; i < 120; i++) {
            content.append("// line ").append(i).append('\n');
        }
        content.append("      *_> CRTBNDRPG PGM(@5/@4) SRCFILE(@2/@1)\n");

        assertTrue(header(content.toString()).isEmpty());
    }

    private SourceHeader header(String aContent) {
        return parsers.parse(aContent, "RPGLE");
    }
}
