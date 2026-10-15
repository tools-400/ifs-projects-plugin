/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.expressions;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.rse.core.IRSESystemType;
import org.eclipse.rse.core.model.IHost;

/**
 * Property tester for RSE connections (IHost).
 */
public class HostPropertyTester extends PropertyTester {

    public static final String PROPERTY_NAMESPACE = "de.tools400.ifsprojects.host"; //$NON-NLS-1$

    public static final String PROPERTY_SYSTEM_TYPE_ID = "systemTypeId"; //$NON-NLS-1$

    public boolean test(Object aReceiver, String aProperty, Object[] anArgs, Object anExpectedValue) {

        if (!(aReceiver instanceof IHost)) {
            return false;
        }

        IHost host = (IHost)aReceiver;

        if (PROPERTY_SYSTEM_TYPE_ID.equals(aProperty) && anExpectedValue instanceof String) {
            IRSESystemType systemType = host.getSystemType();
            if (systemType == null) {
                return false;
            }
            return ((String)anExpectedValue).equalsIgnoreCase(systemType.getId());
        }

        return false;
    }
}
