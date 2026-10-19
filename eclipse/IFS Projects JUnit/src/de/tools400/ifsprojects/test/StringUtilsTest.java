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

import org.junit.Test;

import de.tools400.ifsprojects.utils.StringUtils;

public class StringUtilsTest {

    @Test
    public void testTrimToNull() {
        assertEquals("a", StringUtils.trimToNull("a"));
        assertEquals("a b", StringUtils.trimToNull(" a b "));
        assertEquals("a", StringUtils.trimToNull("\ta\r\n"));
        assertNull(StringUtils.trimToNull("   "));
        assertNull(StringUtils.trimToNull(""));
        assertNull(StringUtils.trimToNull(null));
    }

    @Test
    public void testTrimToNull_sameAsTrim() {
        // Like String.trim(): characters up to U+0020 are removed, other white
        // space, e.g. the no-break space, is kept.
        assertEquals(" ", StringUtils.trimToNull("   "));
    }

    @Test
    public void testIsBlank() {
        assertTrue(StringUtils.isBlank(null));
        assertTrue(StringUtils.isBlank(""));
        assertTrue(StringUtils.isBlank(" \t\r\n"));
        assertFalse(StringUtils.isBlank(" a "));
    }

    @Test
    public void testNullToEmpty() {
        assertEquals("", StringUtils.nullToEmpty(null));
        assertEquals("", StringUtils.nullToEmpty(""));
        assertEquals(" a ", StringUtils.nullToEmpty(" a "));
    }

    @Test
    public void testContainsLineBreak() {
        assertTrue(StringUtils.containsLineBreak("a\nb"));
        assertTrue(StringUtils.containsLineBreak("a\r"));
        assertFalse(StringUtils.containsLineBreak("a\tb"));
        assertFalse(StringUtils.containsLineBreak(""));
    }
}
