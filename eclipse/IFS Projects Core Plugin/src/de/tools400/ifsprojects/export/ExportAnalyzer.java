/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.export;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;

import com.ibm.as400.access.AS400;

import de.tools400.ifsprojects.IfsProjectsPlugin;
import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.project.IProjValues;
import de.tools400.ifsprojects.utils.ExceptionUtils;

/**
 * Reads a project and a library and computes the plan of an export, see
 * {@link ExportPlan}. The files of the project are read once, when the first
 * plan is computed. The library is read again, if another library is
 * specified.
 * <p>
 * The methods access the remote system and must not be called on the UI
 * thread.
 */
public final class ExportAnalyzer {

    private final ExportSource source;

    private String projectDirectory;
    private List<String> files;
    private Map<String, String> contents;
    private Map<String, String> readErrors;

    private String libraryName;
    private LibraryInfo libraryInfo;

    /**
     * Creates the analyzer of a project.
     *
     * @param aSource - project
     */
    public ExportAnalyzer(ExportSource aSource) {
        this.source = aSource;
    }

    /**
     * Computes the plan of an export.
     *
     * @param aSystem - system object
     * @param aSettings - settings of the export
     * @param aMonitor - progress monitor
     * @return plan
     * @throws InterruptedException if the user canceled
     * @throws Exception if the project or the library could not be read
     */
    public ExportPlan analyze(AS400 aSystem, ExportSettings aSettings, IProgressMonitor aMonitor) throws Exception {

        readProject(aSystem, aMonitor);

        if (!aSettings.getLibrary().equals(libraryName)) {
            aMonitor.subTask(Messages.bind(Messages.Task_Reading_library_A, aSettings.getLibrary()));
            libraryInfo = LibraryInfo.read(aSystem, aSettings.getLibrary());
            libraryName = aSettings.getLibrary();
        }

        List<String> includeDirectories = IProjValues.getIncludeDirectories(projectDirectory, source.getConfig(), source.getVariables());
        IncludeResolver resolver = new IncludeResolver(projectDirectory, includeDirectories, files);

        return new ExportPlan(aSettings, files, contents, readErrors, resolver, libraryInfo);
    }

    /**
     * Lists the files of the project and reads the exported files, see
     * {@link ExportMapping#isExported(String)}, once per analyzer. Files, that
     * cannot be read, are logged and reported by the plan.
     */
    private void readProject(AS400 aSystem, IProgressMonitor aMonitor) throws Exception {

        if (files != null) {
            return;
        }

        aMonitor.beginTask(Messages.bind(Messages.Task_Reading_files_of_A, source.getName()), IProgressMonitor.UNKNOWN);

        String directory = source.getProjectDirectory(aSystem);
        List<String> projectFiles = source.listFiles(aSystem);
        Map<String, String> fileContents = new HashMap<String, String>();
        Map<String, String> errors = new HashMap<String, String>();

        for (String path : projectFiles) {
            if (aMonitor.isCanceled()) {
                throw new InterruptedException();
            }
            if (!ExportMapping.isExported(path)) {
                continue;
            }
            aMonitor.subTask(path);
            try {
                fileContents.put(path, source.readFile(aSystem, path));
            } catch (Exception e) {
                IfsProjectsPlugin.logError("Could not read the file for the export: " + path, e); //$NON-NLS-1$
                errors.put(path, ExceptionUtils.getLocalizedMessage(e));
            }
        }

        projectDirectory = directory;
        contents = fileContents;
        readErrors = errors;
        files = projectFiles;
    }
}
