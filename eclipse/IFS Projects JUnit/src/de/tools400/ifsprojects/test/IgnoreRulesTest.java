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

import org.junit.Test;

import de.tools400.ifsprojects.local.IgnoreRules;

public class IgnoreRulesTest {

    @Test
    public void testEmpty() {

        assertFalse(IgnoreRules.empty().isIgnored("src/hello.rpgle", false));
        assertFalse(IgnoreRules.parse(null).isIgnored("src/hello.rpgle", false));
        assertFalse(IgnoreRules.parse("\n# comment\n   \n").isIgnored("# comment", false));
    }

    @Test
    public void testNameAtAnyLevel() {

        IgnoreRules rules = IgnoreRules.parse("*.log\r\nbuild\n");

        assertTrue(rules.isIgnored("make.log", false));
        assertTrue(rules.isIgnored("src/sub/make.log", false));
        assertFalse(rules.isIgnored("make.log.txt", false));
        assertTrue(rules.isIgnored("build", true));
        assertTrue(rules.isIgnored("src/build", false));
        assertFalse(rules.isIgnored("builds", true));
    }

    @Test
    public void testAnchored() {

        IgnoreRules rules = IgnoreRules.parse("/out\ndoc/*.html\n");

        assertTrue(rules.isIgnored("out", true));
        assertFalse(rules.isIgnored("src/out", true));
        assertTrue(rules.isIgnored("doc/index.html", false));
        assertFalse(rules.isIgnored("doc/api/index.html", false));
        assertFalse(rules.isIgnored("src/doc/index.html", false));
    }

    @Test
    public void testDirectoryOnly() {

        IgnoreRules rules = IgnoreRules.parse("tmp/\n");

        assertTrue(rules.isIgnored("tmp", true));
        assertTrue(rules.isIgnored("src/tmp", true));
        assertFalse(rules.isIgnored("tmp", false));
    }

    @Test
    public void testNegation() {

        IgnoreRules rules = IgnoreRules.parse("*.txt\n!keep.txt\n\\!bang.txt\n\\#hash\n");

        assertTrue(rules.isIgnored("a.txt", false));
        assertFalse(rules.isIgnored("keep.txt", false));
        assertFalse(rules.isIgnored("src/keep.txt", false));
        assertTrue(rules.isIgnored("!bang.txt", false));
        assertTrue(rules.isIgnored("#hash", false));
    }

    @Test
    public void testDoubleAsterisk() {

        IgnoreRules rules = IgnoreRules.parse("**/gen\nlogs/**\na/**/z.rpgle\n");

        assertTrue(rules.isIgnored("gen", true));
        assertTrue(rules.isIgnored("src/x/gen", true));
        assertTrue(rules.isIgnored("logs/a", false));
        assertTrue(rules.isIgnored("logs/a/b", false));
        assertFalse(rules.isIgnored("logs", true));
        assertTrue(rules.isIgnored("a/z.rpgle", false));
        assertTrue(rules.isIgnored("a/b/c/z.rpgle", false));
        assertFalse(rules.isIgnored("b/a/z.rpgle", false));
    }

    @Test
    public void testWildcards() {

        IgnoreRules rules = IgnoreRules.parse("file?.bak\n[abc]x.rpgle\n[!0-9]y.rpgle\nwith space\\ \nplus+dot.c\n");

        assertTrue(rules.isIgnored("file1.bak", false));
        assertFalse(rules.isIgnored("file12.bak", false));
        assertFalse(rules.isIgnored("file/.bak", false));
        assertTrue(rules.isIgnored("bx.rpgle", false));
        assertFalse(rules.isIgnored("dx.rpgle", false));
        assertTrue(rules.isIgnored("ay.rpgle", false));
        assertFalse(rules.isIgnored("1y.rpgle", false));
        assertTrue(rules.isIgnored("with space ", false));
        assertTrue(rules.isIgnored("plus+dot.c", false));
        assertFalse(rules.isIgnored("plussdot.c", false));
    }

    @Test
    public void testLastRuleWins() {

        IgnoreRules rules = IgnoreRules.parse("!a.txt\n*.txt\n");

        assertTrue(rules.isIgnored("a.txt", false));
    }
}
