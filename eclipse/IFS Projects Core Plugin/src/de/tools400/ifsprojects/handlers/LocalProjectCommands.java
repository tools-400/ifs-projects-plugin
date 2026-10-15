/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.handlers;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.ide.IDE;

import com.ibm.as400.access.AS400;

import de.tools400.ifsprojects.IfsProjectsPlugin;
import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.build.BuildMode;
import de.tools400.ifsprojects.local.Deployment;
import de.tools400.ifsprojects.local.LocalProject;
import de.tools400.ifsprojects.local.SourceMapping;
import de.tools400.ifsprojects.preferences.Preferences;
import de.tools400.ifsprojects.project.EnvironmentVariables;
import de.tools400.ifsprojects.project.IProjConfig;
import de.tools400.ifsprojects.project.IProjParseException;
import de.tools400.ifsprojects.remote.RemoteJobs;
import de.tools400.ifsprojects.rse.RseConnections;
import de.tools400.ifsprojects.rse.RseSelections;
import de.tools400.ifsprojects.terminal.ShellCommands;
import de.tools400.ifsprojects.terminal.TerminalRequest;

/**
 * Common code of the handlers of local IFS projects. Errors are reported to
 * the user. The methods must be called on the UI thread.
 */
final class LocalProjectCommands {

    /** Number of files listed in the question about files changed in the IFS. */
    private static final int MAX_LISTED_FILES = 20;

    private static final String LINE_SEPARATOR = "\n"; //$NON-NLS-1$
    private static final String INDENTATION = "  "; //$NON-NLS-1$

    private LocalProjectCommands() {
        // Utility class
    }

    /**
     * Returns the selected resource of the workspace.
     *
     * @param anEvent - execution event
     * @return resource or <code>null</code>, if not exactly one resource is
     *         selected
     */
    static IResource getSelectedResource(ExecutionEvent anEvent) {

        Object element = RseSelections.getSingleElement(HandlerUtil.getCurrentSelection(anEvent));
        if (element instanceof IResource) {
            return (IResource)element;
        }

        if (element instanceof IAdaptable) {
            Object resource = ((IAdaptable)element).getAdapter(IResource.class);
            if (resource instanceof IResource) {
                return (IResource)resource;
            }
        }

        return null;
    }

    /**
     * Returns the selected project of the workspace.
     *
     * @param anEvent - execution event
     * @return project or <code>null</code>, if not exactly one project is
     *         selected
     */
    static IProject getSelectedProject(ExecutionEvent anEvent) {

        IResource resource = getSelectedResource(anEvent);
        if (resource instanceof IProject) {
            return (IProject)resource;
        }

        return null;
    }

    /**
     * Returns the local IFS project of a project.
     *
     * @param aShell - parent shell
     * @param aProject - project or <code>null</code>
     * @return local IFS project or <code>null</code>, if the project is not
     *         configured as IFS project
     */
    static LocalProject getLocalProject(Shell aShell, IProject aProject) {

        if (aProject == null) {
            return null;
        }

        LocalProject localProject = LocalProject.get(aProject);
        if (localProject == null) {
            MessageDialog.openError(aShell, Messages.Title_IFS_Projects, Messages.bind(Messages.Error_A_is_not_configured_as_IFS_project,
                aProject.getName()));
        }

        return localProject;
    }

    /**
     * Returns the RSE connection of a local IFS project.
     *
     * @param aShell - parent shell
     * @param aLocalProject - local IFS project
     * @return connection or <code>null</code>, if it does not exist
     */
    static IHost getHost(Shell aShell, LocalProject aLocalProject) {

        IHost host = aLocalProject.getHost();
        if (host == null) {
            MessageDialog.openError(aShell, Messages.Title_IFS_Projects, Messages.bind(Messages.Error_Connection_A_of_B_not_found,
                aLocalProject.getConnectionName(), aLocalProject.getName()));
        }

        return host;
    }

    /**
     * Asks the user to save the editors of the files of a project, that have
     * unsaved changes.
     *
     * @param aProject - project
     * @return <code>false</code>, if the user canceled
     */
    static boolean saveEditors(IProject aProject) {
        return IDE.saveAllEditors(new IResource[] { aProject }, true);
    }

    /**
     * Uploads the changes of a local IFS project and builds the project or
     * compiles a file in the deploy directory, like the build and compile of
     * IFS projects. The errors are shown for the local files.
     * <p>
     * The build runs the build command of the <code>iproj.json</code> file or
     * the default build command, the compile the compile command or the
     * default compile command. The environment variables of the project and
     * the default environment variables are exported before.
     *
     * @param aShell - parent shell
     * @param aProject - project
     * @param aFile - file, that is compiled, or <code>null</code> to build the
     *        project
     */
    static void build(Shell aShell, IProject aProject, IFile aFile) {

        LocalProject localProject = getLocalProject(aShell, aProject);
        if (localProject == null) {
            return;
        }

        IHost host = getHost(aShell, localProject);
        if (host == null || !saveEditors(aProject)) {
            return;
        }

        boolean isRse = Preferences.getInstance().getBuildMode() == BuildMode.RSE;

        TerminalRequest.Builder builder = null;
        if (!isRse) {
            builder = RseConnections.createRequestBuilder(aShell, host);
            if (builder == null) {
                return;
            }
        }

        AS400 system = RseBuildLauncher.getConnectedSystem(aShell, host);
        if (system == null) {
            return;
        }

        IProjConfig config = readConfig(aShell, localProject);
        if (config == null) {
            return;
        }

        Map<String, String> variables = EnvironmentVariables.merge(Preferences.getInstance().getDefaultEnvironmentVariables(),
            localProject.getEnvironmentVariables());

        String command;
        String jobName;
        if (aFile == null) {
            command = config.getBuildCommand();
            if (command == null) {
                command = Preferences.getInstance().getDefaultBuildCommand();
            }
            jobName = Messages.bind(Messages.Task_Building_A, aProject.getName());
        } else {
            String compileCommand = config.getCompileCommand();
            if (compileCommand == null) {
                compileCommand = Preferences.getInstance().getDefaultCompileCommand();
            }
            String userId = isRse ? system.getUserId() : builder.build().getUserId();
            command = ShellCommands.replacePlaceholders(compileCommand, getPlaceholders(aFile, host, userId));
            jobName = Messages.bind(Messages.Task_Compiling_A, aFile.getName());
        }

        Deployment.Result upload = upload(aShell, system, localProject, false);
        if (upload == null) {
            return;
        }

        if (aFile != null && !upload.isDeployed(aFile.getProjectRelativePath().toString())) {
            MessageDialog.openError(aShell, Messages.Title_IFS_Projects, Messages.bind(Messages.Error_File_A_is_not_uploaded,
                aFile.getProjectRelativePath().toString()));
            return;
        }

        String directory = upload.getDirectory();
        SourceMapping sourceMapping = new SourceMapping(directory, aProject.getName());

        if (isRse) {
            RseBuildLauncher.launch(jobName, host, system, directory, config, variables, command, sourceMapping);
            return;
        }

        builder.directory(directory);
        builder.reuseKey(ProjectCommands.getReuseKey(host, directory));

        TerminalBuildLauncher.launch(aShell, jobName, host, directory, config, variables, command, builder, sourceMapping);
    }

    /**
     * Reads the <code>iproj.json</code> file of a local IFS project.
     *
     * @param aShell - parent shell
     * @param aLocalProject - local IFS project
     * @return configuration (empty, if the file does not exist) or
     *         <code>null</code>, if the file could not be read
     */
    static IProjConfig readConfig(Shell aShell, LocalProject aLocalProject) {

        try {
            return aLocalProject.readConfig();
        } catch (IProjParseException e) {
            MessageDialog.openError(aShell, Messages.Title_IFS_Projects, Messages.bind(Messages.Error_Invalid_iproj_json_A, e.getLocalizedMessage()));
        } catch (Exception e) {
            String path = aLocalProject.getProject().getFile(IProjConfig.FILE_NAME).getFullPath().toString();
            IfsProjectsPlugin.logError("Could not read iproj.json: " + path, e); //$NON-NLS-1$
            MessageDialog.openError(aShell, Messages.Title_IFS_Projects, Messages.bind(Messages.Error_Could_not_read_A_B, path,
                e.getLocalizedMessage()));
        }

        return null;
    }

    /**
     * Uploads the files of a local IFS project to its deploy directory, see
     * {@link Deployment#deploy}. Shows a progress dialog. Before files changed
     * in the IFS are replaced or deleted, the user is asked.
     *
     * @param aShell - parent shell
     * @param aRseSystem - connected system object of RSE, which is copied
     * @param aLocalProject - local IFS project
     * @param anIsAll - <code>true</code>, if all files are uploaded, also
     *        unchanged files
     * @return result or <code>null</code>, if the upload failed or has been
     *         canceled
     */
    static Deployment.Result upload(final Shell aShell, final AS400 aRseSystem, final LocalProject aLocalProject, final boolean anIsAll) {

        final Deployment.ConflictHandler conflictHandler = new Deployment.ConflictHandler() {
            public boolean isOverwrite(final List<String> aPaths) {
                final boolean[] isOverwrite = new boolean[1];
                aShell.getDisplay().syncExec(new Runnable() {
                    public void run() {
                        // The progress dialog is the active shell.
                        Shell shell = aShell.getDisplay().getActiveShell();
                        isOverwrite[0] = MessageDialog.openQuestion(shell == null ? aShell : shell, Messages.Title_IFS_Projects, Messages.bind(
                            Messages.Question_Files_changed_in_IFS_A, formatPaths(aPaths)));
                    }
                });
                return isOverwrite[0];
            }
        };

        final Deployment.Result[] result = new Deployment.Result[1];
        try {
            new ProgressMonitorDialog(aShell).run(true, true, new IRunnableWithProgress() {
                public void run(IProgressMonitor aMonitor) throws InvocationTargetException, InterruptedException {
                    AS400 system = RemoteJobs.copySystem(aRseSystem);
                    try {
                        String directory = aLocalProject.getDeployDirectory(RemoteJobs.getHomeDirectory(system));
                        result[0] = Deployment.deploy(system, aLocalProject.getProject(), aLocalProject.getConnectionName(), directory, anIsAll,
                            conflictHandler, aMonitor);
                    } catch (InterruptedException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new InvocationTargetException(e);
                    } finally {
                        system.disconnectAllServices();
                    }
                }
            });
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            IfsProjectsPlugin.logError("Could not upload project: " + aLocalProject.getName(), cause); //$NON-NLS-1$
            MessageDialog.openError(aShell, Messages.Title_IFS_Projects, Messages.bind(Messages.Error_Could_not_upload_A_B, aLocalProject.getName(),
                cause.getLocalizedMessage()));
            return null;
        } catch (InterruptedException e) {
            return null;
        }

        return result[0];
    }

    /**
     * Returns the values of the placeholders of the compile command, like
     * {@link CompileIfsFileHandler}.
     */
    private static Map<String, String> getPlaceholders(IFile aFile, IHost aHost, String aUserId) {

        String fileName = CompileIfsFileHandler.toShellFileName(aFile.getName());

        Map<String, String> values = new HashMap<String, String>();
        values.put(CompileIfsFileHandler.PLACEHOLDER_FILENAME, fileName);
        values.put(CompileIfsFileHandler.PLACEHOLDER_FILES, fileName);
        values.put(CompileIfsFileHandler.PLACEHOLDER_HOST, aHost.getHostName());
        values.put(CompileIfsFileHandler.PLACEHOLDER_USRPRF, aUserId.toUpperCase(Locale.ROOT));

        return values;
    }

    /**
     * Returns the paths, one per line, at most {@link #MAX_LISTED_FILES}.
     */
    private static String formatPaths(List<String> aPaths) {

        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < aPaths.size() && i < MAX_LISTED_FILES; i++) {
            buffer.append(INDENTATION).append(aPaths.get(i)).append(LINE_SEPARATOR);
        }

        if (aPaths.size() > MAX_LISTED_FILES) {
            buffer.append(INDENTATION).append(Messages.bind(Messages.Label_And_A_more_files, Integer.toString(aPaths.size() - MAX_LISTED_FILES)))
                .append(LINE_SEPARATOR);
        }

        return buffer.toString();
    }
}
