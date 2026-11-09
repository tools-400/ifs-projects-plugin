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
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import de.tools400.ifsprojects.export.ExportMapping;
import de.tools400.ifsprojects.export.SourceFileRule;
import de.tools400.ifsprojects.export.SourceTypeProposals;
import de.tools400.ifsprojects.export.SourceTypeProposals.Proposal;
import de.tools400.ifsprojects.export.SourceTypeProposals.Reason;

public class SourceTypeProposalsTest {

    /**
     * The files of the round trip of Peek/400: the extensions of the source
     * directories are proposed, documents, scripts and settings are only
     * offered.
     */
    @Test
    public void testCreate() {

        List<String> files = Arrays.asList("README.md", "LICENSE", "iproj.json", "Rules.mk", ".github/release.cmd", ".github/release.ps1",
            "qddssrc/.ibmi.json", "qddssrc/Rules.mk", "qddssrc/testpeek.tbl", "qddssrc/testpeek2.tbl", "QXML/A_XMLINST.RPGLE",
            "QXML/BASICS1.XML", "doc/whats_new.txt", "doc/download.bat", "doc/Peek400e.pdf", "doc/Peek400e.doc", "tools/mkproduct.sh",
            "qrpglesrc/.ibmi.json", "qrpglesrc/hello.rpgle", "qrpglesrc/notes.my-ext", "qrpglesrc/toolongextension.abcdefghijk");

        List<Proposal> proposals = create(files, Collections.<String> emptyList(), null);

        assertProposal(proposals.get(0), "TBL", 2, "qddssrc/testpeek.tbl", Reason.IBM_SOURCE_TYPE, false);
        assertProposal(proposals.get(1), "BAT", 1, "doc/download.bat", Reason.SOURCE_DIRECTORY, false);
        assertEquals("doc", proposals.get(1).getDirectory());
        assertProposal(proposals.get(2), "XML", 1, "QXML/BASICS1.XML", Reason.SOURCE_DIRECTORY, false);
        assertEquals("QXML", proposals.get(2).getDirectory());
        assertProposal(proposals.get(3), "MK", 2, "Rules.mk", null, false);
        assertEquals(Arrays.asList("TBL", "BAT", "XML", "MK", "DOC", "JSON", "MD", "PDF", "SH"), sourceTypes(proposals));
        for (Proposal proposal : proposals.subList(3, proposals.size())) {
            assertNull(proposal.getSourceType(), proposal.getReason());
            assertFalse(proposal.isRecommended());
        }
    }

    @Test
    public void testCreate_selected() {

        List<String> files = Arrays.asList("qddssrc/testpeek.tbl", "src/data.xyz", "src/hello.rpgle", "notes/todo.xyz");

        List<Proposal> proposals = create(files, Arrays.asList("xyz"), null);

        assertEquals(Arrays.asList("XYZ", "TBL"), sourceTypes(proposals));
        assertProposal(proposals.get(0), "XYZ", 2, "src/data.xyz", Reason.SOURCE_DIRECTORY, true);
        assertTrue(proposals.get(0).isRecommended());
        assertProposal(proposals.get(1), "TBL", 1, "qddssrc/testpeek.tbl", Reason.IBM_SOURCE_TYPE, false);
    }

    @Test
    public void testCreate_projectDirectory() {

        List<Proposal> proposals = create(Arrays.asList("hello.rpgle", "data.xyz", "README.md"), Collections.<String> emptyList(), null);

        assertProposal(proposals.get(0), "XYZ", 1, "data.xyz", Reason.SOURCE_DIRECTORY, false);
        assertEquals("", proposals.get(0).getDirectory());
        assertProposal(proposals.get(1), "MD", 1, "README.md", null, false);
    }

    /**
     * The extensions of the actions of Code for IBM i are proposed, also with
     * comments in the file.
     */
    @Test
    public void testCreate_codeForIbmiActions() {

        String actions = "[\n  // Create the table\n"
            + "  { \"name\": \"Create table\", \"command\": \"CRTTBL TBL(&OPENLIB/&OPENMBR) SRCFILE(&OPENLIB/&OPENSPF)\",\n"
            + "    \"extensions\": [ \"abc\", \"GLOBAL\" ], \"environment\": \"ile\" },\n"
            + "  { \"name\": \"Other\", \"extensions\": [ \"md\" ] }\n]\n";
        List<String> files = Arrays.asList("misc/one.abc", "misc/two.def", "docs/readme.md", ".vscode/actions.json");

        List<Proposal> proposals = create(files, Collections.<String> emptyList(), actions);

        assertProposal(proposals.get(0), "ABC", 1, "misc/one.abc", Reason.CODE_FOR_IBM_I, false);
        assertProposal(proposals.get(1), "DEF", 1, "misc/two.def", null, false);
        // Documents are never proposed.
        assertProposal(proposals.get(2), "MD", 1, "docs/readme.md", null, false);

        assertNull(create(files, Collections.<String> emptyList(), "{ invalid").get(0).getReason());
        assertNull(create(files, Collections.<String> emptyList(), "{ \"extensions\": [ \"abc\" ] }").get(0).getReason());
    }

    private static List<Proposal> create(List<String> aFiles, List<String> aFurtherSourceTypes, String anActions) {

        ExportMapping mapping = new ExportMapping(SourceFileRule.DIRECTORY, null, aFurtherSourceTypes);
        List<String> exported = new ArrayList<>();
        for (String path : aFiles) {
            if (mapping.isExported(path)) {
                exported.add(path);
            }
        }

        return SourceTypeProposals.create(aFiles, exported, aFurtherSourceTypes, anActions);
    }

    private static List<String> sourceTypes(List<Proposal> aProposals) {

        List<String> sourceTypes = new ArrayList<>();
        for (Proposal proposal : aProposals) {
            sourceTypes.add(proposal.getSourceType());
        }

        return sourceTypes;
    }

    private static void assertProposal(Proposal aProposal, String aSourceType, int aFileCount, String anExample, Reason aReason,
        boolean anIsSelected) {
        assertEquals(aSourceType, aProposal.getSourceType());
        assertEquals(aSourceType.toLowerCase(), aProposal.getExtension());
        assertEquals(aFileCount, aProposal.getFileCount());
        assertEquals(anExample, aProposal.getExample());
        assertEquals(aReason, aProposal.getReason());
        assertEquals(anIsSelected, aProposal.isSelected());
    }
}
