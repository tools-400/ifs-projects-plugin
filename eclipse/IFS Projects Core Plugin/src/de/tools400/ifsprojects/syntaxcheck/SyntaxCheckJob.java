/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.syntaxcheck;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.subsystems.files.core.SystemIFileProperties;

import com.ibm.as400.access.AS400;

import de.tools400.ifsprojects.IfsProjectsPlugin;
import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.errorlist.ErrorList;
import de.tools400.ifsprojects.errorlist.EventsFiles;
import de.tools400.ifsprojects.local.SourceMapping;
import de.tools400.ifsprojects.project.IProjConfig;
import de.tools400.ifsprojects.project.IProjValues;
import de.tools400.ifsprojects.utils.ExceptionUtils;
import de.tools400.ifsprojects.utils.PlatformUtils;

/**
 * Checks the syntax of a stream file of an IFS project and shows the errors in
 * the 'Error List' view of RDi.
 */
public class SyntaxCheckJob extends Job {

    private static final long UPLOAD_TIMEOUT_MILLIS = 60000;
    private static final long UPLOAD_POLL_INTERVAL_MILLIS = 200;

    private final IHost host;
    private final AS400 system;
    private final String projectDirectory;
    private final IProjConfig config;
    private final Map<String, String> variables;
    private final String sourcePath;
    private final String fileName;
    private final SourceType sourceType;
    private final List<IFile> savedFiles;
    private final SourceMapping sourceMapping;

    /**
     * Creates the job.
     *
     * @param aHost - RSE connection
     * @param aSystem - connected system object of RSE
     * @param aProjectDirectory - absolute directory of the IFS project
     * @param aConfig - content of <code>iproj.json</code>
     * @param aVariables - environment variables of the project
     * @param aSourcePath - absolute path of the stream file
     * @param aFileName - name of the stream file
     * @param aSourceType - type of the stream file
     * @param aSavedFiles - local files of the editors, that have been saved
     *        before the syntax check. The job waits, until they have been
     *        uploaded.
     * @param aSourceMapping - mapping of the project directory to a local
     *        project or <code>null</code>. The errors of the files of the
     *        local project are shown for the local files.
     */
    public SyntaxCheckJob(IHost aHost, AS400 aSystem, String aProjectDirectory, IProjConfig aConfig, Map<String, String> aVariables,
        String aSourcePath, String aFileName, SourceType aSourceType, List<IFile> aSavedFiles, SourceMapping aSourceMapping) {
        super(Messages.bind(Messages.Task_Checking_syntax_of_A, aFileName));

        this.host = aHost;
        this.system = aSystem;
        this.projectDirectory = aProjectDirectory;
        this.config = aConfig;
        this.variables = aVariables;
        this.sourcePath = aSourcePath;
        this.fileName = aFileName;
        this.sourceType = aSourceType;
        this.savedFiles = new ArrayList<IFile>(aSavedFiles);
        this.sourceMapping = aSourceMapping;

        setUser(true);
    }

    @Override
    protected IStatus run(IProgressMonitor aMonitor) {

        aMonitor.beginTask(getName(), IProgressMonitor.UNKNOWN);
        try {

            waitForUpload(aMonitor);

            String source = SyntaxCheckRunner.readStreamFile(system, sourcePath);
            boolean noMain = SyntaxCheckCommands.isNoMain(source);

            String objectName = SyntaxCheckCommands.getObjectName(fileName);
            List<String> includeDirectories = IProjValues.getIncludeDirectories(projectDirectory, config, variables);
            String compileCommand = SyntaxCheckCommands.getCompileCommand(sourceType, objectName, sourcePath, includeDirectories, noMain);
            List<String> setupCommands = SyntaxCheckCommands.getSetupCommands(projectDirectory, config, variables);

            SyntaxCheckResult result = SyntaxCheckRunner.run(system, setupCommands, objectName, compileCommand, aMonitor);
            if (result.getEvents() == null) {
                throw new SyntaxCheckException(Messages.bind(Messages.Error_No_events_file_A_B, compileCommand, result.getCompileMessages()), null);
            }

            String events = EventsFiles.removeMessages(result.getEvents(), SyntaxCheckCommands.getIgnoredMessageIds(sourceType, noMain));
            ErrorList.showEvents(getName(), host, sourcePath, events, result.getCcsid(), sourceMapping);

        } catch (InterruptedException e) {
            // Thrown on a cancel of the job, too, see waitForUpload() and
            // SyntaxCheckRunner.
            if (!aMonitor.isCanceled()) {
                Thread.currentThread().interrupt();
            }
            return Status.CANCEL_STATUS;
        } catch (SyntaxCheckException e) {
            if (e.getCause() != null) {
                IfsProjectsPlugin.logError("Syntax check failed: " + sourcePath, e.getCause()); //$NON-NLS-1$
            }
            PlatformUtils.openErrorAsync(Messages.Title_IFS_Projects, ExceptionUtils.getLocalizedMessage(e));
        } catch (RuntimeException e) {
            IfsProjectsPlugin.logError("Unexpected error checking the syntax of: " + sourcePath, e); //$NON-NLS-1$
            PlatformUtils.openErrorAsync(Messages.Title_IFS_Projects, Messages.bind(Messages.Error_Unexpected_error_A,
                ExceptionUtils.getLocalizedMessage(e)));
        } finally {
            aMonitor.done();
        }

        return Status.OK_STATUS;
    }

    /**
     * Waits until RSE has uploaded the files saved before the syntax check.
     */
    private void waitForUpload(IProgressMonitor aMonitor) throws SyntaxCheckException, InterruptedException {

        long timeout = System.currentTimeMillis() + UPLOAD_TIMEOUT_MILLIS;
        for (IFile file : savedFiles) {
            while (new SystemIFileProperties(file).getDirty()) {
                if (aMonitor.isCanceled()) {
                    throw new InterruptedException();
                }
                if (System.currentTimeMillis() > timeout) {
                    throw new SyntaxCheckException(Messages.bind(Messages.Error_A_has_not_been_uploaded, sourcePath), null);
                }
                Thread.sleep(UPLOAD_POLL_INTERVAL_MILLIS);
            }
        }
    }
}
