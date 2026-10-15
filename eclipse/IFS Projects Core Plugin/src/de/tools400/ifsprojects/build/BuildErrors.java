/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.build;

import java.util.Collections;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.UIJob;

import de.tools400.ifsprojects.IfsProjectsPlugin;
import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.local.EventsFileMapper;
import de.tools400.ifsprojects.local.SourceMapping;
import de.tools400.ifsprojects.syntaxcheck.ErrorList;
import de.tools400.ifsprojects.syntaxcheck.SyntaxCheckException;

/**
 * Shows the events of a build in the 'Error List' view, see
 * {@link BuildEvents}.
 */
final class BuildErrors {

    private BuildErrors() {
        // Utility class
    }

    /**
     * Shows the events in the 'Error List' view. The local events file is
     * written in a UI job, because the scheduling rule of the build job does
     * not contain the rules of the workspace resources.
     * <p>
     * The project directory of a local project is a copy of the project. Its
     * errors are shown for the local files, see {@link SourceMapping}.
     *
     * @param aJobName - name of the UI job
     * @param aHost - RSE connection
     * @param aDirectory - project directory
     * @param anEvents - events, see {@link BuildEvents#collect}
     * @param aCcsid - CCSID of the job, in which the events files were
     *        written
     * @param aSourceMapping - mapping of the project directory to a local
     *        project or <code>null</code>
     */
    static void show(String aJobName, final IHost aHost, final String aDirectory, String anEvents, final int aCcsid,
        final SourceMapping aSourceMapping) {

        final String events;
        final Set<String> projectLocations;
        if (aSourceMapping == null) {
            events = anEvents;
            projectLocations = Collections.emptySet();
        } else {
            EventsFileMapper.Result result = EventsFileMapper.map(anEvents, aSourceMapping);
            events = result.getEvents();
            projectLocations = result.getLocations();
        }

        UIJob job = new UIJob(aJobName) {
            @Override
            public IStatus runInUIThread(IProgressMonitor aMonitor) {
                try {
                    IFile eventsFile = ErrorList.writeEventsFile(aHost, aDirectory, events, aCcsid, aMonitor);
                    String projectName = aSourceMapping == null ? null : aSourceMapping.getProjectName();
                    ErrorList.show(eventsFile, aHost, ErrorList.getTitle(aHost, aDirectory), projectName, projectLocations);
                } catch (CoreException e) {
                    IfsProjectsPlugin.logError("Could not write the events file of: " + aDirectory, e); //$NON-NLS-1$
                    MessageDialog.openError(getShell(), Messages.Title_IFS_Projects,
                        Messages.bind(Messages.Error_Could_not_show_errors_A, e.getLocalizedMessage()));
                } catch (SyntaxCheckException e) {
                    if (e.getCause() != null) {
                        IfsProjectsPlugin.logError("Could not show the errors of: " + aDirectory, e.getCause()); //$NON-NLS-1$
                    }
                    MessageDialog.openError(getShell(), Messages.Title_IFS_Projects, e.getMessage());
                }
                return Status.OK_STATUS;
            }
        };
        job.schedule();
    }

    static Shell getShell() {

        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window == null) {
            return null;
        }

        return window.getShell();
    }
}
