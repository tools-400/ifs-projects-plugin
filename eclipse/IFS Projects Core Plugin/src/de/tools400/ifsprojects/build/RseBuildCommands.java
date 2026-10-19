/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.build;

import de.tools400.ifsprojects.ibmi.PaseCommands;

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

    private static final String STATEMENT_SEPARATOR = "; "; //$NON-NLS-1$

    private RseBuildCommands() {
        // Utility class
    }

    /**
     * Returns the shell script of a build.
     *
     * @param aShell - shell, {@link PaseCommands#BASH} or
     *        {@link PaseCommands#SH}
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
        script.append("exec < /dev/null >> ").append(PaseCommands.quote(aLogFile)).append(" 2>&1"); //$NON-NLS-1$ //$NON-NLS-2$
        script.append(STATEMENT_SEPARATOR).append("echo $$ > ").append(PaseCommands.quote(aProcessIdFile)); //$NON-NLS-1$
        script.append(STATEMENT_SEPARATOR).append("export HOME=").append(PaseCommands.quote(aHome)); //$NON-NLS-1$
        script.append(STATEMENT_SEPARATOR).append("[ -f /QOpenSys/etc/profile ] && . /QOpenSys/etc/profile"); //$NON-NLS-1$
        if (PaseCommands.BASH.equals(aShell)) {
            script.append(STATEMENT_SEPARATOR).append("if [ -f \"$HOME/.bash_profile\" ]; then . \"$HOME/.bash_profile\";"); //$NON-NLS-1$
            script.append(" elif [ -f \"$HOME/.bash_login\" ]; then . \"$HOME/.bash_login\";"); //$NON-NLS-1$
            script.append(" elif [ -f \"$HOME/.profile\" ]; then . \"$HOME/.profile\"; fi"); //$NON-NLS-1$
        } else {
            script.append(STATEMENT_SEPARATOR).append("[ -f \"$HOME/.profile\" ] && . \"$HOME/.profile\""); //$NON-NLS-1$
        }
        script.append(STATEMENT_SEPARATOR).append("cd ").append(PaseCommands.quote(aDirectory)).append(" && ").append(aCommand.trim()); //$NON-NLS-1$ //$NON-NLS-2$
        script.append(STATEMENT_SEPARATOR).append("echo $? > ").append(PaseCommands.quote(anExitCodeFile)); //$NON-NLS-1$

        return script.toString();
    }

    /**
     * Returns the shell script, that terminates a build: the shell, whose
     * process ID is stored in the file, and all its descendants. The process
     * tree is collected with <code>ps</code> before the processes are
     * terminated, because terminated processes lose their parent.
     *
     * @param aProcessIdFile - stream file with the process ID of the shell
     * @return script for {@link PaseCommands#SH}
     */
    public static String getStopScript(String aProcessIdFile) {

        StringBuilder script = new StringBuilder();
        script.append("exec < /dev/null > /dev/null 2>&1"); //$NON-NLS-1$
        script.append(STATEMENT_SEPARATOR).append("pid=$(cat ").append(PaseCommands.quote(aProcessIdFile)).append(')'); //$NON-NLS-1$
        script.append(STATEMENT_SEPARATOR).append("[ -n \"$pid\" ] || exit 0"); //$NON-NLS-1$
        script.append(STATEMENT_SEPARATOR).append("tree() { echo $1; for child in $(ps -e -o pid= -o ppid= | awk -v p=$1 '$2 == p { print $1 }'); do tree $child; done; }"); //$NON-NLS-1$
        script.append(STATEMENT_SEPARATOR).append("kill -TERM $(tree $pid)"); //$NON-NLS-1$
        script.append(STATEMENT_SEPARATOR).append("exit 0"); //$NON-NLS-1$

        return script.toString();
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
}
