/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.syntaxcheck;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.tools400.ifsprojects.ibmi.ClCommands;
import de.tools400.ifsprojects.project.IProjConfig;
import de.tools400.ifsprojects.project.IProjValues;

/**
 * CL commands of the syntax check of a stream file.
 * <p>
 * The source is compiled into <code>QTEMP</code> of a job of its own with
 * <code>OPTION(*EVENTF)</code>. The compiler creates the events file
 * <code>QTEMP/EVFEVENT</code>; the member has the name of the object.
 * <ul>
 * <li>RPGLE: <code>CRTBNDRPG</code> or, for <code>NOMAIN</code> sources,
 * <code>CRTRPGMOD</code> with <code>OPTION(*NOGEN)</code>. No object is
 * created.</li>
 * <li>SQLRPGLE: <code>CRTSQLRPGI OBJTYPE(*MODULE)</code>. <code>*NOGEN</code>
 * would skip the RPG compiler and <code>COMPILEOPT('OPTION(*NOGEN)')</code>
 * fails with CPD0042, because the precompiler passes <code>OPTION</code>
 * itself. A module is created in <code>QTEMP</code>, if the source has no
 * errors. <code>RPGPPOPT(*LVL2)</code> expands the copy books before the
 * precompile, so that host variables of copy books are known.</li>
 * </ul>
 * <code>TGTCCSID(*JOB)</code> and <code>CVTCCSID(*JOB)</code> are required for
 * UTF-8 sources (RNS9339, SQL8032).
 */
public final class SyntaxCheckCommands {

    /** Library of the compiled objects and of the events file. */
    public static final String LIBRARY = "QTEMP"; //$NON-NLS-1$

    /** Name of the events file created by the compilers. */
    public static final String EVENTS_FILE = "EVFEVENT"; //$NON-NLS-1$

    /**
     * Message of CRTRPGMOD: "Keywords DFTACTGRP, ACTGRP, or USRPRF are not
     * allowed." It is reported, when the source of a program is compiled as
     * module.
     */
    public static final String RNF1324 = "RNF1324"; //$NON-NLS-1$

    /** Object name used, if the file name is not a valid object name. */
    static final String DEFAULT_OBJECT_NAME = "IFSCHECK"; //$NON-NLS-1$

    private static final Pattern FREE_DIRECTIVE = Pattern.compile("^\\*\\*FREE\\b", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$
    private static final Pattern CONTROL_STATEMENT = Pattern.compile("\\bCTL-OPT\\b([^;]*);?", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$
    private static final Pattern NOMAIN = Pattern.compile("(?<![A-Z0-9_$#@])NOMAIN(?![A-Z0-9_$#@])", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$
    private static final String LINE_SEPARATOR_PATTERN = "\\r?\\n"; //$NON-NLS-1$
    private static final String LINE_COMMENT = "//"; //$NON-NLS-1$
    private static final int FIXED_SPEC_TYPE = 5;
    private static final int FIXED_COMMENT = 6;
    private static final char SEPARATOR = '/';

    private SyntaxCheckCommands() {
        // Utility class
    }

    /**
     * Returns the name of the object, into which a stream file is compiled: the
     * file name up to the first period, e.g. <code>HELLO</code> for
     * <code>hello.pgm.rpgle</code>.
     *
     * @param aFileName - name of the stream file
     * @return object name
     */
    public static String getObjectName(String aFileName) {

        if (aFileName == null) {
            return DEFAULT_OBJECT_NAME;
        }

        String name = aFileName;
        int index = name.indexOf('.');
        if (index >= 0) {
            name = name.substring(0, index);
        }

        name = name.toUpperCase(Locale.ROOT);
        if (!ClCommands.isObjectName(name)) {
            return DEFAULT_OBJECT_NAME;
        }

        return name;
    }

    /**
     * Returns <code>true</code>, if the source specifies the control keyword
     * <code>NOMAIN</code>, either in a <code>CTL-OPT</code> statement or in a
     * control specification. Comments are ignored.
     *
     * @param aSource - RPG source
     * @return <code>true</code> for modules without main procedure
     */
    public static boolean isNoMain(String aSource) {

        if (aSource == null) {
            return false;
        }

        String[] lines = aSource.split(LINE_SEPARATOR_PATTERN);
        boolean isFree = lines.length > 0 && FREE_DIRECTIVE.matcher(lines[0]).find();

        StringBuilder code = new StringBuilder();
        for (String line : lines) {

            if (!isFree && line.length() > FIXED_COMMENT && line.charAt(FIXED_COMMENT) == '*') {
                continue;
            }

            String statement = removeLineComment(line);
            if (!isFree && isControlSpecification(statement)) {
                if (NOMAIN.matcher(statement.substring(FIXED_SPEC_TYPE + 1)).find()) {
                    return true;
                }
                continue;
            }

            code.append(statement).append(' ');
        }

        Matcher matcher = CONTROL_STATEMENT.matcher(code);
        while (matcher.find()) {
            if (NOMAIN.matcher(matcher.group(1)).find()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns the command that compiles a stream file for the syntax check.
     *
     * @param aSourceType - type of the source
     * @param anObjectName - name of the object, see
     *        {@link #getObjectName(String)}
     * @param aSourcePath - absolute path of the stream file
     * @param anIncludeDirectories - directories of the include files
     * @param aNoMain - specifies whether the source specifies
     *        <code>NOMAIN</code>, see {@link #isNoMain(String)}
     * @return compile command
     */
    public static String getCompileCommand(SourceType aSourceType, String anObjectName, String aSourcePath, List<String> anIncludeDirectories,
        boolean aNoMain) {

        String object = LIBRARY + SEPARATOR + anObjectName;
        String includeDirectories = getIncludeDirectoriesParameter(anIncludeDirectories);

        StringBuilder command = new StringBuilder();
        switch (aSourceType) {
        case SQLRPGLE:
            StringBuilder compileOptions = new StringBuilder("OUTPUT(*NONE) TGTCCSID(*JOB)"); //$NON-NLS-1$
            if (includeDirectories != null) {
                compileOptions.append(' ').append(includeDirectories);
            }
            command.append("CRTSQLRPGI OBJ(").append(object).append(") SRCSTMF(").append(ClCommands.quote(aSourcePath)); //$NON-NLS-1$ //$NON-NLS-2$
            command.append(") OBJTYPE(*MODULE) OPTION(*EVENTF) OUTPUT(*NONE) CVTCCSID(*JOB) RPGPPOPT(*LVL2) COMPILEOPT("); //$NON-NLS-1$
            command.append(ClCommands.quote(compileOptions.toString())).append(')');
            return command.toString();

        case RPGLE:
            if (aNoMain) {
                command.append("CRTRPGMOD MODULE(").append(object).append(')'); //$NON-NLS-1$
            } else {
                command.append("CRTBNDRPG PGM(").append(object).append(')'); //$NON-NLS-1$
            }
            command.append(" SRCSTMF(").append(ClCommands.quote(aSourcePath)).append(") OPTION(*NOGEN *EVENTF) OUTPUT(*NONE)"); //$NON-NLS-1$ //$NON-NLS-2$
            if (!aNoMain) {
                // DFTACTGRP(*YES), the default, does not allow bound calls.
                // A CTL-OPT DFTACTGRP() of the source overrides it.
                command.append(" DFTACTGRP(*NO)"); //$NON-NLS-1$
            }
            command.append(" TGTCCSID(*JOB)"); //$NON-NLS-1$
            if (includeDirectories != null) {
                command.append(' ').append(includeDirectories);
            }
            return command.toString();

        default:
            throw new IllegalArgumentException("Unsupported source type: " + aSourceType); //$NON-NLS-1$
        }
    }

    /**
     * Returns the IDs of the messages, that are removed from the events file,
     * because they are caused by the syntax check itself.
     *
     * @param aSourceType - type of the source
     * @param aNoMain - specifies whether the source specifies
     *        <code>NOMAIN</code>
     * @return message IDs
     */
    public static Set<String> getIgnoredMessageIds(SourceType aSourceType, boolean aNoMain) {

        if (aSourceType == SourceType.SQLRPGLE && !aNoMain) {
            // The source of a program is compiled as module.
            return Collections.singleton(RNF1324);
        }

        return Collections.emptySet();
    }

    /**
     * Returns the commands, that set up the job of the syntax check like the
     * build jobs of TOBi: the current directory is the project directory, the
     * commands of <code>setIBMiEnvCmd</code> are executed and the libraries of
     * <code>curlib</code>, <code>preUsrlibl</code> and
     * <code>postUsrlibl</code> are added to the library list. Variables
     * (<code>&amp;NAME</code>) are replaced by the environment variables of the
     * project.
     *
     * @param aProjectDirectory - absolute project directory
     * @param aConfig - content of <code>iproj.json</code>
     * @param aVariables - environment variables of the project
     * @return commands
     */
    public static List<String> getSetupCommands(String aProjectDirectory, IProjConfig aConfig, Map<String, String> aVariables) {

        List<String> commands = new ArrayList<String>();
        commands.add("CHGCURDIR DIR(" + ClCommands.quote(aProjectDirectory) + ")"); //$NON-NLS-1$ //$NON-NLS-2$

        for (String command : IProjValues.getValues(aConfig, IProjConfig.SET_IBMI_ENV_CMD, aVariables)) {
            commands.add(command);
        }

        List<String> currentLibrary = IProjValues.getLibraries(aConfig, IProjConfig.CURLIB, aVariables);
        if (!currentLibrary.isEmpty()) {
            commands.add("CHGCURLIB CURLIB(" + currentLibrary.get(0) + ")"); //$NON-NLS-1$ //$NON-NLS-2$
        }

        List<String> preLibraries = IProjValues.getLibraries(aConfig, IProjConfig.PRE_USRLIBL, aVariables);
        for (int i = preLibraries.size() - 1; i >= 0; i--) {
            commands.add("ADDLIBLE LIB(" + preLibraries.get(i) + ") POSITION(*FIRST)"); //$NON-NLS-1$ //$NON-NLS-2$
        }

        for (String library : IProjValues.getLibraries(aConfig, IProjConfig.POST_USRLIBL, aVariables)) {
            commands.add("ADDLIBLE LIB(" + library + ") POSITION(*LAST)"); //$NON-NLS-1$ //$NON-NLS-2$
        }

        return commands;
    }

    /**
     * Returns the command that removes the member of the events file of a
     * previous syntax check.
     *
     * @param anObjectName - object name
     * @return command
     */
    public static String getRemoveEventsMemberCommand(String anObjectName) {
        return "RMVM FILE(" + LIBRARY + SEPARATOR + EVENTS_FILE + ") MBR(" + anObjectName + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    /**
     * Returns the command that copies the member of the events file to a stream
     * file with CCSID 1208 (UTF-8).
     *
     * @param anObjectName - object name
     * @param aStreamFile - absolute path of the stream file
     * @return command
     */
    public static String getCopyEventsMemberCommand(String anObjectName, String aStreamFile) {
        return getCopyEventsMemberCommand(LIBRARY, anObjectName, aStreamFile);
    }

    /**
     * Returns the command that copies a member of the events file of a library
     * to a stream file with CCSID 1208 (UTF-8).
     *
     * @param aLibrary - library of the events file
     * @param aMemberName - member name, i.e. the object name
     * @param aStreamFile - absolute path of the stream file
     * @return command
     */
    public static String getCopyEventsMemberCommand(String aLibrary, String aMemberName, String aStreamFile) {
        String member = ClCommands.getMemberPath(aLibrary, EVENTS_FILE, aMemberName);
        return "CPYTOSTMF FROMMBR(" + ClCommands.quote(member) + ") TOSTMF(" + ClCommands.quote(aStreamFile) + ") STMFOPT(*REPLACE) STMFCCSID(1208)"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    private static String getIncludeDirectoriesParameter(List<String> aDirectories) {

        if (aDirectories == null || aDirectories.isEmpty()) {
            return null;
        }

        StringBuilder parameter = new StringBuilder("INCDIR("); //$NON-NLS-1$
        for (int i = 0; i < aDirectories.size(); i++) {
            if (i > 0) {
                parameter.append(' ');
            }
            parameter.append(ClCommands.quote(aDirectories.get(i)));
        }
        parameter.append(')');

        return parameter.toString();
    }

    private static String removeLineComment(String aLine) {

        int index = aLine.indexOf(LINE_COMMENT);
        if (index < 0) {
            return aLine;
        }

        return aLine.substring(0, index);
    }

    /**
     * Returns <code>true</code> for a control specification of a fixed-form
     * source. Free-form statements of a fixed-form source start in column 8 or
     * later, therefore column 6 identifies the specification.
     */
    private static boolean isControlSpecification(String aLine) {
        return aLine.length() > FIXED_SPEC_TYPE && Character.toUpperCase(aLine.charAt(FIXED_SPEC_TYPE)) == 'H';
    }
}
