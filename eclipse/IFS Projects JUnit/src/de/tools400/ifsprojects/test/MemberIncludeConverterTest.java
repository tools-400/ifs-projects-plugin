/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import de.tools400.ifsprojects.migration.IncludeStyle;
import de.tools400.ifsprojects.migration.MemberIncludeConverter;
import de.tools400.ifsprojects.sources.IncludeConverter.Include;
import de.tools400.ifsprojects.sources.IncludeConverter.Problem;
import de.tools400.ifsprojects.sources.IncludeConverter.Result;

/**
 * The converted forms have been verified on the IBM i: RPG
 * <code>/copy</code> relative to the current directory (also nested and in
 * fixed format), CL <code>INCLUDE SRCSTMF()</code> relative to the including
 * file and <code>INCLUDE SRCMBR()</code> without <code>SRCFILE</code>.
 */
public class MemberIncludeConverterTest {

    private static final String LIBRARY = "JSMITH#CL";

    @Test
    public void testRpg_free() {

        Result result = convert("RPGLE", "QRPGLESRC(HELLO)", IncludeStyle.REPLACE, "**FREE\n" //
            + "/copy QCPYSRC,CONSTS\n" //
            + "  /include qcpysrc,consts // comment\n" //
            + "/COPY LOCAL\n" //
            + "/copy JSMITH#CL/QCPYSRC,CONSTS\n" //
            + "/copy *LIBL/QCPYSRC,CONSTS\n" //
            + "/copy OTHERLIB/QCPYSRC,CONSTS\n" //
            + "/copy QOTHER,X\n" //
            + "/copy QCPYSRC,MISSING\n" //
            + "/copy QRPGLESRC,NOTYPE\n" //
            + "/copy 'qcpysrc/consts.rpgleinc'\n" //
            + "/copy inc/consts.rpgleinc\n" //
            + "exec sql include SQLCA;\n" //
            + "exec sql include HOSTVARS;\n");

        assertEquals("**FREE\n" //
            + "/copy 'qcpysrc/consts.rpgleinc'\n" //
            + "  /include 'qcpysrc/consts.rpgleinc' // comment\n" //
            + "/COPY 'qrpglesrc/local.rpgleinc'\n" //
            + "/copy 'qcpysrc/consts.rpgleinc'\n" //
            + "/copy 'qcpysrc/consts.rpgleinc'\n" //
            + "/copy OTHERLIB/QCPYSRC,CONSTS\n" //
            + "/copy QOTHER,X\n" //
            + "/copy QCPYSRC,MISSING\n" //
            + "/copy QRPGLESRC,NOTYPE\n" //
            + "/copy 'qcpysrc/consts.rpgleinc'\n" //
            + "/copy inc/consts.rpgleinc\n" //
            + "exec sql include SQLCA;\n" //
            + "exec sql include HOSTVARS;\n", result.getContent());

        assertEquals(10, result.getIncludes().size());
        assertInclude(result.getIncludes().get(0), 2, "QCPYSRC,CONSTS", "qcpysrc/consts.rpgleinc", null);
        assertInclude(result.getIncludes().get(1), 3, "qcpysrc,consts", "qcpysrc/consts.rpgleinc", null);
        assertInclude(result.getIncludes().get(2), 4, "LOCAL", "qrpglesrc/local.rpgleinc", null);
        assertInclude(result.getIncludes().get(3), 5, "JSMITH#CL/QCPYSRC,CONSTS", "qcpysrc/consts.rpgleinc", null);
        assertInclude(result.getIncludes().get(4), 6, "*LIBL/QCPYSRC,CONSTS", "qcpysrc/consts.rpgleinc", null);
        assertInclude(result.getIncludes().get(5), 7, "OTHERLIB/QCPYSRC,CONSTS", null, Problem.OTHER_LIBRARY);
        assertInclude(result.getIncludes().get(6), 8, "QOTHER,X", null, Problem.NOT_TRANSFERRED);
        assertInclude(result.getIncludes().get(7), 9, "QCPYSRC,MISSING", null, Problem.NOT_FOUND);
        assertInclude(result.getIncludes().get(8), 10, "QRPGLESRC,NOTYPE", null, Problem.NOT_TRANSFERRED);
        assertInclude(result.getIncludes().get(9), 14, "HOSTVARS", null, Problem.NOT_SUPPORTED);
    }

    @Test
    public void testRpg_fixed() {

        String source = "     H DFTACTGRP(*NO)\n" //
            + pad("      /COPY QCPYSRC,CONSTS", 80) + "COMMENT\n" //
            + "      * /COPY QCPYSRC,CONSTS\n" //
            + pad("", 55) + "/COPY QCPYSRC,CONSTS\n" //
            + "**CTDATA X\n" //
            + "      /COPY QCPYSRC,CONSTS\n";

        Result result = convert("RPGLE", "QRPGLESRC(HELLO)", IncludeStyle.REPLACE, source);

        assertEquals("     H DFTACTGRP(*NO)\n" //
            + pad("      /COPY 'qcpysrc/consts.rpgleinc'", 80) + "COMMENT\n" //
            + "      * /COPY QCPYSRC,CONSTS\n" //
            + pad("", 55) + "/COPY QCPYSRC,CONSTS\n" //
            + "**CTDATA X\n" //
            + "      /COPY QCPYSRC,CONSTS\n", result.getContent());

        assertEquals(2, result.getIncludes().size());
        assertInclude(result.getIncludes().get(0), 2, "QCPYSRC,CONSTS", "qcpysrc/consts.rpgleinc", null);
        assertInclude(result.getIncludes().get(1), 4, "QCPYSRC,CONSTS", null, Problem.TOO_LONG);
    }

    @Test
    public void testRpg_conditional() {

        Result result = convert("SQLRPGLE", "QRPGLESRC(HELLO)", IncludeStyle.CONDITIONAL, "**FREE\n/copy QCPYSRC,CONSTS\n");

        assertEquals("**FREE\n" //
            + "/if defined(MBR_BUILD)\n" //
            + "/copy QCPYSRC,CONSTS\n" //
            + "/else\n" //
            + "/copy 'qcpysrc/consts.rpgleinc'\n" //
            + "/endif\n", result.getContent());

        result = convert("RPGLE", "QRPGLESRC(HELLO)", IncludeStyle.CONDITIONAL, pad("      /COPY QCPYSRC,CONSTS", 80) + "COMMENT\n");

        assertEquals("      /IF DEFINED(MBR_BUILD)\n" //
            + pad("      /COPY QCPYSRC,CONSTS", 80) + "\n" //
            + "      /ELSE\n" //
            + "      /COPY 'qcpysrc/consts.rpgleinc'\n" //
            + pad("      /ENDIF", 80) + "COMMENT\n", result.getContent());
    }

    @Test
    public void testCl() {

        Result result = convert("CLLE", "QCLSRC(START)", IncludeStyle.CONDITIONAL, "             PGM\n" //
            + "             INCLUDE    SRCMBR(VARS)\n" //
            + "             INCLUDE    SRCMBR(VARS) SRCFILE(*INCFILE) /* same */\n" //
            + "             include    srcmbr(clvars) srcfile(qcpysrc)\n" //
            + " LBL:        INCLUDE    SRCFILE(JSMITH#CL/QCPYSRC) SRCMBR(CLVARS)\n" //
            + "             INCLUDE    SRCMBR(CLVARS) SRCFILE(OTHERLIB/QCPYSRC)\n" //
            + "             INCLUDE    SRCSTMF('vars.clleinc')\n" //
            + "             INCLUDE    VARS\n" //
            + "             INCLUDE    SRCMBR(VARS) +\n" //
            + "                          SRCFILE(QCLSRC)\n" //
            + "             ENDPGM\n");

        assertEquals("             PGM\n" //
            + "             INCLUDE    SRCSTMF('vars.clleinc')\n" //
            + "             INCLUDE    SRCSTMF('vars.clleinc') /* same */\n" //
            + "             include    srcstmf('../qcpysrc/clvars.clleinc')\n" //
            + " LBL:        INCLUDE    SRCSTMF('../qcpysrc/clvars.clleinc')\n" //
            + "             INCLUDE    SRCMBR(CLVARS) SRCFILE(OTHERLIB/QCPYSRC)\n" //
            + "             INCLUDE    SRCSTMF('vars.clleinc')\n" //
            + "             INCLUDE    VARS\n" //
            + "             INCLUDE    SRCMBR(VARS) +\n" //
            + "                          SRCFILE(QCLSRC)\n" //
            + "             ENDPGM\n", result.getContent());

        assertEquals(7, result.getIncludes().size());
        assertInclude(result.getIncludes().get(0), 2, "VARS", "vars.clleinc", null);
        assertInclude(result.getIncludes().get(1), 3, "VARS", "vars.clleinc", null);
        assertInclude(result.getIncludes().get(2), 4, "clvars", "../qcpysrc/clvars.clleinc", null);
        assertInclude(result.getIncludes().get(3), 5, "CLVARS", "../qcpysrc/clvars.clleinc", null);
        assertInclude(result.getIncludes().get(4), 6, "CLVARS", null, Problem.OTHER_LIBRARY);
        assertInclude(result.getIncludes().get(5), 8, "VARS", null, Problem.NOT_SUPPORTED);
        assertInclude(result.getIncludes().get(6), 9, "VARS", null, Problem.NOT_SUPPORTED);
    }

    @Test
    public void testCreate() {
        assertNull(MemberIncludeConverter.create("DSPF", LIBRARY, getPaths(), getSourceFiles(), IncludeStyle.REPLACE));
        assertNull(MemberIncludeConverter.create(null, LIBRARY, getPaths(), getSourceFiles(), IncludeStyle.REPLACE));
    }

    @Test
    public void testGetIncludedMembers() {

        MemberIncludeConverter converter = MemberIncludeConverter.create("RPGLE", LIBRARY, getPaths(), getSourceFiles(), IncludeStyle.REPLACE);
        converter.convert("QRPGLESRC(HELLO)", "**FREE\n/copy QCPYSRC,CONSTS\n/copy QCPYSRC,MISSING\n");
        converter.convert("QRPGLESRC(LOCAL)", "**FREE\n/copy LOCAL\n");

        assertEquals(new HashSet<String>(Arrays.asList("QCPYSRC(CONSTS)", "QRPGLESRC(LOCAL)")), converter.getIncludedMembers());
    }

    private static Result convert(String aSourceType, String aKey, IncludeStyle aStyle, String aContent) {
        return MemberIncludeConverter.create(aSourceType, LIBRARY, getPaths(), getSourceFiles(), aStyle).convert(aKey, aContent);
    }

    private static Map<String, String> getPaths() {

        Map<String, String> paths = new HashMap<String, String>();
        paths.put("QRPGLESRC(HELLO)", "qrpglesrc/hello.rpgle");
        paths.put("QRPGLESRC(LOCAL)", "qrpglesrc/local.rpgleinc");
        paths.put("QRPGLESRC(NOTYPE)", null);
        paths.put("QCPYSRC(CONSTS)", "qcpysrc/consts.rpgleinc");
        paths.put("QCPYSRC(CLVARS)", "qcpysrc/clvars.clleinc");
        paths.put("QCLSRC(START)", "qclsrc/start.clle");
        paths.put("QCLSRC(VARS)", "qclsrc/vars.clleinc");

        return paths;
    }

    private static Set<String> getSourceFiles() {
        return new HashSet<String>(Arrays.asList("QRPGLESRC", "QCPYSRC", "QCLSRC"));
    }

    private static void assertInclude(Include anInclude, int aLineNumber, String aName, String aTarget, Problem aProblem) {
        assertEquals(aLineNumber, anInclude.getLineNumber());
        assertEquals(aName, anInclude.getName());
        assertEquals(aTarget, anInclude.getTarget());
        assertEquals(aProblem, anInclude.getProblem());
    }

    private static String pad(String aText, int aLength) {
        StringBuilder buffer = new StringBuilder(aText);
        while (buffer.length() < aLength) {
            buffer.append(' ');
        }
        return buffer.toString();
    }
}
