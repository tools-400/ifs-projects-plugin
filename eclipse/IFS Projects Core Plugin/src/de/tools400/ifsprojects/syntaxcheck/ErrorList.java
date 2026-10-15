/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.syntaxcheck;

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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.services.clientserver.messages.SystemMessage;
import org.eclipse.rse.subsystems.files.core.SystemIFileProperties;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.ibm.etools.iseries.rse.ui.resources.IQSYSMarker;
import com.ibm.etools.iseries.rse.ui.view.errorlist.QSYSErrorListViewPart;
import com.ibm.etools.iseries.rse.util.evfparser.EventsFileParser;

import de.tools400.ifsprojects.Messages;

/**
 * Shows the errors of an events file in the 'Error List' view of RDi.
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
     * Returns the title of the table of a stream file in the 'Error List'
     * view, e.g. <code>myibmi:/home/jsmith/src/hello.rpgle</code>.
     *
     * @param aHost - RSE connection
     * @param aSourcePath - absolute path of the checked stream file
     * @return title
     */
    public static String getTitle(IHost aHost, String aSourcePath) {
        return aHost.getAliasName() + TITLE_SEPARATOR + aSourcePath;
    }

    /**
     * Writes the events to the local events file of a stream file.
     * <p>
     * Must not be called on the UI thread.
     *
     * @param aHost - RSE connection
     * @param aSourcePath - absolute path of the checked stream file
     * @param anEvents - content of the events file
     * @param aCcsid - CCSID of the job, in which the events file was written
     * @param aMonitor - progress monitor
     * @return local events file
     * @throws CoreException if the file could not be written
     */
    public static synchronized IFile writeEventsFile(IHost aHost, String aSourcePath, String anEvents, int aCcsid, IProgressMonitor aMonitor)
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
     * view.
     * <p>
     * Must be called on the UI thread.
     *
     * @param anEventsFile - local events file, see {@link #writeEventsFile}
     * @param aHost - RSE connection
     * @param aTitle - title of the table, see {@link #getTitle}
     * @throws SyntaxCheckException if the errors could not be shown
     */
    public static void show(IFile anEventsFile, IHost aHost, String aTitle) throws SyntaxCheckException {
        show(anEventsFile, aHost, aTitle, null, Collections.<String> emptySet());
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
     * @throws SyntaxCheckException if the errors could not be shown
     */
    public static void show(final IFile anEventsFile, IHost aHost, String aTitle, final String aProjectName,
        final Set<String> aProjectLocations) throws SyntaxCheckException {

        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        IWorkbenchPage page = window == null ? null : window.getActivePage();
        if (page == null) {
            throw new SyntaxCheckException(Messages.Error_No_active_workbench_page, null);
        }

        // Like RDi, the view is opened before the parser creates the markers.
        // A view, that is created while markers exist, asks whether to change
        // the message style ('Show messages as annotations').
        try {
            QSYSErrorListViewPart view = (QSYSErrorListViewPart)page.showView(QSYSErrorListViewPart.ID);
            view.configure(anEventsFile, aTitle);
        } catch (PartInitException e) {
            throw new SyntaxCheckException(Messages.bind(Messages.Error_Could_not_show_errors_A, e.getLocalizedMessage()), e);
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
            throw new SyntaxCheckException(Messages.bind(Messages.Error_Could_not_show_errors_A, e.getLocalizedMessage()), e);
        }

        if (failure[0] instanceof InterruptedException) {
            return;
        }
        if (failure[0] != null) {
            Throwable cause = failure[0].getCause() != null ? failure[0].getCause() : failure[0];
            throw new SyntaxCheckException(Messages.bind(Messages.Error_Could_not_show_errors_A, cause.getLocalizedMessage()), cause);
        }

        SystemMessage error = parser.getException();
        if (error != null) {
            throw new SyntaxCheckException(Messages.bind(Messages.Error_Could_not_show_errors_A, error.getLevelOneText()), null);
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
