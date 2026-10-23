/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.migration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.IFSFile;

import de.tools400.ifsprojects.IfsProjectsPlugin;
import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.dialogs.JobSummary;
import de.tools400.ifsprojects.dialogs.OverwriteQuery;
import de.tools400.ifsprojects.dialogs.RememberingOverwriteQuery;
import de.tools400.ifsprojects.ibmi.IfsPaths;
import de.tools400.ifsprojects.remote.RemoteJobs;
import de.tools400.ifsprojects.remote.RemoteSession;
import de.tools400.ifsprojects.utils.ExceptionUtils;

/**
 * Writes the files of a migration, see {@link MigrationFiles}, into the
 * project directory as stream files with CCSID 1208 (UTF-8). Missing
 * directories are created. Before an existing file is replaced, the user is
 * asked, see {@link OverwriteQuery}. At the end, a summary is shown.
 */
public class MigrationJob extends Job {

    private final AS400 rseSystem;
    private final String library;
    private final String directory;
    private final Map<String, String> files;
    private final OverwriteQuery overwriteQuery;

    private final List<String> errors;
    private int created;
    private int replaced;
    private int skipped;

    /**
     * Creates the job.
     *
     * @param aRseSystem - connected system object of RSE, which is copied
     * @param aLibrary - library of the source files
     * @param aDirectory - project directory
     * @param aFiles - contents of the files by their paths relative to the
     *        project directory
     * @param anOverwriteQuery - asks the user, whether an existing file is
     *        replaced; the job remembers 'Yes To All' and 'No To All'
     */
    public MigrationJob(AS400 aRseSystem, String aLibrary, String aDirectory, Map<String, String> aFiles, OverwriteQuery anOverwriteQuery) {
        super(Messages.bind(Messages.Task_Migrating_A_to_B, aLibrary, aDirectory));

        this.rseSystem = aRseSystem;
        this.library = aLibrary;
        this.directory = aDirectory;
        this.files = aFiles;
        this.overwriteQuery = new RememberingOverwriteQuery(anOverwriteQuery);
        this.errors = new ArrayList<String>();
    }

    @Override
    protected IStatus run(IProgressMonitor aMonitor) {

        aMonitor.beginTask(getName(), files.size());

        boolean isCanceled = false;
        try (RemoteSession session = new RemoteSession(rseSystem)) {

            for (Map.Entry<String, String> file : files.entrySet()) {

                if (aMonitor.isCanceled()) {
                    isCanceled = true;
                    break;
                }

                aMonitor.subTask(file.getKey());

                if (!write(session.getSystem(), file.getKey(), file.getValue())) {
                    isCanceled = true;
                    break;
                }

                aMonitor.worked(1);
            }

        } catch (Exception e) {
            IfsProjectsPlugin.logError("Could not migrate source files of library: " + library, e); //$NON-NLS-1$
            errors.add(Messages.bind(Messages.Error_Could_not_migrate_A_B, library, ExceptionUtils.getLocalizedMessage(e)));
        } finally {
            aMonitor.done();
            showSummary(isCanceled);
        }

        return isCanceled ? Status.CANCEL_STATUS : Status.OK_STATUS;
    }

    /**
     * Writes a file, if it does not exist or if the user agrees to replace it.
     *
     * @return <code>false</code>, if the user canceled the migration
     */
    private boolean write(AS400 aSystem, String aPath, String aContent) {

        String path = IfsPaths.append(directory, aPath);
        try {
            IFSFile file = new IFSFile(aSystem, path);
            boolean isExisting = file.exists();
            if (isExisting) {
                OverwriteQuery.Answer answer = overwriteQuery.ask(path);
                if (answer == OverwriteQuery.Answer.CANCEL) {
                    return false;
                }
                if (!answer.isReplace()) {
                    skipped++;
                    return true;
                }
                // A replaced file would keep its CCSID, see RemoteJobs.writeText().
                if (file.getCCSID() != RemoteJobs.CCSID_UTF8 && !file.delete()) {
                    errors.add(Messages.bind(Messages.Error_A_B, aPath, Messages.bind(Messages.Error_Could_not_delete_A, path)));
                    return true;
                }
            } else {
                // mkdirs() fails, if someone else creates the directory at the
                // same time.
                IFSFile parent = new IFSFile(aSystem, file.getParent());
                if (!parent.exists() && !parent.mkdirs() && !parent.exists()) {
                    errors.add(Messages.bind(Messages.Error_A_B, aPath, Messages.bind(Messages.Error_Could_not_create_directory_A,
                        file.getParent())));
                    return true;
                }
            }

            RemoteJobs.writeText(file, aContent, RemoteJobs.CCSID_UTF8);
            if (isExisting) {
                replaced++;
            } else {
                created++;
            }

        } catch (Exception e) {
            IfsProjectsPlugin.logError("Could not write file: " + path, e); //$NON-NLS-1$
            errors.add(Messages.bind(Messages.Error_A_B, aPath, ExceptionUtils.getLocalizedMessage(e)));
        }

        return true;
    }

    private void showSummary(boolean anIsCanceled) {

        String message = Messages.bind(Messages.Info_Migration_of_A_to_B_created_C_replaced_D_skipped_E, new Object[] { library, directory,
            Integer.toString(created), Integer.toString(replaced), Integer.toString(skipped) });
        if (anIsCanceled) {
            message += '\n' + Messages.Log_Canceled;
        }

        JobSummary.open(Messages.Title_Migrate_to_IFS_project, message, errors);
    }
}
