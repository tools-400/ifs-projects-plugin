/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.iagentbridge.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.tools400.iagentbridge.mcp.McpToolResult;
import de.tools400.ifsprojects.iagentbridge.CompileReport;
import de.tools400.ifsprojects.iagentbridge.CompileReport.Message;

public class CompileReportTest {

    private static final String FILE = "C:/git/demo/qrpglesrc/hello.pgm.rpgle";
    private static final String COPY_BOOK = "C:/git/demo/qprotosrc/hello.rpgleinc";
    private static final String COMMAND = "makei compile -f hello.pgm.rpgle";
    private static final String CHECK = "CRTBNDRPG PGM(QTEMP/HELLO) SRCSTMF('/home/jsmith/demo/hello.pgm.rpgle') OPTION(*EVENTF *NOGEN)";

    @Test
    public void testToJson_syntaxCheckWithoutMessages() {

        CompileReport report = CompileReport.forSyntaxCheck(FILE, CHECK, 10, 100);

        assertEquals("{\"file\":\"" + FILE + "\",\"command\":\"" + CHECK + "\",\"success\":true,\"highestSeverity\":0,"
            + "\"counts\":{},\"truncated\":false,\"messages\":[]}", report.toJson().toString());
    }

    @Test
    public void testToJson_message() {

        CompileReport report = CompileReport.forSyntaxCheck(FILE, CHECK, 10, 100);
        report.addAll(Arrays.asList(new Message(FILE, 6, 1, "RNF7030", 30, "The name or indicator FOO is not defined.")));

        assertEquals("{\"file\":\"" + FILE + "\",\"line\":6,\"column\":1,\"id\":\"RNF7030\",\"severity\":30,"
            + "\"text\":\"The name or indicator FOO is not defined.\"}", messages(report).get(0).toString());
    }

    @Test
    public void testToJson_messageWithoutLineAndColumn() {

        CompileReport report = CompileReport.forSyntaxCheck(FILE, CHECK, 10, 100);
        report.addAll(Arrays.asList(new Message(FILE, 0, 0, "RNS9308", 50, "Compilation stopped.")));

        JsonObject message = messages(report).get(0).getAsJsonObject();

        assertFalse(message.has("line"));
        assertFalse(message.has("column"));
    }

    @Test
    public void testToJson_sortedBySeverityFileAndLine() {

        CompileReport report = CompileReport.forSyntaxCheck(FILE, CHECK, 0, 100);
        report.addAll(Arrays.asList(new Message(FILE, 9, 1, "RNF7031", 0, "Not referenced."), new Message(FILE, 7, 1, "RNF7030", 30, "B"),
            new Message(COPY_BOOK, 3, 1, "RNF3430", 20, "C"), new Message(COPY_BOOK, 8, 1, "RNF7030", 30, "D"),
            new Message(FILE, 2, 1, "RNF7030", 30, "E")));

        JsonArray messages = messages(report);

        assertEquals("D", text(messages, 0));
        assertEquals("E", text(messages, 1));
        assertEquals("B", text(messages, 2));
        assertEquals("C", text(messages, 3));
        assertEquals("Not referenced.", text(messages, 4));
    }

    @Test
    public void testToJson_minSeverityAndCounts() {

        CompileReport report = CompileReport.forSyntaxCheck(FILE, CHECK, 10, 100);
        report.addAll(Arrays.asList(new Message(FILE, 1, 1, "RNF7031", 0, "A"), new Message(FILE, 2, 1, "RNF7066", 10, "B"),
            new Message(FILE, 3, 1, "RNF7030", 30, "C"), new Message(FILE, 4, 1, "RNF7030", 30, "D")));

        JsonObject json = report.toJson();

        assertEquals("{\"30\":2,\"10\":1}", json.get("counts").toString());
        assertEquals(3, json.getAsJsonArray("messages").size());
        assertEquals(30, json.get("highestSeverity").getAsInt());
    }

    @Test
    public void testToJson_allSeveritiesWithMinSeverity0() {

        CompileReport report = CompileReport.forSyntaxCheck(FILE, CHECK, 0, 100);
        report.addAll(Arrays.asList(new Message(FILE, 1, 1, "RNF7031", 0, "A")));

        JsonObject json = report.toJson();

        assertEquals("{\"00\":1}", json.get("counts").toString());
        assertEquals(1, json.getAsJsonArray("messages").size());
    }

    @Test
    public void testToJson_truncatedCountsAllMessages() {

        CompileReport report = CompileReport.forSyntaxCheck(FILE, CHECK, 10, 2);
        report.addAll(Arrays.asList(new Message(FILE, 1, 1, "RNF7030", 30, "A"), new Message(FILE, 2, 1, "RNF7030", 30, "B"),
            new Message(FILE, 3, 1, "RNF7030", 30, "C")));

        JsonObject json = report.toJson();

        assertTrue(json.get("truncated").getAsBoolean());
        assertEquals(2, json.getAsJsonArray("messages").size());
        assertEquals(3, json.getAsJsonObject("counts").get("30").getAsInt());
    }

    @Test
    public void testIsSuccess_syntaxCheck() {

        CompileReport warning = CompileReport.forSyntaxCheck(FILE, CHECK, 10, 100);
        warning.addAll(Arrays.asList(new Message(FILE, 1, 1, "RNF7066", 10, "Warning")));
        CompileReport error = CompileReport.forSyntaxCheck(FILE, CHECK, 10, 100);
        error.addAll(Arrays.asList(new Message(FILE, 1, 1, "RNF3430", 20, "Error")));

        assertTrue(warning.isSuccess());
        assertFalse(error.isSuccess());
    }

    @Test
    public void testIsSuccess_compileByExitCode() {

        CompileReport success = CompileReport.forCompile(FILE, COMMAND, Integer.valueOf(0), 10, 100);
        success.addAll(Arrays.asList(new Message(FILE, 1, 1, "RNF7066", 10, "Warning")));
        CompileReport failure = CompileReport.forCompile(FILE, COMMAND, Integer.valueOf(2), 10, 100);
        CompileReport unknown = CompileReport.forCompile(FILE, COMMAND, null, 10, 100);

        assertTrue(success.isSuccess());
        assertFalse(failure.isSuccess());
        assertFalse(unknown.isSuccess());
    }

    @Test
    public void testIsSuccess_compileWithErrorDespiteExitCode0() {

        // A compile command may ignore failed compiles, e.g. with '|| true'.
        CompileReport error = CompileReport.forCompile(FILE, COMMAND, Integer.valueOf(0), 10, 100);
        error.addAll(Arrays.asList(new Message(FILE, 6, 1, "RNF7030", 30, "Error")));
        CompileReport warning = CompileReport.forCompile(FILE, COMMAND, Integer.valueOf(0), 10, 100);
        warning.addAll(Arrays.asList(new Message(FILE, 3, 26, "RNF3430", 20, "Warning")));

        assertFalse(error.isSuccess());
        assertTrue(warning.isSuccess());
    }

    @Test
    public void testToJson_compileWithExitCodeAndOutput() {

        CompileReport report = CompileReport.forCompile(FILE, COMMAND, Integer.valueOf(2), 10, 100);
        report.setOutput(Arrays.asList("make: *** [HELLO.PGM] Error 1", "Finished with exit code 2."));

        JsonObject json = report.toJson();

        assertEquals(2, json.get("exitCode").getAsInt());
        assertFalse(json.get("success").getAsBoolean());
        assertEquals("[\"make: *** [HELLO.PGM] Error 1\",\"Finished with exit code 2.\"]", json.get("output").toString());
    }

    @Test
    public void testToJson_noOutputOnSuccess() {

        CompileReport report = CompileReport.forCompile(FILE, COMMAND, Integer.valueOf(0), 10, 100);
        report.setOutput(Arrays.asList("Finished with exit code 0."));

        assertFalse(report.toJson().has("output"));
    }

    @Test
    public void testToJson_outputCutToTheLastLines() {

        List<String> lines = new ArrayList<>();
        for (int i = 1; i <= 40; i++) {
            lines.add("line " + i);
        }
        CompileReport report = CompileReport.forCompile(FILE, COMMAND, Integer.valueOf(1), 10, 100);
        report.setOutput(lines);

        JsonArray output = report.toJson().getAsJsonArray("output");

        assertEquals(30, output.size());
        assertEquals("line 11", output.get(0).getAsString());
        assertEquals("line 40", output.get(29).getAsString());
    }

    @Test
    public void testToJson_syntaxCheckHasNoExitCode() {
        assertFalse(CompileReport.forSyntaxCheck(FILE, CHECK, 10, 100).toJson().has("exitCode"));
    }

    @Test
    public void testToToolResult_failedCompileIsNoToolError() {

        CompileReport report = CompileReport.forCompile(FILE, COMMAND, Integer.valueOf(2), 10, 100);

        McpToolResult result = report.toToolResult();

        assertFalse(result.isError());
        assertEquals(report.toJson(), new JsonParser().parse(result.getText()));
    }

    private static JsonArray messages(CompileReport aReport) {
        return aReport.toJson().getAsJsonArray("messages");
    }

    private static String text(JsonArray aMessages, int anIndex) {
        return aMessages.get(anIndex).getAsJsonObject().get("text").getAsString();
    }
}
