/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.iagentbridge;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobFunction;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.tools400.iagentbridge.mcp.IMcpTool;
import de.tools400.iagentbridge.mcp.McpToolResult;
import de.tools400.iagentbridge.mcp.ToolArguments;
import de.tools400.iagentbridge.mcp.ToolJobs;
import de.tools400.ifsprojects.api.CompileException;
import de.tools400.ifsprojects.api.CompileResult;
import de.tools400.ifsprojects.api.FileCompiler;

/**
 * Tool, that compiles a file of a local IFS project on the IBM i or checks its
 * syntax, see {@link FileCompiler}. It runs only, if the user has allowed it in
 * the preferences and approved the project, see {@link AgentSettings}, and if
 * no file of the project has unsaved changes in an editor of RDi.
 * <p>
 * The work runs in a job, which the view Progress shows and in which the user
 * can cancel it, while the thread of the server waits.
 */
public abstract class AbstractFileCompilerTool implements IMcpTool {

    private static final String ARGUMENT_FILE = "file"; //$NON-NLS-1$
    private static final String ARGUMENT_MIN_SEVERITY = "minSeverity"; //$NON-NLS-1$
    private static final String ARGUMENT_MAX_MESSAGES = "maxMessages"; //$NON-NLS-1$

    /** Argument of the timeout of the tools, that have one. */
    static final String ARGUMENT_TIMEOUT_SECONDS = "timeoutSeconds"; //$NON-NLS-1$

    private static final int DEFAULT_MIN_SEVERITY = 10;
    private static final int DEFAULT_MAX_MESSAGES = 100;

    /** Input schema of both tools. */
    private static final String INPUT_SCHEMA = "{\"type\":\"object\",\"properties\":{" //$NON-NLS-1$
        + "\"file\":{\"type\":\"string\",\"description\":\"Absolute path of a source file of a local IFS project of the RDi " //$NON-NLS-1$
        + "workspace, e.g. C:/git/demo/qrpglesrc/hello.pgm.rpgle.\"}," //$NON-NLS-1$
        + "\"minSeverity\":{\"type\":\"integer\",\"minimum\":0,\"description\":\"Lowest severity of the messages returned, " //$NON-NLS-1$
        + "e.g. 0 for all messages including the informational ones, 20 for errors only. Default: 10.\"}," //$NON-NLS-1$
        + "\"maxMessages\":{\"type\":\"integer\",\"minimum\":1,\"description\":\"Maximum number of messages returned. If there " //$NON-NLS-1$
        + "are more, truncated is true. Default: 100.\"}},\"required\":[\"file\"]}"; //$NON-NLS-1$

    /** Conditions of both tools, for their descriptions. */
    static final String CONDITIONS = "The project must be a local IFS project of RDi (a project of the workspace with an RDi connection, " //$NON-NLS-1$
        + "see IFS Projects), the user must have allowed it in RDi (Window > Preferences > iAgentBridge > IFS Projects) and approved the " //$NON-NLS-1$
        + "project (Project > Properties > IFS Project > iAgentBridge), the RDi connection must be connected, and no file of the " //$NON-NLS-1$
        + "project may have unsaved changes in an RDi editor. Files of the project are returned with their local paths, copy books " //$NON-NLS-1$
        + "and other sources of the IBM i with their paths there. While the tool runs, the server answers no other call; the user " //$NON-NLS-1$
        + "can cancel it in RDi."; //$NON-NLS-1$

    @Override
    public JsonObject getInputSchema() {
        return new JsonParser().parse(INPUT_SCHEMA).getAsJsonObject();
    }

    @Override
    public McpToolResult execute(JsonObject anArguments) throws IOException, InterruptedException {

        String path = ToolArguments.getString(anArguments, ARGUMENT_FILE);
        int minSeverity = ToolArguments.getInt(anArguments, ARGUMENT_MIN_SEVERITY, DEFAULT_MIN_SEVERITY, 0);
        int maxMessages = ToolArguments.getInt(anArguments, ARGUMENT_MAX_MESSAGES, DEFAULT_MAX_MESSAGES, 1);
        int timeoutSeconds = getTimeoutSeconds(anArguments);

        final IFile file = findFile(path);
        IProject project = file.getProject();

        if (!AgentSettings.isCompileAllowed()) {
            return McpToolResult.error("Compiling on the IBM i is not allowed for AI agents in this RDi workspace. The user can allow it " //$NON-NLS-1$
                + "in RDi in Window > Preferences > iAgentBridge > IFS Projects and must approve the project in Project > Properties > " //$NON-NLS-1$
                + "IFS Project > iAgentBridge."); //$NON-NLS-1$
        }

        if (!AgentSettings.isApproved(project)) {
            return McpToolResult.error("Project " + project.getName() + " is not approved for AI agents. The user can approve it in RDi " //$NON-NLS-1$ //$NON-NLS-2$
                + "in Project > Properties > IFS Project > iAgentBridge."); //$NON-NLS-1$
        }

        List<String> unsavedFiles = getFilesWithUnsavedChanges(project);
        if (!unsavedFiles.isEmpty()) {
            return McpToolResult.error("Files of project " + project.getName() + " have unsaved changes in an RDi editor: " //$NON-NLS-1$ //$NON-NLS-2$
                + String.join(", ", unsavedFiles) + ". The tool uploads the files on disk. Save or discard the changes in RDi first."); //$NON-NLS-1$ //$NON-NLS-2$
        }

        final Outcome outcome = new Outcome();
        final Job job = Job.create(getJobName(file), new IJobFunction() {
            public IStatus run(IProgressMonitor aMonitor) {
                try {
                    outcome.result = AbstractFileCompilerTool.this.run(file, aMonitor);
                } catch (CompileException e) {
                    outcome.exception = e;
                } catch (InterruptedException e) {
                    return Status.CANCEL_STATUS;
                }
                return Status.OK_STATUS;
            }
        });
        job.setPriority(Job.LONG);

        // Cancels the job like the user in the view Progress: the upload and
        // the compile command stop.
        Watchdog watchdog = null;
        if (timeoutSeconds > 0) {
            watchdog = Watchdog.start(new Runnable() {
                public void run() {
                    job.cancel();
                }
            }, TimeUnit.SECONDS.toMillis(timeoutSeconds));
        }

        IStatus status;
        try {
            status = ToolJobs.runAndWait(job);
        } finally {
            if (watchdog != null) {
                watchdog.stop();
            }
        }

        if (status.getSeverity() == IStatus.CANCEL) {
            if (watchdog != null && watchdog.isExpired()) {
                return McpToolResult.error("The tool was cancelled after " + timeoutSeconds + " seconds, see the argument " //$NON-NLS-1$ //$NON-NLS-2$
                    + ARGUMENT_TIMEOUT_SECONDS + "."); //$NON-NLS-1$
            }
            return McpToolResult.error("The tool was cancelled in RDi."); //$NON-NLS-1$
        }
        if (outcome.exception != null) {
            return McpToolResult.error(outcome.exception.getMessage());
        }
        if (outcome.result == null) {
            // An unexpected exception of the job; the error log of RDi has its
            // stack trace.
            Throwable cause = status.getException();
            return McpToolResult.error("The tool failed: " + status.getMessage() + (cause == null ? "" : " " + cause)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }

        CompileResult result = outcome.result;
        CompileReport report = createReport(toPath(file), result, minSeverity, maxMessages);
        report.addAll(EventsFileMessages.parse(result.getEvents(), result.getCcsid(), result.getLocalFiles()));
        report.setOutput(result.getOutput());

        return report.toToolResult();
    }

    /**
     * Returns the timeout of the tool, after which its job is cancelled.
     *
     * @param anArguments - arguments of the tool
     * @return timeout in seconds or 0, if the tool has no timeout
     * @throws IllegalArgumentException if the argument is invalid
     */
    protected int getTimeoutSeconds(JsonObject anArguments) {
        return 0;
    }

    /**
     * Returns the name of the job, which the view Progress shows.
     *
     * @param aFile - file
     * @return name
     */
    protected abstract String getJobName(IFile aFile);

    /**
     * Compiles the file or checks its syntax, see {@link FileCompiler}.
     *
     * @param aFile - file of a local IFS project
     * @param aMonitor - progress monitor of the job
     * @return result
     * @throws CompileException if the file could not be compiled or checked
     * @throws InterruptedException if the job has been canceled
     */
    protected abstract CompileResult run(IFile aFile, IProgressMonitor aMonitor) throws CompileException, InterruptedException;

    /**
     * Creates the empty report of the result.
     *
     * @param aFile - absolute path of the file
     * @param aResult - result
     * @param aMinSeverity - lowest severity of the messages returned
     * @param aMaxMessages - maximum number of messages returned
     * @return report
     */
    protected abstract CompileReport createReport(String aFile, CompileResult aResult, int aMinSeverity, int aMaxMessages);

    /**
     * Returns the file of a local IFS project, that is stored at a path of the
     * local file system.
     *
     * @param aPath - absolute path passed by the client, also a path of Git
     *        Bash like '/c/dir/file'
     * @return file of the workspace
     * @throws IllegalArgumentException if the file does not exist or does not
     *         belong to a local IFS project
     */
    private static IFile findFile(String aPath) throws IOException {

        File localFile = new File(ToolArguments.toLocalPath(aPath));
        if (!localFile.isFile()) {
            throw new IllegalArgumentException(aPath + ": File not found."); //$NON-NLS-1$
        }

        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IProject otherProject = null;
        for (IFile file : root.findFilesForLocationURI(localFile.getCanonicalFile().toURI())) {
            if (FileCompiler.isLocalIfsProject(file.getProject())) {
                return file;
            }
            if (file.getProject().isOpen()) {
                otherProject = file.getProject();
            }
        }

        if (otherProject != null) {
            throw new IllegalArgumentException(aPath + ": Project " + otherProject.getName() + " is not configured as local IFS project " //$NON-NLS-1$ //$NON-NLS-2$
                + "in RDi. The user can configure it, e.g. with 'Configure as IFS Project...' in the context menu of the project."); //$NON-NLS-1$
        }

        throw new IllegalArgumentException(aPath + ": The file does not belong to an open project of the RDi workspace " //$NON-NLS-1$
            + root.getLocation().toOSString() + "; import_projects imports a project."); //$NON-NLS-1$
    }

    /**
     * Returns the files of a project, that have unsaved changes in an editor of
     * RDi. The editors are checked in the UI thread.
     */
    private static List<String> getFilesWithUnsavedChanges(final IProject aProject) {

        final List<String> files = new ArrayList<>();
        if (!PlatformUI.isWorkbenchRunning()) {
            return files;
        }

        final IWorkbench workbench = PlatformUI.getWorkbench();
        workbench.getDisplay().syncExec(new Runnable() {
            public void run() {
                for (IWorkbenchWindow window : workbench.getWorkbenchWindows()) {
                    for (IWorkbenchPage page : window.getPages()) {
                        for (IEditorReference editor : page.getEditorReferences()) {
                            IFile file = editor.isDirty() ? getFile(editor) : null;
                            if (file != null && aProject.equals(file.getProject())) {
                                files.add(toPath(file));
                            }
                        }
                    }
                }
            }
        });

        return files;
    }

    private static IFile getFile(IEditorReference anEditor) {

        IEditorInput input;
        try {
            input = anEditor.getEditorInput();
        } catch (PartInitException e) {
            return null;
        }

        if (input instanceof IFileEditorInput) {
            return ((IFileEditorInput)input).getFile();
        }

        return null;
    }

    /**
     * Returns the path of a file, that the tool returns to the client.
     */
    private static String toPath(IFile aFile) {

        if (aFile.getLocation() == null) {
            return aFile.getFullPath().toString();
        }

        return aFile.getLocation().toString();
    }

    /**
     * Result or error of the job.
     */
    private static final class Outcome {
        private volatile CompileResult result;
        private volatile CompileException exception;
    }
}
