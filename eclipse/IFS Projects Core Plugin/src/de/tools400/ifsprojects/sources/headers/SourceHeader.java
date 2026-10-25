/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.sources.headers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * The commands of the header of a source, that a build tool runs to create the
 * object of the source, e.g. of a <code>STRPREPRC</code> header:
 *
 * <pre>
 * // &gt;&gt;PRE-COMPILER&lt;&lt;
 * // &gt;&gt;CRTCMD&lt;&lt; CRTSQLRPGI OBJ(&amp;LI/&amp;OB) SRCSTMF(...)
 * // INCFILE(&amp;SL/QCPYSRC) RPGPPOPT(*LVL2)
 * // &gt;&gt;END-PRE-COMPILER&lt;&lt;
 * </pre>
 *
 * The commands are found by the parser of the build tool, see
 * {@link ISourceHeaderParser}; this class knows the CL commands, that all build
 * tools have in common.
 */
public final class SourceHeader {

    /** Header of a source, that has none. */
    public static final SourceHeader NONE = new SourceHeader(Collections.<CreateCommand> emptyList());

    /**
     * Suffix of the commands, that create modules, e.g. <code>CRTRPGMOD</code>.
     */
    private static final String MODULE_COMMAND_SUFFIX = "MOD"; //$NON-NLS-1$

    private static final String OBJECT_TYPE = "OBJTYPE"; //$NON-NLS-1$
    private static final String MODULE_OBJECT_TYPE = "*MODULE"; //$NON-NLS-1$

    private static final String INCLUDE_FILE = "INCFILE"; //$NON-NLS-1$

    /** Value of <code>INCFILE</code>, that means the own source file. */
    private static final String SOURCE_FILE = "*SRCFILE"; //$NON-NLS-1$

    private static final char LIBRARY_SEPARATOR = '/';

    private final List<CreateCommand> commands;
    private final CreateCommand createCommand;

    /**
     * Creates the header from the commands of a parser.
     *
     * @param aCommands - commands in the order of the source
     */
    public SourceHeader(List<CreateCommand> aCommands) {
        this.commands = Collections.unmodifiableList(new ArrayList<>(aCommands));
        this.createCommand = findCreateCommand(this.commands);
    }

    /**
     * Returns <code>true</code>, if the source has no header.
     */
    public boolean isEmpty() {
        return commands.isEmpty();
    }

    /**
     * Returns the commands of the header in the order of the source.
     */
    public List<CreateCommand> getCommands() {
        return commands;
    }

    /**
     * Returns the command, that creates the object of the source: the last
     * command, whose name starts with <code>CRT</code>, because a header, that
     * runs several commands, creates the modules of a program before it binds
     * them. Commands behind it, e.g. <code>CHGOBJOWN</code>, do not create the
     * object.
     *
     * @return command or <code>null</code>, if the header creates no object
     */
    public CreateCommand getCreateCommand() {
        return createCommand;
    }

    /**
     * Returns <code>true</code>, if the create command of the header creates a
     * module, e.g. <code>CRTRPGMOD</code> or
     * <code>CRTSQLRPGI OBJTYPE(*MODULE)</code>.
     */
    public boolean isModule() {

        if (createCommand == null) {
            return false;
        }

        return createCommand.getName().endsWith(MODULE_COMMAND_SUFFIX)
            || MODULE_OBJECT_TYPE.equalsIgnoreCase(createCommand.getParameter(OBJECT_TYPE));
    }

    /**
     * Returns the source file of the parameter <code>INCFILE</code> of the
     * create command, in which the SQL precompiler searches the members of the
     * <code>EXEC SQL INCLUDE</code> statements. A library, that is a
     * substitution variable of the build tool, e.g. <code>&amp;SL</code>, is
     * removed, because it is the library of the member.
     *
     * @return source file, e.g. <code>QCPYSRC</code> or
     *         <code>*LIBL/QCPYSRC</code>, or <code>null</code>, if the header
     *         has no <code>INCFILE</code> or specifies the own source file
     *         (<code>*SRCFILE</code>)
     */
    public String getIncludeFile() {

        if (createCommand == null) {
            return null;
        }

        String value = createCommand.getParameter(INCLUDE_FILE);
        if (value == null) {
            return null;
        }

        String file = value.toUpperCase(Locale.ROOT);
        int slash = file.indexOf(LIBRARY_SEPARATOR);
        if (slash >= 0 && slash + 1 < file.length() && CreateCommand.isVariable(file)) {
            file = file.substring(slash + 1);
        }

        if (SOURCE_FILE.equals(file)) {
            return null;
        }

        return file;
    }

    @Override
    public String toString() {
        return commands.toString();
    }

    private static CreateCommand findCreateCommand(List<CreateCommand> aCommands) {

        for (int i = aCommands.size() - 1; i >= 0; i--) {
            if (aCommands.get(i).isCreateCommand()) {
                return aCommands.get(i);
            }
        }

        return null;
    }
}
