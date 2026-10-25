/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.migration;

/**
 * Settings of a migration of source files.
 */
public final class MigrationSettings {

    private final IncludeStyle includeStyle;
    private final boolean isMemberText;
    private final int hexCcsid;

    /**
     * Creates the settings.
     *
     * @param anIncludeStyle - style of the converted RPG directives
     * @param anIsMemberText - <code>true</code>, if the member text is inserted
     *        as comment
     * @param aHexCcsid - CCSID of the members of source files with CCSID 65535,
     *        see {@link SourceFile#isHex()}
     */
    public MigrationSettings(IncludeStyle anIncludeStyle, boolean anIsMemberText, int aHexCcsid) {
        this.includeStyle = anIncludeStyle;
        this.isMemberText = anIsMemberText;
        this.hexCcsid = aHexCcsid;
    }

    public IncludeStyle getIncludeStyle() {
        return includeStyle;
    }

    /**
     * Returns <code>true</code>, if the member text is inserted as comment, see
     * {@link de.tools400.ifsprojects.sources.MemberText}.
     */
    public boolean isMemberText() {
        return isMemberText;
    }

    /**
     * Returns the CCSID of the members of source files with CCSID 65535.
     */
    public int getHexCcsid() {
        return hexCcsid;
    }
}
