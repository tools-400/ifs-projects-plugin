/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.migration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

import de.tools400.ifsprojects.ibmi.IfsPaths;
import de.tools400.ifsprojects.sources.RpgSqlIncludeDirective;
import de.tools400.ifsprojects.sources.RpgSqlStatements;
import de.tools400.ifsprojects.sources.SourceTypes;
import de.tools400.ifsprojects.sources.headers.SourceHeader;

/**
 * Contents of the build files of TOBi for migrated source files:
 * <ul>
 * <li><code>.ibmi.json</code> of a directory with the CCSID of its source file,
 * like <code>makei cvtsrcpf</code>.</li>
 * <li><code>Rules.mk</code> of the project directory with the directories of
 * the source files.</li>
 * <li>A draft of the <code>Rules.mk</code> of a directory with one rule per
 * member. The object type comes from the create command of the header of the
 * build tool, e.g. <code>STRPREPRC</code> (see {@link SourceHeader}), otherwise
 * from the source type. The copy books of a member are its prerequisites with
 * their paths relative to the project directory, because TOBi passes
 * prerequisites with unknown extensions like <code>rpgleinc</code> unchanged to
 * make, which runs in the project directory. CL programs and panel groups get
 * the recipe, that TOBi 3.4 does not assign itself, CL sources the directory of
 * the source as INCDIR, and sources with an <code>EXEC SQL INCLUDE</code> of a
 * stream file <code>DBGVIEW := *LIST</code>, because the default
 * <code>*SOURCE</code> of TOBi fails for them.</li>
 * </ul>
 */
public final class TobiBuildFiles {

    /** Name of the rules file of TOBi. */
    public static final String RULES_FILE = "Rules.mk"; //$NON-NLS-1$

    /** Name of the directory settings of TOBi. */
    public static final String IBMI_JSON_FILE = ".ibmi.json"; //$NON-NLS-1$

    private static final String LF = "\n"; //$NON-NLS-1$
    private static final String TAB = "\t"; //$NON-NLS-1$
    private static final String JOB_CCSID = "*JOB"; //$NON-NLS-1$

    private static final String MODULE = "MODULE"; //$NON-NLS-1$
    private static final String PGM = "PGM"; //$NON-NLS-1$
    private static final String FILE = "FILE"; //$NON-NLS-1$
    private static final String CMD = "CMD"; //$NON-NLS-1$
    private static final String PNLGRP = "PNLGRP"; //$NON-NLS-1$

    private static final String CL_PROGRAM_RECIPE = "$(PGM.CLLE_TO_PGM_RECIPE)"; //$NON-NLS-1$
    private static final String PANEL_GROUP_RECIPE = "$(PNLGRPSRC_TO_PNLGRP_RECIPE)"; //$NON-NLS-1$

    /** Reason of the DBGVIEW of a program with an EXEC SQL INCLUDE. */
    private static final String DEBUG_VIEW_COMMENT = "# DBGVIEW(*SOURCE) fails for EXEC SQL INCLUDE of a stream file (SQL8009)."; //$NON-NLS-1$

    private TobiBuildFiles() {
        // Utility class
    }

    /**
     * Returns the content of the <code>.ibmi.json</code> file of the directory
     * of a source file.
     *
     * @param aSourceFile - source file
     * @param aHexCcsid - CCSID of the members of a source file with CCSID
     *        65535, with which they have been read
     * @return content
     */
    public static String getIbmiJson(SourceFile aSourceFile, int aHexCcsid) {

        String ccsid;
        if (aSourceFile.isHex()) {
            ccsid = aHexCcsid > 0 ? Integer.toString(aHexCcsid) : JOB_CCSID;
        } else {
            ccsid = Integer.toString(aSourceFile.getCcsid());
        }

        return "{" + LF //$NON-NLS-1$
            + "    \"build\": {" + LF //$NON-NLS-1$
            + "        \"tgtCcsid\": \"" + ccsid + "\"" + LF //$NON-NLS-1$ //$NON-NLS-2$
            + "    }" + LF //$NON-NLS-1$
            + "}" + LF; //$NON-NLS-1$
    }

    /**
     * Returns the content of the <code>Rules.mk</code> file of the project
     * directory.
     *
     * @param aDirectories - directories of the source files
     * @return content
     */
    public static String getProjectRules(Collection<String> aDirectories) {

        StringBuilder rules = new StringBuilder();
        rules.append("# Rules of TOBi (The Object Builder for i), created by the migration of IFS Projects.").append(LF); //$NON-NLS-1$
        rules.append("# The objects are defined in the Rules.mk files of the directories.").append(LF); //$NON-NLS-1$
        rules.append(LF);
        rules.append("SUBDIRS ="); //$NON-NLS-1$
        for (String directory : new TreeSet<>(aDirectories)) {
            rules.append(' ').append(directory);
        }
        rules.append(LF);

        return rules.toString();
    }

    /**
     * Returns the draft of the <code>Rules.mk</code> file of the directory of a
     * source file.
     *
     * @param aSourceFile - source file, e.g. <code>JSMITH#CL/QRPGLESRC</code>
     * @param anItems - migrated members of the source file
     * @return content
     */
    public static String getDirectoryRules(SourceFile aSourceFile, List<MigrationPlan.Item> anItems) {

        StringBuilder rules = new StringBuilder();
        rules.append("# Draft of the rules of TOBi for the members of ").append(aSourceFile).append(", created by the migration") //$NON-NLS-1$ //$NON-NLS-2$
            .append(LF);
        rules.append("# of IFS Projects. Check the object types and set the parameters of the installer or of the").append(LF); //$NON-NLS-1$
        rules.append("# STRPREPRC headers, because the defaults of TOBi differ from the CL commands, e.g.:").append(LF); //$NON-NLS-1$
        rules.append("#   %.MODULE: private DBGVIEW := *LIST").append(LF); //$NON-NLS-1$
        rules.append("#   HELLO.PGM: private ACTGRP := *NEW").append(LF); //$NON-NLS-1$

        List<String> unsupported = new ArrayList<>();
        for (MigrationPlan.Item item : anItems) {
            String rule = getRule(item);
            if (rule == null) {
                unsupported.add(IfsPaths.getFileName(item.getPath()));
            } else if (rule.length() > 0) {
                rules.append(LF).append(rule);
            }
        }

        if (!unsupported.isEmpty()) {
            rules.append(LF).append("# No rule created for:").append(LF); //$NON-NLS-1$
            for (String fileName : unsupported) {
                rules.append("#   ").append(fileName).append(LF); //$NON-NLS-1$
            }
        }

        return rules.toString();
    }

    /**
     * Returns the rule of a member.
     *
     * @return rule, an empty string for copy books, which need no rule, or
     *         <code>null</code>, if no rule can be created
     */
    private static String getRule(MigrationPlan.Item anItem) {

        String fileName = IfsPaths.getFileName(anItem.getPath());
        String sourceType = anItem.getMember().getSourceType().toUpperCase(Locale.ROOT);
        if (!fileName.toUpperCase(Locale.ROOT).endsWith('.' + sourceType)) {
            return ""; // copy book //$NON-NLS-1$
        }

        String objectType = getObjectType(sourceType, anItem.getHeader());
        if (objectType == null) {
            return null;
        }

        String target = anItem.getMember().getName() + '.' + objectType;
        StringBuilder rule = new StringBuilder();
        if (SourceTypes.CLLE.equals(sourceType)) {
            // TOBi compiles CL from a copy in QTEMP, so that INCLUDE finds
            // relative paths only in INCDIR (verified with makei 3.4).
            rule.append(target).append(": private INCDIR := '").append(MigrationMapping.getDirectory(anItem.getMember().getSourceFile())).append("'") //$NON-NLS-1$ //$NON-NLS-2$
                .append(LF);
        } else if (SourceTypes.SQLRPGLE.equals(sourceType) && hasSqlIncludeOfStreamFile(anItem.getContent())) {
            // DBGVIEW(*SOURCE), the default of TOBi, fails for a statement
            // that includes a stream file (verified with makei 3.4).
            rule.append(DEBUG_VIEW_COMMENT).append(LF);
            rule.append(target).append(": private DBGVIEW := *LIST").append(LF); //$NON-NLS-1$
        }
        rule.append(target).append(": ").append(fileName); //$NON-NLS-1$
        for (String copyBook : anItem.getIncludedPaths()) {
            rule.append(' ').append(copyBook);
        }
        rule.append(LF);

        if (PGM.equals(objectType) && SourceTypes.CLLE.equals(sourceType)) {
            rule.append(TAB).append(CL_PROGRAM_RECIPE).append(LF);
        } else if (PNLGRP.equals(objectType)) {
            rule.append(TAB).append(PANEL_GROUP_RECIPE).append(LF);
        }

        return rule.toString();
    }

    /**
     * Returns <code>true</code>, if a source includes a stream file with an
     * <code>EXEC SQL INCLUDE</code> statement, i.e. the statement has a quoted
     * name.
     */
    private static boolean hasSqlIncludeOfStreamFile(String aContent) {

        for (RpgSqlIncludeDirective directive : RpgSqlStatements.find(aContent)) {
            if (directive.isQuoted()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns the object type of a member: from the create command of the
     * header of its build tool, otherwise from its source type.
     *
     * @return object type or <code>null</code>, if no rule is created for the
     *         source type
     */
    private static String getObjectType(String aSourceType, SourceHeader aHeader) {

        if (SourceTypes.RPGLE.equals(aSourceType) || SourceTypes.SQLRPGLE.equals(aSourceType) || SourceTypes.CLLE.equals(aSourceType)) {
            return aHeader.isModule() ? MODULE : PGM;
        }

        if ("DSPF".equals(aSourceType) || "PRTF".equals(aSourceType) || "PF".equals(aSourceType) || "LF".equals(aSourceType)) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            return FILE;
        }

        if (CMD.equals(aSourceType)) {
            return CMD;
        }

        if (PNLGRP.equals(aSourceType)) {
            return PNLGRP;
        }

        return null;
    }
}
