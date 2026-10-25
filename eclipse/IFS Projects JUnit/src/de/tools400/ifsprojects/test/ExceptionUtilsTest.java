/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.junit.Test;

import de.tools400.ifsprojects.utils.ExceptionUtils;

public class ExceptionUtilsTest {

    @Test
    public void testGetLocalizedMessage() throws Exception {

        assertEquals("Disk full", ExceptionUtils.getLocalizedMessage(new IOException("Disk full")));
        assertEquals("Disk full", ExceptionUtils.getLocalizedMessage(new IOException(" Disk full ")));
    }

    @Test
    public void testGetLocalizedMessage_withoutMessage() throws Exception {

        assertEquals("java.lang.NullPointerException", ExceptionUtils.getLocalizedMessage(new NullPointerException()));
        assertEquals("java.io.IOException", ExceptionUtils.getLocalizedMessage(new IOException("  ")));
    }

    @Test
    public void testGetLocalizedMessage_ofTheCause() throws Exception {

        assertEquals("Not found", ExceptionUtils.getLocalizedMessage(new InvocationTargetException(new IOException("Not found"))));
        assertEquals("java.lang.NullPointerException", ExceptionUtils.getLocalizedMessage(new InvocationTargetException(new NullPointerException())));

        // The own message comes first.
        assertEquals("Not created", ExceptionUtils.getLocalizedMessage(new InvocationTargetException(new IOException("Not found"), "Not created")));
    }

    @Test
    public void testGetLocalizedMessage_cycleOfCauses() throws Exception {

        IOException first = new IOException();
        IOException second = new IOException();
        first.initCause(second);
        second.initCause(first);

        assertEquals("java.io.IOException", ExceptionUtils.getLocalizedMessage(first));
    }

    @Test
    public void testGetCause() throws Exception {

        IOException target = new IOException("Not found");
        assertSame(target, ExceptionUtils.getCause(new InvocationTargetException(target)));

        InvocationTargetException withoutCause = new InvocationTargetException(null, "Not created");
        assertSame(withoutCause, ExceptionUtils.getCause(withoutCause));
    }
}
