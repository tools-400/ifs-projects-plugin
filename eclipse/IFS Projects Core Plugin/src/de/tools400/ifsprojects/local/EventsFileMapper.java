/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.local;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Replaces the paths of the files of a deploy directory in an events file by
 * their locations in the local project, see {@link SourceMapping}.
 * <p>
 * A FILEID record contains the length of the path, followed by the path, the
 * time stamp and the temporary flag:
 *
 * <pre>
 * FILEID     0 001 000000 047 /home/Jsmith/temp/demo/hello.pgm.rpgle 20261013213315 0
 * </pre>
 *
 * The parser of RDi reads the path with this length and continues it in
 * FILEIDCONT records, if the line is too short. A replaced path is written to a
 * single FILEID record with its new length. The paths of other files, e.g. of
 * system include files, are kept.
 */
public final class EventsFileMapper {

    private static final String LINE_SEPARATOR = "\n"; //$NON-NLS-1$
    private static final String CARRIAGE_RETURN = "\r"; //$NON-NLS-1$
    private static final String BLANK = " "; //$NON-NLS-1$

    /**
     * FILEID record: record type, version, source ID and line, length, rest.
     */
    private static final Pattern FILEID = Pattern.compile("^(FILEID\\s+\\S+\\s+\\S+\\s+\\S+\\s+)(\\d+)(.*)$"); //$NON-NLS-1$

    /** FILEIDCONT record: record type and four fields, rest. */
    private static final Pattern FILEIDCONT = Pattern.compile("^FILEIDCONT\\s+\\S+\\s+\\S+\\s+\\S+\\s+\\S+(.*)$"); //$NON-NLS-1$

    private EventsFileMapper() {
        // Utility class
    }

    /**
     * Replaces the paths of the files of the deploy directory.
     *
     * @param anEvents - content of the events file
     * @param aMapping - mapping of the deploy directory to the local project
     * @return events with replaced paths and the locations of the replaced
     *         paths
     */
    public static Result map(String anEvents, SourceMapping aMapping) {

        String[] lines = anEvents.split(LINE_SEPARATOR, -1);
        StringBuilder buffer = new StringBuilder(anEvents.length());
        Set<String> locations = new LinkedHashSet<>();

        int index = 0;
        while (index < lines.length) {

            int count = 1;
            String mappedLine = null;

            Matcher matcher = FILEID.matcher(stripCarriageReturn(lines[index]));
            if (matcher.matches()) {
                int length = Integer.parseInt(matcher.group(2));
                StringBuilder rest = new StringBuilder(matcher.group(3).trim());
                while (rest.length() < length && index + count < lines.length) {
                    Matcher continuation = FILEIDCONT.matcher(stripCarriageReturn(lines[index + count]));
                    if (!continuation.matches()) {
                        break;
                    }
                    rest.append(continuation.group(1).trim());
                    count++;
                }

                if (rest.length() >= length) {
                    String location = aMapping.toProjectLocation(rest.substring(0, length));
                    if (location != null) {
                        String tail = rest.substring(length);
                        if (tail.length() > 0 && !tail.startsWith(BLANK)) {
                            tail = BLANK + tail;
                        }
                        mappedLine = matcher.group(1) + formatLength(location.length(), matcher.group(2).length()) + BLANK + location + tail;
                        if (lines[index].endsWith(CARRIAGE_RETURN)) {
                            mappedLine = mappedLine + CARRIAGE_RETURN;
                        }
                        locations.add(location);
                    }
                }
            }

            if (mappedLine == null) {
                count = 1;
                append(buffer, lines[index], index, lines.length);
            } else {
                append(buffer, mappedLine, index + count - 1, lines.length);
            }

            index += count;
        }

        return new Result(buffer.toString(), locations);
    }

    private static void append(StringBuilder aBuffer, String aLine, int anIndex, int aCount) {

        aBuffer.append(aLine);
        if (anIndex < aCount - 1) {
            aBuffer.append(LINE_SEPARATOR);
        }
    }

    private static String stripCarriageReturn(String aLine) {

        if (aLine.endsWith(CARRIAGE_RETURN)) {
            return aLine.substring(0, aLine.length() - 1);
        }

        return aLine;
    }

    private static String formatLength(int aLength, int aWidth) {

        StringBuilder buffer = new StringBuilder(Integer.toString(aLength));
        while (buffer.length() < aWidth) {
            buffer.insert(0, '0');
        }

        return buffer.toString();
    }

    /**
     * Events with replaced paths.
     */
    public static final class Result {

        private final String events;
        private final Set<String> locations;

        private Result(String anEvents, Set<String> aLocations) {
            this.events = anEvents;
            this.locations = Collections.unmodifiableSet(aLocations);
        }

        /**
         * Returns the content of the events file with replaced paths.
         */
        public String getEvents() {
            return events;
        }

        /**
         * Returns the locations of the replaced paths, e.g.
         * <code>/demo/src/hello.rpgle</code>.
         */
        public Set<String> getLocations() {
            return locations;
        }
    }
}
