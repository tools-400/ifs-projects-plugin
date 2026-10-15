/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.syntaxcheck;

import java.util.Locale;

/**
 * Source types, whose syntax can be checked.
 */
public enum SourceType {

    RPGLE("rpgle"), //$NON-NLS-1$
    SQLRPGLE("sqlrpgle"); //$NON-NLS-1$

    private static final char EXTENSION_SEPARATOR = '.';

    private final String extension;

    private SourceType(String anExtension) {
        this.extension = anExtension;
    }

    /**
     * Returns the source type of a stream file, e.g. {@link #RPGLE} for
     * <code>hello.pgm.rpgle</code>.
     *
     * @param aFileName - name of the stream file
     * @return source type or <code>null</code>, if the syntax of the file
     *         cannot be checked
     */
    public static SourceType fromFileName(String aFileName) {

        if (aFileName == null) {
            return null;
        }

        int index = aFileName.lastIndexOf(EXTENSION_SEPARATOR);
        if (index < 0) {
            return null;
        }

        String extension = aFileName.substring(index + 1).toLowerCase(Locale.ROOT);
        for (SourceType sourceType : values()) {
            if (sourceType.extension.equals(extension)) {
                return sourceType;
            }
        }

        return null;
    }
}
