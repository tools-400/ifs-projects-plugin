/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.project;

import java.util.regex.Pattern;

import de.tools400.ifsprojects.utils.StringUtils;

/**
 * Build tools with their build and compile commands. They are offered as
 * presets on the preference page and in the 'New IFS Project' wizard.
 */
public enum BuildTool {

    /** GNU Make with a makefile in the project directory. */
    GMAKE("gmake", "gmake", "gmake {filename}", "gmake"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

    /** TOBi (The Object Builder for i) with iproj.json and Rules.mk. */
    TOBI("TOBi (makei)", "makei build", "makei compile -f {filename}", "makei"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

    private final String label;
    private final String buildCommand;
    private final String compileCommand;

    /*
     * Program of the build tool as a whole word of a command, e.g.
     * '/QOpenSys/pkgs/bin/gmake all', but not 'gmake-4.3' or 'xgmake'.
     */
    private final Pattern programPattern;

    private BuildTool(String aLabel, String aBuildCommand, String aCompileCommand, String aProgram) {
        this.label = aLabel;
        this.buildCommand = aBuildCommand;
        this.compileCommand = aCompileCommand;
        this.programPattern = Pattern.compile("(?<![\\w.-])" + Pattern.quote(aProgram) + "(?![\\w.-])"); //$NON-NLS-1$ //$NON-NLS-2$
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

    /**
     * Returns the build tool, that the commands of an existing
     * <code>iproj.json</code> file use: the build tool, whose program
     * (<code>gmake</code>, <code>makei</code>) is a word of the build command
     * or, if there is no build command, of the compile command, e.g.
     * <code>gmake all</code> or <code>makei b</code>. <code>make</code> is not
     * GNU Make, but the make of AIX in PASE.
     *
     * @param aBuildCommand - build command or <code>null</code>
     * @param aCompileCommand - compile command or <code>null</code>
     * @return build tool or <code>null</code>, if the command contains the
     *         program of no or of several build tools
     */
    public static BuildTool findExisting(String aBuildCommand, String aCompileCommand) {

        String command = aBuildCommand;
        if (StringUtils.isBlank(command)) {
            command = aCompileCommand;
        }

        if (command == null) {
            return null;
        }

        BuildTool foundBuildTool = null;
        for (BuildTool buildTool : values()) {
            if (buildTool.programPattern.matcher(command).find()) {
                if (foundBuildTool != null) {
                    return null;
                }
                foundBuildTool = buildTool;
            }
        }

        return foundBuildTool;
    }
}
