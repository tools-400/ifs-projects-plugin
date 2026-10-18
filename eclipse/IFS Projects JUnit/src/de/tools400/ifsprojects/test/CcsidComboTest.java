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

import de.tools400.ifsprojects.swt.CcsidCombo;

public class CcsidComboTest {

    @Test
    public void testParseCcsid() {
        assertEquals(Integer.valueOf(1141), CcsidCombo.parseCcsid("1141"));
        assertEquals(Integer.valueOf(1141), CcsidCombo.parseCcsid(" 1141 "));
        assertEquals(Integer.valueOf(1141), CcsidCombo.parseCcsid("1141 - Germany, Austria (euro)"));
        assertEquals(Integer.valueOf(1141), CcsidCombo.parseCcsid("1141-Deutschland"));
        assertEquals(Integer.valueOf(37), CcsidCombo.parseCcsid("37 - USA, Canada"));
        assertNull(CcsidCombo.parseCcsid("1141 Germany"));
        assertNull(CcsidCombo.parseCcsid("Germany"));
        assertNull(CcsidCombo.parseCcsid("0"));
        assertNull(CcsidCombo.parseCcsid("65534"));
        assertNull(CcsidCombo.parseCcsid(""));
    }
}
