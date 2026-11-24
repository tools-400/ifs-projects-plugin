/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.iagentbridge;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Settings, that allow AI agents to compile on the IBM i: the switch of the
 * workspace and the approval of a project. The tools run only, if both are on.
 * <p>
 * Both are stored in the workspace, not in the files of a project, so that
 * neither a clone of a repository nor a change of the project files approves a
 * project. They protect against an unintended use, not against an agent, that
 * may write any file, e.g. the preferences in the metadata of the workspace.
 */
public final class AgentSettings {

    /** ID of the plug-in, which is the node of its preferences. */
    public static final String PLUGIN_ID = "de.tools400.ifsprojects.iagentbridge.plugin"; //$NON-NLS-1$

    /** Preference of the switch, stored in the instance scope. */
    public static final String COMPILE_ALLOWED = "compileAllowed"; //$NON-NLS-1$

    private static final QualifiedName APPROVED = new QualifiedName(PLUGIN_ID, "approved"); //$NON-NLS-1$
    private static final String TRUE = Boolean.TRUE.toString();

    private AgentSettings() {
        // Utility class
    }

    /**
     * Tells, whether AI agents may compile on the IBM i in this workspace.
     *
     * @return <code>true</code>, if the switch is on; default:
     *         <code>false</code>
     */
    public static boolean isCompileAllowed() {
        return getPreferences().getBoolean(COMPILE_ALLOWED, false);
    }

    /**
     * Turns the switch on or off.
     *
     * @param anIsAllowed - <code>true</code>, if AI agents may compile on the
     *        IBM i
     * @throws BackingStoreException if the preferences could not be saved
     */
    public static void setCompileAllowed(boolean anIsAllowed) throws BackingStoreException {
        IEclipsePreferences preferences = getPreferences();
        preferences.putBoolean(COMPILE_ALLOWED, anIsAllowed);
        preferences.flush();
    }

    /**
     * Tells, whether the user has approved a project for AI agents.
     *
     * @param aProject - project
     * @return <code>true</code>, if the project is approved
     */
    public static boolean isApproved(IProject aProject) {

        try {
            return TRUE.equals(aProject.getPersistentProperty(APPROVED));
        } catch (CoreException e) {
            // The project is closed or does not exist.
            return false;
        }
    }

    /**
     * Approves a project for AI agents or withdraws the approval. The approval
     * is a persistent property of the project in the metadata of the workspace.
     *
     * @param aProject - open project
     * @param anIsApproved - <code>true</code> to approve the project
     * @throws CoreException if the property could not be stored
     */
    public static void setApproved(IProject aProject, boolean anIsApproved) throws CoreException {
        aProject.setPersistentProperty(APPROVED, anIsApproved ? TRUE : null);
    }

    private static IEclipsePreferences getPreferences() {
        return InstanceScope.INSTANCE.getNode(PLUGIN_ID);
    }
}
