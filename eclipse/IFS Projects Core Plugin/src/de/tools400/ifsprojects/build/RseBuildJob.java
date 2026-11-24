/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.build;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.rse.core.model.IHost;

import de.tools400.ifsprojects.IfsProjectsPlugin;
import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.errorlist.ErrorList;
import de.tools400.ifsprojects.local.SourceMapping;
import de.tools400.ifsprojects.utils.ExceptionUtils;
import de.tools400.ifsprojects.utils.PlatformUtils;

/**
 * Runs the build or compile command of an IFS project in a remote command
 * server job of the RSE connection instead of the SSH terminal, see
 * {@link RseBuild}.
 * <p>
 * The output is shown line by line in the 'Commands Log' view. Afterwards the
 * events files of the compiled objects are shown in the 'Error List' view, see
 * {@link BuildEvents}.
 * <p>
 * Builds of the same project run one after the other. A canceled build is
 * stopped by terminating its processes, see
 * {@link RseBuildCommands#getStopScript(String)}.
 */
public class RseBuildJob extends Job {

    private final IHost host;
    private final String directory;
    private final RseBuild build;
    private final SourceMapping sourceMapping;

    /**
     * Creates the job.
     *
     * @param aName - name of the job, e.g. "Building DSPSRC ..."
     * @param aHost - RSE connection
     * @param aBuild - build, see {@link RseBuild#create}
     * @param aSourceMapping - mapping of the project directory to a local
     *        project or <code>null</code>, see {@link ErrorList#showEvents}
     */
    public RseBuildJob(String aName, IHost aHost, RseBuild aBuild, SourceMapping aSourceMapping) {
        super(aName);

        this.host = aHost;
        this.directory = aBuild.getDirectory();
        this.build = aBuild;
        this.sourceMapping = aSourceMapping;

        setRule(RseBuild.getRule(aHost, directory));
    }

    @Override
    protected IStatus run(IProgressMonitor aMonitor) {

        aMonitor.beginTask(getName(), IProgressMonitor.UNKNOWN);

        CommandsLog.show(host);
        CommandsLog.log(host, Messages.bind(Messages.Log_Running_A_in_B, build.getDisplayCommand(), directory));

        try {

            RseBuild.Result result = build.run(new RseBuild.IOutput() {
                public void line(String aLine) {
                    CommandsLog.log(host, aLine);
                }
            }, aMonitor);

            if (result.getEvents() != null) {
                ErrorList.showEvents(getName(), host, directory, result.getEvents(), result.getCcsid(), sourceMapping);
            }

        } catch (OperationCanceledException e) {
            return Status.CANCEL_STATUS;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Status.CANCEL_STATUS;
        } catch (Exception e) {
            IfsProjectsPlugin.logError("Could not run command: " + build.getDisplayCommand(), e); //$NON-NLS-1$
            String message = Messages.bind(Messages.Error_Could_not_run_A_B, build.getDisplayCommand(), ExceptionUtils.getLocalizedMessage(e));
            CommandsLog.log(host, message);
            PlatformUtils.openErrorAsync(Messages.Title_IFS_Projects, message);
        } finally {
            aMonitor.done();
        }

        return Status.OK_STATUS;
    }
}
