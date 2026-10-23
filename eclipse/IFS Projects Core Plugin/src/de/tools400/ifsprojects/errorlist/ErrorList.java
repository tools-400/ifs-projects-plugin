/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.errorlist;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.services.clientserver.messages.SystemMessage;
import org.eclipse.rse.subsystems.files.core.SystemIFileProperties;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.UIJob;

import com.ibm.etools.iseries.rse.ui.resources.IQSYSMarker;
import com.ibm.etools.iseries.rse.ui.view.errorlist.QSYSErrorListViewPart;
import com.ibm.etools.iseries.rse.util.evfparser.EventsFileParser;

import de.tools400.ifsprojects.IfsProjectsPlugin;
import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.local.EventsFileMapper;
import de.tools400.ifsprojects.local.SourceMapping;
import de.tools400.ifsprojects.utils.ExceptionUtils;
import de.tools400.ifsprojects.utils.PlatformUtils;

/**
 * Shows the errors of an events file in the 'Error List' view of RDi, see
 * {@link #showEvents}.
 * <p>
 * Like RDi, the events file is stored as local file in project
 * <code>RemoteSystemsTempFiles</code>, folder <code>IFSProjects</code>. RSE
 * does not upload it, because it has no remote file properties. The parser of
 * RDi attaches the errors as markers to this file. The view shows
 * one table per local file. Each checked stream file has a local file of its
 * own, so that the table of a stream file is replaced, when it is checked
 * again.
 * <p>
 * The view opens the file of an error with the location of the error, e.g. a
 * stream file <code>/home/jsmith/src/hello.rpgle</code>. If the error also has
 * a project name, the view opens the local file of the project:
 * <code>/demo/src/hello.rpgle</code> is file <code>src/hello.rpgle</code> of
 * project <code>demo</code>.
 * <p>
 * The classes of RDi are available in RDi 9.6, 9.8 and 9.9.
 */
public final class ErrorList {

    /**
     * Project of the temporary files of RSE, that also contains the events
     * files of RDi. The project must not be hidden: the view finds new markers
     * via resource deltas, which do not contain hidden resources.
     */
    public static final String PROJECT_NAME = "RemoteSystemsTempFiles"; //$NON-NLS-1$

    /** Folder of the local events files of this plug-in. */
    public static final String FOLDER_NAME = "IFSProjects"; //$NON-NLS-1$

    private static final String EVENTS_FILE_EXTENSION = ".evfevent"; //$NON-NLS-1$
    private static final String CHARSET_UTF8 = "UTF-8"; //$NON-NLS-1$
    private static final String INVALID_NAME_CHARACTERS = "[\\\\/:*?\"<>|]"; //$NON-NLS-1$
    private static final String REPLACEMENT = "_"; //$NON-NLS-1$
    private static final String SEPARATOR = "/"; //$NON-NLS-1$
    private static final String TITLE_SEPARATOR = ":"; //$NON-NLS-1$

    private ErrorList() {
        // Utility class
    }

    /**
     * Shows events in the 'Error List' view. The local events file is written
     * in a workspace job, because the scheduling rules of the jobs of the
     * plug-in do not contain the workspace resources, and its errors are shown
     * in a UI job. Errors are logged and reported to the user. May be called
     * from any thread.
     * <p>
     * The project directory of a local project is a copy of the project. Its
     * errors are shown for the local files, see {@link SourceMapping}.
     *
     * @param aJobName - name of the jobs
     * @param aHost - RSE connection
     * @param aSourcePath - absolute path of the checked stream file or of the
     *        built project directory, which names the local events file and
     *        the table of the view
     * @param anEvents - content of the events files
     * @param aCcsid - CCSID of the job, in which the events files were written
     * @param aSourceMapping - mapping of the project directory to a local
     *        project or <code>null</code>
     */
    public static void showEvents(String aJobName, final IHost aHost, final String aSourcePath, String anEvents, final int aCcsid,
        SourceMapping aSourceMapping) {

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
        final String projectName = aSourceMapping == null ? null : aSourceMapping.getProjectName();

        WorkspaceJob job = new WorkspaceJob(aJobName) {
            @Override
            public IStatus runInWorkspace(IProgressMonitor aMonitor) {
                IFile eventsFile;
                try {
                    eventsFile = writeEventsFile(aHost, aSourcePath, events, aCcsid, aMonitor);
                } catch (CoreException e) {
                    IfsProjectsPlugin.logError("Could not write the events file of: " + aSourcePath, e); //$NON-NLS-1$
                    PlatformUtils.openErrorAsync(Messages.Title_IFS_Projects, Messages.bind(Messages.Error_Could_not_show_errors_A,
                        ExceptionUtils.getLocalizedMessage(e)));
                    return Status.OK_STATUS;
                }
                showLater(getName(), eventsFile, aHost, aSourcePath, projectName, projectLocations);
                return Status.OK_STATUS;
            }
        };
        job.setRule(ResourcesPlugin.getWorkspace().getRoot());
        job.setSystem(true);
        job.schedule();
    }

    /**
     * Shows the errors of a local events file in a UI job.
     */
    private static void showLater(String aJobName, final IFile anEventsFile, final IHost aHost, final String aSourcePath, final String aProjectName,
        final Set<String> aProjectLocations) {

        UIJob job = new UIJob(aJobName) {
            @Override
            public IStatus runInUIThread(IProgressMonitor aMonitor) {
                try {
                    show(anEventsFile, aHost, getTitle(aHost, aSourcePath), aProjectName, aProjectLocations);
                } catch (CoreException e) {
                    if (e.getStatus().getException() != null) {
                        IfsProjectsPlugin.logError("Could not show the errors of: " + aSourcePath, e.getStatus().getException()); //$NON-NLS-1$
                    }
                    MessageDialog.openError(PlatformUtils.getWorkbenchShell(), Messages.Title_IFS_Projects, e.getStatus().getMessage());
                }
                return Status.OK_STATUS;
            }
        };
        job.schedule();
    }

    /**
     * Returns the title of the table of a stream file in the 'Error List'
     * view, e.g. <code>myibmi:/home/jsmith/src/hello.rpgle</code>.
     */
    private static String getTitle(IHost aHost, String aSourcePath) {
        return aHost.getAliasName() + TITLE_SEPARATOR + aSourcePath;
    }

    /**
     * Writes the events to the local events file of a stream file. Runs in
     * the workspace job of {@link #showEvents}.
     *
     * @param aHost - RSE connection
     * @param aSourcePath - absolute path of the checked stream file
     * @param anEvents - content of the events file
     * @param aCcsid - CCSID of the job, in which the events file was written
     * @param aMonitor - progress monitor
     * @return local events file
     * @throws CoreException if the file could not be written
     */
    private static IFile writeEventsFile(IHost aHost, String aSourcePath, String anEvents, int aCcsid, IProgressMonitor aMonitor)
        throws CoreException {

        IProject project = getProject(aMonitor);

        IPath path = new Path(FOLDER_NAME).append(toResourceName(aHost.getAliasName()));
        String[] segments = aSourcePath.split(SEPARATOR);
        for (int i = 0; i < segments.length; i++) {
            if (segments[i].length() == 0) {
                continue;
            }
            if (i < segments.length - 1) {
                path = path.append(toResourceName(segments[i]));
            } else {
                path = path.append(toResourceName(segments[i]) + EVENTS_FILE_EXTENSION);
            }
        }

        IFile file = project.getFile(path);
        createFolders(file.getParent(), aMonitor);

        InputStream content = new ByteArrayInputStream(toBytes(anEvents));
        if (file.exists()) {
            file.setContents(content, IResource.FORCE, aMonitor);
        } else {
            file.create(content, IResource.FORCE, aMonitor);
        }
        file.setCharset(CHARSET_UTF8, aMonitor);

        // The parser of RDi computes the length of the messages with the CCSID
        // of the job, which wrote the events file.
        new SystemIFileProperties(file).setRemoteCCSID(aCcsid);

        return file;
    }

    /**
     * Parses a local events file and shows its errors in the 'Error List'
     * view. The errors of the specified locations are shown for the files of
     * a local project.
     * <p>
     * Must be called on the UI thread.
     *
     * @param anEventsFile - local events file, see {@link #writeEventsFile}
     * @param aHost - RSE connection
     * @param aTitle - title of the table, see {@link #getTitle}
     * @param aProjectName - name of the local project or <code>null</code>
     * @param aProjectLocations - locations of the files of the local project,
     *        e.g. <code>/demo/src/hello.rpgle</code>
     * @throws CoreException if the errors could not be shown
     */
    private static void show(final IFile anEventsFile, IHost aHost, String aTitle, final String aProjectName,
        final Set<String> aProjectLocations) throws CoreException {

        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        IWorkbenchPage page = window == null ? null : window.getActivePage();
        if (page == null) {
            throw error(Messages.Error_No_active_workbench_page, null);
        }

        // Like RDi, the view is opened before the parser creates the markers.
        // A view, that is created while markers exist, asks whether to change
        // the message style ('Show messages as annotations').
        try {
            QSYSErrorListViewPart view = (QSYSErrorListViewPart)page.showView(QSYSErrorListViewPart.ID);
            view.configure(anEventsFile, aTitle);
        } catch (PartInitException e) {
            throw error(Messages.bind(Messages.Error_Could_not_show_errors_A, ExceptionUtils.getLocalizedMessage(e)), e);
        }

        // The project name is set in the same workspace operation, so that the
        // view is notified once.
        final EventsFileParser parser = new EventsFileParser(anEventsFile, null, aHost, null);
        final Exception[] failure = new Exception[1];
        IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
            public void run(IProgressMonitor aMonitor) throws CoreException {
                try {
                    parser.run(aMonitor);
                } catch (InvocationTargetException e) {
                    failure[0] = e;
                    return;
                } catch (InterruptedException e) {
                    failure[0] = e;
                    return;
                }
                if (aProjectName != null) {
                    setProjectName(anEventsFile, aProjectName, aProjectLocations);
                }
            }
        };

        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        try {
            workspace.run(runnable, workspace.getRoot(), IWorkspace.AVOID_UPDATE, new NullProgressMonitor());
        } catch (CoreException e) {
            throw error(Messages.bind(Messages.Error_Could_not_show_errors_A, ExceptionUtils.getLocalizedMessage(e)), e);
        }

        if (failure[0] instanceof InterruptedException) {
            return;
        }
        if (failure[0] != null) {
            Throwable cause = ExceptionUtils.getCause(failure[0]);
            throw error(Messages.bind(Messages.Error_Could_not_show_errors_A, ExceptionUtils.getLocalizedMessage(cause)), cause);
        }

        SystemMessage parserError = parser.getException();
        if (parserError != null) {
            throw error(Messages.bind(Messages.Error_Could_not_show_errors_A, parserError.getLevelOneText()), null);
        }
    }

    /**
     * Sets the project name of the errors of the files of a local project.
     */
    private static void setProjectName(IFile anEventsFile, String aProjectName, Set<String> aProjectLocations) throws CoreException {

        for (IMarker marker : anEventsFile.findMarkers(IQSYSMarker.AS400_PROBLEM, true, IResource.DEPTH_ZERO)) {
            if (aProjectLocations.contains(marker.getAttribute(IQSYSMarker.LOCATION, ""))) { //$NON-NLS-1$
                marker.setAttribute(IQSYSMarker.PROJECT_NAME, aProjectName);
            }
        }
    }

    private static IProject getProject(IProgressMonitor aMonitor) throws CoreException {

        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IProject project = workspace.getRoot().getProject(PROJECT_NAME);

        if (!project.exists()) {
            // Usually RSE has already created the project.
            IProjectDescription description = workspace.newProjectDescription(PROJECT_NAME);
            project.create(description, aMonitor);
        }

        if (!project.isOpen()) {
            project.open(aMonitor);
        }

        return project;
    }

    private static void createFolders(IContainer aContainer, IProgressMonitor aMonitor) throws CoreException {

        if (!(aContainer instanceof IFolder) || aContainer.exists()) {
            return;
        }

        createFolders(aContainer.getParent(), aMonitor);
        ((IFolder)aContainer).create(IResource.FORCE, true, aMonitor);
    }

    private static CoreException error(String aMessage, Throwable aCause) {
        return new CoreException(new Status(IStatus.ERROR, IfsProjectsPlugin.PLUGIN_ID, aMessage, aCause));
    }

    private static String toResourceName(String aName) {
        return aName.replaceAll(INVALID_NAME_CHARACTERS, REPLACEMENT);
    }

    private static byte[] toBytes(String aText) {
        try {
            return aText.getBytes(CHARSET_UTF8);
        } catch (UnsupportedEncodingException e) {
            // UTF-8 is always supported
            throw new IllegalStateException(e);
        }
    }
}
