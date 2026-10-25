/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import de.tools400.ifsprojects.project.ProjectFiles;

public class ProjectFilesTest {

    @Test
    public void testIsExcluded() {

        assertTrue(ProjectFiles.isExcluded(".git"));
        assertTrue(ProjectFiles.isExcluded(".git/config"));
        assertTrue(ProjectFiles.isExcluded(".project"));
        assertTrue(ProjectFiles.isExcluded(".settings/org.eclipse.core.resources.prefs"));
        assertTrue(ProjectFiles.isExcluded(".logs/joblog.json"));
        assertTrue(ProjectFiles.isExcluded(".evfevent/HELLO.PGM.evfevent"));
        assertTrue(ProjectFiles.isExcluded(".deps/hello.d"));
        assertTrue(ProjectFiles.isExcluded(".deployignore"));
        assertTrue(ProjectFiles.isExcluded(".ifsprojects-deploy"));

        assertFalse(ProjectFiles.isExcluded("iproj.json"));
        assertFalse(ProjectFiles.isExcluded("src/.logs/x"));
        assertFalse(ProjectFiles.isExcluded(".gitignore"));
        assertFalse(ProjectFiles.isExcluded("Rules.mk"));
    }

    @Test
    public void testReadIgnoreRules() throws Exception {

        final Map<String, String> files = new HashMap<>();
        ProjectFiles.ProjectDirectory directory = new ProjectFiles.ProjectDirectory() {
            public String readFile(String aName) {
                return files.get(aName);
            }
        };

        assertFalse(ProjectFiles.readIgnoreRules(directory).isIgnored("build/x", false));

        files.put(".gitignore", "build/");
        assertTrue(ProjectFiles.readIgnoreRules(directory).isIgnored("build", true));

        files.put(".deployignore", "*.bak");
        assertFalse(ProjectFiles.readIgnoreRules(directory).isIgnored("build", true));
        assertTrue(ProjectFiles.readIgnoreRules(directory).isIgnored("src/x.bak", false));
    }
}
