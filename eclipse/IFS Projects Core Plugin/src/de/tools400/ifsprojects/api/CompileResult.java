/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Result of the compile or the syntax check of a file of a local IFS project,
 * see {@link FileCompiler}.
 */
public final class CompileResult {

    private final String command;
    private final Integer exitCode;
    private final List<String> output;
    private final String events;
    private final int ccsid;
    private final Map<String, String> localFiles;

    CompileResult(String aCommand, Integer anExitCode, List<String> anOutput, String anEvents, int aCcsid, Map<String, String> aLocalFiles) {
        this.command = aCommand;
        this.exitCode = anExitCode;
        this.output = Collections.unmodifiableList(new ArrayList<>(anOutput));
        this.events = anEvents;
        this.ccsid = aCcsid;
        this.localFiles = Collections.unmodifiableMap(new LinkedHashMap<>(aLocalFiles));
    }

    /**
     * Returns the command, that compiled or checked the file.
     *
     * @return command, e.g. <code>makei compile -f hello.pgm.rpgle</code>
     */
    public String getCommand() {
        return command;
    }

    /**
     * Returns the exit code of the compile command.
     *
     * @return exit code or <code>null</code>, if it is not known or the result
     *         is the one of a syntax check
     */
    public Integer getExitCode() {
        return exitCode;
    }

    /**
     * Returns the output of the compile command or, for a syntax check, the
     * messages of the compiler command.
     *
     * @return lines, maybe empty
     */
    public List<String> getOutput() {
        return output;
    }

    /**
     * Returns the events of the compiled objects (EVFEVENT), one events file
     * after the other. The paths of the files of the project are replaced by
     * their locations in the workspace, e.g.
     * <code>/demo/src/hello.rpgle</code>, see {@link #getLocalFiles()}.
     *
     * @return events or <code>null</code>, if no events file has been found
     */
    public String getEvents() {
        return events;
    }

    /**
     * Returns the CCSID of the job, in which the events were read. The lengths
     * of the texts of the events are counted in bytes of this CCSID.
     *
     * @return CCSID
     */
    public int getCcsid() {
        return ccsid;
    }

    /**
     * Returns the files of the events, that are files of the local project.
     *
     * @return locations in the workspace, e.g.
     *         <code>/demo/src/hello.rpgle</code>, and their absolute paths in
     *         the local file system, e.g.
     *         <code>C:/git/demo/src/hello.rpgle</code>
     */
    public Map<String, String> getLocalFiles() {
        return localFiles;
    }
}
