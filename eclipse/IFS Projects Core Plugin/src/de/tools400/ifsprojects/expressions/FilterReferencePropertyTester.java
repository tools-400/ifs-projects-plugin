/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.expressions;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.rse.core.filters.ISystemFilter;
import org.eclipse.rse.core.filters.ISystemFilterReference;
import org.eclipse.rse.core.subsystems.ISubSystem;

import de.tools400.ifsprojects.rse.IfsFilterStrings;

/**
 * Property tester for RSE filter references.
 * <p>
 * The tester only reads the RSE model. It never accesses the remote system.
 */
public class FilterReferencePropertyTester extends PropertyTester {

    public static final String PROPERTY_NAMESPACE = "de.tools400.ifsprojects.filter"; //$NON-NLS-1$

    public static final String PROPERTY_SUBSYSTEM_CONFIG_ID = "subsystemConfigId"; //$NON-NLS-1$
    public static final String PROPERTY_HAS_DIRECTORY = "hasDirectory"; //$NON-NLS-1$

    public boolean test(Object aReceiver, String aProperty, Object[] anArgs, Object anExpectedValue) {

        if (!(aReceiver instanceof ISystemFilterReference)) {
            return false;
        }

        ISystemFilterReference filterReference = (ISystemFilterReference)aReceiver;

        if (PROPERTY_SUBSYSTEM_CONFIG_ID.equals(aProperty)) {
            ISubSystem subSystem = filterReference.getSubSystem();
            if (subSystem == null || !(anExpectedValue instanceof String)) {
                return false;
            }
            return ((String)anExpectedValue).equalsIgnoreCase(subSystem.getConfigurationId());
        }

        if (PROPERTY_HAS_DIRECTORY.equals(aProperty)) {
            ISystemFilter filter = filterReference.getReferencedFilter();
            return toBoolean(anExpectedValue) == (IfsFilterStrings.getDirectory(filter.getFilterStrings()) != null);
        }

        return false;
    }

    private boolean toBoolean(Object anExpectedValue) {

        if (anExpectedValue == null) {
            return true;
        }

        if (anExpectedValue instanceof Boolean) {
            return ((Boolean)anExpectedValue).booleanValue();
        }

        return Boolean.valueOf(anExpectedValue.toString()).booleanValue();
    }
}
