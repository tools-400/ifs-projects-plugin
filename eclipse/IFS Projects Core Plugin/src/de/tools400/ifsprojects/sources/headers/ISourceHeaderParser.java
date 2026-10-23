/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.sources.headers;

import java.util.List;

/**
 * Reads the commands of the header of a build tool, e.g. of
 * <code>STRPREPRC</code> or of the <code>BUILD</code> command of Scott
 * Klement. Parsers are contributed via extension point
 * <code>de.tools400.ifsprojects.core.plugin.sourceHeaderParsers</code>; the
 * parser with the highest priority, that finds a header, wins, see
 * {@link SourceHeaderParsers}.
 * <p>
 * A parser only finds the commands, it does not interpret them: where the
 * header stands, which characters start a comment of the source, how a command
 * is continued and where it ends is specific to the build tool, the CL commands
 * themselves are not. The parser therefore returns each command as one line,
 * without the comment characters and with its continuation lines joined; the
 * common part, i.e. the object type and the parameters of the command, is
 * computed by {@link SourceHeader}.
 * <p>
 * The substitution variables of the build tool, e.g. <code>&amp;OB</code>, stay
 * in the commands. They have to start with <code>&amp;</code>, see
 * {@link CreateCommand#VARIABLE}, so that a library, that is a variable, can be
 * recognized, e.g. in <code>INCFILE(&amp;SL/QCPYSRC)</code>.
 */
public interface ISourceHeaderParser {

    /**
     * Returns the commands of the header of a source.
     *
     * @param aContent - content of the source with the line separator
     *        <code>\n</code>
     * @param aSourceType - source type of the source, e.g. <code>RPGLE</code>,
     *        or <code>null</code>, if it is unknown
     * @return commands in the order of the source or an empty list, if the
     *         source has no header of this build tool
     */
    List<CreateCommand> parse(String aContent, String aSourceType);
}
