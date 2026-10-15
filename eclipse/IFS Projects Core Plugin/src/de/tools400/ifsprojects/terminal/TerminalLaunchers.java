/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.terminal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

import de.tools400.ifsprojects.IfsProjectsPlugin;
import de.tools400.ifsprojects.Messages;

/**
 * Selects the terminal backend that is used to open terminals. Backends are
 * contributed via extension point <code>terminalLaunchers</code>. The available
 * backend with the highest priority wins.
 */
public final class TerminalLaunchers {

    private static final String EXTENSION_POINT_ID = IfsProjectsPlugin.PLUGIN_ID + ".terminalLaunchers"; //$NON-NLS-1$
    private static final String ELEMENT_LAUNCHER = "launcher"; //$NON-NLS-1$
    private static final String ATTRIBUTE_ID = "id"; //$NON-NLS-1$
    private static final String ATTRIBUTE_CLASS = "class"; //$NON-NLS-1$
    private static final String ATTRIBUTE_PRIORITY = "priority"; //$NON-NLS-1$

    private static ITerminalLauncher launcher;

    private TerminalLaunchers() {
        // Utility class
    }

    /**
     * Returns the terminal launcher that is used to open terminals.
     *
     * @return terminal launcher
     * @throws TerminalException if no supported terminal implementation is
     *         installed
     */
    public static synchronized ITerminalLauncher getLauncher() throws TerminalException {

        if (launcher == null) {
            launcher = findLauncher();
        }

        if (launcher == null) {
            throw new TerminalException(Messages.Error_No_supported_terminal_implementation_found);
        }

        return launcher;
    }

    /**
     * Closes all terminals of the terminal backend.
     */
    public static void closeAllTerminals() {
        try {
            getLauncher().closeAllTerminals();
        } catch (TerminalException e) {
            // No terminal backend, no terminals
        }
    }

    private static ITerminalLauncher findLauncher() {

        IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor(EXTENSION_POINT_ID);

        List<IConfigurationElement> launchers = new ArrayList<IConfigurationElement>();
        for (IConfigurationElement element : elements) {
            if (ELEMENT_LAUNCHER.equals(element.getName())) {
                launchers.add(element);
            }
        }

        Collections.sort(launchers, new Comparator<IConfigurationElement>() {
            public int compare(IConfigurationElement anElement1, IConfigurationElement anElement2) {
                return getPriority(anElement2) - getPriority(anElement1);
            }
        });

        for (IConfigurationElement element : launchers) {
            try {
                Object object = element.createExecutableExtension(ATTRIBUTE_CLASS);
                if (object instanceof ITerminalLauncher) {
                    ITerminalLauncher candidate = (ITerminalLauncher)object;
                    if (candidate.isAvailable()) {
                        return candidate;
                    }
                }
            } catch (CoreException e) {
                IfsProjectsPlugin.logError("Could not create terminal launcher: " + element.getAttribute(ATTRIBUTE_ID), e); //$NON-NLS-1$
            } catch (LinkageError e) {
                IfsProjectsPlugin.logError("Could not load terminal launcher: " + element.getAttribute(ATTRIBUTE_ID), e); //$NON-NLS-1$
            }
        }

        return null;
    }

    private static int getPriority(IConfigurationElement anElement) {
        try {
            return Integer.parseInt(anElement.getAttribute(ATTRIBUTE_PRIORITY));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
