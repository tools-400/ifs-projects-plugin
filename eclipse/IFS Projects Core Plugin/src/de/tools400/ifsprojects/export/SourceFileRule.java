/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.export;

/**
 * Rules, that determine the source file of an exported stream file, see
 * {@link ExportMapping}.
 */
public enum SourceFileRule {

    /**
     * Name of the directory of the stream file, e.g. <code>QRPGLESRC</code>
     * for <code>qrpglesrc/hello.pgm.rpgle</code>. Stream files in the project
     * directory and in directories, whose names are not valid source file
     * names, are exported by {@link #SOURCE_TYPE}.
     */
    DIRECTORY,

    /**
     * Default source file of the source type, e.g. <code>QRPGLESRC</code> for
     * <code>RPGLE</code>, see {@link ExportMapping#getDefaultSourceFile}.
     */
    SOURCE_TYPE,

    /** One source file for all stream files. */
    SINGLE_FILE;
}
