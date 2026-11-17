/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.expressions;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import de.tools400.ifsprojects.rse.RseSelections;

/**
 * Property tester for files, directories and directory filters of the subsystem
 * 'Local Files' of RSE. Besides its own property it tests the properties of
 * {@link LocalProjectPropertyTester} for the resource of the workspace, that is
 * located at the element, see {@link RseSelections#getLocalResource(Object)}.
 * Every property is <code>false</code>, if the element is not located in an
 * open project.
 */
public class LocalFilePropertyTester extends PropertyTester {

    public static final String PROPERTY_NAMESPACE = "de.tools400.ifsprojects.localfile"; //$NON-NLS-1$

    /** The directory is the location of an open project. */
    public static final String PROPERTY_IS_PROJECT = "isProject"; //$NON-NLS-1$

    private final LocalProjectPropertyTester resourceTester = new LocalProjectPropertyTester();

    public boolean test(Object aReceiver, String aProperty, Object[] anArgs, Object anExpectedValue) {

        IResource resource = RseSelections.getLocalResource(aReceiver);
        if (resource == null) {
            return false;
        }

        if (PROPERTY_IS_PROJECT.equals(aProperty)) {
            return toBoolean(anExpectedValue) == (resource instanceof IProject);
        }

        return resourceTester.test(resource, aProperty, anArgs, anExpectedValue);
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
