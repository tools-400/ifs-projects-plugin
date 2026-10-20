/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.export;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.rse.core.model.IHost;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.CommandCall;
import com.ibm.as400.access.IFSFile;

import de.tools400.ifsprojects.IfsProjectsPlugin;
import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.build.CommandsLog;
import de.tools400.ifsprojects.dialogs.JobSummary;
import de.tools400.ifsprojects.dialogs.OverwriteQuery;
import de.tools400.ifsprojects.dialogs.RememberingOverwriteQuery;
import de.tools400.ifsprojects.remote.RemoteJobs;
import de.tools400.ifsprojects.remote.RemoteSession;

/**
 * Exports the files of an {@link ExportPlan} into the members of a library in
 * a remote command server job of its own:
 * <ol>
 * <li>Creates the library, if it does not exist, and the new source
 * files.</li>
 * <li>Writes the content of each file into a temporary stream file with CCSID
 * 1208, from which <code>CPYFRMSTMF</code> copies it into the member. Its
 * content may differ from the file of the project, because the include
 * directives have been converted. If the stream file cannot be written, the
 * export is aborted, before the member is added or changed, because the error
 * would recur for each file.</li>
 * <li>A new member is added before with source type and text, because
 * <code>CPYFRMSTMF</code> creates it without them, and is removed again, if the
 * copy fails. Before an existing member is replaced, the user is asked, see
 * {@link OverwriteQuery}.</li>
 * </ol>
 * The commands and their messages are written to the 'Commands Log' view. At
 * the end, a summary is shown.
 */
public class ExportJob extends Job {

    private static final String TEMPORARY_FILE_SUFFIX = ".export"; //$NON-NLS-1$
    private static final String LINE_SEPARATOR = "\n"; //$NON-NLS-1$

    private final IHost host;
    private final AS400 rseSystem;
    private final String projectName;
    private final String library;
    private final boolean isCreateLibrary;
    private final Map<String, Integer> newSourceFiles;
    private final int ccsid;
    private final List<ExportPlan.Item> items;
    private final OverwriteQuery overwriteQuery;

    private final List<String> errors;
    private int created;
    private int replaced;
    private int skipped;

    /**
     * Creates the job.
     *
     * @param aHost - RSE connection
     * @param aRseSystem - connected system object of RSE, which is copied
     * @param aProjectName - name of the project
     * @param aPlan - plan of the export
     * @param anOverwriteQuery - asks the user, whether an existing member is
     *        replaced; the job remembers 'Yes To All' and 'No To All'
     */
    public ExportJob(IHost aHost, AS400 aRseSystem, String aProjectName, ExportPlan aPlan, OverwriteQuery anOverwriteQuery) {
        super(Messages.bind(Messages.Task_Exporting_A_to_B, aProjectName, aPlan.getSettings().getLibrary()));

        this.host = aHost;
        this.rseSystem = aRseSystem;
        this.projectName = aProjectName;
        this.library = aPlan.getSettings().getLibrary();
        this.isCreateLibrary = aPlan.isCreateLibrary();
        this.newSourceFiles = aPlan.getNewSourceFiles();
        this.ccsid = aPlan.getSettings().getCcsid();
        this.items = aPlan.getExportedItems();
        this.overwriteQuery = new RememberingOverwriteQuery(anOverwriteQuery);
        this.errors = new ArrayList<String>();
    }

    @Override
    protected IStatus run(IProgressMonitor aMonitor) {

        aMonitor.beginTask(getName(), items.size());

        CommandsLog.show(host);
        CommandsLog.log(host, getName());

        boolean isCanceled = false;
        try (RemoteSession session = new RemoteSession(rseSystem)) {

            CommandCall commandCall = session.getCommandCall();
            IFSFile temporaryFile = session.newTemporaryFile(TEMPORARY_FILE_SUFFIX);

            if (isCreateLibrary && !run(commandCall, ExportCommands.getCreateLibraryCommand(library), library)) {
                return Status.OK_STATUS;
            }

            Set<String> failedSourceFiles = createSourceFiles(commandCall);

            for (ExportPlan.Item item : items) {

                if (aMonitor.isCanceled()) {
                    isCanceled = true;
                    break;
                }

                aMonitor.subTask(item.getPath());

                if (!exportItem(commandCall, temporaryFile, item, failedSourceFiles)) {
                    isCanceled = true;
                    break;
                }

                aMonitor.worked(1);
            }

        } catch (Exception e) {
            IfsProjectsPlugin.logError("Could not export project: " + projectName, e); //$NON-NLS-1$
            String message = Messages.bind(Messages.Error_Could_not_export_A_B, projectName, e.getLocalizedMessage());
            CommandsLog.log(host, message);
            errors.add(message);
        } finally {
            aMonitor.done();
            showSummary(isCanceled);
        }

        return isCanceled ? Status.CANCEL_STATUS : Status.OK_STATUS;
    }

    /**
     * Creates the new source files of the plan.
     *
     * @return source files, that could not be created
     */
    private Set<String> createSourceFiles(CommandCall aCommandCall) throws Exception {

        Set<String> failedSourceFiles = new HashSet<String>();
        for (Map.Entry<String, Integer> sourceFile : newSourceFiles.entrySet()) {
            String command = ExportCommands.getCreateSourceFileCommand(library, sourceFile.getKey(), sourceFile.getValue().intValue(), ccsid);
            if (!run(aCommandCall, command, sourceFile.getKey())) {
                failedSourceFiles.add(sourceFile.getKey());
            }
        }

        return failedSourceFiles;
    }

    /**
     * Exports a file of the project: adds a new member or replaces an existing
     * member, if the user agrees.
     *
     * @return <code>false</code>, if the user canceled the export
     */
    private boolean exportItem(CommandCall aCommandCall, IFSFile aTemporaryFile, ExportPlan.Item anItem, Set<String> aFailedSourceFiles)
        throws Exception {

        if (aFailedSourceFiles.contains(anItem.getSourceFile())) {
            errors.add(Messages.bind(Messages.Error_A_B, anItem.getPath(), Messages.bind(Messages.Error_Source_file_A_not_created,
                anItem.getSourceFile())));
            return true;
        }

        if (!anItem.isExistingMember()) {
            create(aCommandCall, aTemporaryFile, anItem);
            return true;
        }

        OverwriteQuery.Answer answer = overwriteQuery.ask(getMemberName(anItem));
        if (answer == OverwriteQuery.Answer.CANCEL) {
            return false;
        }

        if (answer.isReplace()) {
            replace(aCommandCall, aTemporaryFile, anItem);
        } else {
            skipped++;
            CommandsLog.log(host, Messages.bind(Messages.Log_Member_A_skipped, getMemberName(anItem)));
        }

        return true;
    }

    private void create(CommandCall aCommandCall, IFSFile aTemporaryFile, ExportPlan.Item anItem) throws Exception {

        String sourceFile = anItem.getSourceFile();
        String member = anItem.getMember();

        RemoteJobs.writeText(aTemporaryFile, anItem.getContent(), RemoteJobs.CCSID_UTF8);
        if (!run(aCommandCall, ExportCommands.getAddMemberCommand(library, sourceFile, member, anItem.getSourceType(), anItem.getText()),
            anItem.getPath())) {
            return;
        }

        if (!run(aCommandCall, ExportCommands.getCopyCommand(aTemporaryFile.getAbsolutePath(), library, sourceFile, member), anItem.getPath())) {
            run(aCommandCall, ExportCommands.getRemoveMemberCommand(library, sourceFile, member), null);
            return;
        }

        created++;
    }

    private void replace(CommandCall aCommandCall, IFSFile aTemporaryFile, ExportPlan.Item anItem) throws Exception {

        String sourceFile = anItem.getSourceFile();
        String member = anItem.getMember();

        RemoteJobs.writeText(aTemporaryFile, anItem.getContent(), RemoteJobs.CCSID_UTF8);
        if (!run(aCommandCall, ExportCommands.getCopyCommand(aTemporaryFile.getAbsolutePath(), library, sourceFile, member), anItem.getPath())) {
            return;
        }

        if (!run(aCommandCall, ExportCommands.getChangeMemberCommand(library, sourceFile, member, anItem.getSourceType(), anItem.getText()),
            anItem.getPath())) {
            return;
        }

        replaced++;
    }

    /**
     * Runs a command and logs it. If it fails, its messages are logged and
     * added to the errors.
     *
     * @param aCommandCall - command call
     * @param aCommand - command
     * @param anObject - object of the error message, e.g. the path of the file,
     *        or <code>null</code>, if the error is only logged
     * @return <code>true</code>, if the command has been successful
     */
    private boolean run(CommandCall aCommandCall, String aCommand, String anObject) throws Exception {

        CommandsLog.log(host, aCommand);
        if (aCommandCall.run(aCommand)) {
            return true;
        }

        String messages = RemoteJobs.getMessages(aCommandCall);
        CommandsLog.log(host, messages);
        if (anObject != null) {
            int index = messages.indexOf(LINE_SEPARATOR);
            errors.add(Messages.bind(Messages.Error_A_B, anObject, index < 0 ? messages : messages.substring(0, index)));
        }

        return false;
    }

    private String getMemberName(ExportPlan.Item anItem) {
        return library + '/' + anItem.getSourceFile() + '(' + anItem.getMember() + ')';
    }

    private void showSummary(boolean anIsCanceled) {

        StringBuilder message = new StringBuilder();
        message.append(Messages.bind(Messages.Info_Export_of_A_to_B_created_C_replaced_D_skipped_E, new Object[] { projectName, library,
            Integer.toString(created), Integer.toString(replaced), Integer.toString(skipped) }));
        if (anIsCanceled) {
            message.append(LINE_SEPARATOR).append(Messages.Log_Canceled);
        }

        CommandsLog.log(host, message.toString());

        JobSummary.open(Messages.Title_Export_to_library, message.toString(), errors);
    }
}
