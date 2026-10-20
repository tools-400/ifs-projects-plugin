/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.migration;

/**
 * Source physical file, whose members are migrated.
 */
public final class SourceFile {

    /** CCSID, which means, that the data are not tagged. */
    public static final int CCSID_HEX = 65535;

    private final String library;
    private final String name;
    private final String text;
    private final int ccsid;
    private final int dataLength;
    private final int memberCount;

    /**
     * Creates a source file.
     *
     * @param aLibrary - library
     * @param aName - name of the file
     * @param aText - text of the file or <code>null</code>
     * @param aCcsid - CCSID of the column <code>SRCDTA</code>
     * @param aDataLength - length of the column <code>SRCDTA</code>
     * @param aMemberCount - number of members
     */
    public SourceFile(String aLibrary, String aName, String aText, int aCcsid, int aDataLength, int aMemberCount) {
        this.library = aLibrary;
        this.name = aName;
        this.text = aText;
        this.ccsid = aCcsid;
        this.dataLength = aDataLength;
        this.memberCount = aMemberCount;
    }

    public String getLibrary() {
        return library;
    }

    public String getName() {
        return name;
    }

    /**
     * Returns the text of the file or <code>null</code>.
     */
    public String getText() {
        return text;
    }

    public int getCcsid() {
        return ccsid;
    }

    /**
     * Returns <code>true</code>, if the CCSID is 65535. The members must be
     * copied with another CCSID, because <code>CPYTOSTMF</code> does not
     * convert them (CPDA08C, verified on the IBM i).
     */
    public boolean isHex() {
        return ccsid == CCSID_HEX;
    }

    public int getDataLength() {
        return dataLength;
    }

    public int getMemberCount() {
        return memberCount;
    }

    @Override
    public String toString() {
        return library + '/' + name;
    }
}
