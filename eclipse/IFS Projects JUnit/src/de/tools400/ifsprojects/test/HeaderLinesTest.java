/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.test;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import de.tools400.ifsprojects.sources.headers.HeaderLines;

public class HeaderLinesTest {

    @Test
    public void testRead() {

        assertEquals(Arrays.asList("**FREE", "// >>CRTCMD<< CRTBNDRPG PGM(&LI/&OB);", "", "  dsply 'Hello';"),
            HeaderLines.read("**FREE  \r\n// >>CRTCMD<< CRTBNDRPG PGM(&LI/&OB);\t\n\n  dsply 'Hello';"));
        assertEquals(Arrays.asList(""), HeaderLines.read(""));
    }

    @Test
    public void testRead_onlyTheFirstLines() {

        StringBuilder content = new StringBuilder();
        for (int i = 0; i < HeaderLines.MAX_LINES + 20; i++) {
            content.append("// line ").append(i).append('\n');
        }

        List<String> lines = HeaderLines.read(content.toString());

        assertEquals(HeaderLines.MAX_LINES, lines.size());
        assertEquals("// line " + (HeaderLines.MAX_LINES - 1), lines.get(lines.size() - 1));
    }

    @Test
    public void testTrimRight() {

        assertEquals("  x", HeaderLines.trimRight("  x \t\r"));
        assertEquals("", HeaderLines.trimRight(" \t"));
        assertEquals("", HeaderLines.trimRight(""));
    }
}
