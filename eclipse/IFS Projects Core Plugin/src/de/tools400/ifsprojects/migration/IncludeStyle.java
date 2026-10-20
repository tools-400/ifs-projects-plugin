/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.migration;

/**
 * How the include directives of RPG sources are converted from members to
 * stream files.
 */
public enum IncludeStyle {

    /**
     * The directive refers to the stream file, e.g.
     * <code>/copy 'qcpysrc/consts.rpgleinc'</code>.
     */
    REPLACE,

    /**
     * The directive refers to the stream file, unless the condition
     * {@link #MEMBER_CONDITION} is defined, so that the source can still be
     * compiled as member, e.g. by <code>STRPREPRC</code> with
     * <code>DEFINE(MBR_BUILD)</code>:
     *
     * <pre>
     * /if defined(MBR_BUILD)
     * /copy QCPYSRC,CONSTS
     * /else
     * /copy 'qcpysrc/consts.rpgleinc'
     * /endif
     * </pre>
     *
     * The build from the IFS needs no condition. ILE CL has no conditional
     * compilation, so CL sources are always converted with {@link #REPLACE}.
     */
    CONDITIONAL;

    /** Condition of the member build. */
    public static final String MEMBER_CONDITION = "MBR_BUILD"; //$NON-NLS-1$
}
