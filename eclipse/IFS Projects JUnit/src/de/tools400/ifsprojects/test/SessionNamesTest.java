/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import de.tools400.ifsprojects.terminal.tm.SessionNames;

public class SessionNamesTest {

    @Test
    public void testGetFreeInstanceNumber() {
        assertEquals(1, SessionNames.getFreeInstanceNumber(Collections.<Integer> emptySet()));
        assertEquals(3, SessionNames.getFreeInstanceNumber(Arrays.asList(1, 2)));
    }

    @Test
    public void testGetFreeInstanceNumber_afterClose() {
        // "X (2)" of "X", "X (2)" and "X (3)" has been closed: the next session
        // is "X (2)" again, not a second "X (3)".
        assertEquals(2, SessionNames.getFreeInstanceNumber(Arrays.asList(1, 3)));
        assertEquals(1, SessionNames.getFreeInstanceNumber(Arrays.asList(2, 3)));
    }

    @Test
    public void testCompare_byName() {
        assertTrue(SessionNames.compare("MYIBMI: dspsrc", 1, "MYIBMI: src", 1) < 0);
        assertTrue(SessionNames.compare("Build", 1, "atest", 1) > 0);
        assertTrue(SessionNames.compare("Build", 2, "build", 1) > 0);
    }

    @Test
    public void testCompare_byInstanceNumber() {
        // "X (10)" follows "X (2)", although "X (10)" < "X (2)" as text.
        assertTrue(SessionNames.compare("X", 10, "X", 2) > 0);
        assertTrue(SessionNames.compare("X", 2, "X", 10) < 0);
        assertEquals(0, SessionNames.compare("X", 2, "X", 2));
    }
}
