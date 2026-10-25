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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import de.tools400.ifsprojects.export.ExportPlan;
import de.tools400.ifsprojects.export.ExportPlan.Item;
import de.tools400.ifsprojects.export.ExportPlan.ProblemType;
import de.tools400.ifsprojects.export.ExportSettings;
import de.tools400.ifsprojects.export.IncludeResolver;
import de.tools400.ifsprojects.export.LibraryInfo;
import de.tools400.ifsprojects.sources.IncludeConverter;

public class ExportPlanTest {

    private static final String PROJECT = "/home/jsmith/proj";
    private static final String LONG_LINE = "// " + repeat('x', 107);

    @Test
    public void testPlan() {

        Map<String, String> files = new LinkedHashMap<>();
        files.put("qrpglesrc/hello.pgm.rpgle", "\uFEFF**FREE\n// %METADATA\n// %TEXT Say hello\n// %EMETADATA\n/copy 'inc/consts.rpgleinc'\n");
        files.put("inc/consts.rpgleinc", "**FREE\ndcl-c GREETING 'Hello';\n");
        files.put("qrpglesrc/long.rpgle", "**FREE\n" + LONG_LINE + "\n");
        files.put("qrpglesrc/euro.rpgle", "**FREE\n// 5 \u20AC\n");
        files.put("qrpglesrc/toolongname1.rpgle", "**FREE\n");
        files.put("qclsrc/a.clle", "PGM\nENDPGM\n");
        files.put("qclsrc/a.clp", "PGM\nENDPGM\n");
        files.put("README.md", "# Project\n");

        LibraryInfo library = new LibraryInfo(true);
        library.addFile("QRPGLESRC", true, 100, 273);
        library.addMember("QRPGLESRC", "HELLO");
        library.addMember("QRPGLESRC", "OTHER");

        ExportPlan plan = createPlan(files, library, new ExportSettings("JSMITH#CL", 1141, 112));

        assertFalse(plan.isCreateLibrary());
        assertEquals(1, plan.getOtherFileCount());
        assertEquals(7, plan.getItems().size());

        Item hello = plan.getItems().get(0);
        assertTrue(hello.isExported());
        assertTrue(hello.isExistingMember());
        assertTrue(hello.getProblems().isEmpty());
        assertEquals("Say hello", hello.getText());
        assertEquals("**FREE\n// %METADATA\n// %TEXT Say hello\n// %EMETADATA\n/copy inc,consts\n", hello.getContent());
        assertEquals(1, hello.getIncludes().getIncludes().size());

        Item consts = plan.getItems().get(1);
        assertTrue(consts.isExported());
        assertFalse(consts.isExistingMember());
        assertNull(consts.getText());

        Item longItem = plan.getItems().get(2);
        assertFalse(longItem.isExported());
        assertProblem(longItem.getProblems().get(0), ProblemType.LINE_TOO_LONG, 2);
        assertEquals(110, longItem.getProblems().get(0).getLength());
        assertEquals(100, longItem.getProblems().get(0).getLimit());

        Item euro = plan.getItems().get(3);
        assertTrue(euro.isExported());
        assertProblem(euro.getProblems().get(0), ProblemType.UNMAPPABLE_CHARACTER, 2);
        assertEquals(273, euro.getProblems().get(0).getLimit());

        assertProblem(plan.getItems().get(4).getProblems().get(0), ProblemType.INVALID_MEMBER_NAME, 0);
        assertProblem(plan.getItems().get(5).getProblems().get(0), ProblemType.DUPLICATE, 0);
        assertProblem(plan.getItems().get(6).getProblems().get(0), ProblemType.DUPLICATE, 0);

        assertEquals(3, plan.getExportedItems().size());
        assertEquals(Arrays.asList("INC"), new ArrayList<>(plan.getNewSourceFiles().keySet()));
        assertEquals(Integer.valueOf(112), plan.getNewSourceFiles().get("INC"));
    }

    @Test
    public void testPlan_deselectCopyBook() {

        Map<String, String> files = new LinkedHashMap<>();
        files.put("qrpglesrc/hello.pgm.rpgle", "**FREE\n/copy 'inc/consts.rpgleinc'\n");
        files.put("inc/consts.rpgleinc", "**FREE\n");

        ExportPlan plan = createPlan(files, new LibraryInfo(true), new ExportSettings("JSMITH#CL", 1141, 112));
        Item hello = plan.getItems().get(0);
        Item consts = plan.getItems().get(1);

        plan.setSelected(consts, false);

        assertFalse(consts.isExported());
        assertTrue(consts.getProblems().isEmpty());
        assertEquals("**FREE\n/copy 'inc/consts.rpgleinc'\n", hello.getContent());
        assertEquals(IncludeConverter.Problem.NOT_TRANSFERRED, hello.getProblems().get(0).getIncludeProblem());
        assertEquals(Arrays.asList("QRPGLESRC"), new ArrayList<>(plan.getNewSourceFiles().keySet()));

        plan.setSelected(consts, true);

        assertEquals("**FREE\n/copy inc,consts\n", hello.getContent());
        assertTrue(hello.getProblems().isEmpty());
    }

    @Test
    public void testPlan_setSelectedItems() {

        Map<String, String> files = new LinkedHashMap<>();
        files.put("qrpglesrc/hello.rpgle", "**FREE\n/copy 'inc/consts.rpgleinc'\n");
        files.put("inc/consts.rpgleinc", "**FREE\n");

        ExportPlan plan = createPlan(files, new LibraryInfo(true), new ExportSettings("JSMITH#CL", 1141, 112));
        Item hello = plan.getItems().get(0);
        Item consts = plan.getItems().get(1);

        plan.setSelectedItems(Arrays.asList(hello));

        assertTrue(hello.isSelected());
        assertFalse(consts.isSelected());
        assertEquals(1, plan.getExportedItems().size());
        assertEquals(IncludeConverter.Problem.NOT_TRANSFERRED, hello.getProblems().get(0).getIncludeProblem());

        plan.setSelectedItems(plan.getItems());

        assertEquals(2, plan.getExportedItems().size());
        assertEquals("**FREE\n/copy inc,consts\n", hello.getContent());

        plan.setSelectedItems(new ArrayList<Item>());

        assertTrue(plan.getExportedItems().isEmpty());
    }

    @Test
    public void testPlan_includeOfFailedFile() {

        Map<String, String> files = new LinkedHashMap<>();
        files.put("qrpglesrc/main.rpgle", "**FREE\n/copy big\n");
        files.put("qrpglesrc/big.rpgleinc", "**FREE\n" + LONG_LINE + "\n");

        LibraryInfo library = new LibraryInfo(true);
        library.addFile("QRPGLESRC", true, 100, 1141);

        ExportPlan plan = createPlan(files, library, new ExportSettings("JSMITH#CL", 1141, 112));
        Item main = plan.getItems().get(0);

        assertTrue(main.isExported());
        assertEquals("**FREE\n/copy big\n", main.getContent());
        assertProblem(main.getProblems().get(0), ProblemType.INCLUDE, 2);
        assertEquals(IncludeConverter.Problem.NOT_TRANSFERRED, main.getProblems().get(0).getIncludeProblem());
        assertFalse(plan.getItems().get(1).isExported());
    }

    @Test
    public void testPlan_newLibrary() {

        Map<String, String> files = new LinkedHashMap<>();
        files.put("qrpglesrc/long.rpgle", "**FREE\n" + LONG_LINE + "\n");
        files.put("qcmdsrc/cmd1.cmd", "CMD PROMPT('Command')\n");

        ExportSettings settings = new ExportSettings("JSMITH#CL", 1141, 92);
        settings.setMemberText(false);
        ExportPlan plan = createPlan(files, new LibraryInfo(false), settings);

        assertTrue(plan.isCreateLibrary());
        assertEquals(2, plan.getExportedItems().size());
        assertEquals(Integer.valueOf(122), plan.getNewSourceFiles().get("QRPGLESRC"));
        assertEquals(Integer.valueOf(92), plan.getNewSourceFiles().get("QCMDSRC"));
    }

    @Test
    public void testPlan_libraryErrors() {

        Map<String, String> files = new LinkedHashMap<>();
        files.put("inc/consts.rpgleinc", "**FREE\n");
        files.put("qrpglesrc/hello.rpgle", "**FREE\n");

        LibraryInfo library = new LibraryInfo(true);
        library.addFile("INC", false, 0, 0);

        Map<String, String> readErrors = new HashMap<>();
        readErrors.put("qrpglesrc/hello.rpgle", "Permission denied");
        Map<String, String> contents = new HashMap<>(files);
        contents.remove("qrpglesrc/hello.rpgle");

        List<String> paths = new ArrayList<>(files.keySet());
        ExportPlan plan = new ExportPlan(new ExportSettings("JSMITH#CL", 1141, 112), paths, contents, readErrors,
            new IncludeResolver(PROJECT, null, paths), library);

        assertProblem(plan.getItems().get(0).getProblems().get(0), ProblemType.NOT_A_SOURCE_FILE, 0);
        assertEquals("INC", plan.getItems().get(0).getProblems().get(0).getText());
        assertProblem(plan.getItems().get(1).getProblems().get(0), ProblemType.NOT_READ, 0);
        assertEquals("Permission denied", plan.getItems().get(1).getProblems().get(0).getText());
        assertTrue(plan.getExportedItems().isEmpty());
        assertTrue(plan.getNewSourceFiles().isEmpty());
    }

    private ExportPlan createPlan(Map<String, String> aFiles, LibraryInfo aLibrary, ExportSettings aSettings) {

        List<String> paths = new ArrayList<>(aFiles.keySet());
        IncludeResolver resolver = new IncludeResolver(PROJECT, Arrays.asList(PROJECT + "/inc"), paths);

        return new ExportPlan(aSettings, paths, aFiles, new HashMap<String, String>(), resolver, aLibrary);
    }

    private void assertProblem(ExportPlan.Problem aProblem, ProblemType aType, int aLineNumber) {
        assertEquals(aType, aProblem.getType());
        assertEquals(aLineNumber, aProblem.getLineNumber());
    }

    private static String repeat(char aChar, int aCount) {
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < aCount; i++) {
            buffer.append(aChar);
        }
        return buffer.toString();
    }
}
