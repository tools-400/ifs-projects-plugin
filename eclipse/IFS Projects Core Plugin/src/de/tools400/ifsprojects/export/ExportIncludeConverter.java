/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.export;

import java.util.Map;

import de.tools400.ifsprojects.sources.IncludeConverter;
import de.tools400.ifsprojects.sources.SourceTypes;

/**
 * Converts the include directives of a source, that refer to stream files, so
 * that they refer to the members of the export, e.g.
 * <code>/copy 'inc/consts.rpgleinc'</code> to <code>/copy inc,consts</code>.
 * Directives, that refer to members, are not changed.
 */
public abstract class ExportIncludeConverter extends IncludeConverter {

    private final IncludeResolver resolver;
    private final Map<String, ExportMapping.Entry> members;

    /**
     * Creates a converter.
     *
     * @param aResolver - resolver, that finds the files of the directives
     * @param aMembers - exported files by their path relative to the project
     *        directory
     */
    ExportIncludeConverter(IncludeResolver aResolver, Map<String, ExportMapping.Entry> aMembers) {
        this.resolver = aResolver;
        this.members = aMembers;
    }

    /**
     * Returns the converter of a source type.
     *
     * @param aSourceType - source type, see {@link SourceTypes#getSourceType}
     * @param aResolver - resolver, that finds the files of the directives
     * @param aMembers - exported files by their path relative to the project
     *        directory
     * @return converter or <code>null</code>, if the includes of the source
     *         type are not converted
     */
    public static ExportIncludeConverter create(String aSourceType, IncludeResolver aResolver, Map<String, ExportMapping.Entry> aMembers) {

        if (SourceTypes.RPGLE.equals(aSourceType) || SourceTypes.SQLRPGLE.equals(aSourceType)) {
            return new RpgIncludeConverter(aResolver, aMembers);
        }

        if (SourceTypes.CLLE.equals(aSourceType)) {
            return new ClIncludeConverter(aResolver, aMembers);
        }

        return null;
    }

    /**
     * Returns the resolver of the include directives.
     */
    IncludeResolver getResolver() {
        return resolver;
    }

    /**
     * Returns the member, into which a file is exported.
     *
     * @param aPath - path relative to the project directory
     * @return member or <code>null</code>, if the file is not exported
     */
    ExportMapping.Entry getMember(String aPath) {
        return members.get(aPath);
    }
}
