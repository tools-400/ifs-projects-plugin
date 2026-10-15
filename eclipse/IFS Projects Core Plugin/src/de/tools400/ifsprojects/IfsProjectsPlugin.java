/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

import de.tools400.ifsprojects.preferences.Preferences;
import de.tools400.ifsprojects.subsystem.IfsProjectRefreshListener;
import de.tools400.ifsprojects.terminal.TerminalLaunchers;

/**
 * The activator class controls the plug-in life cycle.
 */
public class IfsProjectsPlugin extends AbstractUIPlugin {

    /** The plug-in ID */
    public static final String PLUGIN_ID = "de.tools400.ifsprojects.core.plugin"; //$NON-NLS-1$

    public static final String IMAGE_TERMINAL = "terminal_view.gif"; //$NON-NLS-1$
    public static final String IMAGE_IFS_PROJECT = "obj16/prj_obj.png"; //$NON-NLS-1$

    /*
     * Images of the terminal view toolbar (icons of the RDi 9.8 'Terminal'
     * view). Use with getEnabledImageDescriptor() and
     * getDisabledImageDescriptor().
     */
    public static final String IMAGE_CONNECT = "connect_co.gif"; //$NON-NLS-1$
    public static final String IMAGE_DISCONNECT = "disconnect.gif"; //$NON-NLS-1$
    public static final String IMAGE_CLEAR = "clear_co.gif"; //$NON-NLS-1$
    public static final String IMAGE_NEW_TERMINAL = "new_terminal_view.gif"; //$NON-NLS-1$
    public static final String IMAGE_SCROLL_LOCK = "lock_co.png"; //$NON-NLS-1$
    public static final String IMAGE_COMMAND_INPUT_FIELD = "command_input_field.gif"; //$NON-NLS-1$

    private static final String ICONS_ENABLED = "elcl16/"; //$NON-NLS-1$
    private static final String ICONS_DISABLED = "dlcl16/"; //$NON-NLS-1$

    /** The shared instance */
    private static IfsProjectsPlugin plugin;

    /** The install URL of the plugin */
    private static URL installURL;

    /** Closes the terminals when the workbench shuts down */
    private IWorkbenchListener workbenchListener;

    /**
     * The constructor.
     */
    public IfsProjectsPlugin() {
    }

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
        installURL = context.getBundle().getEntry("/"); //$NON-NLS-1$

        if (PlatformUI.isWorkbenchRunning()) {
            workbenchListener = new ShutdownListener();
            PlatformUI.getWorkbench().addWorkbenchListener(workbenchListener);
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {

        if (workbenchListener != null && PlatformUI.isWorkbenchRunning()) {
            PlatformUI.getWorkbench().removeWorkbenchListener(workbenchListener);
        }
        workbenchListener = null;

        IfsProjectRefreshListener.unregister();

        plugin = null;
        super.stop(context);
    }

    /**
     * Returns the shared instance.
     *
     * @return the shared instance
     */
    public static IfsProjectsPlugin getDefault() {
        return plugin;
    }

    /**
     * Returns the version of the plug-in, as assigned to "Bundle-Version" in
     * "MANIFEST.MF".
     *
     * @return Version of the plug-in.
     */
    public String getVersion() {
        String version = getBundle().getHeaders().get(Constants.BUNDLE_VERSION);
        if (version == null) {
            version = "0.0.0"; //$NON-NLS-1$
        }

        return version;
    }

    /**
     * Convenience method to log error messages to the application log.
     *
     * @param message Message
     * @param e The exception that has produced the error
     */
    public static void logError(String message, Throwable e) {
        if (plugin == null) {
            logToConsole("ERROR", message, e); //$NON-NLS-1$
            return;
        }
        plugin.getLog().log(new Status(Status.ERROR, PLUGIN_ID, message, e));
    }

    /**
     * Convenience method to log warning messages to the application log.
     *
     * @param message Message
     */
    public static void logWarning(String message) {
        if (plugin == null) {
            logToConsole("WARN", message, null); //$NON-NLS-1$
            return;
        }
        plugin.getLog().log(new Status(Status.WARNING, PLUGIN_ID, message));
    }

    private static void logToConsole(String level, String message, Throwable e) {
        System.out.println(String.format("%s: %s", level, message)); //$NON-NLS-1$
        if (e != null) {
            e.printStackTrace();
        }
    }

    @Override
    protected void initializeImageRegistry(ImageRegistry reg) {
        super.initializeImageRegistry(reg);
        reg.put(IMAGE_TERMINAL, getImageDescriptor(IMAGE_TERMINAL));
    }

    /**
     * Returns the image descriptor of an enabled toolbar action.
     *
     * @param name - image name, e.g. {@link #IMAGE_SCROLL_LOCK}
     * @return image descriptor
     */
    public static ImageDescriptor getEnabledImageDescriptor(String name) {
        return getImageDescriptor(ICONS_ENABLED + name);
    }

    /**
     * Returns the image descriptor of a disabled toolbar action or
     * <code>null</code>, if there is no disabled image. Then the workbench
     * creates a grayed image.
     *
     * @param name - image name, e.g. {@link #IMAGE_SCROLL_LOCK}
     * @return image descriptor or <code>null</code>
     */
    public static ImageDescriptor getDisabledImageDescriptor(String name) {
        if (installURL == null || getDefault().getBundle().getEntry("icons/" + ICONS_DISABLED + name) == null) { //$NON-NLS-1$
            return null;
        }
        return getImageDescriptor(ICONS_DISABLED + name);
    }

    /**
     * Closes all terminals before the workbench saves its state, unless the
     * terminals shall be restored after a restart. Otherwise all terminals are
     * opened again (disconnected) when RDi is started.
     */
    private static class ShutdownListener implements IWorkbenchListener {

        public boolean preShutdown(IWorkbench aWorkbench, boolean aForced) {
            try {
                if (!Preferences.getInstance().isRestoreTerminals()) {
                    TerminalLaunchers.closeAllTerminals();
                }
            } catch (Throwable e) {
                logError("Could not close terminals on shutdown.", e); //$NON-NLS-1$
            }
            return true;
        }

        public void postShutdown(IWorkbench aWorkbench) {
        }
    }

    public Image getImage(String name) {
        return getImageRegistry().get(name);
    }

    public static ImageDescriptor getImageDescriptor(String name) {
        String iconPath = "icons/"; //$NON-NLS-1$
        try {
            URL url = new URL(installURL, iconPath + name);
            return ImageDescriptor.createFromURL(url);
        } catch (MalformedURLException e) {
            return ImageDescriptor.getMissingImageDescriptor();
        }
    }
}
