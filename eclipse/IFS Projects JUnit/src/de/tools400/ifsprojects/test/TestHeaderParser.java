/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.test;

import java.util.ArrayList;
import java.util.List;

import de.tools400.ifsprojects.sources.headers.CreateCommand;
import de.tools400.ifsprojects.sources.headers.ISourceHeaderParser;

/**
 * Parser of a fictitious build tool for the tests of the migration: a create
 * command stands behind <code>BUILD:</code> in a line of the source, e.g.
 * <code>// BUILD: CRTRPGMOD MODULE(&amp;LI/&amp;OB)</code>. The rest of the
 * line is the command; characters, that are no keyword parameter, e.g. the end
 * of a CL comment, are ignored by {@link CreateCommand#parse(String)}.
 * <p>
 * The tests of the migration need a header, not a build tool. The parsers of
 * the real build tools are plug-ins of their own and are tested in fragments of
 * those, e.g. <code>IFS Projects STRPREPRC JUnit</code>: this fragment must not
 * depend on them, because the dependencies of a fragment belong to its host and
 * the plug-ins of the build tools require the core plug-in themselves - the
 * update site would not build (cycle in the classpath).
 */
public final class TestHeaderParser implements ISourceHeaderParser {

    private static final String TAG = "BUILD:";

    public List<CreateCommand> parse(String aContent, String aSourceType) {

        List<CreateCommand> commands = new ArrayList<CreateCommand>();
        for (String line : aContent.split("\n")) {
            int tag = line.indexOf(TAG);
            if (tag >= 0) {
                CreateCommand command = CreateCommand.parse(line.substring(tag + TAG.length()));
                if (command != null) {
                    commands.add(command);
                }
            }
        }

        return commands;
    }
}
