/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.migration;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The compile command of the <code>STRPREPRC</code> header in the first lines
 * of a member, e.g.
 *
 * <pre>
 *  //&gt;&gt;PRE-COMPILER&lt;&lt;
 *  //&gt;&gt;CRTCMD&lt;&lt; CRTSQLRPGI OBJ(&amp;LI/&amp;OB) SRCSTMF(...)
 *  //            INCFILE(&amp;SL/QCPYSRC) RPGPPOPT(*LVL2)
 * </pre>
 *
 * The header is searched as a whole, so that the parameters of continuation
 * lines are found, too.
 */
public final class SourceHeader {

    private static final Pattern CREATE_COMMAND = Pattern.compile(">>CRTCMD<<\\s*(\\w+)", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$
    private static final Pattern MODULE_OBJECT_TYPE = Pattern.compile("OBJTYPE\\(\\s*\\*MODULE\\s*\\)", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$
    private static final Pattern INCLUDE_FILE = Pattern.compile("\\bINCFILE\\(\\s*([^)\\s]+)\\s*\\)", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$

    /** Number of lines, in which the header is searched. */
    private static final int HEADER_LINES = 100;

    /** Suffix of the commands, that create modules, e.g. <code>CRTRPGMOD</code>. */
    private static final String MODULE_COMMAND_SUFFIX = "MOD"; //$NON-NLS-1$

    /** Value of <code>INCFILE</code>, that means the own source file. */
    private static final String SOURCE_FILE = "*SRCFILE"; //$NON-NLS-1$

    /** Prefix of the substitution variables of <code>STRPREPRC</code>. */
    private static final char VARIABLE = '&';

    private static final char LIBRARY_SEPARATOR = '/';
    private static final String LF = "\n"; //$NON-NLS-1$

    private SourceHeader() {
        // Utility class
    }

    /**
     * Returns <code>true</code>, if the create command of the header creates a
     * module, e.g. <code>CRTRPGMOD</code> or
     * <code>CRTSQLRPGI OBJTYPE(*MODULE)</code>.
     *
     * @param aContent - content of the member
     * @return <code>true</code>, if a module is created
     */
    public static boolean isModule(String aContent) {

        String header = getHeader(aContent);

        Matcher matcher = CREATE_COMMAND.matcher(header);
        if (!matcher.find()) {
            return false;
        }

        String command = matcher.group(1).toUpperCase(Locale.ROOT);

        return command.endsWith(MODULE_COMMAND_SUFFIX) || MODULE_OBJECT_TYPE.matcher(header).find();
    }

    /**
     * Returns the source file of the parameter <code>INCFILE</code> of the
     * header, in which the SQL precompiler searches the members of the
     * <code>EXEC SQL INCLUDE</code> statements. A library, that is a
     * substitution variable of <code>STRPREPRC</code>, e.g.
     * <code>&amp;SL</code>, is removed, because it is the library of the
     * member.
     *
     * @param aContent - content of the member
     * @return source file, e.g. <code>QCPYSRC</code> or
     *         <code>*LIBL/QCPYSRC</code>, or <code>null</code>, if the header
     *         has no <code>INCFILE</code> or specifies the own source file
     *         (<code>*SRCFILE</code>)
     */
    public static String getIncludeFile(String aContent) {

        Matcher matcher = INCLUDE_FILE.matcher(getHeader(aContent));
        if (!matcher.find()) {
            return null;
        }

        String file = matcher.group(1).toUpperCase(Locale.ROOT);
        int slash = file.indexOf(LIBRARY_SEPARATOR);
        if (slash >= 0 && slash + 1 < file.length() && file.charAt(0) == VARIABLE) {
            file = file.substring(slash + 1);
        }

        if (SOURCE_FILE.equals(file)) {
            return null;
        }

        return file;
    }

    /**
     * Returns the first lines of a member, in which the header stands.
     */
    private static String getHeader(String aContent) {

        int end = 0;
        for (int i = 0; i < HEADER_LINES && end >= 0; i++) {
            end = aContent.indexOf(LF, end + 1);
        }

        return end < 0 ? aContent : aContent.substring(0, end);
    }
}
