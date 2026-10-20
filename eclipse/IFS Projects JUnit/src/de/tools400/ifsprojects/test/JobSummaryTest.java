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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import de.tools400.ifsprojects.dialogs.JobSummary;

public class JobSummaryTest {

    @Test
    public void testGetText_noErrors() {
        assertEquals("2 files created.", JobSummary.getText("2 files created.", Collections.<String> emptyList()));
    }

    @Test
    public void testGetText_errors() {

        String text = JobSummary.getText("Done.", Arrays.asList("a: failed", "b: failed"));

        assertTrue(text.startsWith("Done.\n\n"));
        assertTrue(text.endsWith("\n  a: failed\n  b: failed\n"));
    }

    @Test
    public void testGetText_manyErrors() {

        List<String> errors = new ArrayList<String>();
        for (int i = 1; i <= 25; i++) {
            errors.add("error " + i);
        }

        String text = JobSummary.getText("Done.", errors);

        assertTrue(text.contains("  error 20\n"));
        assertTrue(!text.contains("error 21"));
        assertTrue(text.contains("5"));
    }
}
