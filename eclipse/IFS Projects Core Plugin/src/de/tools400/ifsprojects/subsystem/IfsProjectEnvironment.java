/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.subsystem;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.rse.core.filters.ISystemFilter;
import org.eclipse.rse.core.model.IPropertySet;

import de.tools400.ifsprojects.preferences.Preferences;
import de.tools400.ifsprojects.project.EnvironmentVariables;
import de.tools400.ifsprojects.utils.StringUtils;

/**
 * Stores the environment variables of an IFS project in a property set of the
 * project filter. The filter is part of the connection private filter pool, so
 * the variables are specific for the connection and are not stored in the
 * project.
 * <p>
 * Each variable is stored as property <code>variable.&lt;n&gt;</code> with the
 * value <code>NAME=value</code>, so that the order of the variables is kept.
 * <p>
 * The variables of a project override the default environment variables of the
 * preferences, see {@link #getEffectiveVariables(ISystemFilter)}.
 */
public final class IfsProjectEnvironment {

    public static final String PROPERTY_SET_NAME = "de.tools400.ifsprojects.environment"; //$NON-NLS-1$

    private static final String KEY_VARIABLE_PREFIX = "variable."; //$NON-NLS-1$
    private static final char ASSIGNMENT = '=';

    private IfsProjectEnvironment() {
        // Utility class
    }

    /**
     * Returns the environment variables of an IFS project.
     *
     * @param aFilter - filter of the IFS project
     * @return variables in the order of the definition, never
     *         <code>null</code>
     */
    public static Map<String, String> getVariables(ISystemFilter aFilter) {

        Map<String, String> variables = new LinkedHashMap<String, String>();
        if (aFilter == null) {
            return variables;
        }

        IPropertySet propertySet = aFilter.getPropertySet(PROPERTY_SET_NAME);
        if (propertySet == null) {
            return variables;
        }

        Map<Integer, String> definitions = new TreeMap<Integer, String>();
        for (String key : propertySet.getPropertyKeys()) {
            if (key.startsWith(KEY_VARIABLE_PREFIX)) {
                try {
                    definitions.put(Integer.valueOf(key.substring(KEY_VARIABLE_PREFIX.length())), propertySet.getPropertyValue(key));
                } catch (NumberFormatException e) {
                    // Ignore unknown keys
                }
            }
        }

        for (String definition : definitions.values()) {
            int index = definition == null ? -1 : definition.indexOf(ASSIGNMENT);
            if (index > 0 && EnvironmentVariables.isValidName(definition.substring(0, index))) {
                variables.put(definition.substring(0, index), definition.substring(index + 1));
            }
        }

        return variables;
    }

    /**
     * Returns the environment variables, that are exported before the build and
     * compile commands of an IFS project are executed: the default environment
     * variables of the preferences, overridden by the variables of the project.
     *
     * @param aFilter - filter of the IFS project
     * @return variables, never <code>null</code>
     */
    public static Map<String, String> getEffectiveVariables(ISystemFilter aFilter) {
        return EnvironmentVariables.merge(Preferences.getInstance().getDefaultEnvironmentVariables(), getVariables(aFilter));
    }

    /**
     * Sets the environment variables of an IFS project and saves the RSE
     * profile.
     *
     * @param aFilter - filter of the IFS project
     * @param aVariables - variables, an empty map removes all variables
     */
    public static void setVariables(ISystemFilter aFilter, Map<String, String> aVariables) {

        for (String name : aVariables.keySet()) {
            if (!EnvironmentVariables.isValidName(name)) {
                throw new IllegalArgumentException("Invalid name of environment variable: " + name); //$NON-NLS-1$
            }
        }

        aFilter.removePropertySet(PROPERTY_SET_NAME);

        if (!aVariables.isEmpty()) {
            IPropertySet propertySet = aFilter.createPropertySet(PROPERTY_SET_NAME);
            int number = 0;
            for (Map.Entry<String, String> entry : aVariables.entrySet()) {
                number++;
                String value = StringUtils.nullToEmpty(entry.getValue());
                propertySet.addProperty(KEY_VARIABLE_PREFIX + number, entry.getKey() + ASSIGNMENT + value);
            }
        }

        aFilter.setDirty(true);
        aFilter.commit();
    }
}
