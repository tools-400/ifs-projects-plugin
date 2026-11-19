/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.decorators;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.rse.core.RSECorePlugin;
import org.eclipse.rse.core.events.ISystemModelChangeEvent;
import org.eclipse.rse.core.events.ISystemModelChangeEvents;
import org.eclipse.rse.core.events.ISystemModelChangeListener;
import org.eclipse.rse.core.filters.ISystemFilterReference;
import org.eclipse.rse.core.subsystems.ISubSystem;
import org.eclipse.rse.subsystems.files.core.subsystems.IRemoteFile;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import com.ibm.etools.iseries.subsystems.ifs.files.IFSFileSubSystemConfiguration;

import de.tools400.ifsprojects.IfsProjectsPlugin;
import de.tools400.ifsprojects.local.LocalProject;
import de.tools400.ifsprojects.rse.IfsFilterStrings;
import de.tools400.ifsprojects.rse.LocalFilterStrings;
import de.tools400.ifsprojects.rse.RseSelections;
import de.tools400.ifsprojects.subsystem.IfsProject;
import de.tools400.ifsprojects.subsystem.IfsProjects;

/**
 * Marks IFS projects with an overlay at the top right of the icon and a suffix:
 * <ul>
 * <li>local IFS projects in the <i>Project Explorer</i> and their directory in
 * the subsystem 'Local Files' of RSE (directory or directory filter). The
 * suffix is the RSE connection of the project.</li>
 * <li>IFS projects of an IBM i connection at their directory in IBM's IFS
 * subsystem (directory or directory filter). The suffix are the names of the
 * projects, if they differ from the name of the directory.</li>
 * </ul>
 * The filters of the subsystem 'IFS Projects' are not decorated, because they
 * already have the icon of a project.
 * <p>
 * The decorations are updated, when projects of the workspace are added,
 * removed, opened, closed or moved, when the RSE model changes, e.g. when an
 * IFS project is created or deleted, and when a project is configured as local
 * IFS project, see {@link #refresh()}.
 */
public class IfsProjectDecorator extends LabelProvider implements ILightweightLabelDecorator {

    public static final String ID = "de.tools400.ifsprojects.decorators.ifsProject"; //$NON-NLS-1$

    private static final String OVERLAY = "ovr16/ifsprj_ovr.png"; //$NON-NLS-1$

    /** Flags of a project delta, that change the IFS project at a location. */
    private static final int PROJECT_FLAGS = IResourceDelta.OPEN | IResourceDelta.MOVED_FROM | IResourceDelta.MOVED_TO;

    private final ImageDescriptor overlay;
    private final IResourceChangeListener projectChangeListener;
    private ISystemModelChangeListener modelChangeListener;
    private boolean disposed;

    public IfsProjectDecorator() {

        overlay = IfsProjectsPlugin.getImageDescriptor(OVERLAY);

        projectChangeListener = new ProjectChangeListener();
        ResourcesPlugin.getWorkspace().addResourceChangeListener(projectChangeListener, IResourceChangeEvent.POST_CHANGE);
    }

    /**
     * Updates the decorations of all elements, e.g. after a project has been
     * configured as local IFS project. Can be called from any thread.
     */
    public static void refresh() {

        if (!PlatformUI.isWorkbenchRunning()) {
            return;
        }

        Display display = PlatformUI.getWorkbench().getDisplay();
        if (display == null || display.isDisposed()) {
            return;
        }

        display.asyncExec(new Runnable() {
            public void run() {
                if (PlatformUI.isWorkbenchRunning()) {
                    PlatformUI.getWorkbench().getDecoratorManager().update(ID);
                }
            }
        });
    }

    public void decorate(Object anElement, IDecoration aDecoration) {

        try {
            if (anElement instanceof IProject) {
                decorateLocalProject((IProject)anElement, aDecoration);
            } else if (anElement instanceof IRemoteFile || anElement instanceof ISystemFilterReference) {
                decorateRseElement(anElement, aDecoration);
            }
        } catch (RuntimeException e) {
            IfsProjectsPlugin.logError("Could not decorate element: " + anElement, e); //$NON-NLS-1$
        }
    }

    @Override
    public void dispose() {

        synchronized (this) {
            disposed = true;
            if (modelChangeListener != null) {
                try {
                    RSECorePlugin.getTheSystemRegistry().removeSystemModelChangeListener(modelChangeListener);
                } catch (Throwable e) {
                    // Ignore errors on shutdown
                }
                modelChangeListener = null;
            }
        }

        ResourcesPlugin.getWorkspace().removeResourceChangeListener(projectChangeListener);

        super.dispose();
    }

    private void decorateLocalProject(IProject aProject, IDecoration aDecoration) {

        LocalProject project = LocalProject.get(aProject);
        if (project == null) {
            return;
        }

        aDecoration.addOverlay(overlay, IDecoration.TOP_RIGHT);
        aDecoration.addSuffix(IfsProjectDecorations.getLocalSuffix(project.getConnectionName()));
    }

    private void decorateRseElement(Object anElement, IDecoration aDecoration) {

        ISubSystem subSystem = getSubSystem(anElement);
        if (subSystem == null) {
            return;
        }

        String configurationId = subSystem.getConfigurationId();

        if (LocalFilterStrings.SUBSYSTEM_CONFIGURATION_ID.equals(configurationId)) {
            IResource resource = RseSelections.getLocalResource(anElement);
            if (resource instanceof IProject) {
                decorateLocalProject((IProject)resource, aDecoration);
            }
            return;
        }

        if (IFSFileSubSystemConfiguration.CONFIGURATION_ID.equals(configurationId)) {
            // The RSE model is in use, so that the listener can be added
            // without initializing RSE.
            addModelChangeListener();
            decorateIfsDirectory(subSystem, getIfsDirectory(anElement), aDecoration);
        }
    }

    private void decorateIfsDirectory(ISubSystem aSubSystem, String aDirectory, IDecoration aDecoration) {

        if (aDirectory == null) {
            return;
        }

        List<IfsProject> projects = IfsProjects.getProjectsOfDirectory(aSubSystem.getHost(), aDirectory);
        if (projects.isEmpty()) {
            return;
        }

        List<String> projectNames = new ArrayList<>();
        for (IfsProject project : projects) {
            projectNames.add(project.getName());
        }

        aDecoration.addOverlay(overlay, IDecoration.TOP_RIGHT);

        String suffix = IfsProjectDecorations.getRemoteSuffix(aDirectory, projectNames);
        if (suffix != null) {
            aDecoration.addSuffix(suffix);
        }
    }

    /**
     * Returns the subsystem of a directory or filter. For the files of an IFS
     * project it is IBM's IFS subsystem, not the subsystem 'IFS Projects'.
     */
    private ISubSystem getSubSystem(Object anElement) {

        if (anElement instanceof IRemoteFile) {
            return ((IRemoteFile)anElement).getParentRemoteFileSubSystem();
        }

        if (anElement instanceof ISystemFilterReference) {
            return ((ISystemFilterReference)anElement).getSubSystem();
        }

        return null;
    }

    /**
     * Returns the IFS directory of a directory or of a filter with exactly one
     * filter string.
     *
     * @return directory or <code>null</code>, e.g. for a file
     */
    private String getIfsDirectory(Object anElement) {

        if (anElement instanceof IRemoteFile) {
            IRemoteFile remoteFile = (IRemoteFile)anElement;
            return remoteFile.isDirectory() ? remoteFile.getAbsolutePath() : null;
        }

        if (anElement instanceof ISystemFilterReference) {
            return IfsFilterStrings.getDirectory(((ISystemFilterReference)anElement).getReferencedFilter().getFilterStrings());
        }

        return null;
    }

    private synchronized void addModelChangeListener() {

        if (modelChangeListener == null && !disposed) {
            modelChangeListener = new ModelChangeListener();
            RSECorePlugin.getTheSystemRegistry().addSystemModelChangeListener(modelChangeListener);
        }
    }

    /**
     * Updates the decorations, when a project of the workspace is added,
     * removed, opened, closed or moved.
     */
    private static class ProjectChangeListener implements IResourceChangeListener {

        public void resourceChanged(IResourceChangeEvent anEvent) {

            IResourceDelta delta = anEvent.getDelta();
            if (delta == null) {
                return;
            }

            for (IResourceDelta projectDelta : delta.getAffectedChildren()) {
                int kind = projectDelta.getKind();
                if (kind == IResourceDelta.ADDED || kind == IResourceDelta.REMOVED || (projectDelta.getFlags() & PROJECT_FLAGS) != 0) {
                    refresh();
                    return;
                }
            }
        }
    }

    /**
     * Updates the decorations, when a filter, filter pool, subsystem,
     * connection or profile of RSE is added, removed, renamed or changed, e.g.
     * when an IFS project is created or deleted.
     */
    private static class ModelChangeListener implements ISystemModelChangeListener {

        public void systemModelResourceChanged(ISystemModelChangeEvent anEvent) {

            if (anEvent.getEventType() != ISystemModelChangeEvents.SYSTEM_RESOURCE_REORDERED) {
                refresh();
            }
        }
    }
}
