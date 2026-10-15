/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.build;

import java.util.Locale;
import java.util.Map;

import de.tools400.ifsprojects.project.EnvironmentVariables;
import de.tools400.ifsprojects.project.IProjConfig;
import de.tools400.ifsprojects.syntaxcheck.SyntaxCheckCommands;
import de.tools400.ifsprojects.terminal.ShellCommands;

/**
 * Commands of a build, that runs in a remote command server job of the RSE
 * connection instead of the SSH terminal.
 * <p>
 * The shell is started with <code>QP2SHELL2</code>. The remote command server
 * job does not set <code>HOME</code>, therefore the script sets it and runs the
 * profiles like a login shell, so that the <code>PATH</code> contains
 * <code>/QOpenSys/pkgs/bin</code> (<code>makei</code>, <code>gmake</code>).
 * The output is appended to a log file, which is read while the build is
 * running. <code>QP2SHELL2</code> does not return the exit code of the shell,
 * therefore it is written to a file.
 * <p>
 * The script writes its process ID to a file. A build is canceled by
 * terminating the shell and all its descendants (e.g. <code>makei</code>,
 * <code>make</code>, <code>system</code>), which run in jobs of their own, see
 * {@link #getStopScript(String)}.
 */
public final class RseBuildCommands {

    /** Bash of the IBM i open source packages. */
    public static final String BASH = "/QOpenSys/pkgs/bin/bash"; //$NON-NLS-1$

    /** Default shell of PASE, used if bash is not installed. */
    public static final String SH = "/QOpenSys/usr/bin/sh"; //$NON-NLS-1$

    private static final String OBJECT_NAME_PATTERN = "[A-Z$#@][A-Z0-9$#@_]{0,9}"; //$NON-NLS-1$
    private static final String STATEMENT_SEPARATOR = "; "; //$NON-NLS-1$

    private RseBuildCommands() {
        // Utility class
    }

    /**
     * Returns the shell script of a build.
     *
     * @param aShell - shell, {@link #BASH} or {@link #SH}
     * @param aHome - home directory of the user
     * @param aLogFile - stream file, to which the output is appended
     * @param anExitCodeFile - stream file, into which the exit code of the
     *        command is written
     * @param aProcessIdFile - stream file, into which the process ID of the
     *        shell is written
     * @param aDirectory - project directory
     * @param aCommand - command, e.g. <code>makei build</code>, including the
     *        exports of the environment variables
     * @return script
     */
    public static String getScript(String aShell, String aHome, String aLogFile, String anExitCodeFile, String aProcessIdFile, String aDirectory,
        String aCommand) {

        StringBuilder script = new StringBuilder();
        script.append("exec < /dev/null >> ").append(ShellCommands.quote(aLogFile)).append(" 2>&1"); //$NON-NLS-1$ //$NON-NLS-2$
        script.append(STATEMENT_SEPARATOR).append("echo $$ > ").append(ShellCommands.quote(aProcessIdFile)); //$NON-NLS-1$
        script.append(STATEMENT_SEPARATOR).append("export HOME=").append(ShellCommands.quote(aHome)); //$NON-NLS-1$
        script.append(STATEMENT_SEPARATOR).append("[ -f /QOpenSys/etc/profile ] && . /QOpenSys/etc/profile"); //$NON-NLS-1$
        if (BASH.equals(aShell)) {
            script.append(STATEMENT_SEPARATOR).append("if [ -f \"$HOME/.bash_profile\" ]; then . \"$HOME/.bash_profile\";"); //$NON-NLS-1$
            script.append(" elif [ -f \"$HOME/.bash_login\" ]; then . \"$HOME/.bash_login\";"); //$NON-NLS-1$
            script.append(" elif [ -f \"$HOME/.profile\" ]; then . \"$HOME/.profile\"; fi"); //$NON-NLS-1$
        } else {
            script.append(STATEMENT_SEPARATOR).append("[ -f \"$HOME/.profile\" ] && . \"$HOME/.profile\""); //$NON-NLS-1$
        }
        script.append(STATEMENT_SEPARATOR).append("cd ").append(ShellCommands.quote(aDirectory)).append(" && ").append(aCommand.trim()); //$NON-NLS-1$ //$NON-NLS-2$
        script.append(STATEMENT_SEPARATOR).append("echo $? > ").append(ShellCommands.quote(anExitCodeFile)); //$NON-NLS-1$

        return script.toString();
    }

    /**
     * Returns the CL command, that runs a shell script with
     * <code>QP2SHELL2</code>.
     *
     * @param aShell - shell, {@link #BASH} or {@link #SH}
     * @param aScript - script, see {@link #getScript}
     * @return CL command
     */
    public static String getCallCommand(String aShell, String aScript) {
        return "CALL PGM(QP2SHELL2) PARM(" + SyntaxCheckCommands.quote(aShell) + " '-c' " + SyntaxCheckCommands.quote(aScript) + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    /**
     * Returns the shell script, that terminates a build: the shell, whose
     * process ID is stored in the file, and all its descendants. The process
     * tree is collected with <code>ps</code> before the processes are
     * terminated, because terminated processes lose their parent.
     *
     * @param aProcessIdFile - stream file with the process ID of the shell
     * @return script for {@link #SH}
     */
    public static String getStopScript(String aProcessIdFile) {

        StringBuilder script = new StringBuilder();
        script.append("exec < /dev/null > /dev/null 2>&1"); //$NON-NLS-1$
        script.append(STATEMENT_SEPARATOR).append("pid=$(cat ").append(ShellCommands.quote(aProcessIdFile)).append(')'); //$NON-NLS-1$
        script.append(STATEMENT_SEPARATOR).append("[ -n \"$pid\" ] || exit 0"); //$NON-NLS-1$
        script.append(STATEMENT_SEPARATOR).append("tree() { echo $1; for child in $(ps -e -o pid= -o ppid= | awk -v p=$1 '$2 == p { print $1 }'); do tree $child; done; }"); //$NON-NLS-1$
        script.append(STATEMENT_SEPARATOR).append("kill -TERM $(tree $pid)"); //$NON-NLS-1$
        script.append(STATEMENT_SEPARATOR).append("exit 0"); //$NON-NLS-1$

        return script.toString();
    }

    /**
     * Returns the library, whose events file (EVFEVENT) contains the events of
     * the objects created by a build: <code>objlib</code> of
     * <code>iproj.json</code> or, without <code>objlib</code>,
     * <code>curlib</code>. Variables are resolved.
     *
     * @param aConfig - content of <code>iproj.json</code>
     * @param aVariables - environment variables of the project
     * @return library or <code>null</code>, if the library is unknown
     */
    public static String getObjectLibrary(IProjConfig aConfig, Map<String, String> aVariables) {

        String library = getLibrary(aConfig, IProjConfig.OBJLIB, aVariables);
        if (library == null) {
            library = getLibrary(aConfig, IProjConfig.CURLIB, aVariables);
        }

        return library;
    }

    /**
     * Returns the exit code written by the script.
     *
     * @param aContent - content of the exit code file
     * @return exit code or <code>null</code>, if the content is not a number
     */
    public static Integer parseExitCode(String aContent) {

        if (aContent == null) {
            return null;
        }

        try {
            return Integer.valueOf(aContent.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String getLibrary(IProjConfig aConfig, String aKey, Map<String, String> aVariables) {

        String value = EnvironmentVariables.resolveReferences(aConfig.getString(aKey), aVariables);
        if (value == null) {
            return null;
        }

        String library = value.trim().toUpperCase(Locale.ROOT);
        if (!library.matches(OBJECT_NAME_PATTERN)) {
            return null;
        }

        return library;
    }
}
