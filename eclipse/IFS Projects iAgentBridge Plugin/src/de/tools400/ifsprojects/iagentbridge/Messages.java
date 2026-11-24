/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.iagentbridge;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
    private static final String BUNDLE_NAME = "de.tools400.ifsprojects.iagentbridge.messages"; //$NON-NLS-1$

    public static String Description_preference_page;
    public static String Label_Allow_compile;
    public static String Label_Warning_commands;
    public static String Label_Approve_projects_in_properties;

    public static String Description_property_page;
    public static String Label_Approve_project;
    public static String Link_Preference_page;

    public static String Error_Could_not_save_A;

    public static String Task_Compiling_A;
    public static String Task_Checking_syntax_of_A;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
