/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.iagentbridge;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.tools400.iagentbridge.mcp.ToolArguments;
import de.tools400.ifsprojects.api.CompileException;
import de.tools400.ifsprojects.api.CompileResult;
import de.tools400.ifsprojects.api.FileCompiler;

/**
 * Tool 'ifs_projects_compile': compiles a file of a local IFS project on the
 * IBM i, like 'Build on IBM i' of a file, see
 * {@link FileCompiler#compile(IFile, IProgressMonitor)}.
 */
public class IfsProjectsCompileTool extends AbstractFileCompilerTool {

    private static final String DESCRIPTION = "Compiles a source file of a local IFS project of RDi (Eclipse) on the IBM i, like " //$NON-NLS-1$
        + "'Build on IBM i' of a file of the plug-in IFS Projects: uploads the changes of the project to its deploy directory in the " //$NON-NLS-1$
        + "IFS and runs the compile command of iproj.json or the default compile command of RDi there, e.g. 'makei compile -f " //$NON-NLS-1$
        + "hello.pgm.rpgle', in a job of the RDi connection. The objects are created like in a build of the user. Returns the command, " //$NON-NLS-1$
        + "its exit code, 'success' (exit code 0 and no message with a severity of 30 or higher), the highest severity, the number of " //$NON-NLS-1$
        + "messages per severity and the messages of the " //$NON-NLS-1$
        + "compilers from the events files (EVFEVENT) with file, line, column, message ID, severity and text, sorted by severity, " //$NON-NLS-1$
        + "file and line, and, if the compile failed, the end of the output of the command. The command may compile further " //$NON-NLS-1$
        + "objects, whose messages are returned, too. Use it to check, that a source of any type compiles and that its program " //$NON-NLS-1$
        + "or service program can be created; for RPGLE and SQLRPGLE ifs_projects_check_syntax is faster and creates no object. " //$NON-NLS-1$
        + "After timeoutSeconds the tool cancels the upload or the compile command like 'Cancel' in RDi and fails, when they have stopped. " //$NON-NLS-1$
        + CONDITIONS;

    private static final int DEFAULT_TIMEOUT_SECONDS = 600;

    private static final String TIMEOUT_SCHEMA = "{\"type\":\"integer\",\"minimum\":1,\"description\":\"Time in seconds, including the " //$NON-NLS-1$
        + "upload and the wait for other builds of the project, after which the tool cancels the upload or the compile command like " //$NON-NLS-1$
        + "'Cancel' in RDi and fails. The tool returns, when they have stopped, which may take up to about 30 seconds more. " //$NON-NLS-1$
        + "Default: 600.\"}"; //$NON-NLS-1$

    @Override
    public String getName() {
        return "ifs_projects_compile"; //$NON-NLS-1$
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public JsonObject getInputSchema() {

        JsonObject schema = super.getInputSchema();
        schema.getAsJsonObject("properties").add(ARGUMENT_TIMEOUT_SECONDS, new JsonParser().parse(TIMEOUT_SCHEMA)); //$NON-NLS-1$

        return schema;
    }

    @Override
    protected int getTimeoutSeconds(JsonObject anArguments) {
        return ToolArguments.getInt(anArguments, ARGUMENT_TIMEOUT_SECONDS, DEFAULT_TIMEOUT_SECONDS, 1);
    }

    @Override
    protected String getJobName(IFile aFile) {
        return Messages.bind(Messages.Task_Compiling_A, aFile.getName());
    }

    @Override
    protected CompileResult run(IFile aFile, IProgressMonitor aMonitor) throws CompileException, InterruptedException {
        return FileCompiler.compile(aFile, aMonitor);
    }

    @Override
    protected CompileReport createReport(String aFile, CompileResult aResult, int aMinSeverity, int aMaxMessages) {
        return CompileReport.forCompile(aFile, aResult.getCommand(), aResult.getExitCode(), aMinSeverity, aMaxMessages);
    }
}
