/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;

/**
 * Initializes default preference values of the IFS Projects plug-in.
 */
public class PreferencesInitializer extends AbstractPreferenceInitializer {

    @Override
    public void initializeDefaultPreferences() {

        Preferences.getInstance().initializeDefaultPreferences();
    }
}
