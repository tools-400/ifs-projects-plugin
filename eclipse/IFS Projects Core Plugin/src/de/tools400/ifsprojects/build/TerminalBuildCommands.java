/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.build;

import java.util.regex.Pattern;

import de.tools400.ifsprojects.ibmi.PaseCommands;

/**
 * Commands of a build, that runs in the SSH terminal of the project.
 * <p>
 * The plug-in passes the command line to the interactive shell of the terminal
 * and does not learn, when the command ends. Therefore the command is wrapped
 * in a subshell, that sources a script of the plug-in and writes marker files:
 *
 * <pre>
 * ( _ifsp=&lt;id&gt;; . '&lt;directory&gt;/ifsprojects.sh'; makei build; _ifsp_end $? )
 * </pre>
 * <ul>
 * <li>The start file contains the process ID of the subshell. Its modification
 * time is the start time of the command (time of the server).</li>
 * <li>The exit code file contains the exit code of the command or 129, if the
 * terminal has been closed or the connection has been lost (SIGHUP), 130 for
 * Ctrl+C (SIGINT) and 143 for SIGTERM.</li>
 * </ul>
 * The command is parsed by the shell of the terminal, so that its aliases,
 * functions and variables are available. The exit code is written by traps,
 * because bash skips a command after the build command (<code>; echo $? &gt;
 * file</code>), when the build command is interrupted with Ctrl+C. An EXIT trap
 * cannot be used: bsh does not run it in a subshell, bash and ksh run it with
 * exit code 0 after a signal. Nothing is written, if the subshell is killed
 * (SIGKILL), therefore the job checks, whether the subshell still exists, see
 * {@link #getCheckScript(int, String)}.
 * <p>
 * Verified with bash, ksh and bsh of the IBM i.
 */
public final class TerminalBuildCommands {

    /**
     * Name of the script in the directory of temporary files. It does not start
     * with the prefix of the temporary files, because it is used by all builds
     * and kept.
     */
    public static final String SCRIPT_FILE_NAME = "ifsprojects.sh"; //$NON-NLS-1$

    public static final String START_FILE_SUFFIX = ".start"; //$NON-NLS-1$
    public static final String EXIT_CODE_FILE_SUFFIX = ".rc"; //$NON-NLS-1$
    public static final String GONE_FILE_SUFFIX = ".gone"; //$NON-NLS-1$

    private static final String ID_VARIABLE = "_ifsp"; //$NON-NLS-1$
    private static final String FILE_VARIABLE = "_ifsp_f"; //$NON-NLS-1$
    private static final String END_FUNCTION = "_ifsp_end"; //$NON-NLS-1$

    private static final int EXIT_CODE_SIGHUP = 129;
    private static final int EXIT_CODE_SIGINT = 130;
    private static final int EXIT_CODE_SIGTERM = 143;

    private static final String PS = "/QOpenSys/usr/bin/ps"; //$NON-NLS-1$
    private static final String LINE_SEPARATOR = "\n"; //$NON-NLS-1$
    private static final String STATEMENT_SEPARATOR = "; "; //$NON-NLS-1$

    /** Comment: '#' at the beginning of a word. */
    private static final Pattern COMMENT = Pattern.compile("(^|\\s)#"); //$NON-NLS-1$

    /**
     * Operator at the end, that requires another command or runs the command in
     * the background.
     */
    private static final Pattern OPERATOR_AT_END = Pattern.compile("[&|\\\\]$"); //$NON-NLS-1$

    private static final Pattern SEMICOLONS_AT_END = Pattern.compile("[;\\s]+$"); //$NON-NLS-1$

    private static final Pattern PROCESS_ID = Pattern.compile("[0-9]{1,9}"); //$NON-NLS-1$

    private TerminalBuildCommands() {
        // Utility class
    }

    /**
     * Returns the path of the script.
     *
     * @param aTemporaryDirectory - directory of temporary files
     * @return path
     */
    public static String getScriptFile(String aTemporaryDirectory) {
        return aTemporaryDirectory + "/" + SCRIPT_FILE_NAME; //$NON-NLS-1$
    }

    /**
     * Returns the script, that is sourced by the subshell. It sets the traps,
     * defines the function, that writes the exit code and ends the subshell,
     * and writes the start file.
     *
     * @param aTemporaryFilePrefix - beginning of the paths of the marker files,
     *        e.g. <code>/home/jsmith/.ifsprojects/tmp/ifsprojects_</code>
     * @return script
     */
    public static String getScript(String aTemporaryFilePrefix) {

        StringBuilder script = new StringBuilder();
        script.append("# IFS Projects: writes the marker files of a build or compile command in the terminal.").append(LINE_SEPARATOR); //$NON-NLS-1$
        script.append("# Sourced by: ( ").append(ID_VARIABLE).append("=<id>; . <this script>; <command>; ").append(END_FUNCTION) //$NON-NLS-1$ //$NON-NLS-2$
            .append(" $? )").append(LINE_SEPARATOR); //$NON-NLS-1$
        script.append(FILE_VARIABLE).append('=').append(PaseCommands.quote(aTemporaryFilePrefix)).append("\"$").append(ID_VARIABLE).append('"') //$NON-NLS-1$
            .append(LINE_SEPARATOR);
        script.append(END_FUNCTION).append("() { echo $1 > \"$").append(FILE_VARIABLE).append(EXIT_CODE_FILE_SUFFIX).append("\"; exit $1; }") //$NON-NLS-1$ //$NON-NLS-2$
            .append(LINE_SEPARATOR);
        appendTrap(script, EXIT_CODE_SIGHUP, 1);
        appendTrap(script, EXIT_CODE_SIGINT, 2);
        appendTrap(script, EXIT_CODE_SIGTERM, 15);
        script.append(PaseCommands.SH).append(" -c 'echo $PPID' > \"$").append(FILE_VARIABLE).append(START_FILE_SUFFIX).append('"') //$NON-NLS-1$
            .append(LINE_SEPARATOR);

        return script.toString();
    }

    /**
     * Returns the command wrapped in a subshell, that writes the marker files.
     *
     * @param aCommand - command, e.g. <code>makei build</code>
     * @param anId - ID of the marker files, see
     *        {@link de.tools400.ifsprojects.remote.RemoteJobs#createTemporaryFileId()}
     * @param aScriptFile - path of the script, see {@link #getScriptFile}
     * @return wrapped command or <code>null</code>, if the command cannot be
     *         wrapped: it is empty, contains a comment or ends with an
     *         operator, e.g. <code>&amp;</code>
     */
    public static String wrap(String aCommand, String anId, String aScriptFile) {

        if (aCommand == null) {
            return null;
        }

        String command = SEMICOLONS_AT_END.matcher(aCommand.trim()).replaceAll(""); //$NON-NLS-1$
        if (command.length() == 0 || COMMENT.matcher(command).find() || OPERATOR_AT_END.matcher(command).find()) {
            return null;
        }

        StringBuilder buffer = new StringBuilder();
        buffer.append("( ").append(ID_VARIABLE).append('=').append(anId); //$NON-NLS-1$
        buffer.append(STATEMENT_SEPARATOR).append(". ").append(PaseCommands.quote(aScriptFile)); //$NON-NLS-1$
        buffer.append(STATEMENT_SEPARATOR).append(command);
        buffer.append(STATEMENT_SEPARATOR).append(END_FUNCTION).append(" $? )"); //$NON-NLS-1$

        return buffer.toString();
    }

    /**
     * Returns the shell script, that writes the gone file, if the subshell of a
     * command does not exist any more. <code>ps</code> is used instead of
     * <code>kill -0</code>, because it also finds the processes of other users.
     * The gone file is not written, if <code>ps</code> does not find its own
     * process.
     *
     * @param aProcessId - process ID of the subshell, see the start file
     * @param aGoneFile - path of the gone file
     * @return script for {@link RseBuildCommands#SH}
     */
    public static String getCheckScript(int aProcessId, String aGoneFile) {

        StringBuilder script = new StringBuilder();
        script.append("exec < /dev/null > /dev/null 2>&1"); //$NON-NLS-1$
        script.append(STATEMENT_SEPARATOR).append("[ -n \"$(").append(PS).append(" -p $$ -o pid=)\" ]"); //$NON-NLS-1$ //$NON-NLS-2$
        script.append(" && [ -z \"$(").append(PS).append(" -p ").append(aProcessId).append(" -o pid=)\" ]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        script.append(" && echo gone > ").append(PaseCommands.quote(aGoneFile)); //$NON-NLS-1$
        script.append(STATEMENT_SEPARATOR).append("exit 0"); //$NON-NLS-1$

        return script.toString();
    }

    /**
     * Returns the process ID written to the start file.
     *
     * @param aContent - content of the start file
     * @return process ID or <code>null</code>, if the content is not a valid
     *         process ID
     */
    public static Integer parseProcessId(String aContent) {

        if (aContent == null) {
            return null;
        }

        String content = aContent.trim();
        if (!PROCESS_ID.matcher(content).matches()) {
            return null;
        }

        int processId = Integer.parseInt(content);
        if (processId <= 0) {
            return null;
        }

        return Integer.valueOf(processId);
    }

    private static void appendTrap(StringBuilder aScript, int anExitCode, int aSignal) {
        aScript.append("trap '").append(END_FUNCTION).append(' ').append(anExitCode).append("' ").append(aSignal).append(LINE_SEPARATOR); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
