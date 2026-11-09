/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.export;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import de.tools400.ifsprojects.ibmi.ClCommands;
import de.tools400.ifsprojects.sources.MemberText;
import de.tools400.ifsprojects.utils.StringUtils;

/**
 * Settings of the export of a project. They are saved per project, so that the
 * next export starts with the values of the last one.
 */
public final class ExportSettings {

    /** Length of the sequence number and the date of a source record. */
    public static final int SEQUENCE_AND_DATE_LENGTH = 12;

    public static final int MIN_RECORD_LENGTH = SEQUENCE_AND_DATE_LENGTH + 1;
    public static final int MAX_RECORD_LENGTH = 32766;
    public static final int MIN_CCSID = 1;
    public static final int MAX_CCSID = 65533;

    /**
     * Source file of {@link SourceFileRule#SINGLE_FILE}, until one is entered.
     */
    public static final String DEFAULT_SOURCE_FILE = "QSOURCE"; //$NON-NLS-1$

    private static final String LIBRARY = "library"; //$NON-NLS-1$
    private static final String SOURCE_FILE_RULE = "sourceFileRule"; //$NON-NLS-1$
    private static final String SOURCE_FILE = "sourceFile"; //$NON-NLS-1$
    private static final String CCSID = "ccsid"; //$NON-NLS-1$
    private static final String RECORD_LENGTH = "recordLength"; //$NON-NLS-1$
    private static final String MEMBER_TEXT = "memberText"; //$NON-NLS-1$
    private static final String FURTHER_SOURCE_TYPES = "furtherSourceTypes"; //$NON-NLS-1$

    /** Separators of the further source types, when they are entered. */
    private static final String SOURCE_TYPE_SEPARATORS = "[,;\\s]+"; //$NON-NLS-1$
    private static final String SOURCE_TYPE_SEPARATOR = ","; //$NON-NLS-1$
    private static final String FORMATTED_SOURCE_TYPE_SEPARATOR = ", "; //$NON-NLS-1$

    private String library;
    private SourceFileRule sourceFileRule;
    private String sourceFile;
    private int ccsid;
    private int recordLength;
    private boolean isMemberText;
    private boolean isConvertIncludes;
    private List<String> furtherSourceTypes;

    /**
     * Creates the settings of a project, that has not been exported before.
     *
     * @param aLibrary - library or <code>null</code>
     * @param aCcsid - CCSID of new source files
     * @param aRecordLength - minimum record length of new source files
     */
    public ExportSettings(String aLibrary, int aCcsid, int aRecordLength) {
        this.library = aLibrary;
        this.sourceFileRule = SourceFileRule.DIRECTORY;
        this.sourceFile = DEFAULT_SOURCE_FILE;
        this.ccsid = aCcsid;
        this.recordLength = aRecordLength;
        this.isMemberText = true;
        this.isConvertIncludes = true;
        this.furtherSourceTypes = Collections.emptyList();
    }

    /**
     * Restores saved settings. Missing or invalid values are replaced by the
     * values of the defaults.
     *
     * @param aValues - values, see {@link #toMap()}
     * @param aDefaults - defaults
     * @return settings
     */
    public static ExportSettings fromMap(Map<String, String> aValues, ExportSettings aDefaults) {

        ExportSettings settings = new ExportSettings(aDefaults.library, aDefaults.ccsid, aDefaults.recordLength);

        String library = StringUtils.trimToNull(aValues.get(LIBRARY));
        if (library != null) {
            settings.library = library.toUpperCase(Locale.ROOT);
        }

        try {
            settings.sourceFileRule = SourceFileRule.valueOf(aValues.get(SOURCE_FILE_RULE));
        } catch (RuntimeException e) {
            // Keep the default
        }

        String sourceFile = StringUtils.trimToNull(aValues.get(SOURCE_FILE));
        if (sourceFile != null) {
            settings.sourceFile = sourceFile.toUpperCase(Locale.ROOT);
        }

        settings.ccsid = getInt(aValues.get(CCSID), MIN_CCSID, MAX_CCSID, aDefaults.ccsid);
        settings.recordLength = getInt(aValues.get(RECORD_LENGTH), MIN_RECORD_LENGTH, MAX_RECORD_LENGTH, aDefaults.recordLength);

        if (aValues.containsKey(MEMBER_TEXT)) {
            settings.isMemberText = Boolean.parseBoolean(aValues.get(MEMBER_TEXT));
        }

        String furtherSourceTypes = aValues.get(FURTHER_SOURCE_TYPES);
        if (furtherSourceTypes != null && parseSourceTypes(furtherSourceTypes) != null) {
            settings.furtherSourceTypes = parseSourceTypes(furtherSourceTypes);
        }

        return settings;
    }

    /**
     * Parses source types, that have been entered, e.g. <code>tbl, xml</code>.
     * They are separated by commas, semicolons or blanks; a leading period of
     * an extension is ignored.
     *
     * @param aText - source types
     * @return source types in uppercase without duplicates, an empty list for
     *         an empty text, or <code>null</code>, if a source type is not a
     *         valid name
     */
    public static List<String> parseSourceTypes(String aText) {

        Set<String> sourceTypes = new LinkedHashSet<>();
        for (String value : aText.trim().split(SOURCE_TYPE_SEPARATORS)) {
            if (value.length() == 0) {
                continue;
            }
            String sourceType = (value.startsWith(".") ? value.substring(1) : value).toUpperCase(Locale.ROOT); //$NON-NLS-1$
            if (!ClCommands.isObjectName(sourceType)) {
                return null;
            }
            sourceTypes.add(sourceType);
        }

        return Collections.unmodifiableList(new ArrayList<>(sourceTypes));
    }

    /**
     * Formats source types for the input, as extensions in lowercase, e.g.
     * <code>tbl, xml</code>.
     *
     * @param aSourceTypes - source types
     * @return text
     */
    public static String formatSourceTypes(List<String> aSourceTypes) {

        StringBuilder text = new StringBuilder();
        for (String sourceType : aSourceTypes) {
            if (text.length() > 0) {
                text.append(FORMATTED_SOURCE_TYPE_SEPARATOR);
            }
            text.append(sourceType.toLowerCase(Locale.ROOT));
        }

        return text.toString();
    }

    /**
     * Returns the values, that are saved.
     *
     * @return values by key
     */
    public Map<String, String> toMap() {

        Map<String, String> values = new LinkedHashMap<>();
        if (library != null) {
            values.put(LIBRARY, library);
        }
        values.put(SOURCE_FILE_RULE, sourceFileRule.name());
        values.put(SOURCE_FILE, sourceFile);
        values.put(CCSID, Integer.toString(ccsid));
        values.put(RECORD_LENGTH, Integer.toString(recordLength));
        values.put(MEMBER_TEXT, Boolean.toString(isMemberText));

        StringBuilder sourceTypes = new StringBuilder();
        for (String sourceType : furtherSourceTypes) {
            if (sourceTypes.length() > 0) {
                sourceTypes.append(SOURCE_TYPE_SEPARATOR);
            }
            sourceTypes.append(sourceType);
        }
        values.put(FURTHER_SOURCE_TYPES, sourceTypes.toString());

        return values;
    }

    /**
     * Returns the library, into which the project is exported.
     *
     * @return library in uppercase or <code>null</code>
     */
    public String getLibrary() {
        return library;
    }

    public void setLibrary(String aLibrary) {
        this.library = aLibrary == null ? null : aLibrary.trim().toUpperCase(Locale.ROOT);
    }

    public SourceFileRule getSourceFileRule() {
        return sourceFileRule;
    }

    public void setSourceFileRule(SourceFileRule aSourceFileRule) {
        this.sourceFileRule = aSourceFileRule;
    }

    /**
     * Returns the source file of {@link SourceFileRule#SINGLE_FILE}.
     */
    public String getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(String aSourceFile) {
        this.sourceFile = aSourceFile == null ? null : aSourceFile.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * Returns the CCSID of new source files.
     */
    public int getCcsid() {
        return ccsid;
    }

    public void setCcsid(int aCcsid) {
        this.ccsid = aCcsid;
    }

    /**
     * Returns the minimum record length of new source files, including sequence
     * number and date.
     */
    public int getRecordLength() {
        return recordLength;
    }

    public void setRecordLength(int aRecordLength) {
        this.recordLength = aRecordLength;
    }

    /**
     * Returns <code>true</code>, if the member text is taken from the
     * <code>%TEXT</code> comment of the source, see {@link MemberText}.
     */
    public boolean isMemberText() {
        return isMemberText;
    }

    public void setMemberText(boolean anIsMemberText) {
        this.isMemberText = anIsMemberText;
    }

    /**
     * Returns <code>true</code>, if the include directives are converted, see
     * {@link ExportIncludeConverter}. There is no option for it yet.
     */
    public boolean isConvertIncludes() {
        return isConvertIncludes;
    }

    public void setConvertIncludes(boolean anIsConvertIncludes) {
        this.isConvertIncludes = anIsConvertIncludes;
    }

    /**
     * Returns the source types, that are exported in addition to the source
     * types of TOBi, see {@link de.tools400.ifsprojects.sources.SourceTypes},
     * e.g. <code>TBL</code> for <code>qddssrc/table.tbl</code>. The migration
     * writes a member of any source type as a stream file with the source type
     * as extension.
     *
     * @return source types in uppercase
     */
    public List<String> getFurtherSourceTypes() {
        return furtherSourceTypes;
    }

    /**
     * Sets the further source types.
     *
     * @param aSourceTypes - source types, see
     *        {@link #parseSourceTypes(String)}
     */
    public void setFurtherSourceTypes(List<String> aSourceTypes) {

        List<String> sourceTypes = new ArrayList<>();
        for (String sourceType : aSourceTypes) {
            String upperCase = sourceType.toUpperCase(Locale.ROOT);
            if (!sourceTypes.contains(upperCase)) {
                sourceTypes.add(upperCase);
            }
        }

        this.furtherSourceTypes = Collections.unmodifiableList(sourceTypes);
    }

    /**
     * Parses an integer in a range.
     *
     * @param aValue - value or <code>null</code>
     * @param aMinimum - minimum
     * @param aMaximum - maximum
     * @param aDefault - default
     * @return value or default, if the value is not an integer in the range
     */
    public static int getInt(String aValue, int aMinimum, int aMaximum, int aDefault) {

        Integer value = parseInt(aValue, aMinimum, aMaximum);
        if (value == null) {
            return aDefault;
        }

        return value.intValue();
    }

    /**
     * Parses an integer in a range.
     *
     * @param aValue - value or <code>null</code>
     * @param aMinimum - minimum
     * @param aMaximum - maximum
     * @return value or <code>null</code>, if the value is not an integer in the
     *         range
     */
    public static Integer parseInt(String aValue, int aMinimum, int aMaximum) {

        if (aValue == null) {
            return null;
        }

        try {
            int value = Integer.parseInt(aValue.trim());
            if (value < aMinimum || value > aMaximum) {
                return null;
            }
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
