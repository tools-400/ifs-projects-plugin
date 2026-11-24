/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.core.subsystems.IConnectorService;

import com.ibm.as400.access.AS400;
import com.ibm.etools.iseries.subsystems.qsys.api.IBMiConnection;
import com.ibm.etools.iseries.toolbox.IToolboxSessionProvider;

import de.tools400.ifsprojects.build.BuildCommands;
import de.tools400.ifsprojects.build.RseBuild;
import de.tools400.ifsprojects.local.Deployment;
import de.tools400.ifsprojects.local.EventsFileMapper;
import de.tools400.ifsprojects.local.LocalProject;
import de.tools400.ifsprojects.local.SourceMapping;
import de.tools400.ifsprojects.preferences.Preferences;
import de.tools400.ifsprojects.project.EnvironmentVariables;
import de.tools400.ifsprojects.project.IProjConfig;
import de.tools400.ifsprojects.project.IProjParseException;
import de.tools400.ifsprojects.remote.RemoteJobs;
import de.tools400.ifsprojects.remote.RemoteSession;
import de.tools400.ifsprojects.syntaxcheck.SourceType;
import de.tools400.ifsprojects.syntaxcheck.SyntaxCheckException;
import de.tools400.ifsprojects.syntaxcheck.SyntaxCheckResult;
import de.tools400.ifsprojects.syntaxcheck.SyntaxCheckRunner;
import de.tools400.ifsprojects.utils.ExceptionUtils;

/**
 * Compiles a file of a local IFS project on the IBM i or checks its syntax,
 * like <i>Build on IBM i</i> and <i>Check Syntax</i> of a file, for other
 * plug-ins, e.g. for the tools of AI agents.
 * <p>
 * Both methods upload the changes of the project to its deploy directory first.
 * Unlike the commands of the menus, they open no dialog and no view:
 * <ul>
 * <li>The RSE connection of the project must be connected. They never connect
 * it, because RDi would ask for the password in a dialog.</li>
 * <li>Files changed in the IFS since the last upload are not replaced; the
 * methods fail instead.</li>
 * <li>Unsaved changes in the editors of RDi are not saved; the caller checks
 * them. The files on disk are uploaded.</li>
 * <li>The compile command always runs in a job of the RSE connection, never in
 * the SSH terminal.</li>
 * </ul>
 * The upload holds the scheduling rule of the project and of its deploy
 * directory, see {@link Deployment#deploy}, the compile the rule of the builds
 * of the project, see {@link RseBuild#getRule}. The thread must not hold a
 * conflicting rule.
 * <p>
 * The methods access the remote system and must not be called on the UI thread.
 * The progress monitor cancels the upload and the compile; the syntax check
 * stops only between its commands.
 */
public final class FileCompiler {

    private static final char SEPARATOR = '/';
    private static final String LINE_SEPARATOR_PATTERN = "\\r?\\n"; //$NON-NLS-1$
    private static final int MAX_LISTED_FILES = 20;

    private FileCompiler() {
        // Utility class
    }

    /**
     * Tells, whether a project is configured as local IFS project.
     *
     * @param aProject - project or <code>null</code>
     * @return <code>true</code>, if the project is an open local IFS project
     */
    public static boolean isLocalIfsProject(IProject aProject) {
        return LocalProject.get(aProject) != null;
    }

    /**
     * Uploads the changes of the local IFS project of a file and checks the
     * syntax of the file, like <i>Check Syntax</i>: compiles it without
     * creating an object, with the library list and the include directories of
     * the project.
     *
     * @param aFile - RPGLE or SQLRPGLE file of a local IFS project
     * @param aMonitor - progress monitor
     * @return result with the events of the compiler
     * @throws CompileException if the syntax could not be checked
     * @throws InterruptedException if the syntax check has been canceled
     */
    public static CompileResult checkSyntax(IFile aFile, IProgressMonitor aMonitor) throws CompileException, InterruptedException {

        SourceType sourceType = SourceType.fromFileName(aFile.getName());
        if (sourceType == null) {
            throw new CompileException("The syntax check supports RPGLE and SQLRPGLE sources, not " + aFile.getName() + ".", null); //$NON-NLS-1$ //$NON-NLS-2$
        }

        SubMonitor progress = SubMonitor.convert(aMonitor, 2);
        Upload upload = upload(aFile, progress.newChild(1, SubMonitor.SUPPRESS_NONE));

        SyntaxCheckResult result;
        try {
            result = SyntaxCheckRunner.check(upload.rseSystem, upload.directory, upload.config, upload.variables, upload.path, aFile.getName(),
                sourceType, progress.newChild(1, SubMonitor.SUPPRESS_NONE));
        } catch (SyntaxCheckException e) {
            throw new CompileException("The syntax check failed: " + ExceptionUtils.getLocalizedMessage(e), e); //$NON-NLS-1$
        }

        return createResult(result.getCommand(), null, toLines(result.getCompileMessages()), result.getEvents(), result.getCcsid(), upload);
    }

    /**
     * Uploads the changes of the local IFS project of a file and compiles the
     * file, like <i>Build on IBM i</i> of a file: runs the compile command of
     * the <code>iproj.json</code> file or the default compile command of the
     * preferences, e.g. <code>makei compile -f hello.pgm.rpgle</code>, and
     * reads the events files of the compiled objects.
     *
     * @param aFile - file of a local IFS project
     * @param aMonitor - progress monitor
     * @return result with the output of the command and the events
     * @throws CompileException if the file could not be compiled
     * @throws InterruptedException if the compile has been canceled
     */
    public static CompileResult compile(IFile aFile, IProgressMonitor aMonitor) throws CompileException, InterruptedException {

        SubMonitor progress = SubMonitor.convert(aMonitor, 2);
        Upload upload = upload(aFile, progress.newChild(1, SubMonitor.SUPPRESS_NONE));

        String command = BuildCommands.getCompileCommand(upload.config, aFile.getName(), upload.host.getHostName(), upload.rseSystem.getUserId());
        RseBuild build = RseBuild.create(upload.rseSystem, upload.directory, upload.config, upload.variables, command);

        final List<String> output = new ArrayList<>();
        IProgressMonitor buildMonitor = progress.newChild(1, SubMonitor.SUPPRESS_NONE);
        ISchedulingRule rule = RseBuild.getRule(upload.host, upload.directory);
        RseBuild.Result result;
        try {
            // endRule() is called also, if beginRule() fails.
            Job.getJobManager().beginRule(rule, buildMonitor);
            result = build.run(new RseBuild.IOutput() {
                public void line(String aLine) {
                    output.add(aLine);
                }
            }, buildMonitor);
        } catch (OperationCanceledException e) {
            throw new InterruptedException();
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            throw new CompileException("The compile command could not be run: " + ExceptionUtils.getLocalizedMessage(e), e); //$NON-NLS-1$
        } finally {
            Job.getJobManager().endRule(rule);
        }

        return createResult(build.getDisplayCommand(), result.getExitCode(), output, result.getEvents(), result.getCcsid(), upload);
    }

    /**
     * Uploads the changes of the local IFS project of a file.
     */
    private static Upload upload(IFile aFile, IProgressMonitor aMonitor) throws CompileException, InterruptedException {

        IProject project = aFile.getProject();
        LocalProject localProject = LocalProject.get(project);
        if (localProject == null) {
            throw new CompileException("Project " + project.getName() + " is not configured as local IFS project. Configure it in RDi, " //$NON-NLS-1$ //$NON-NLS-2$
                + "e.g. with 'Configure as IFS Project...' in the context menu of the project.", null); //$NON-NLS-1$
        }

        IHost host = localProject.getHost();
        if (host == null) {
            throw new CompileException("The RSE connection " + localProject.getConnectionName() + " of project " + project.getName() //$NON-NLS-1$ //$NON-NLS-2$
                + " does not exist in RDi.", null); //$NON-NLS-1$
        }

        AS400 rseSystem = getConnectedSystem(host);
        IProjConfig config = readConfig(localProject);
        Map<String, String> variables = EnvironmentVariables.merge(Preferences.getInstance().getDefaultEnvironmentVariables(),
            localProject.getEnvironmentVariables());

        final List<String> changedFiles = new ArrayList<>();
        Deployment.ConflictHandler conflictHandler = new Deployment.ConflictHandler() {
            public boolean isOverwrite(List<String> aPaths) {
                changedFiles.addAll(aPaths);
                return false;
            }
        };

        String directory = null;
        Deployment.Result result;
        try (RemoteSession session = new RemoteSession(rseSystem)) {
            AS400 system = session.getSystem();
            directory = localProject.getDeployDirectory(RemoteJobs.getHomeDirectory(system));
            result = Deployment.deploy(system, project, localProject.getConnectionName(), directory, false, conflictHandler, aMonitor);
        } catch (InterruptedException e) {
            if (changedFiles.isEmpty()) {
                throw e;
            }
            throw new CompileException("Files of the deploy directory " + directory + " have been changed in the IFS since the last upload, " //$NON-NLS-1$ //$NON-NLS-2$
                + "e.g. by someone else: " + formatPaths(changedFiles) + ". They are not replaced. Check them and upload the project in RDi " //$NON-NLS-1$ //$NON-NLS-2$
                + "with 'Upload All to IFS', which replaces them.", null); //$NON-NLS-1$
        } catch (Exception e) {
            throw new CompileException("The upload of project " + project.getName() + " failed: " + ExceptionUtils.getLocalizedMessage(e), e); //$NON-NLS-1$ //$NON-NLS-2$
        }

        String relativePath = aFile.getProjectRelativePath().toString();
        if (!result.isDeployed(relativePath)) {
            throw new CompileException("The file " + relativePath + " is not uploaded, because it is excluded, e.g. by .deployignore or " //$NON-NLS-1$ //$NON-NLS-2$
                + ".gitignore of the project.", null); //$NON-NLS-1$
        }

        return new Upload(project, host, rseSystem, config, variables, directory, directory + SEPARATOR + relativePath);
    }

    /**
     * Returns the system object of a connected RSE connection.
     * <code>IBMiConnection.getAS400ToolboxObject()</code> is not used: it
     * connects a disconnected connection, and RSE would ask for the password in
     * a dialog.
     *
     * @return system object of RSE, which is copied for the remote jobs
     */
    private static AS400 getConnectedSystem(IHost aHost) throws CompileException {

        IBMiConnection connection = IBMiConnection.getConnection(aHost);
        if (connection == null) {
            throw new CompileException("The RSE connection " + aHost.getAliasName() + " is no IBM i connection.", null); //$NON-NLS-1$ //$NON-NLS-2$
        }

        AS400 system = null;
        if (connection.isConnected()) {
            IConnectorService service = connection.getQSYSObjectSubSystem().getConnectorService();
            if (service instanceof IToolboxSessionProvider) {
                system = ((IToolboxSessionProvider)service).getAS400();
            }
        }

        if (system == null) {
            throw new CompileException("The RSE connection " + aHost.getAliasName() + " is not connected. Connect it in RDi first.", null); //$NON-NLS-1$ //$NON-NLS-2$
        }

        return system;
    }

    private static IProjConfig readConfig(LocalProject aLocalProject) throws CompileException {

        try {
            return aLocalProject.readConfig();
        } catch (IProjParseException e) {
            throw new CompileException("The file " + IProjConfig.FILE_NAME + " of project " + aLocalProject.getName() + " is invalid: " //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                + ExceptionUtils.getLocalizedMessage(e), e);
        } catch (Exception e) {
            throw new CompileException("The file " + IProjConfig.FILE_NAME + " of project " + aLocalProject.getName() + " could not be read: " //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                + ExceptionUtils.getLocalizedMessage(e), e);
        }
    }

    /**
     * Creates the result: replaces the paths of the files of the deploy
     * directory in the events by the locations of the local files.
     */
    private static CompileResult createResult(String aCommand, Integer anExitCode, List<String> anOutput, String anEvents, int aCcsid,
        Upload anUpload) {

        if (anEvents == null) {
            return new CompileResult(aCommand, anExitCode, anOutput, null, aCcsid, Collections.<String, String> emptyMap());
        }

        EventsFileMapper.Result mapped = EventsFileMapper.map(anEvents, new SourceMapping(anUpload.directory, anUpload.project.getName()));
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        Map<String, String> localFiles = new LinkedHashMap<>();
        for (String location : mapped.getLocations()) {
            IPath path = root.getFile(new Path(location)).getLocation();
            if (path != null) {
                localFiles.put(location, path.toString());
            }
        }

        return new CompileResult(aCommand, anExitCode, anOutput, mapped.getEvents(), aCcsid, localFiles);
    }

    private static List<String> toLines(String aText) {

        List<String> lines = new ArrayList<>();
        if (aText != null) {
            for (String line : aText.split(LINE_SEPARATOR_PATTERN)) {
                if (!line.trim().isEmpty()) {
                    lines.add(line);
                }
            }
        }

        return lines;
    }

    private static String formatPaths(List<String> aPaths) {

        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < aPaths.size() && i < MAX_LISTED_FILES; i++) {
            if (i > 0) {
                buffer.append(", "); //$NON-NLS-1$
            }
            buffer.append(aPaths.get(i));
        }

        if (aPaths.size() > MAX_LISTED_FILES) {
            buffer.append(" and ").append(aPaths.size() - MAX_LISTED_FILES).append(" more"); //$NON-NLS-1$ //$NON-NLS-2$
        }

        return buffer.toString();
    }

    /**
     * Uploaded local IFS project.
     */
    private static final class Upload {

        private final IProject project;
        private final IHost host;
        private final AS400 rseSystem;
        private final IProjConfig config;
        private final Map<String, String> variables;
        private final String directory;
        private final String path;

        private Upload(IProject aProject, IHost aHost, AS400 aRseSystem, IProjConfig aConfig, Map<String, String> aVariables, String aDirectory,
            String aPath) {
            this.project = aProject;
            this.host = aHost;
            this.rseSystem = aRseSystem;
            this.config = aConfig;
            this.variables = aVariables;
            this.directory = aDirectory;
            this.path = aPath;
        }
    }
}
