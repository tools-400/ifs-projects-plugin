/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.iagentbridge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import de.tools400.iagentbridge.mcp.McpToolResult;

/**
 * Result of the tools ifs_projects_compile and ifs_projects_check_syntax: the
 * messages of the compilers sorted by severity, file and line, their number per
 * severity, whether the compile succeeded and, if not, the end of the output.
 * <p>
 * The class has no Eclipse dependencies, so that it can be tested outside of
 * RDi.
 */
public final class CompileReport {

    /** Highest severity of the messages of a successful syntax check. */
    private static final int MAX_SEVERITY_OF_SUCCESS = 10;

    /**
     * Lowest severity of the messages, that fail a compile with exit code 0,
     * e.g. of a compile command, that ignores a failed compile.
     */
    private static final int MIN_SEVERITY_OF_COMPILE_ERROR = 30;

    /** Number of lines of the output, that are returned. */
    private static final int MAX_OUTPUT_LINES = 30;

    private static final Comparator<Message> ORDER = new Comparator<Message>() {
        public int compare(Message aMessage1, Message aMessage2) {

            int result = Integer.compare(aMessage2.severity, aMessage1.severity);
            if (result == 0) {
                result = aMessage1.file.compareTo(aMessage2.file);
            }
            if (result == 0) {
                result = Integer.compare(aMessage1.line, aMessage2.line);
            }

            return result;
        }
    };

    private final String file;
    private final String command;
    private final boolean isCompile;
    private final Integer exitCode;
    private final int minSeverity;
    private final int maxMessages;
    private final List<Message> messages;
    private final List<String> output;

    private CompileReport(String aFile, String aCommand, boolean anIsCompile, Integer anExitCode, int aMinSeverity, int aMaxMessages) {
        this.file = aFile;
        this.command = aCommand;
        this.isCompile = anIsCompile;
        this.exitCode = anExitCode;
        this.minSeverity = aMinSeverity;
        this.maxMessages = aMaxMessages;
        this.messages = new ArrayList<>();
        this.output = new ArrayList<>();
    }

    /**
     * Creates the empty report of a compile, that succeeded, if its exit code
     * is 0 and no message has a severity of
     * {@value #MIN_SEVERITY_OF_COMPILE_ERROR} or higher.
     *
     * @param aFile - absolute path of the compiled file
     * @param aCommand - compile command
     * @param anExitCode - exit code of the command or <code>null</code>, if it
     *        is not known
     * @param aMinSeverity - lowest severity of the messages, that the report
     *        returns
     * @param aMaxMessages - maximum number of messages, that the report returns
     * @return report
     */
    public static CompileReport forCompile(String aFile, String aCommand, Integer anExitCode, int aMinSeverity, int aMaxMessages) {
        return new CompileReport(aFile, aCommand, true, anExitCode, aMinSeverity, aMaxMessages);
    }

    /**
     * Creates the empty report of a syntax check, that succeeded, if no message
     * has a severity higher than {@value #MAX_SEVERITY_OF_SUCCESS}, like a
     * compile with <code>GENLVL(10)</code>.
     *
     * @param aFile - absolute path of the checked file
     * @param aCommand - compile command of the syntax check
     * @param aMinSeverity - lowest severity of the messages, that the report
     *        returns
     * @param aMaxMessages - maximum number of messages, that the report returns
     * @return report
     */
    public static CompileReport forSyntaxCheck(String aFile, String aCommand, int aMinSeverity, int aMaxMessages) {
        return new CompileReport(aFile, aCommand, false, null, aMinSeverity, aMaxMessages);
    }

    /**
     * Adds messages of the compilers.
     *
     * @param aMessages - messages
     */
    public void addAll(List<Message> aMessages) {
        messages.addAll(aMessages);
    }

    /**
     * Sets the output of the compile command or the messages of the command of
     * the syntax check.
     *
     * @param aLines - lines
     */
    public void setOutput(List<String> aLines) {
        output.clear();
        output.addAll(aLines);
    }

    /**
     * Tells, whether the compile or the syntax check succeeded.
     *
     * @return <code>true</code>, if the exit code of a compile is 0 and no
     *         message has a severity of {@value #MIN_SEVERITY_OF_COMPILE_ERROR}
     *         or higher, or if no message of a syntax check has a severity
     *         higher than {@value #MAX_SEVERITY_OF_SUCCESS}
     */
    public boolean isSuccess() {

        if (isCompile) {
            return exitCode != null && exitCode.intValue() == 0 && getHighestSeverity() < MIN_SEVERITY_OF_COMPILE_ERROR;
        }

        return getHighestSeverity() <= MAX_SEVERITY_OF_SUCCESS;
    }

    /**
     * Returns the report as JSON object.
     *
     * @return the report with 'file', 'command', 'exitCode' (compile only, if
     *         known), 'success', 'highestSeverity', 'counts', 'truncated',
     *         'messages' and 'output' (if it did not succeed)
     */
    public JsonObject toJson() {

        List<Message> selected = new ArrayList<>();
        Map<String, Integer> counts = new TreeMap<>(Collections.reverseOrder());
        for (Message message : messages) {
            if (message.severity >= minSeverity) {
                selected.add(message);
                String severity = formatSeverity(message.severity);
                Integer count = counts.get(severity);
                counts.put(severity, Integer.valueOf(count == null ? 1 : count.intValue() + 1));
            }
        }
        Collections.sort(selected, ORDER);

        JsonObject countObject = new JsonObject();
        for (Map.Entry<String, Integer> count : counts.entrySet()) {
            countObject.addProperty(count.getKey(), count.getValue());
        }

        JsonArray messageArray = new JsonArray();
        for (Message message : selected.subList(0, Math.min(selected.size(), maxMessages))) {
            messageArray.add(message.toJson());
        }

        JsonObject report = new JsonObject();
        report.addProperty("file", file); //$NON-NLS-1$
        report.addProperty("command", command); //$NON-NLS-1$
        if (isCompile && exitCode != null) {
            report.addProperty("exitCode", exitCode); //$NON-NLS-1$
        }
        report.addProperty("success", Boolean.valueOf(isSuccess())); //$NON-NLS-1$
        report.addProperty("highestSeverity", Integer.valueOf(getHighestSeverity())); //$NON-NLS-1$
        report.add("counts", countObject); //$NON-NLS-1$
        report.addProperty("truncated", Boolean.valueOf(selected.size() > maxMessages)); //$NON-NLS-1$
        report.add("messages", messageArray); //$NON-NLS-1$
        if (!isSuccess() && !output.isEmpty()) {
            JsonArray outputArray = new JsonArray();
            for (String line : output.subList(Math.max(0, output.size() - MAX_OUTPUT_LINES), output.size())) {
                outputArray.add(new JsonPrimitive(line));
            }
            report.add("output", outputArray); //$NON-NLS-1$
        }

        return report;
    }

    /**
     * Returns the report as result of the tool. A compile, that failed, is a
     * result, not an error of the tool.
     *
     * @return successful result with the report as text
     */
    public McpToolResult toToolResult() {
        return McpToolResult.success(new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(toJson()));
    }

    private int getHighestSeverity() {

        int highest = 0;
        for (Message message : messages) {
            highest = Math.max(highest, message.severity);
        }

        return highest;
    }

    private static String formatSeverity(int aSeverity) {

        if (aSeverity < 10) {
            return "0" + aSeverity; //$NON-NLS-1$
        }

        return Integer.toString(aSeverity);
    }

    /**
     * Message of a compiler.
     */
    public static final class Message {

        private final String file;
        private final int line;
        private final int column;
        private final String id;
        private final int severity;
        private final String text;

        /**
         * Creates a message.
         *
         * @param aFile - absolute path of the local file or path of the file on
         *        the IBM i
         * @param aLine - line, 0 for a message without line
         * @param aColumn - column, 0 for a message without column
         * @param anId - message ID, e.g. RNF7030
         * @param aSeverity - severity, e.g. 30
         * @param aText - text
         */
        public Message(String aFile, int aLine, int aColumn, String anId, int aSeverity, String aText) {
            this.file = aFile;
            this.line = aLine;
            this.column = aColumn;
            this.id = anId;
            this.severity = aSeverity;
            this.text = aText;
        }

        private JsonObject toJson() {

            JsonObject json = new JsonObject();
            json.addProperty("file", file); //$NON-NLS-1$
            if (line > 0) {
                json.addProperty("line", Integer.valueOf(line)); //$NON-NLS-1$
            }
            if (column > 0) {
                json.addProperty("column", Integer.valueOf(column)); //$NON-NLS-1$
            }
            json.addProperty("id", id); //$NON-NLS-1$
            json.addProperty("severity", Integer.valueOf(severity)); //$NON-NLS-1$
            json.addProperty("text", text); //$NON-NLS-1$

            return json;
        }

        @Override
        public String toString() {
            return file + ":" + line + ":" + column + " " + id + " " + severity + " " + text; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
        }
    }
}
