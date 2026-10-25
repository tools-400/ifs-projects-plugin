/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.errorlist;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Processes the content of events files (EVFEVENT), as written by the IBM i
 * compilers with <code>OPTION(*EVENTF)</code>.
 * <p>
 * Each record starts with its type, e.g.
 * <code>ERROR 0 001 1 000006 000006 001 000006 012 RNF7030 S 30 050 The name ...</code>.
 * The text of a long message may continue on the following lines, which do not
 * start with a record type.
 */
public final class EventsFiles {

    /** Name of the events file created by the compilers. */
    public static final String EVENTS_FILE = "EVFEVENT"; //$NON-NLS-1$

    private static final String ERROR = "ERROR"; //$NON-NLS-1$
    private static final Set<String> RECORD_TYPES = new HashSet<>(Arrays.asList("TIMESTAMP", "PROCESSOR", "FILEID", "FILEIDCONT", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        "FILEEND", ERROR, "EXPANSION", "PROGRAM", "MAPDEFINE", "MAPSTART", "MAPEND", "FEEDBACK")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
    private static final int MESSAGE_ID_INDEX = 9;
    private static final String LINE_SEPARATOR_PATTERN = "\\r?\\n"; //$NON-NLS-1$
    private static final String WHITE_SPACE_PATTERN = "\\s+"; //$NON-NLS-1$
    private static final String LINE_SEPARATOR = "\n"; //$NON-NLS-1$

    private EventsFiles() {
        // Utility class
    }

    /**
     * Removes the error records of the specified messages.
     *
     * @param aContent - content of an events file
     * @param aMessageIds - IDs of the messages that are removed, e.g. RNF1324
     * @return content without the error records of the messages
     */
    public static String removeMessages(String aContent, Set<String> aMessageIds) {

        if (aContent == null || aMessageIds == null || aMessageIds.isEmpty()) {
            return aContent;
        }

        StringBuilder buffer = new StringBuilder();
        boolean isRemoving = false;
        for (String line : aContent.split(LINE_SEPARATOR_PATTERN)) {

            String[] tokens = line.trim().split(WHITE_SPACE_PATTERN);
            if (RECORD_TYPES.contains(tokens[0])) {
                isRemoving = ERROR.equals(tokens[0]) && tokens.length > MESSAGE_ID_INDEX && aMessageIds.contains(tokens[MESSAGE_ID_INDEX]);
            }

            if (!isRemoving) {
                buffer.append(line).append(LINE_SEPARATOR);
            }
        }

        return buffer.toString();
    }
}
