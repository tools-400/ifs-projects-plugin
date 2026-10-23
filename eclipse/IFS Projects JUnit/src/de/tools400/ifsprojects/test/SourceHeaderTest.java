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

/**
 * The part of the headers, that all build tools have in common: the CL
 * commands and the selection of the parser. The headers themselves are read by
 * plug-ins of their own, which are tested in fragments of those, see
 * {@link TestHeaderParser}.
 * <p>
 * A parser, that throws an exception, is skipped and logged as well. That path
 * is not tested here: the logging loads <code>IfsProjectsPlugin</code> and with
 * it <code>org.eclipse.core.runtime.Plugin</code>, which fails in a plain JUnit
 * run of RDi, because signed and unsigned jars of that package are on the class
 * path (<code>SecurityException: signer information does not match</code>).
 */
public class SourceHeaderTest {

    private final SourceHeaderParsers parsers = new SourceHeaderParsers(new TestHeaderParser());

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
    public void testCommand_endOfAClComment() {

        // The parser of a build tool removes it; what is left is ignored.
        CreateCommand command = CreateCommand.parse("CRTCLMOD MODULE(&LI/&OB) */");

        assertEquals("CRTCLMOD", command.getName());
        assertEquals("&LI/&OB", command.getParameter("MODULE"));
    }

    @Test
    public void testHeader_isModule() {

        assertTrue(header("// BUILD: CRTRPGMOD MODULE(&LI/&OB)\n").isModule());
        assertTrue(header("// BUILD: CRTSQLRPGI OBJ(&LI/&OB) OBJTYPE(*MODULE)\n").isModule());
        assertFalse(header("// BUILD: CRTBNDRPG PGM(&LI/&OB)\n").isModule());
        assertFalse(header("**FREE\ndsply 'Hello';\n").isModule());
    }

    @Test
    public void testHeader_getIncludeFile() {

        assertEquals("QCPYSRC", header("// BUILD: CRTSQLRPGI OBJ(&LI/&OB) INCFILE(&SL/QCPYSRC)\n").getIncludeFile());
        assertEquals("QCPYSRC", header("// BUILD: CRTSQLRPGI OBJ(&LI/&OB) INCFILE(qcpysrc)\n").getIncludeFile());
        assertEquals("*LIBL/QCPYSRC", header("// BUILD: CRTSQLRPGI OBJ(&LI/&OB) INCFILE(*LIBL/QCPYSRC)\n").getIncludeFile());
        assertNull(header("// BUILD: CRTSQLRPGI OBJ(&LI/&OB) INCFILE(*SRCFILE)\n").getIncludeFile());
        assertNull(header("// BUILD: CRTSQLRPGI OBJ(&LI/&OB)\n").getIncludeFile());
    }

    @Test
    public void testHeader_createCommandIsTheLastOne() {

        // The modules of a program are created before it is bound.
        SourceHeader header = header("// BUILD: CRTRPGMOD MODULE(&LI/&OB)\n" //
            + "// BUILD: CRTPGM PGM(&LI/&OB) MODULE(&OB)\n" //
            + "// BUILD: CHGOBJOWN OBJ(&LI/&OB) OBJTYPE(*PGM) NEWOWN(QPGMR)\n");

        assertEquals(3, header.getCommands().size());
        assertEquals("CRTPGM", header.getCreateCommand().getName());
        assertFalse(header.isModule());
    }

    @Test
    public void testHeader_withoutACreateCommand() {

        SourceHeader header = header("// BUILD: CHGOBJOWN OBJ(&LI/&OB) OBJTYPE(*PGM)\n");

        assertFalse(header.isEmpty());
        assertNull(header.getCreateCommand());
        assertFalse(header.isModule());
        assertNull(header.getIncludeFile());
    }

    @Test
    public void testParsers_withoutAnyParser() {

        SourceHeader header = new SourceHeaderParsers().parse("// BUILD: CRTRPGMOD MODULE(&LI/&OB)\n", "RPGLE");

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

        SourceHeaderParsers parsers = new SourceHeaderParsers(new TestHeaderParser(), new OtherToolParser());

        assertEquals("CRTRPGMOD", parsers.parse("// BUILD: CRTRPGMOD MODULE(&LI/&OB)\n", "RPGLE").getCreateCommand().getName());
        assertEquals("CRTBNDRPG", parsers.parse("     * MAKE: CRTBNDRPG PGM(&LI/&OB)\n", "RPGLE").getCreateCommand().getName());
        assertTrue(parsers.parse("**FREE\ndsply 'Hello';\n", "RPGLE").isEmpty());
    }

    @Test
    public void testParsers_aParserWithoutAResultIsSkipped() {

        SourceHeaderParsers parsers = new SourceHeaderParsers(new NoResultParser(), new TestHeaderParser());

        assertEquals("CRTRPGMOD", parsers.parse("// BUILD: CRTRPGMOD MODULE(&LI/&OB)\n", "RPGLE").getCreateCommand().getName());
    }

    private SourceHeader header(String aContent) {
        return parsers.parse(aContent, "RPGLE");
    }

    /**
     * Parser of a second build tool, which shows, how the parser of a source is
     * selected.
     */
    private static final class OtherToolParser implements ISourceHeaderParser {

        public List<CreateCommand> parse(String aContent, String aSourceType) {

            List<CreateCommand> commands = new ArrayList<CreateCommand>();
            for (String line : aContent.split("\n")) {
                int tag = line.indexOf("MAKE:");
                if (tag >= 0) {
                    commands.add(CreateCommand.parse(line.substring(tag + "MAKE:".length())));
                }
            }

            return commands;
        }
    }

    /**
     * Parser of a plug-in, that returns nothing at all: the next one is asked.
     */
    private static final class NoResultParser implements ISourceHeaderParser {

        public List<CreateCommand> parse(String aContent, String aSourceType) {
            return null;
        }
    }
}
