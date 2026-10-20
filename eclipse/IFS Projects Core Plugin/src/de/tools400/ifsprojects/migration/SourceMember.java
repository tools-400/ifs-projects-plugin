/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.migration;

/**
 * Member of a source file.
 */
public final class SourceMember {

    private final String sourceFile;
    private final String name;
    private final String sourceType;
    private final String text;

    /**
     * Creates a member.
     *
     * @param aSourceFile - name of the source file
     * @param aName - name of the member
     * @param aSourceType - source type or <code>null</code>, if the member has
     *        none
     * @param aText - text of the member or <code>null</code>
     */
    public SourceMember(String aSourceFile, String aName, String aSourceType, String aText) {
        this.sourceFile = aSourceFile;
        this.name = aName;
        this.sourceType = aSourceType;
        this.text = aText;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public String getName() {
        return name;
    }

    /**
     * Returns the source type or <code>null</code>, if the member has none.
     */
    public String getSourceType() {
        return sourceType;
    }

    /**
     * Returns the text of the member or <code>null</code>.
     */
    public String getText() {
        return text;
    }

    /**
     * Returns the key of the member in the source files, see
     * {@link #getKey(String, String)}.
     */
    public String getKey() {
        return getKey(sourceFile, name);
    }

    /**
     * Returns the key of a member in the source files of a library.
     *
     * @param aSourceFile - source file
     * @param aMember - member
     * @return key, e.g. <code>QRPGLESRC(HELLO)</code>
     */
    public static String getKey(String aSourceFile, String aMember) {
        return aSourceFile + '(' + aMember + ')';
    }

    @Override
    public String toString() {
        return getKey() + ' ' + sourceType;
    }
}
