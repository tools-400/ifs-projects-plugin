/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.build;

/**
 * Specifies, where the build and compile commands of the IFS projects run.
 */
public enum BuildMode {

    /**
     * In the SSH terminal of the project. Afterwards the errors are shown in
     * the 'Error List' view, see {@link TerminalBuildJob}.
     */
    TERMINAL("terminal"), //$NON-NLS-1$

    /**
     * In a job of the RSE connection. The output is shown in the 'Commands
     * Log' view, the errors in the 'Error List' view.
     */
    RSE("rse"); //$NON-NLS-1$

    private final String value;

    private BuildMode(String aValue) {
        this.value = aValue;
    }

    /**
     * Returns the value stored in the preferences.
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns the build mode of a value of the preferences.
     *
     * @param aValue - value
     * @return build mode, {@link #TERMINAL} for unknown values
     */
    public static BuildMode fromValue(String aValue) {

        for (BuildMode buildMode : values()) {
            if (buildMode.value.equals(aValue)) {
                return buildMode;
            }
        }

        return TERMINAL;
    }
}
