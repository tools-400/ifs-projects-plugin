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
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import de.tools400.ifsprojects.project.IProjConfig;
import de.tools400.ifsprojects.project.IProjValues;

public class IProjValuesTest {

    @Test
    public void testGetObjectLibrary() throws Exception {

        Map<String, String> variables = new LinkedHashMap<String, String>();
        variables.put("LIB", "dspsrc");

        assertEquals("JSMITH#CL", IProjValues.getObjectLibrary(IProjConfig.parse("{\"objlib\": \"JSMITH#CL\", \"curlib\": \"QGPL\"}"), variables));
        assertEquals("QGPL", IProjValues.getObjectLibrary(IProjConfig.parse("{\"curlib\": \"QGPL\"}"), variables));
        assertEquals("DSPSRC", IProjValues.getObjectLibrary(IProjConfig.parse("{\"objlib\": \"&LIB\"}"), variables));
        assertNull(IProjValues.getObjectLibrary(IProjConfig.parse("{\"objlib\": \"*CURLIB\"}"), variables));
        assertNull(IProjValues.getObjectLibrary(IProjConfig.parse("{\"objlib\": \"&UNDEFINED\"}"), variables));
        assertNull(IProjValues.getObjectLibrary(IProjConfig.parse("{}"), variables));
    }

    @Test
    public void testGetIncludeDirectories() throws Exception {

        IProjConfig config = IProjConfig.parse("{\"includePath\": [\"qprotsrc\", \"/QIBM/include\", \"&INCDIR\", \"\"]}");

        Map<String, String> variables = new LinkedHashMap<String, String>();
        variables.put("INCDIR", "/home/jsmith/inc");

        assertEquals(Arrays.asList("/home/jsmith/git/dspsrc/qprotsrc", "/QIBM/include", "/home/jsmith/inc"),
            IProjValues.getIncludeDirectories("/home/jsmith/git/dspsrc", config, variables));
        assertEquals(Arrays.asList("/qprotsrc"), IProjValues.getIncludeDirectories("/", IProjConfig.parse("{\"includePath\": \"qprotsrc\"}"),
            new LinkedHashMap<String, String>()));
        assertTrue(IProjValues.getIncludeDirectories("/p", IProjConfig.parse("{}"), new LinkedHashMap<String, String>()).isEmpty());
    }
}
