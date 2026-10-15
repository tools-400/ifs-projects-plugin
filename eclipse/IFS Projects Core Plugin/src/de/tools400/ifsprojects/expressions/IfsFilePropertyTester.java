/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.expressions;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.rse.subsystems.files.core.subsystems.IRemoteFile;

import de.tools400.ifsprojects.subsystem.IfsProjects;
import de.tools400.ifsprojects.syntaxcheck.SourceType;

/**
 * Property tester for IFS files and directories.
 * <p>
 * The tester only reads the RSE model. It never accesses the remote system.
 */
public class IfsFilePropertyTester extends PropertyTester {

    public static final String PROPERTY_NAMESPACE = "de.tools400.ifsprojects.file"; //$NON-NLS-1$

    /** The file is part of an IFS project of the same connection. */
    public static final String PROPERTY_IS_IN_IFS_PROJECT = "isInIfsProject"; //$NON-NLS-1$

    /** The file is part of an IFS project and its syntax can be checked. */
    public static final String PROPERTY_CAN_CHECK_SYNTAX = "canCheckSyntax"; //$NON-NLS-1$

    public boolean test(Object aReceiver, String aProperty, Object[] anArgs, Object anExpectedValue) {

        if (!(aReceiver instanceof IRemoteFile)) {
            return false;
        }

        IRemoteFile remoteFile = (IRemoteFile)aReceiver;

        if (PROPERTY_IS_IN_IFS_PROJECT.equals(aProperty)) {
            return toBoolean(anExpectedValue) == isInProject(remoteFile);
        }

        if (PROPERTY_CAN_CHECK_SYNTAX.equals(aProperty)) {
            boolean canCheckSyntax = SourceType.fromFileName(remoteFile.getName()) != null && isInProject(remoteFile);
            return toBoolean(anExpectedValue) == canCheckSyntax;
        }

        return false;
    }

    private boolean isInProject(IRemoteFile aRemoteFile) {
        return IfsProjects.findProject(aRemoteFile.getHost(), aRemoteFile.getAbsolutePath()) != null;
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
