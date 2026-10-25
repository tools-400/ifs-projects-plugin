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

import de.tools400.ifsprojects.local.LocalProject;
import de.tools400.ifsprojects.project.IProjConfig;
import de.tools400.ifsprojects.syntaxcheck.SourceType;

/**
 * Tests properties of projects and other resources of the workspace, see
 * {@link LocalProject}.
 */
public class LocalProjectPropertyTester extends PropertyTester {

    public static final String PROPERTY_NAMESPACE = "de.tools400.ifsprojects.project"; //$NON-NLS-1$

    /** The project is open and configured as local IFS project. */
    public static final String PROPERTY_IS_LOCAL_IFS_PROJECT = "isLocalIfsProject"; //$NON-NLS-1$

    /** The project is open and contains an <code>iproj.json</code> file. */
    public static final String PROPERTY_HAS_IPROJ_FILE = "hasIProjFile"; //$NON-NLS-1$

    /** The resource is part of a local IFS project. */
    public static final String PROPERTY_IS_IN_LOCAL_IFS_PROJECT = "isInLocalIfsProject"; //$NON-NLS-1$

    /**
     * The resource is a file of a local IFS project and its syntax can be
     * checked.
     */
    public static final String PROPERTY_CAN_CHECK_SYNTAX = "canCheckSyntax"; //$NON-NLS-1$

    public boolean test(Object aReceiver, String aProperty, Object[] anArgs, Object anExpectedValue) {

        if (!(aReceiver instanceof IResource)) {
            return false;
        }

        IResource resource = (IResource)aReceiver;

        if (PROPERTY_IS_IN_LOCAL_IFS_PROJECT.equals(aProperty)) {
            return toBoolean(anExpectedValue) == (LocalProject.get(resource.getProject()) != null);
        }

        if (PROPERTY_CAN_CHECK_SYNTAX.equals(aProperty)) {
            boolean canCheckSyntax = resource.getType() == IResource.FILE && SourceType.fromFileName(resource.getName()) != null
                && LocalProject.get(resource.getProject()) != null;
            return toBoolean(anExpectedValue) == canCheckSyntax;
        }

        if (!(resource instanceof IProject)) {
            return false;
        }

        IProject project = (IProject)resource;

        if (PROPERTY_IS_LOCAL_IFS_PROJECT.equals(aProperty)) {
            return toBoolean(anExpectedValue) == (LocalProject.get(project) != null);
        }

        if (PROPERTY_HAS_IPROJ_FILE.equals(aProperty)) {
            boolean hasIProjFile = project.isOpen() && project.getFile(IProjConfig.FILE_NAME).exists();
            return toBoolean(anExpectedValue) == hasIProjFile;
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
