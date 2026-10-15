/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.project;

/**
 * Build tools with their build and compile commands. They are offered as
 * presets on the preference page and in the 'New IFS Project' wizard.
 */
public enum BuildTool {

    /** GNU Make with a makefile in the project directory. */
    GMAKE("gmake", "gmake", "gmake {filename}"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

    /** TOBi (The Object Builder for i) with iproj.json and Rules.mk. */
    TOBI("TOBi (makei)", "makei build", "makei compile -f {filename}"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

    private final String label;
    private final String buildCommand;
    private final String compileCommand;

    private BuildTool(String aLabel, String aBuildCommand, String aCompileCommand) {
        this.label = aLabel;
        this.buildCommand = aBuildCommand;
        this.compileCommand = aCompileCommand;
    }

    /**
     * Returns the name of the build tool, which is shown on buttons. It is a
     * product name and therefore not translated.
     */
    public String getLabel() {
        return label;
    }

    /**
     * Returns the command that builds the project.
     */
    public String getBuildCommand() {
        return buildCommand;
    }

    /**
     * Returns the command that compiles a single file of the project. The
     * placeholder <code>{filename}</code> is replaced with the name of the
     * file.
     */
    public String getCompileCommand() {
        return compileCommand;
    }

    /**
     * Returns the build tool, whose build and compile commands are equal to the
     * specified commands. Leading and trailing blanks are ignored.
     *
     * @param aBuildCommand - build command or <code>null</code>
     * @param aCompileCommand - compile command or <code>null</code>
     * @return build tool or <code>null</code>, if the commands do not belong
     *         to a build tool
     */
    public static BuildTool find(String aBuildCommand, String aCompileCommand) {

        if (aBuildCommand == null || aCompileCommand == null) {
            return null;
        }

        for (BuildTool buildTool : values()) {
            if (buildTool.buildCommand.equals(aBuildCommand.trim()) && buildTool.compileCommand.equals(aCompileCommand.trim())) {
                return buildTool;
            }
        }

        return null;
    }
}
