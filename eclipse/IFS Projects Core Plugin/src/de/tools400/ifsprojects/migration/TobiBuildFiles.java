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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import de.tools400.ifsprojects.ibmi.IfsPaths;
import de.tools400.ifsprojects.project.BuildTool;
import de.tools400.ifsprojects.sources.RpgSqlIncludeDirective;
import de.tools400.ifsprojects.sources.RpgSqlStatements;
import de.tools400.ifsprojects.sources.SourceTypes;
import de.tools400.ifsprojects.sources.headers.SourceHeader;

/**
 * Contents of the build files of TOBi for migrated source files:
 * <ul>
 * <li><code>.ibmi.json</code> of a directory with the CCSID of its source file,
 * like <code>makei cvtsrcpf</code>, and the version of the format, which the
 * schema of TOBi requires.</li>
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
 * <code>*SOURCE</code> of TOBi fails for them. A binding source becomes a
 * service program with the module of the same name, if there is one. The rule
 * of a member with <code>$</code> or <code>#</code> in the name is commented
 * out, with <code>#</code> escaped as <code>\#</code>, because TOBi 3.5 does
 * not build the targets with a recipe, if such a target has none; copy books
 * with <code>$</code> or <code>#</code> cannot be prerequisites and are named in
 * a comment.</li>
 * </ul>
 */
public final class TobiBuildFiles {

    /** Name of the rules file of TOBi. */
    public static final String RULES_FILE = "Rules.mk"; //$NON-NLS-1$

    /** Name of the directory settings of TOBi. */
    public static final String IBMI_JSON_FILE = ".ibmi.json"; //$NON-NLS-1$

    /**
     * Version of the format of <code>.ibmi.json</code>, which the schema of
     * TOBi requires and its documentation uses.
     */
    private static final String IBMI_JSON_VERSION = "0.0.1"; //$NON-NLS-1$

    private static final String LF = "\n"; //$NON-NLS-1$
    private static final String TAB = "\t"; //$NON-NLS-1$
    private static final String JOB_CCSID = "*JOB"; //$NON-NLS-1$

    /**
     * Character, that starts a comment in make, and its escaped form, with
     * which TOBi 3.4 and later accept it in the names of targets and sources.
     */
    private static final String HASH = "#"; //$NON-NLS-1$
    private static final String ESCAPED_HASH = "\\#"; //$NON-NLS-1$

    private static final String MODULE = "MODULE"; //$NON-NLS-1$
    private static final String PGM = "PGM"; //$NON-NLS-1$
    private static final String SRVPGM = "SRVPGM"; //$NON-NLS-1$
    private static final String FILE = "FILE"; //$NON-NLS-1$
    private static final String CMD = "CMD"; //$NON-NLS-1$
    private static final String PNLGRP = "PNLGRP"; //$NON-NLS-1$
    private static final String BND = "BND"; //$NON-NLS-1$

    private static final String CL_PROGRAM_RECIPE = "$(PGM.CLLE_TO_PGM_RECIPE)"; //$NON-NLS-1$
    private static final String PANEL_GROUP_RECIPE = "$(PNLGRPSRC_TO_PNLGRP_RECIPE)"; //$NON-NLS-1$

    /** Reason of the DBGVIEW of a program with an EXEC SQL INCLUDE. */
    private static final String DEBUG_VIEW_COMMENT = "# DBGVIEW(*SOURCE) fails for EXEC SQL INCLUDE of a stream file (SQL8009)."; //$NON-NLS-1$

    /**
     * Hint of the rules of members with <code>$</code> or <code>#</code> in the
     * name, which are commented out (verified with makei 3.5).
     */
    private static final String[] MAKE_CHARACTERS_RULES_COMMENT = {
        "# Rules of members with $ or # in the name, which make interprets. TOBi 3.4 and later accept", //$NON-NLS-1$
        "# them in names (# written as \\#), but TOBi 3.5 does not build the targets with a recipe, if a", //$NON-NLS-1$
        "# target with $ or # has none. Check the rules, before you remove the comment characters." }; //$NON-NLS-1$

    /** Characters, that comment a line of a rule out. */
    private static final String COMMENT_PREFIX = "# "; //$NON-NLS-1$

    /** Hint of the copy books, that cannot be prerequisites. */
    private static final String MAKE_CHARACTERS_COMMENT = "# Copy books with $ or # cannot be prerequisites in TOBi:"; //$NON-NLS-1$

    /** Hint of a service program, whose modules are not known. */
    private static final String SERVICE_PROGRAM_COMMENT = //
        "# Add the modules of the service program, e.g. MODULE1.MODULE MODULE2.MODULE."; //$NON-NLS-1$

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
            + "    \"version\": \"" + IBMI_JSON_VERSION + "\"," + LF //$NON-NLS-1$ //$NON-NLS-2$
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
     * @param aMigratedItems - all migrated members, which determine the
     *        modules of the service programs, see
     *        {@link #getServiceProgramModules(Collection)}
     * @return content
     */
    public static String getDirectoryRules(SourceFile aSourceFile, List<MigrationPlan.Item> anItems, Collection<MigrationPlan.Item> aMigratedItems) {

        Set<String> bindingSources = getBindingSources(aMigratedItems);
        Set<String> modules = getServiceProgramModules(aMigratedItems);

        StringBuilder rules = new StringBuilder();
        rules.append("# Draft of the rules of TOBi for the members of ").append(aSourceFile).append(", created by the migration") //$NON-NLS-1$ //$NON-NLS-2$
            .append(LF);
        rules.append("# of IFS Projects. Check the object types and set the parameters of the installer or of the").append(LF); //$NON-NLS-1$
        rules.append("# STRPREPRC headers, because the defaults of TOBi differ from the CL commands, e.g.:").append(LF); //$NON-NLS-1$
        rules.append("#   %.MODULE: private DBGVIEW := *LIST").append(LF); //$NON-NLS-1$
        rules.append("#   HELLO.PGM: private ACTGRP := *NEW").append(LF); //$NON-NLS-1$

        List<String> unsupported = new ArrayList<>();
        StringBuilder makeCharacterRules = new StringBuilder();
        for (MigrationPlan.Item item : anItems) {
            String rule = getRule(item, bindingSources, modules);
            if (rule == null) {
                unsupported.add(IfsPaths.getFileName(item.getPath()));
            } else if (rule.length() > 0 && BuildTool.hasMakeCharacters(item.getMember().getName())) {
                makeCharacterRules.append(LF).append(commentOut(rule));
            } else if (rule.length() > 0) {
                rules.append(LF).append(rule);
            }
        }

        if (makeCharacterRules.length() > 0) {
            rules.append(LF);
            for (String line : MAKE_CHARACTERS_RULES_COMMENT) {
                rules.append(line).append(LF);
            }
            rules.append(makeCharacterRules);
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
     * Comments the lines of a rule out, except of the comments.
     *
     * @param aRule - lines of the rule, each ended by a line feed
     * @return rule, whose lines start with <code>#</code>
     */
    private static String commentOut(String aRule) {

        StringBuilder rule = new StringBuilder();
        for (String line : aRule.split(LF)) {
            if (!line.startsWith(HASH)) {
                rule.append(COMMENT_PREFIX);
            }
            rule.append(line).append(LF);
        }

        return rule.toString();
    }

    /**
     * Returns the names of the members with binding source (<code>BND</code>),
     * which become service programs.
     *
     * @param aMigratedItems - migrated members
     * @return names in uppercase
     */
    private static Set<String> getBindingSources(Collection<MigrationPlan.Item> aMigratedItems) {

        Set<String> names = new HashSet<>();
        for (MigrationPlan.Item item : aMigratedItems) {
            if (BND.equalsIgnoreCase(item.getMember().getSourceType())) {
                names.add(item.getMember().getName().toUpperCase(Locale.ROOT));
            }
        }

        return names;
    }

    /**
     * Returns the names of the RPG and CL members, that have the name of a
     * binding source and therefore become the module of its service program,
     * like the rules of TOBi do it for a service program with one module, e.g.
     * <code>UTILS.SRVPGM: utils.bnd UTILS.MODULE</code>.
     *
     * @param aMigratedItems - migrated members
     * @return names in uppercase
     */
    private static Set<String> getServiceProgramModules(Collection<MigrationPlan.Item> aMigratedItems) {

        Set<String> bindingSources = getBindingSources(aMigratedItems);

        Set<String> names = new HashSet<>();
        for (MigrationPlan.Item item : aMigratedItems) {
            String name = item.getMember().getName().toUpperCase(Locale.ROOT);
            if (bindingSources.contains(name) && isProgramSource(item.getMember().getSourceType()) && !isCopyBook(item)) {
                names.add(name);
            }
        }

        return names;
    }

    /**
     * Returns <code>true</code>, if a member has the extension of a copy book,
     * i.e. the extension is not its source type, see {@link MigrationMapping}.
     */
    private static boolean isCopyBook(MigrationPlan.Item anItem) {
        String sourceType = anItem.getMember().getSourceType().toUpperCase(Locale.ROOT);
        return !IfsPaths.getFileName(anItem.getPath()).toUpperCase(Locale.ROOT).endsWith('.' + sourceType);
    }

    /**
     * Returns <code>true</code>, if a source type is compiled to a program or a
     * module.
     */
    private static boolean isProgramSource(String aSourceType) {
        String sourceType = aSourceType.toUpperCase(Locale.ROOT);
        return SourceTypes.RPGLE.equals(sourceType) || SourceTypes.SQLRPGLE.equals(sourceType) || SourceTypes.CLLE.equals(sourceType);
    }

    /**
     * Returns a name of a target or of a source, as TOBi 3.4 and later accept
     * it in a rule: a <code>#</code>, which starts a comment in make, is
     * escaped as <code>\#</code>; a <code>$</code> is accepted as it is.
     *
     * @param aName - name, e.g. <code>#INIT.PGM</code>
     * @return name, e.g. <code>\#INIT.PGM</code>
     */
    private static String escape(String aName) {
        return aName.replace(HASH, ESCAPED_HASH);
    }

    /**
     * Returns the rule of a member.
     *
     * @param anItem - member
     * @param aBindingSources - names of the binding sources, see
     *        {@link #getBindingSources(Collection)}
     * @param aModules - names of the modules of the service programs, see
     *        {@link #getServiceProgramModules(Collection)}
     * @return rule, an empty string for copy books, which need no rule, or
     *         <code>null</code>, if no rule can be created
     */
    private static String getRule(MigrationPlan.Item anItem, Set<String> aBindingSources, Set<String> aModules) {

        if (isCopyBook(anItem)) {
            return ""; //$NON-NLS-1$
        }

        String fileName = IfsPaths.getFileName(anItem.getPath());
        String name = anItem.getMember().getName().toUpperCase(Locale.ROOT);
        String sourceType = anItem.getMember().getSourceType().toUpperCase(Locale.ROOT);

        String objectType = getObjectType(sourceType, anItem.getHeader(), aBindingSources.contains(name));
        if (objectType == null) {
            return null;
        }

        String target = escape(anItem.getMember().getName() + '.' + objectType);
        StringBuilder rule = new StringBuilder();
        if (SRVPGM.equals(objectType) && !aModules.contains(name)) {
            rule.append(SERVICE_PROGRAM_COMMENT).append(LF);
        }
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

        // TOBi replaces $ and # of a prerequisite, that is no object, by
        // DOLLARESCAPE_ and HASHESCAPE_, so that make does not find the copy
        // book (verified with makei 3.5).
        StringBuilder copyBooks = new StringBuilder();
        StringBuilder omittedCopyBooks = new StringBuilder();
        for (String copyBook : anItem.getIncludedPaths()) {
            if (BuildTool.hasMakeCharacters(copyBook)) {
                omittedCopyBooks.append(' ').append(copyBook);
            } else {
                copyBooks.append(' ').append(copyBook);
            }
        }
        if (omittedCopyBooks.length() > 0) {
            rule.append(MAKE_CHARACTERS_COMMENT).append(omittedCopyBooks).append(LF);
        }

        rule.append(target).append(": ").append(escape(fileName)); //$NON-NLS-1$
        if (SRVPGM.equals(objectType) && aModules.contains(name)) {
            rule.append(' ').append(escape(anItem.getMember().getName() + '.' + MODULE));
        }
        rule.append(copyBooks).append(LF);

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
     * header of its build tool, otherwise from its source type. An RPG or CL
     * member with the name of a binding source is the module of its service
     * program.
     *
     * @param aSourceType - source type in uppercase
     * @param aHeader - header of the build tool
     * @param anIsBindingSourceName - <code>true</code>, if a binding source has
     *        the name of the member
     * @return object type or <code>null</code>, if no rule is created for the
     *         source type
     */
    private static String getObjectType(String aSourceType, SourceHeader aHeader, boolean anIsBindingSourceName) {

        if (isProgramSource(aSourceType)) {
            return aHeader.isModule() || anIsBindingSourceName ? MODULE : PGM;
        }

        if (BND.equals(aSourceType)) {
            return SRVPGM;
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
