/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.buildcmd.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.tools400.ifsprojects.buildcmd.BuildCommandHeaderParser;
import de.tools400.ifsprojects.sources.headers.SourceHeader;
import de.tools400.ifsprojects.sources.headers.SourceHeaderParsers;

/**
 * The header of the <code>BUILD</code> command of Scott Klement. The examples
 * are taken from its documentation.
 */
public class BuildCommandHeaderParserTest {

    private final SourceHeaderParsers parsers = new SourceHeaderParsers(new BuildCommandHeaderParser());

    @Test
    public void testFixedFormRpg() {

        SourceHeader header = header("      * To compile:\n" //
            + "      *>  CRTBNDRPG PYUEMPLR4 SRCFILE(&L/QRPGLESRC) DBGVIEW(*LIST)\n" //
            + "     H DFTACTGRP(*NO)\n");

        assertEquals("CRTBNDRPG", header.getCreateCommand().getName());
        assertEquals("&L/QRPGLESRC", header.getCreateCommand().getParameter("SRCFILE"));
        assertEquals("*LIST", header.getCreateCommand().getParameter("DBGVIEW"));
        assertFalse(header.isModule());
    }

    @Test
    public void testContinuation() {

        SourceHeader header = header("      * To compile:\n" //
            + "      *>  CRTPGM PGM(TESTPGM) MODULE(TESTMOD01 TESTMOD02) -\n" //
            + "      *>         ACTGRP(KLEMENT) BNDDIR(MAIN QC2LE) -\n" //
            + "      *>          BNDSRVPGM(QHTTPSVR/QZHBCGI)\n");

        assertEquals("CRTPGM", header.getCreateCommand().getName());
        assertEquals("TESTPGM", header.getCreateCommand().getParameter("PGM"));
        assertEquals("TESTMOD01 TESTMOD02", header.getCreateCommand().getParameter("MODULE"));
        assertEquals("KLEMENT", header.getCreateCommand().getParameter("ACTGRP"));
        assertEquals("QHTTPSVR/QZHBCGI", header.getCreateCommand().getParameter("BNDSRVPGM"));
    }

    @Test
    public void testEndTag() {

        SourceHeader header = header("      *>   CRTCLPGM PGM(MYPGM) SRCFILE(&L/QCLSRC)   <*   anything else\n" //
            + "PGM\nENDPGM\n");

        assertEquals("CRTCLPGM", header.getCreateCommand().getName());
        assertEquals("&L/QCLSRC", header.getCreateCommand().getParameter("SRCFILE"));
        assertNull(header.getCreateCommand().getParameter("ANYTHING"));
    }

    @Test
    public void testEndTagInAQuotedString() {

        SourceHeader header = header("      *>  CRTBNDRPG PGM(&O/&ON) TEXT('In <* Out') DBGVIEW(*LIST) <* anything else\n");

        assertEquals("'In <* Out'", header.getCreateCommand().getParameter("TEXT"));
        assertEquals("*LIST", header.getCreateCommand().getParameter("DBGVIEW"));
        assertNull(header.getCreateCommand().getParameter("ANYTHING"));
    }

    @Test
    public void testEndTagInAQuotedStringOfTwoLines() {

        SourceHeader header = header("      *>  CRTBNDRPG PGM(&O/&ON) TEXT('Orders <* -\n" //
            + "      *>  invoices') DBGVIEW(*LIST) <* anything else\n");

        String text = header.getCreateCommand().getParameter("TEXT");
        assertTrue(text, text.startsWith("'Orders <*") && text.endsWith("invoices'"));
        assertEquals("*LIST", header.getCreateCommand().getParameter("DBGVIEW"));
        assertNull(header.getCreateCommand().getParameter("ANYTHING"));
    }

    @Test
    public void testContinuationBeforeTheEndTag() {

        // In CL, the end tag cuts off the end of the comment of each line.
        SourceHeader header = header("/* *>  CRTBNDCL PGM(&O/&ON) -                    <* */\n" //
            + "/* *>           SRCFILE(&L/QCLSRC) DBGVIEW(*LIST) <* */\n" //
            + "PGM\nENDPGM\n");

        assertEquals("CRTBNDCL", header.getCreateCommand().getName());
        assertEquals("&L/QCLSRC", header.getCreateCommand().getParameter("SRCFILE"));
        assertEquals("*LIST", header.getCreateCommand().getParameter("DBGVIEW"));
    }

    @Test
    public void testIgnorePrefix() {

        SourceHeader header = header("      *>  ign: DLTPGM PYUEMPLR4\n" //
            + "      *>  IGN: DLTMOD MODULE(&O/&ON)\n" //
            + "      *>  Ign: DLTSRVPGM SRVPGM(&O/&ON)\n");

        assertEquals(3, header.getCommands().size());
        assertEquals("DLTPGM", header.getCommands().get(0).getName());
        assertEquals("DLTMOD", header.getCommands().get(1).getName());
        assertEquals("DLTSRVPGM", header.getCommands().get(2).getName());
        assertNull(header.getCreateCommand());
    }

    @Test
    public void testTheCreateCommandOfAScript() {

        // The example of the documentation: only CRTBNDRPG creates the object.
        SourceHeader header = header("      * To compile:\n" //
            + "      *>  OVRDBF FILE(CBOMASyy) TOFILE(CBOMAS&YY)\n" //
            + "      *>  ign: DLTPGM PYUEMPLR4\n" //
            + "      *>  CRTBNDRPG PYUEMPLR4 SRCFILE(&L/QRPGLESRC) DBGVIEW(*LIST)\n" //
            + "      *>  CHGOBJOWN OBJ(PYUEMPLR4) OBJTYPE(*PGM) NEWOWN(KLOWN)\n" //
            + "      *>  CHGPGM PGM(PYUEMPLR4) USRPRF(*OWNER)\n" //
            + "      *>  DLTOVR *ALL\n");

        assertEquals(6, header.getCommands().size());
        assertEquals("CRTBNDRPG", header.getCreateCommand().getName());
        assertFalse(header.isModule());
    }

    @Test
    public void testModule() {

        assertTrue(header("      *>  CRTRPGMOD TESTMOD01 SRCFILE(&L/QRPGLESRC)\n").isModule());
        assertTrue(header("      *>  CRTSQLRPGI OBJ(&O/&ON) OBJTYPE(*MODULE)\n").isModule());
    }

    @Test
    public void testIncludeFile() {

        assertEquals("QCPYSRC", header("      *>  CRTSQLRPGI OBJ(&O/&ON) INCFILE(&L/QCPYSRC)\n").getIncludeFile());
        assertNull(header("      *>  CRTSQLRPGI OBJ(&O/&ON) INCFILE(*SRCFILE)\n").getIncludeFile());
        assertNull(header("      *>  CRTSQLRPGI OBJ(&O/&ON)\n").getIncludeFile());
    }

    @Test
    public void testEndsAtTheSource() {

        // The command is not terminated, the source must not become part of it.
        SourceHeader header = header("      *>  CRTBNDRPG PYUEMPLR4 SRCFILE(&L/QRPGLESRC) -\n" //
            + "     H DFTACTGRP(*NO)\n" //
            + "      * INCFILE(QOTHER)\n");

        assertEquals("CRTBNDRPG", header.getCreateCommand().getName());
        assertNull(header.getIncludeFile());
    }

    @Test
    public void testNotContinuedBeforeTheNextCommand() {

        // A line without the marker ends the command; the next one is found.
        SourceHeader header = header("      *>  CRTRPGMOD MODULE(&O/&ON) -\n" //
            + "     H NOMAIN\n" //
            + "      *>  CRTPGM PGM(&O/&ON) MODULE(&ON)\n");

        assertEquals(2, header.getCommands().size());
        assertEquals("&O/&ON", header.getCommands().get(0).getParameter("MODULE"));
        assertEquals("CRTPGM", header.getCreateCommand().getName());
    }

    @Test
    public void testNoHeader() {

        assertTrue(header("**FREE\ndsply 'Hello';\n").isEmpty());
        assertTrue(header("      * To compile: CRTBNDRPG PYUEMPLR4\n").isEmpty());
        assertTrue(header("").isEmpty());
    }

    @Test
    public void testBehindTheHeaderLines() {

        StringBuilder content = new StringBuilder("**FREE\n");
        for (int i = 0; i < 120; i++) {
            content.append("// line ").append(i).append('\n');
        }
        content.append("      *>  CRTBNDRPG PYUEMPLR4 SRCFILE(&L/QRPGLESRC)\n");

        assertTrue(header(content.toString()).isEmpty());
    }

    private SourceHeader header(String aContent) {
        return parsers.parse(aContent, "RPGLE");
    }
}
