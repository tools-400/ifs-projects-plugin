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

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import de.tools400.ifsprojects.export.ExportSettings;
import de.tools400.ifsprojects.export.SourceFileRule;
import de.tools400.ifsprojects.project.EnvironmentVariables;

public class ExportSettingsTest {

    @Test
    public void testDefaults() {

        ExportSettings settings = new ExportSettings("JSMITH#CL", 1141, 112);

        assertEquals("JSMITH#CL", settings.getLibrary());
        assertEquals(SourceFileRule.DIRECTORY, settings.getSourceFileRule());
        assertEquals("QSOURCE", settings.getSourceFile());
        assertEquals(1141, settings.getCcsid());
        assertEquals(112, settings.getRecordLength());
        assertTrue(settings.isMemberText());
        assertTrue(settings.isConvertIncludes());
    }

    @Test
    public void testToMapFromMap() {

        ExportSettings settings = new ExportSettings(null, 1141, 112);
        settings.setLibrary(" jsmith#cl ");
        settings.setSourceFileRule(SourceFileRule.SINGLE_FILE);
        settings.setSourceFile("src");
        settings.setCcsid(273);
        settings.setRecordLength(92);
        settings.setMemberText(false);

        ExportSettings restored = ExportSettings.fromMap(settings.toMap(), new ExportSettings("OTHER", 37, 120));

        assertEquals("JSMITH#CL", restored.getLibrary());
        assertEquals(SourceFileRule.SINGLE_FILE, restored.getSourceFileRule());
        assertEquals("SRC", restored.getSourceFile());
        assertEquals(273, restored.getCcsid());
        assertEquals(92, restored.getRecordLength());
        assertFalse(restored.isMemberText());
    }

    /**
     * Local IFS projects save the settings in the format of the environment
     * variables, see LocalProject.setExportSettings().
     */
    @Test
    public void testEnvironmentVariablesFormat() {

        ExportSettings settings = new ExportSettings("JSMITH#CL", 1141, 112);
        settings.setSourceFileRule(SourceFileRule.SINGLE_FILE);

        String text = EnvironmentVariables.format(settings.toMap());

        assertEquals(0, EnvironmentVariables.findInvalidLine(text));
        assertEquals(settings.toMap(), EnvironmentVariables.parse(text));
    }

    @Test
    public void testFromMap_invalidValues() {

        Map<String, String> values = new HashMap<>();
        values.put("library", " ");
        values.put("sourceFileRule", "UNKNOWN");
        values.put("ccsid", "x");
        values.put("recordLength", "12");

        ExportSettings settings = ExportSettings.fromMap(values, new ExportSettings("JSMITH#CL", 1141, 112));

        assertEquals("JSMITH#CL", settings.getLibrary());
        assertEquals(SourceFileRule.DIRECTORY, settings.getSourceFileRule());
        assertEquals(1141, settings.getCcsid());
        assertEquals(112, settings.getRecordLength());
        assertTrue(settings.isMemberText());

        assertNull(ExportSettings.fromMap(new HashMap<String, String>(), new ExportSettings(null, 1141, 112)).getLibrary());
    }

    @Test
    public void testParseInt() {
        assertEquals(Integer.valueOf(13), ExportSettings.parseInt(" 13 ", 13, 32766));
        assertEquals(Integer.valueOf(32766), ExportSettings.parseInt("32766", 13, 32766));
        assertNull(ExportSettings.parseInt("12", 13, 32766));
        assertNull(ExportSettings.parseInt("32767", 13, 32766));
        assertNull(ExportSettings.parseInt("", 13, 32766));
        assertNull(ExportSettings.parseInt(null, 13, 32766));
        assertEquals(42, ExportSettings.getInt("x", 1, 100, 42));
    }
}
