/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.terminal.tm;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.tm.internal.terminal.provisional.api.ISettingsStore;

/**
 * Settings store that is used to pass the connection settings to a terminal
 * connector. The settings are never persisted.
 */
@SuppressWarnings("restriction")
class MapSettingsStore implements ISettingsStore {

    private final Map<String, String> values = new HashMap<String, String>();

    public String get(String aKey) {
        return values.get(aKey);
    }

    public String get(String aKey, String aDefaultValue) {
        String value = values.get(aKey);
        if (value == null) {
            return aDefaultValue;
        }
        return value;
    }

    public void put(String aKey, String aValue) {
        if (aValue == null) {
            values.remove(aKey);
        } else {
            values.put(aKey, aValue);
        }
    }
}
