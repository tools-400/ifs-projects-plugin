/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.ibm.as400.access.CharConverter;

import de.tools400.ifsprojects.export.SourceCheck;

/**
 * The lengths are those of CPYFRMSTMF, verified on the IBM i with a source
 * file with 10 characters per line.
 */
public class SourceCheckTest {

    @Test
    public void testGetLength() {
        assertEquals(10, SourceCheck.getLength("1234567890"));
        assertEquals(10, SourceCheck.getLength("1234567890   "));
        assertEquals(10, SourceCheck.getLength("1234567890\t \t"));
        assertEquals(9, SourceCheck.getLength("ab\tc"));
        assertEquals(17, SourceCheck.getLength("ab\t\tc"));
        assertEquals(17, SourceCheck.getLength("abcdefgh\tc"));
        assertEquals(10, SourceCheck.getLength("\u00E4\u00F6\u00FC\u00E4\u00F6\u00FC\u00E4\u00F6\u00FC\u00E4"));
        assertEquals(10, SourceCheck.getLength("123456789\uD83D\uDE00"));
        assertEquals(0, SourceCheck.getLength("\t"));
        assertEquals(0, SourceCheck.getLength(""));
    }

    @Test
    public void testGetLongestLine() {

        SourceCheck.Line line = SourceCheck.getLongestLine("a\nabc\r\nab\rabc\n");
        assertEquals(2, line.getNumber());
        assertEquals(3, line.getLength());

        line = SourceCheck.getLongestLine("");
        assertEquals(0, line.getNumber());
        assertEquals(0, line.getLength());
    }

    @Test
    public void testFindUnmappableLine() throws Exception {

        String text = "**FREE\n// Gr\u00FC\u00DFe\n// 5 \u20AC\n";
        assertEquals(3, SourceCheck.findUnmappableLine(text, new CharConverter(273)));
        assertEquals(0, SourceCheck.findUnmappableLine(text, new CharConverter(1141)));
        assertEquals(3, SourceCheck.findUnmappableLine(text, new CharConverter(37)));
        assertEquals(2, SourceCheck.findUnmappableLine("// x\n// \u0141\n", new CharConverter(1141)));
        assertEquals(1, SourceCheck.findUnmappableLine("// \uD83D\uDE00", new CharConverter(1141)));
        assertEquals(0, SourceCheck.findUnmappableLine("", new CharConverter(1141)));
    }

    /**
     * CRTSRCPF accepts the single-byte and mixed EBCDIC CCSIDs, 1208 and 65535
     * and rejects the other ASCII and Unicode CCSIDs (verified on the IBM i).
     * Only single-byte EBCDIC CCSIDs are supported.
     */
    @Test
    public void testIsSingleByteEbcdic() {
        for (int ccsid : new int[] { 1141, 273, 37, 500, 1140, 1148, 290 }) {
            assertTrue(Integer.toString(ccsid), SourceCheck.isSingleByteEbcdic(ccsid));
        }
        for (int ccsid : new int[] { 930, 1399, 5035, 1252, 819, 1208, 1200, 13488, 367, 850, 65535, 999, 0 }) {
            assertFalse(Integer.toString(ccsid), SourceCheck.isSingleByteEbcdic(ccsid));
        }
    }

    @Test
    public void testRemoveByteOrderMark() {
        assertEquals("**FREE", SourceCheck.removeByteOrderMark("\uFEFF**FREE"));
        assertEquals("**FREE", SourceCheck.removeByteOrderMark("**FREE"));
        assertEquals("", SourceCheck.removeByteOrderMark(""));
    }
}
