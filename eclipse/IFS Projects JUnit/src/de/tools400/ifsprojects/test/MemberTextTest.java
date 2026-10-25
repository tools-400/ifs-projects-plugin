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

import de.tools400.ifsprojects.sources.MemberText;

/**
 * The comment blocks are formatted like those of
 * <code>makei cvtsrcpf --text</code> of TOBi: comment from column 7 to 72.
 */
public class MemberTextTest {

    @Test
    public void testParse_fixedRpg() {
        assertEquals("Display a message",
            MemberText.parse("     * %METADATA                                                      *\n"
                + "     *  %TEXT Display a message                                       *\n"
                + "     * %EMETADATA                                                     *\n" + "     H DFTACTGRP(*NO)\n"));
    }

    @Test
    public void testParse_freeRpg() {
        assertEquals("It's free",
            MemberText.parse("**FREE\r\n" + "      // %METADATA                                                     *\r\n"
                + "      //  %TEXT It's free                                              *\r\n"
                + "      // %EMETADATA                                                    *\r\n" + "dcl-s x int(10);\r\n"));
    }

    @Test
    public void testParse_otherComments() {
        assertEquals("Start the application", MemberText.parse("      /* %METADATA                                                    */\n"
            + "      /*  %TEXT Start the application                                 */\n      /* %EMETADATA                                                   */\n"
            + "PGM\n"));
        assertEquals("Customers", MemberText.parse("      -- %METADATA                                                     *\n"
            + "      --  %TEXT Customers                                              *\n      -- %EMETADATA                                                    *\n"));
        assertEquals("Help", MemberText.parse(".* %METADATA *\n.*  %TEXT Help *\n.* %EMETADATA *\n:PNLGRP.\n"));
        assertEquals("Lowercase keywords", MemberText.parse("// %metadata\n// %text Lowercase keywords\n// %emetadata\n"));
    }

    @Test
    public void testParse_noText() {
        assertNull(MemberText.parse("**FREE\ndcl-s x int(10);\n"));
        assertNull(MemberText.parse("// %TEXT Text without metadata\n"));
        assertNull(MemberText.parse("// %TEXT Text before metadata\n// %METADATA\n// %EMETADATA\n"));
        assertNull(MemberText.parse("// %METADATA\n// %EMETADATA\n// %TEXT Text after metadata\n"));
        assertNull(MemberText.parse("// %METADATA\n//  %TEXT     *\n// %EMETADATA\n"));
        assertNull(MemberText.parse(""));
        assertNull(MemberText.parse(null));
    }

    @Test
    public void testParse_lines() {

        StringBuilder source = new StringBuilder();
        for (int i = 0; i < 14; i++) {
            source.append("// line\n");
        }
        assertEquals("Line 15", MemberText.parse(source + "// %METADATA\n// %TEXT Line 15\n"));
        assertNull(MemberText.parse(source + "// line 15\n// %METADATA\n// %TEXT Line 16\n"));
    }

    @Test
    public void testParse_maximumLength() {
        assertEquals("12345678901234567890123456789012345678901234567890",
            MemberText.parse("// %METADATA\n// %TEXT 12345678901234567890123456789012345678901234567890123\n"));
    }

    /**
     * The expected blocks have been created on the IBM i with
     * <code>import_member_text()</code> of <code>makei cvtsrcpf</code> (TOBi
     * 3.4).
     */
    @Test
    public void testInsert_asTobi() {
        assertEquals(
            "      *%METADATA                                                       *\n"
                + "      * %TEXT Fixed format program                                     *\n"
                + "      *%EMETADATA                                                      *\n" + "     H DFTACTGRP(*NO)\n",
            MemberText.insert("     H DFTACTGRP(*NO)\n", "RPGLE", "Fixed format program"));
        assertEquals(
            "**FREE\n" + "      //%METADATA                                                      *\n"
                + "      // %TEXT 12345678901234567890123456789012345678901234567890      *\n"
                + "      //%EMETADATA                                                     *\n" + "dcl-s x int(10);\n",
            MemberText.insert("**FREE\ndcl-s x int(10);\n", "RPGLE", "12345678901234567890123456789012345678901234567890"));
        assertEquals(
            "      /*%METADATA                                                     */\n"
                + "      /* %TEXT Start the application                                  */\n"
                + "      /*%EMETADATA                                                    */\n" + "PGM\n",
            MemberText.insert("PGM\n", "CLLE", "Start the application"));
        assertEquals(
            "      --%METADATA                                                      *\n"
                + "      -- %TEXT Customers                                               *\n"
                + "      --%EMETADATA                                                     *\n" + "create table x (a int);\n",
            MemberText.insert("create table x (a int);\n", "SQL", "Customers"));
    }

    @Test
    public void testInsert_lineSeparators() {
        assertEquals(
            "**FREE\r\n      //%METADATA                                                      *\r\n"
                + "      // %TEXT Free                                                    *\r\n"
                + "      //%EMETADATA                                                     *\r\n",
            MemberText.insert("**FREE\r\n", "SQLRPGLE", "Free"));
        assertEquals("**free\n      //%METADATA                                                      *\n"
            + "      // %TEXT Free                                                    *\n"
            + "      //%EMETADATA                                                     *\n", MemberText.insert("**free", "RPGLE", "Free"));
    }

    @Test
    public void testInsert_panelGroup() {
        assertEquals(
            ".*%METADATA                                                            *\n"
                + ".* %TEXT Help                                                          *\n"
                + ".*%EMETADATA                                                           *\n" + ":PNLGRP.\n",
            MemberText.insert(":PNLGRP.\n", "PNLGRP", "Help"));
    }

    @Test
    public void testInsert_unchanged() {
        assertEquals("some text\n", MemberText.insert("some text\n", "TXT", "No comment style"));
        assertEquals("some text\n", MemberText.insert("some text\n", null, "No source type"));
        assertEquals("PGM\n", MemberText.insert("PGM\n", "CLLE", " "));
        assertEquals("PGM\n", MemberText.insert("PGM\n", "CLLE", null));

        String source = "// %METADATA\n// %TEXT Old text\n// %EMETADATA\n";
        assertEquals(source, MemberText.insert(source, "RPGLE", "New text"));
    }

    @Test
    public void testInsert_parse() {
        for (String sourceType : new String[] { "RPGLE", "SQLRPGLE", "CLLE", "CMD", "DSPF", "SQL", "PNLGRP" }) {
            assertEquals(sourceType, "Text of " + sourceType,
                MemberText.parse(MemberText.insert("line 1\nline 2\n", sourceType, "Text of " + sourceType)));
        }
    }
}
