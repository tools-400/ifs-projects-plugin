/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.project;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import de.tools400.ifsprojects.utils.ExceptionUtils;
import de.tools400.ifsprojects.utils.StringUtils;

/**
 * Content of an <code>iproj.json</code> file, as used by Code for IBM i and
 * Better Object Builder.
 * <p>
 * The parsed JSON object is changed in place, so that unknown attributes and
 * the order of the attributes are preserved, when the file is saved.
 * <p>
 * Only the gson API of version 2.2.4 is used (RDi 9.6).
 */
public final class IProjConfig {

    public static final String FILE_NAME = "iproj.json"; //$NON-NLS-1$

    public static final String DESCRIPTION = "description"; //$NON-NLS-1$
    public static final String VERSION = "version"; //$NON-NLS-1$
    public static final String REPOSITORY = "repository"; //$NON-NLS-1$
    public static final String OBJLIB = "objlib"; //$NON-NLS-1$
    public static final String CURLIB = "curlib"; //$NON-NLS-1$
    public static final String INCLUDE_PATH = "includePath"; //$NON-NLS-1$
    public static final String PRE_USRLIBL = "preUsrlibl"; //$NON-NLS-1$
    public static final String POST_USRLIBL = "postUsrlibl"; //$NON-NLS-1$
    public static final String SET_IBMI_ENV_CMD = "setIBMiEnvCmd"; //$NON-NLS-1$
    public static final String BUILD_COMMAND = "buildCommand"; //$NON-NLS-1$
    public static final String COMPILE_COMMAND = "compileCommand"; //$NON-NLS-1$

    private static final char BYTE_ORDER_MARK = (char)0xFEFF;
    private static final String LINE_FEED = "\n"; //$NON-NLS-1$

    private final JsonObject json;

    /**
     * Creates an empty configuration.
     */
    public IProjConfig() {
        this(new JsonObject());
    }

    private IProjConfig(JsonObject aJson) {
        this.json = aJson;
    }

    /**
     * Parses the content of an <code>iproj.json</code> file.
     *
     * @param aContent - file content
     * @return configuration
     * @throws IProjParseException if the content is not a JSON object
     */
    public static IProjConfig parse(String aContent) throws IProjParseException {

        String content = aContent;
        if (content == null) {
            return new IProjConfig();
        }

        if (content.length() > 0 && content.charAt(0) == BYTE_ORDER_MARK) {
            content = content.substring(1);
        }

        if (StringUtils.isBlank(content)) {
            return new IProjConfig();
        }

        try {
            JsonElement element = new JsonParser().parse(content);
            if (!element.isJsonObject()) {
                throw new IProjParseException("The root element of iproj.json must be a JSON object.", null); //$NON-NLS-1$
            }
            return new IProjConfig(element.getAsJsonObject());
        } catch (JsonParseException e) {
            throw new IProjParseException(ExceptionUtils.getLocalizedMessage(e), e);
        }
    }

    /**
     * Returns the value of a string attribute.
     *
     * @param aKey - attribute name
     * @return value or <code>null</code>, if the attribute does not exist or is
     *         not a string
     */
    public String getString(String aKey) {

        JsonElement element = json.get(aKey);
        if (element == null || !element.isJsonPrimitive()) {
            return null;
        }

        return element.getAsString();
    }

    /**
     * Sets the value of a string attribute. An empty value removes the
     * attribute. An unchanged value is kept as it is, so that a number, e.g.
     * <code>"version": 1</code>, does not become a string and <code>null</code>
     * is not removed, when the other attributes are saved.
     *
     * @param aKey - attribute name
     * @param aValue - value
     */
    public void setString(String aKey, String aValue) {

        String value = StringUtils.trimToNull(aValue);
        if (isUnchanged(json.get(aKey), value)) {
            return;
        }

        if (value == null) {
            json.remove(aKey);
        } else {
            json.add(aKey, new JsonPrimitive(value));
        }
    }

    private static boolean isUnchanged(JsonElement anElement, String aValue) {

        if (anElement == null) {
            return aValue == null;
        }

        if (anElement.isJsonNull()) {
            return aValue == null;
        }

        return anElement.isJsonPrimitive() && anElement.getAsString().equals(aValue);
    }

    /**
     * Returns the value of a string list attribute.
     *
     * @param aKey - attribute name
     * @return list of values (empty if the attribute does not exist) or
     *         <code>null</code>, if the attribute is not a list of strings
     */
    public List<String> getStringList(String aKey) {

        List<String> values = new ArrayList<>();

        JsonElement element = json.get(aKey);
        if (element == null || element.isJsonNull()) {
            return values;
        }

        if (!element.isJsonArray()) {
            return null;
        }

        JsonArray array = element.getAsJsonArray();
        for (int i = 0; i < array.size(); i++) {
            JsonElement item = array.get(i);
            if (!item.isJsonPrimitive()) {
                return null;
            }
            values.add(item.getAsString());
        }

        return values;
    }

    /**
     * Returns the values of an attribute, that is a list of strings or a
     * string, e.g. <code>includePath</code>.
     *
     * @param aKey - attribute name
     * @return values, empty if the attribute does not exist or is neither a
     *         list of strings nor a string
     */
    public List<String> getStrings(String aKey) {

        List<String> values = getStringList(aKey);
        if (values != null) {
            return values;
        }

        values = new ArrayList<>();
        String value = getString(aKey);
        if (value != null) {
            values.add(value);
        }

        return values;
    }

    /**
     * Sets the value of a string list attribute. An empty list removes the
     * attribute.
     *
     * @param aKey - attribute name
     * @param aValues - values
     */
    public void setStringList(String aKey, List<String> aValues) {

        JsonArray array = new JsonArray();
        if (aValues != null) {
            for (String value : aValues) {
                String item = StringUtils.trimToNull(value);
                if (item != null) {
                    array.add(new JsonPrimitive(item));
                }
            }
        }

        if (array.size() == 0) {
            json.remove(aKey);
        } else {
            json.add(aKey, array);
        }
    }

    /**
     * Returns <code>true</code>, if the attribute exists, but its value cannot
     * be edited as string or string list.
     *
     * @param aKey - attribute name
     * @param aIsList - specifies whether the attribute is a string list
     * @return <code>true</code>, if the attribute cannot be edited
     */
    public boolean isNotEditable(String aKey, boolean aIsList) {

        if (!json.has(aKey)) {
            return false;
        }

        if (aIsList) {
            return getStringList(aKey) == null;
        }

        JsonElement element = json.get(aKey);
        return !element.isJsonNull() && !element.isJsonPrimitive();
    }

    /**
     * Returns the JSON text of an attribute.
     *
     * @param aKey - attribute name
     * @return JSON text or <code>null</code>
     */
    public String getJsonText(String aKey) {

        JsonElement element = json.get(aKey);
        if (element == null) {
            return null;
        }

        return element.toString();
    }

    /**
     * Returns the build command.
     *
     * @return build command or <code>null</code>
     */
    public String getBuildCommand() {
        return StringUtils.trimToNull(getString(BUILD_COMMAND));
    }

    /**
     * Returns the command that compiles a single file of the project.
     *
     * @return compile command or <code>null</code>
     */
    public String getCompileCommand() {
        return StringUtils.trimToNull(getString(COMPILE_COMMAND));
    }

    /**
     * Returns the content of the <code>iproj.json</code> file.
     *
     * @return pretty printed JSON, terminated by a line feed
     */
    public String toJson() {
        return new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().serializeNulls().create().toJson(json) + LINE_FEED;
    }
}
