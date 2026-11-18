/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import de.tools400.ifsprojects.project.GitIgnoreEntries;
import de.tools400.ifsprojects.project.GitIgnoreEntries.Group;

public class GitIgnoreEntriesTest {

    private static final String MIGRATION_TOBI = "# Output of TOBi (makei build)\n.logs\n.evfevent\n.deps\n.Rules.mk.build\n\n"
        + "# Environment variables of Code for IBM i, e.g. the library of the build\n.env\n\n"
        + "# Log directory, that IBM i Project Explorer ignores, too\n.itest\n\n"
        + "# Project file of RDi (Import as Local IFS Project)\n.project\n";

    private static final List<Group> LOCAL = GitIgnoreEntries.getLocalProjectGroups();

    @Test
    public void testGetGroups() {

        assertEquals(Arrays.asList(Group.TOBI_OUTPUT, Group.ENVIRONMENT, Group.TEST_LOGS, Group.RDI_PROJECT_FILE),
            GitIgnoreEntries.getMigrationGroups(true));
        assertEquals(Arrays.asList(Group.ENVIRONMENT, Group.TEST_LOGS, Group.RDI_PROJECT_FILE), GitIgnoreEntries.getMigrationGroups(false));
        assertEquals(Arrays.asList(Group.RDI_PROJECT_FILE), LOCAL);
    }

    @Test
    public void testAddEntries_newFile() {

        assertEquals(MIGRATION_TOBI, GitIgnoreEntries.addEntries(null, GitIgnoreEntries.getMigrationGroups(true)));
        assertEquals("# Project file of RDi (Import as Local IFS Project)\n.project\n", GitIgnoreEntries.addEntries(null, LOCAL));
        assertEquals("# Project file of RDi (Import as Local IFS Project)\n.project\n", GitIgnoreEntries.addEntries(" \n", LOCAL));
    }

    @Test
    public void testAddEntries_missingEntries() {

        assertEquals(".logs\n.project\n", GitIgnoreEntries.addEntries(".logs\n", LOCAL));
        assertEquals(".logs\n.project\n", GitIgnoreEntries.addEntries(".logs", LOCAL));
        assertEquals(".logs/\r\n.project\r\n", GitIgnoreEntries.addEntries(".logs/\r\n", LOCAL));
        assertEquals("# x\r\n.logs/\r\n.project\r\n", GitIgnoreEntries.addEntries("# x\r\n.logs/", LOCAL));
    }

    @Test
    public void testAddEntries_alreadyIgnored() {

        assertNull(GitIgnoreEntries.addEntries(".project\n", LOCAL));
        assertNull(GitIgnoreEntries.addEntries("/.project\n", LOCAL));
        assertNull(GitIgnoreEntries.addEntries(".*\n", LOCAL));
        // Directories are recognized with and without trailing slash.
        assertNull(GitIgnoreEntries.addEntries(".logs/\n.evfevent/\n.deps/\n.Rules.mk.build\n.env\n.itest/\n.project\n",
            GitIgnoreEntries.getMigrationGroups(true)));
    }

    @Test
    public void testAddEntries_notIgnoredAnyMore() {

        // The last matching rule wins: the entry is added again at the end.
        assertEquals(".project\n!.project\n.project\n", GitIgnoreEntries.addEntries(".project\n!.project\n", LOCAL));
        // A comment is no rule.
        assertEquals("# .project\n.project\n", GitIgnoreEntries.addEntries("# .project\n", LOCAL));
        // A rule for a directory does not ignore a file.
        assertEquals(".project/\n.project\n", GitIgnoreEntries.addEntries(".project/\n", LOCAL));
    }

    @Test
    public void testMerge() {

        // The entries of Project Explorer are kept, the missing ones are added
        // without comments.
        assertEquals(".logs\n.evfevent\n.itest\n.env\n.deps\n.Rules.mk.build\n.project\n",
            GitIgnoreEntries.merge(".logs\n.evfevent\n.itest\n.env", MIGRATION_TOBI));
        assertNull(GitIgnoreEntries.merge(MIGRATION_TOBI, MIGRATION_TOBI));
        assertEquals("build/\r\n.env\r\n.itest\r\n.project\r\n",
            GitIgnoreEntries.merge("build/\r\n", GitIgnoreEntries.addEntries(null, GitIgnoreEntries.getMigrationGroups(false))));
    }

    @Test
    public void testIsInGitWorkingCopy() throws Exception {

        File root = Files.createTempDirectory("gitignore").toFile();
        try {
            File project = new File(root, "repo/sub/project");
            assertFalse(GitIgnoreEntries.isInGitWorkingCopy(project));

            File gitDirectory = new File(root, "repo/.git");
            assertTrue(gitDirectory.mkdirs());
            assertTrue(GitIgnoreEntries.isInGitWorkingCopy(project));
            assertTrue(GitIgnoreEntries.isInGitWorkingCopy(new File(root, "repo")));
            assertFalse(GitIgnoreEntries.isInGitWorkingCopy(root));
            assertTrue(gitDirectory.delete());

            // Linked working tree or submodule: .git is a file.
            File gitFile = new File(root, "repo/sub/.git");
            assertTrue(gitFile.getParentFile().mkdirs());
            Files.write(gitFile.toPath(), "gitdir: ../.git/modules/sub\n".getBytes("UTF-8"));
            assertTrue(GitIgnoreEntries.isInGitWorkingCopy(project));
            assertTrue(gitFile.delete());
        } finally {
            deleteTree(root);
        }
    }

    private static void deleteTree(File aFile) {

        File[] children = aFile.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteTree(child);
            }
        }
        aFile.delete();
    }
}
