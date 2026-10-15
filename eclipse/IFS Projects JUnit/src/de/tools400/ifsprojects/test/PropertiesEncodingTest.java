/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;

import org.junit.Test;

/**
 * Verifies, that the message files are encoded in ISO-8859-1, which is the
 * encoding Java and Eclipse NLS use for properties files. A file saved as UTF-8
 * shows broken umlauts, e.g. "gepr&Atilde;&frac14;ft" instead of
 * "gepr&uuml;ft".
 */
public class PropertiesEncodingTest {

    private static final String[] RESOURCES = { "/de/tools400/ifsprojects/messages.properties", "/de/tools400/ifsprojects/messages_de.properties" };

    @Test
    public void testMessagesAreNotUtf8() throws Exception {

        for (String resource : RESOURCES) {
            byte[] bytes = read(resource);
            if (containsNonAscii(bytes) && isValidUtf8(bytes)) {
                fail("File is encoded in UTF-8, but must be encoded in ISO-8859-1: " + resource);
            }
        }
    }

    private boolean containsNonAscii(byte[] aBytes) {

        for (byte b : aBytes) {
            if ((b & 0xFF) > 127) {
                return true;
            }
        }

        return false;
    }

    private boolean isValidUtf8(byte[] aBytes) {

        try {
            Charset.forName("UTF-8").newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(aBytes));
            return true;
        } catch (CharacterCodingException e) {
            return false;
        }
    }

    private byte[] read(String aResource) throws Exception {

        InputStream inputStream = getClass().getResourceAsStream(aResource);
        assertNotNull("Resource not found: " + aResource, inputStream);
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] bytes = new byte[4096];
            int count;
            while ((count = inputStream.read(bytes)) >= 0) {
                buffer.write(bytes, 0, count);
            }
            return buffer.toByteArray();
        } finally {
            inputStream.close();
        }
    }
}
