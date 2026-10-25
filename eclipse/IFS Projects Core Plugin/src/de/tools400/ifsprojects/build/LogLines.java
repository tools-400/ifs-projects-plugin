/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.build;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.util.ArrayList;
import java.util.List;

/**
 * Splits the output of a build, which is read in chunks, into lines.
 * <p>
 * Lines are decoded as UTF-8, e.g. the output of TOBi. Lines, that are no valid
 * UTF-8, are decoded as ISO-8859-1, e.g. the messages of the
 * <code>system</code> command in a job with a single byte CCSID.
 */
public final class LogLines {

    private static final Charset UTF_8 = Charset.forName("UTF-8"); //$NON-NLS-1$
    private static final Charset ISO_8859_1 = Charset.forName("ISO-8859-1"); //$NON-NLS-1$
    private static final byte LINE_FEED = '\n';
    private static final byte CARRIAGE_RETURN = '\r';

    private final ByteArrayOutputStream incompleteLine = new ByteArrayOutputStream();

    /**
     * Adds a chunk of the output.
     *
     * @param aBytes - bytes
     * @return the lines completed by the chunk
     */
    public List<String> add(byte[] aBytes) {

        List<String> lines = new ArrayList<>();
        for (byte b : aBytes) {
            if (b == LINE_FEED) {
                lines.add(decode(incompleteLine.toByteArray()));
                incompleteLine.reset();
            } else {
                incompleteLine.write(b);
            }
        }

        return lines;
    }

    /**
     * Returns the last line, if it is not terminated by a line feed.
     *
     * @return line or <code>null</code>
     */
    public String flush() {

        if (incompleteLine.size() == 0) {
            return null;
        }

        String line = decode(incompleteLine.toByteArray());
        incompleteLine.reset();

        return line;
    }

    private static String decode(byte[] aBytes) {

        int length = aBytes.length;
        if (length > 0 && aBytes[length - 1] == CARRIAGE_RETURN) {
            length--;
        }

        try {
            return UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(aBytes, 0, length)).toString();
        } catch (CharacterCodingException e) {
            return new String(aBytes, 0, length, ISO_8859_1);
        }
    }
}
