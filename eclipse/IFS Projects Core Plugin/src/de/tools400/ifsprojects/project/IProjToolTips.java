/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.project;

import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.preferences.Preferences;

/**
 * Tool tips of the attributes of <code>iproj.json</code>. They are used by the
 * property page of IFS projects and by the 'New IFS Project' wizard.
 */
public final class IProjToolTips {

    private static final String LINE_SEPARATOR = "\n"; //$NON-NLS-1$

    private IProjToolTips() {
        // Utility class
    }

    /**
     * Returns the tool tip of an attribute of <code>iproj.json</code>. The tool
     * tips of the build and compile command name the default command of the
     * preferences, which is used, if the attribute is empty.
     *
     * @param aKey - name of the attribute, e.g. {@link IProjConfig#OBJLIB}
     * @return tool tip or <code>null</code> for unknown attributes
     */
    public static String get(String aKey) {

        if (IProjConfig.DESCRIPTION.equals(aKey)) {
            return Messages.Tooltip_iproj_description;
        } else if (IProjConfig.VERSION.equals(aKey)) {
            return Messages.Tooltip_iproj_version;
        } else if (IProjConfig.REPOSITORY.equals(aKey)) {
            return Messages.Tooltip_iproj_repository;
        } else if (IProjConfig.OBJLIB.equals(aKey)) {
            return Messages.Tooltip_iproj_objlib;
        } else if (IProjConfig.CURLIB.equals(aKey)) {
            return Messages.Tooltip_iproj_curlib;
        } else if (IProjConfig.BUILD_COMMAND.equals(aKey)) {
            return Messages.Tooltip_Build_command + LINE_SEPARATOR
                + Messages.bind(Messages.Tooltip_Empty_build_command_uses_A, Preferences.getInstance().getDefaultBuildCommand());
        } else if (IProjConfig.COMPILE_COMMAND.equals(aKey)) {
            return Messages.Tooltip_Compile_command + LINE_SEPARATOR
                + Messages.bind(Messages.Tooltip_Empty_compile_command_uses_A, Preferences.getInstance().getDefaultCompileCommand());
        } else if (IProjConfig.INCLUDE_PATH.equals(aKey)) {
            return Messages.Tooltip_iproj_includePath;
        } else if (IProjConfig.PRE_USRLIBL.equals(aKey)) {
            return Messages.Tooltip_iproj_preUsrlibl;
        } else if (IProjConfig.POST_USRLIBL.equals(aKey)) {
            return Messages.Tooltip_iproj_postUsrlibl;
        } else if (IProjConfig.SET_IBMI_ENV_CMD.equals(aKey)) {
            return Messages.Tooltip_iproj_setIBMiEnvCmd;
        }

        return null;
    }
}
