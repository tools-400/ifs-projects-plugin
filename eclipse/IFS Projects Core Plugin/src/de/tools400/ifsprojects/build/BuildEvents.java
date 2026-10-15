/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.build;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.CommandCall;
import com.ibm.as400.access.IFSFile;

import de.tools400.ifsprojects.IfsProjectsPlugin;
import de.tools400.ifsprojects.remote.RemoteJobs;
import de.tools400.ifsprojects.syntaxcheck.SyntaxCheckCommands;

/**
 * Collects the events files of the objects, that have been compiled by a
 * build:
 * <ol>
 * <li>the files of directory <code>.evfevent</code> of the project, which TOBi
 * writes, e.g. <code>HELLO.PGM.evfevent</code>,</li>
 * <li>otherwise the members of file EVFEVENT of the object library, e.g. of a
 * gmake build.</li>
 * </ol>
 * Only events files changed since the start of the build are collected. The
 * compilers replace the member of an object, therefore its modification time
 * is the time of the last compile.
 * <p>
 * The methods access the remote system and must not be called on the UI
 * thread.
 */
public final class BuildEvents {

    private static final String EVENTS_DIRECTORY = "/.evfevent"; //$NON-NLS-1$
    private static final String MEMBER_SUFFIX = ".MBR"; //$NON-NLS-1$
    private static final String TEMPORARY_FILE_SUFFIX = ".evfevent"; //$NON-NLS-1$
    private static final String LINE_SEPARATOR = "\n"; //$NON-NLS-1$

    private BuildEvents() {
        // Utility class
    }

    /**
     * Returns the events of the objects compiled by a build.
     *
     * @param aSystem - system object of the build job
     * @param aCommandCall - command call of the build job
     * @param aProjectDirectory - project directory
     * @param anObjectLibrary - object library or <code>null</code>
     * @param aStartTime - start time of the build (time of the server)
     * @param aTemporaryDirectory - directory of temporary files
     * @return events, one events file after the other, or <code>null</code>,
     *         if no events files have been found
     */
    public static String collect(AS400 aSystem, CommandCall aCommandCall, String aProjectDirectory, String anObjectLibrary, long aStartTime,
        String aTemporaryDirectory) {

        List<String> events = collectStreamFiles(aSystem, aProjectDirectory + EVENTS_DIRECTORY, aStartTime);
        if (events.isEmpty() && anObjectLibrary != null) {
            events = collectMembers(aSystem, aCommandCall, anObjectLibrary, aStartTime, aTemporaryDirectory);
        }

        if (events.isEmpty()) {
            return null;
        }

        StringBuilder buffer = new StringBuilder();
        for (String event : events) {
            buffer.append(event);
            if (!event.endsWith(LINE_SEPARATOR)) {
                buffer.append(LINE_SEPARATOR);
            }
        }

        return buffer.toString();
    }

    private static List<String> collectStreamFiles(AS400 aSystem, String aDirectory, long aStartTime) {

        List<String> events = new ArrayList<String>();
        try {
            for (IFSFile file : getChangedFiles(new IFSFile(aSystem, aDirectory), aStartTime)) {
                events.add(RemoteJobs.readText(file));
            }
        } catch (Exception e) {
            IfsProjectsPlugin.logError("Could not read the events files of directory: " + aDirectory, e); //$NON-NLS-1$
        }

        return events;
    }

    private static List<String> collectMembers(AS400 aSystem, CommandCall aCommandCall, String aLibrary, long aStartTime, String aTemporaryDirectory) {

        List<String> events = new ArrayList<String>();
        String eventsFile = "/QSYS.LIB/" + aLibrary + ".LIB/" + SyntaxCheckCommands.EVENTS_FILE + ".FILE"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        try {
            for (IFSFile member : getChangedFiles(new IFSFile(aSystem, eventsFile), aStartTime)) {
                String name = member.getName();
                if (!name.toUpperCase().endsWith(MEMBER_SUFFIX)) {
                    continue;
                }
                String memberName = name.substring(0, name.length() - MEMBER_SUFFIX.length());
                String streamFile = RemoteJobs.getTemporaryFile(aTemporaryDirectory, TEMPORARY_FILE_SUFFIX);
                if (!aCommandCall.run(SyntaxCheckCommands.getCopyEventsMemberCommand(aLibrary, memberName, streamFile))) {
                    IfsProjectsPlugin.logWarning("Could not copy events file member: " + aLibrary + "/" + memberName); //$NON-NLS-1$ //$NON-NLS-2$
                    continue;
                }
                IFSFile file = new IFSFile(aSystem, streamFile);
                try {
                    events.add(RemoteJobs.readText(file, RemoteJobs.CCSID_UTF8));
                } finally {
                    RemoteJobs.delete(file);
                }
            }
        } catch (Exception e) {
            IfsProjectsPlugin.logError("Could not read the events file of library: " + aLibrary, e); //$NON-NLS-1$
        }

        return events;
    }

    /**
     * Returns the files of a directory, that have been changed since the start
     * time, ordered by their modification time.
     */
    private static List<IFSFile> getChangedFiles(IFSFile aDirectory, long aStartTime) throws Exception {

        List<IFSFile> files = new ArrayList<IFSFile>();
        if (!aDirectory.exists() || !aDirectory.isDirectory()) {
            return files;
        }

        for (IFSFile file : aDirectory.listFiles()) {
            if (file.isFile() && file.lastModified() >= aStartTime) {
                files.add(file);
            }
        }

        IFSFile[] sorted = files.toArray(new IFSFile[files.size()]);
        Arrays.sort(sorted, new Comparator<IFSFile>() {
            public int compare(IFSFile aFile1, IFSFile aFile2) {
                return Long.valueOf(lastModified(aFile1)).compareTo(Long.valueOf(lastModified(aFile2)));
            }
        });

        return Arrays.asList(sorted);
    }

    private static long lastModified(IFSFile aFile) {
        try {
            return aFile.lastModified();
        } catch (Exception e) {
            return 0;
        }
    }
}
