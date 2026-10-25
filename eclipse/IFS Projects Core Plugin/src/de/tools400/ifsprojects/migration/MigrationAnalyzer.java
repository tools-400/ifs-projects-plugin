/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.migration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;

import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.ibmi.ClCommands;
import de.tools400.ifsprojects.remote.RemoteJobs;
import de.tools400.ifsprojects.remote.RemoteSession;
import de.tools400.ifsprojects.sources.headers.SourceHeaderParsers;

/**
 * Reads the members of source files from the IBM i and computes the
 * {@link MigrationPlan}. The members of source files with CCSID 65535 are read
 * with the CCSID of the settings.
 */
public final class MigrationAnalyzer {

    private MigrationAnalyzer() {
        // Utility class
    }

    /**
     * Reads the members and computes the plan.
     *
     * @param aSession - remote session
     * @param aLibrary - library of the source files
     * @param aSourceFiles - source files to migrate
     * @param aSettings - settings
     * @param aMonitor - progress monitor
     * @return plan
     * @throws InterruptedException if the user canceled the analysis
     * @throws Exception if the catalog could not be read
     */
    public static MigrationPlan analyze(RemoteSession aSession, String aLibrary, List<SourceFile> aSourceFiles, MigrationSettings aSettings,
        IProgressMonitor aMonitor) throws Exception {

        List<SourceMember> members = new ArrayList<>();
        Map<SourceMember, SourceFile> sourceFiles = new HashMap<>();
        List<String> sourceFileNames = new ArrayList<>();
        for (SourceFile sourceFile : aSourceFiles) {
            sourceFileNames.add(sourceFile.getName());
            for (SourceMember member : SourceFileCatalog.getMembers(aSession.getSystem(), sourceFile)) {
                members.add(member);
                sourceFiles.put(member, sourceFile);
            }
        }

        aMonitor.beginTask(Messages.bind(Messages.Task_Reading_source_files_of_A, aLibrary), members.size());

        Map<String, String> contents = new HashMap<>();
        Map<String, String> readErrors = new HashMap<>();
        for (SourceMember member : members) {

            if (aMonitor.isCanceled()) {
                throw new InterruptedException();
            }
            aMonitor.subTask(member.getKey());

            SourceFile sourceFile = sourceFiles.get(member);
            int dataCcsid = sourceFile.isHex() ? aSettings.getHexCcsid() : ClCommands.CCSID_OF_FILE;
            String content = aSession.readMember(aLibrary, sourceFile.getName(), member.getName(), dataCcsid);
            if (content == null) {
                readErrors.put(member.getKey(), RemoteJobs.getMessages(aSession.getCommandCall()));
            } else {
                contents.put(member.getKey(), content);
            }

            aMonitor.worked(1);
        }

        return new MigrationPlan(aLibrary, sourceFileNames, members, contents, readErrors, aSettings, SourceHeaderParsers.getEnabled());
    }
}
