/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.project;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Environment variables of an IFS project. The variables are system specific
 * and are therefore not stored in the project, but in the IFS project filter.
 * Default variables for all projects are stored in the preferences. They are
 * exported before the build and compile commands are executed.
 * <p>
 * The variables are edited as text with one variable per line:
 * <code>NAME=value</code>. Empty lines and lines starting with <code>#</code>
 * are ignored.
 * <p>
 * TOBi resolves <code>&amp;NAME</code> in some attributes of
 * <code>iproj.json</code>, e.g. in <code>setIBMiEnvCmd</code>, with the value
 * of the environment variable <code>NAME</code>.
 */
public final class EnvironmentVariables {

    /** Attributes of <code>iproj.json</code>, in which TOBi resolves variables. */
    public static final String[] IPROJ_VARIABLE_KEYS = { IProjConfig.OBJLIB, IProjConfig.CURLIB, IProjConfig.PRE_USRLIBL,
        IProjConfig.POST_USRLIBL, IProjConfig.INCLUDE_PATH, IProjConfig.SET_IBMI_ENV_CMD };

    private static final String NAME_PATTERN = "[A-Za-z_][A-Za-z0-9_]*"; //$NON-NLS-1$
    private static final String LINE_SEPARATOR_PATTERN = "\\r?\\n"; //$NON-NLS-1$
    private static final String LINE_SEPARATOR = "\n"; //$NON-NLS-1$
    private static final String TOKEN_SEPARATOR_PATTERN = "[/ ]"; //$NON-NLS-1$
    private static final String COMMENT = "#"; //$NON-NLS-1$
    private static final char ASSIGNMENT = '=';
    private static final char VARIABLE_PREFIX = '&';

    private EnvironmentVariables() {
        // Utility class
    }

    /**
     * Returns <code>true</code>, if the name can be used as name of a shell
     * variable.
     *
     * @param aName - name of the variable
     * @return <code>true</code> for valid names
     */
    public static boolean isValidName(String aName) {
        return aName != null && aName.matches(NAME_PATTERN);
    }

    /**
     * Returns the number of the first invalid line of the text.
     *
     * @param aText - variables, one per line: <code>NAME=value</code>
     * @return line number (1-based) or 0, if all lines are valid
     */
    public static int findInvalidLine(String aText) {

        Set<String> names = new HashSet<String>();

        String[] lines = splitLines(aText);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (isIgnored(line)) {
                continue;
            }
            int index = line.indexOf(ASSIGNMENT);
            if (index < 0) {
                return i + 1;
            }
            String name = line.substring(0, index).trim();
            if (!isValidName(name) || !names.add(name)) {
                return i + 1;
            }
            if (containsLineBreak(line.substring(index + 1))) {
                return i + 1;
            }
        }

        return 0;
    }

    /**
     * Parses the variables of a text.
     *
     * @param aText - variables, one per line: <code>NAME=value</code>
     * @return variables in the order of the text
     * @throws IllegalArgumentException if the text contains an invalid line
     */
    public static Map<String, String> parse(String aText) {

        int invalidLine = findInvalidLine(aText);
        if (invalidLine > 0) {
            throw new IllegalArgumentException("Invalid environment variable in line " + invalidLine); //$NON-NLS-1$
        }

        Map<String, String> variables = new LinkedHashMap<String, String>();
        for (String line : splitLines(aText)) {
            if (isIgnored(line)) {
                continue;
            }
            int index = line.indexOf(ASSIGNMENT);
            variables.put(line.substring(0, index).trim(), line.substring(index + 1));
        }

        return variables;
    }

    /**
     * Formats variables as text.
     *
     * @param aVariables - variables
     * @return variables, one per line: <code>NAME=value</code>
     */
    public static String format(Map<String, String> aVariables) {

        StringBuilder buffer = new StringBuilder();
        for (Map.Entry<String, String> entry : aVariables.entrySet()) {
            if (buffer.length() > 0) {
                buffer.append(LINE_SEPARATOR);
            }
            buffer.append(entry.getKey()).append(ASSIGNMENT).append(entry.getValue() == null ? "" : entry.getValue()); //$NON-NLS-1$
        }

        return buffer.toString();
    }

    /**
     * Merges the default variables of the preferences with the variables of an
     * IFS project. A variable of the project overrides the default variable of
     * the same name, even if its value is empty.
     *
     * @param aDefaults - default variables or <code>null</code>
     * @param aVariables - variables of the project or <code>null</code>
     * @return the default variables followed by the other variables of the
     *         project, never <code>null</code>
     */
    public static Map<String, String> merge(Map<String, String> aDefaults, Map<String, String> aVariables) {

        Map<String, String> variables = new LinkedHashMap<String, String>();
        if (aDefaults != null) {
            variables.putAll(aDefaults);
        }

        if (aVariables != null) {
            variables.putAll(aVariables);
        }

        return variables;
    }

    /**
     * Returns the names of the variables, that are used in the attributes of
     * <code>iproj.json</code>, in which TOBi resolves variables. Like TOBi, a
     * variable is a word starting with <code>&amp;</code>, that is separated
     * by blanks or slashes, e.g. <code>CALL &amp;LIB/SETUP</code>.
     *
     * @param aConfig - content of <code>iproj.json</code>
     * @return names of the variables in the order of their first usage
     */
    public static Set<String> findReferences(IProjConfig aConfig) {

        Set<String> names = new LinkedHashSet<String>();

        for (String key : IPROJ_VARIABLE_KEYS) {
            for (String value : getValues(aConfig, key)) {
                for (String token : value.split(TOKEN_SEPARATOR_PATTERN)) {
                    if (token.length() > 1 && token.charAt(0) == VARIABLE_PREFIX) {
                        names.add(token.substring(1));
                    }
                }
            }
        }

        return names;
    }

    /**
     * Replaces the variables of a value of <code>iproj.json</code> by their
     * values, as TOBi does. A variable is a word starting with
     * <code>&amp;</code>, that is separated by blanks or slashes, e.g.
     * <code>CALL &amp;LIB/SETUP</code>. Undefined variables are kept.
     *
     * @param aValue - value of an attribute of <code>iproj.json</code>
     * @param aVariables - environment variables
     * @return value with resolved variables
     */
    public static String resolveReferences(String aValue, Map<String, String> aVariables) {

        if (aValue == null || aVariables == null || aVariables.isEmpty()) {
            return aValue;
        }

        StringBuilder buffer = new StringBuilder();
        int start = 0;
        for (int i = 0; i <= aValue.length(); i++) {
            if (i == aValue.length() || isTokenSeparator(aValue.charAt(i))) {
                String token = aValue.substring(start, i);
                String name = token.length() > 1 && token.charAt(0) == VARIABLE_PREFIX ? token.substring(1) : null;
                if (name != null && aVariables.containsKey(name)) {
                    String value = aVariables.get(name);
                    buffer.append(value == null ? "" : value); //$NON-NLS-1$
                } else {
                    buffer.append(token);
                }
                if (i < aValue.length()) {
                    buffer.append(aValue.charAt(i));
                }
                start = i + 1;
            }
        }

        return buffer.toString();
    }

    private static boolean isTokenSeparator(char aChar) {
        return aChar == ' ' || aChar == '/';
    }

    private static List<String> getValues(IProjConfig aConfig, String aKey) {

        List<String> values = aConfig.getStringList(aKey);
        if (values != null) {
            return values;
        }

        values = new ArrayList<String>();
        String value = aConfig.getString(aKey);
        if (value != null) {
            values.add(value);
        }

        return values;
    }

    private static boolean isIgnored(String aLine) {
        String line = aLine.trim();
        return line.length() == 0 || line.startsWith(COMMENT);
    }

    private static boolean containsLineBreak(String aValue) {
        return aValue.indexOf('\r') >= 0 || aValue.indexOf('\n') >= 0;
    }

    private static String[] splitLines(String aText) {

        if (aText == null || aText.length() == 0) {
            return new String[0];
        }

        return aText.split(LINE_SEPARATOR_PATTERN);
    }
}
