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

import de.tools400.ifsprojects.rse.LocalFilterStrings;

public class LocalFilterStringsTest {

    @Test
    public void testGetDirectory_windows() {
        assertEquals("C:\\github.com\\tools400\\serviceprograms\\IFS2",
            LocalFilterStrings.getDirectory("C:\\github.com\\tools400\\serviceprograms\\IFS2\\*"));
        assertEquals("C:\\git\\my project", LocalFilterStrings.getDirectory("C:\\git\\my project\\*.rpgle"));
        assertEquals("C:\\git", LocalFilterStrings.getDirectory("C:\\git\\* /nf"));
        assertEquals("C:\\git", LocalFilterStrings.getDirectory(" C:\\git\\* /NS /nf "));
        assertEquals("C:\\", LocalFilterStrings.getDirectory("C:\\*"));
        assertEquals("\\\\server\\share\\git", LocalFilterStrings.getDirectory("\\\\server\\share\\git\\*"));
    }

    @Test
    public void testGetDirectory_unix() {
        assertEquals("/home/jsmith/git", LocalFilterStrings.getDirectory("/home/jsmith/git/*"));
        assertEquals("/", LocalFilterStrings.getDirectory("/*"));
    }

    @Test
    public void testGetDirectory_noDirectory() {
        assertNull(LocalFilterStrings.getDirectory("*"));
        assertNull(LocalFilterStrings.getDirectory("* /nf"));
        assertNull(LocalFilterStrings.getDirectory(""));
        assertNull(LocalFilterStrings.getDirectory((String)null));
    }

    @Test
    public void testGetDirectory_filterStrings() {
        assertEquals("C:\\git", LocalFilterStrings.getDirectory(new String[] { "C:\\git\\*" }));
        assertNull(LocalFilterStrings.getDirectory(new String[] { "C:\\git\\*", "D:\\git\\*" }));
        assertNull(LocalFilterStrings.getDirectory(new String[0]));
        assertNull(LocalFilterStrings.getDirectory((String[])null));
    }

}
