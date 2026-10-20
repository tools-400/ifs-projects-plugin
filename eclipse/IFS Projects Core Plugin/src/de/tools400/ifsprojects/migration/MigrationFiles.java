/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.migration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Files, that a migration writes into the project directory: the stream files
 * of the migrated members and, for TOBi projects, the build files of
 * {@link TobiBuildFiles}.
 */
public final class MigrationFiles {

    private static final char SEPARATOR = '/';

    private MigrationFiles() {
        // Utility class
    }

    /**
     * Returns the files of a migration.
     *
     * @param aPlan - plan of the migration
     * @param aSourceFiles - migrated source files
     * @param anIsTobi - <code>true</code>, if the build files of TOBi are
     *        written
     * @return contents by the paths relative to the project directory, in the
     *         order, in which they are written: the members, then the build
     *         files of the directories, then the build file of the project
     */
    public static Map<String, String> getFiles(MigrationPlan aPlan, List<SourceFile> aSourceFiles, boolean anIsTobi) {

        Map<String, String> files = new LinkedHashMap<String, String>();
        for (MigrationPlan.Item item : aPlan.getMigratedItems()) {
            files.put(item.getPath(), item.getContent());
        }

        if (!anIsTobi) {
            return files;
        }

        List<String> directories = new ArrayList<String>();
        for (SourceFile sourceFile : aSourceFiles) {
            List<MigrationPlan.Item> items = getItems(aPlan, sourceFile);
            if (items.isEmpty()) {
                continue;
            }
            String directory = MigrationMapping.getDirectory(sourceFile.getName());
            directories.add(directory);
            files.put(directory + SEPARATOR + TobiBuildFiles.IBMI_JSON_FILE, TobiBuildFiles.getIbmiJson(sourceFile, aPlan.getSettings()
                .getHexCcsid()));
            files.put(directory + SEPARATOR + TobiBuildFiles.RULES_FILE, TobiBuildFiles.getDirectoryRules(sourceFile, items));
        }

        if (!directories.isEmpty()) {
            files.put(TobiBuildFiles.RULES_FILE, TobiBuildFiles.getProjectRules(directories));
        }

        return files;
    }

    private static List<MigrationPlan.Item> getItems(MigrationPlan aPlan, SourceFile aSourceFile) {

        List<MigrationPlan.Item> items = new ArrayList<MigrationPlan.Item>();
        for (MigrationPlan.Item item : aPlan.getMigratedItems()) {
            if (item.getMember().getSourceFile().equals(aSourceFile.getName())) {
                items.add(item);
            }
        }

        return items;
    }
}
