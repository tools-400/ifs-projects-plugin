/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.sources;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Encodings of text files: the CCSIDs of the stream files, that contain text in
 * an encoding of Java, and the encoding, that an XML file declares in its first
 * line, e.g. <code>&lt;?xml version="1.0" encoding="ISO-8859-1"?&gt;</code>.
 * <p>
 * A stream file of an XML file may be tagged with another CCSID than its
 * encoding, e.g. a file with ISO-8859-1, that has been copied into the IFS with
 * CCSID 1208. Therefore {@link #decode(byte[], Charset)} uses the declared
 * encoding, if the bytes are not valid in the encoding of the CCSID.
 */
public final class TextEncoding {

    /** CCSID of UTF-8. */
    public static final int CCSID_UTF8 = 1208;

    /** Character, that replaces bytes, that are not valid in an encoding. */
    public static final char REPLACEMENT_CHARACTER = '\uFFFD';

    private static final Pattern XML_ENCODING = Pattern
        .compile("^\uFEFF?<\\?xml\\s[^>]*?encoding\\s*=\\s*[\"']([A-Za-z][A-Za-z0-9._:-]*)[\"']"); //$NON-NLS-1$

    /** Number of bytes, in which the XML declaration is searched. */
    private static final int XML_DECLARATION_LENGTH = 200;

    /** Encodings and CCSIDs of single-byte ASCII and UTF-8 stream files. */
    private static final String[] ENCODINGS = { "UTF-8", "ISO-8859-1", "ISO-8859-15", "windows-1252", "US-ASCII" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
    private static final int[] CCSIDS = { CCSID_UTF8, 819, 923, 1252, 367 };

    private TextEncoding() {
        // Utility class
    }

    /**
     * Returns the CCSID of a stream file, that contains text of an encoding.
     *
     * @param anEncoding - name or alias of the encoding, e.g.
     *        <code>ISO-8859-1</code>, <code>latin1</code> or
     *        <code>Cp1252</code>
     * @return CCSID, e.g. 819, or 0, if the encoding is not supported
     */
    public static int getCcsid(String anEncoding) {

        Charset charset = forName(anEncoding);
        if (charset == null) {
            return 0;
        }

        for (int i = 0; i < ENCODINGS.length; i++) {
            if (ENCODINGS[i].equals(charset.name())) {
                return CCSIDS[i];
            }
        }

        return 0;
    }

    /**
     * Returns the encoding of the text of a stream file with a CCSID.
     *
     * @param aCcsid - CCSID, e.g. 1208
     * @return encoding or <code>null</code>, if it is not supported, e.g.
     *         for EBCDIC
     */
    public static Charset getCharset(int aCcsid) {

        for (int i = 0; i < CCSIDS.length; i++) {
            if (CCSIDS[i] == aCcsid) {
                return forName(ENCODINGS[i]);
            }
        }

        return null;
    }

    /**
     * Returns the encoding, that the XML declaration in the first line of a
     * text names.
     *
     * @param aText - text
     * @return name of the encoding, e.g. <code>ISO-8859-1</code>, or
     *         <code>null</code>, if the text has no XML declaration with an
     *         encoding
     */
    public static String getXmlEncoding(String aText) {

        if (aText == null) {
            return null;
        }

        Matcher matcher = XML_ENCODING.matcher(aText.length() > XML_DECLARATION_LENGTH ? aText.substring(0, XML_DECLARATION_LENGTH) : aText);
        if (!matcher.find()) {
            return null;
        }

        return matcher.group(1);
    }

    /**
     * Returns <code>true</code>, if all characters of a text exist in an
     * encoding.
     *
     * @param aText - text
     * @param anEncoding - name of the encoding
     * @return <code>true</code>, if the encoding is supported and contains all
     *         characters
     */
    public static boolean canEncode(String aText, String anEncoding) {
        Charset charset = forName(anEncoding);
        return charset != null && charset.newEncoder().canEncode(aText);
    }

    /**
     * Decodes the bytes of a text file. If the bytes are not valid in the
     * encoding, e.g. because a file with ISO-8859-1 has been tagged with CCSID
     * 1208, the encoding of the XML declaration is used, if it decodes the
     * bytes. Otherwise the invalid bytes are replaced by
     * {@link #REPLACEMENT_CHARACTER}, so that no other character is lost.
     *
     * @param aBytes - bytes of the file
     * @param aCharset - encoding of the file, e.g. of the CCSID of the stream
     *        file
     * @return text
     */
    public static String decode(byte[] aBytes, Charset aCharset) {

        String text = decodeStrictly(aBytes, aCharset);
        if (text != null) {
            return text;
        }

        int length = Math.min(aBytes.length, XML_DECLARATION_LENGTH);
        Charset declared = forName(getXmlEncoding(new String(aBytes, 0, length, StandardCharsets.ISO_8859_1)));
        if (declared != null && !declared.equals(aCharset)) {
            text = decodeStrictly(aBytes, declared);
            if (text != null) {
                return text;
            }
        }

        return new String(aBytes, aCharset);
    }

    private static String decodeStrictly(byte[] aBytes, Charset aCharset) {

        try {
            return aCharset.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(aBytes)).toString();
        } catch (CharacterCodingException e) {
            return null;
        }
    }

    private static Charset forName(String anEncoding) {

        if (anEncoding == null) {
            return null;
        }

        try {
            return Charset.forName(anEncoding);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
