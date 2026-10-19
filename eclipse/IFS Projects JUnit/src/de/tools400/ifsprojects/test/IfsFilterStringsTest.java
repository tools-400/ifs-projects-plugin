/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import de.tools400.ifsprojects.rse.IfsFilterStrings;

public class IfsFilterStringsTest {

    @Test
    public void testToFilterString() {
        assertEquals("/home/jsmith/*", IfsFilterStrings.toFilterString("/home/jsmith"));
        assertEquals("/home/jsmith/*", IfsFilterStrings.toFilterString("/home/jsmith/"));
        assertEquals("/home/jsmith/*", IfsFilterStrings.toFilterString(" /home/jsmith// "));
        assertEquals("/*", IfsFilterStrings.toFilterString("/"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToFilterString_relative() {
        IfsFilterStrings.toFilterString("home/jsmith");
    }

    @Test
    public void testGetDirectory() {
        assertEquals("/home/Jsmith", IfsFilterStrings.getDirectory("/home/Jsmith/*"));
        assertEquals("/home/jsmith/my project", IfsFilterStrings.getDirectory("/home/jsmith/my project/*.rpgle"));
        assertEquals("/home/jsmith", IfsFilterStrings.getDirectory("/home/jsmith/* /nf"));
        assertEquals("/home/jsmith", IfsFilterStrings.getDirectory("/home/jsmith/* /NS /nf"));
        assertEquals("/", IfsFilterStrings.getDirectory("/*"));
        assertNull(IfsFilterStrings.getDirectory("*"));
        assertNull(IfsFilterStrings.getDirectory(""));
        assertNull(IfsFilterStrings.getDirectory((String)null));
    }

    @Test
    public void testGetDirectory_filterStrings() {
        assertEquals("/home/jsmith", IfsFilterStrings.getDirectory(new String[] { "/home/jsmith/*" }));
        assertNull(IfsFilterStrings.getDirectory(new String[] { "/home/jsmith/*", "/tmp/*" }));
        assertNull(IfsFilterStrings.getDirectory(new String[0]));
        assertNull(IfsFilterStrings.getDirectory((String[])null));
    }


}
