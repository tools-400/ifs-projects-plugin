/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.terminal.tm;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.eclipse.tm.internal.terminal.control.ITerminalViewControl;
import org.eclipse.tm.internal.terminal.textcanvas.ITextCanvasModel;
import org.eclipse.tm.internal.terminal.textcanvas.TextCanvas;

import de.tools400.ifsprojects.IfsProjectsPlugin;

/**
 * Calls of the TM terminal, that differ between the supported versions (RDi
 * 9.6, 9.8, 9.9) and are therefore made by reflection:
 * <ul>
 * <li><code>ITerminalViewControl.connectTerminal()</code> returns
 * <code>boolean</code> in the old TM terminal, but <code>void</code> in the new
 * one.</li>
 * <li><code>TextCanvas.clearSelection()</code> does not exist in RDi 9.6.
 * Therefore the selection is cleared in the model of the canvas.</li>
 * <li>The TM terminal of RDi 9.9 stops updating the display, when it is
 * disconnected, but does not start again, when it is connected again, see
 * {@link #restartDisplayUpdates(ITerminalViewControl)}.</li>
 * </ul>
 */
@SuppressWarnings("restriction")
final class TmCompatibility {

    private static final String METHOD_CONNECT_TERMINAL = "connectTerminal"; //$NON-NLS-1$

    private static final String FIELD_CELL_CANVAS_MODEL = "fCellCanvasModel"; //$NON-NLS-1$

    private static final String FIELD_POLLING_TEXT_CANVAS_MODEL = "fPollingTextCanvasModel"; //$NON-NLS-1$

    private static final String METHOD_START_POLLING = "startPolling"; //$NON-NLS-1$

    private TmCompatibility() {
        // Utility class
    }

    /**
     * Connects the terminal.
     *
     * @param aControl - terminal control
     * @throws ReflectiveOperationException if the terminal could not be
     *         connected, e.g. an {@link java.lang.reflect.InvocationTargetException}
     *         with the exception of the terminal
     */
    static void connectTerminal(ITerminalViewControl aControl) throws ReflectiveOperationException {
        Method method = ITerminalViewControl.class.getMethod(METHOD_CONNECT_TERMINAL);
        method.invoke(aControl);
    }

    /**
     * Restarts the display updates of the terminal. The TM terminal stops them,
     * when it is disconnected. The TM terminal of RDi 9.8 starts them again,
     * when it is connected again, but the TM terminal of RDi 9.9 does not: it
     * shows neither the output of the shell nor the typed keys. The TM terminal
     * of RDi 9.6 never stops them and has neither the field nor
     * <code>startPolling()</code>. Errors are logged.
     *
     * @param aControl - terminal control
     */
    static void restartDisplayUpdates(ITerminalViewControl aControl) {

        try {
            Field field = aControl.getClass().getDeclaredField(FIELD_POLLING_TEXT_CANVAS_MODEL);
            field.setAccessible(true);
            Object model = field.get(aControl);
            model.getClass().getMethod(METHOD_START_POLLING).invoke(model);
        } catch (NoSuchFieldException e) {
            // RDi 9.6: the display updates are never stopped
        } catch (Exception e) {
            IfsProjectsPlugin.logWarning("Could not restart the display updates of the terminal: " + e.getLocalizedMessage()); //$NON-NLS-1$
        }
    }

    /**
     * Clears the selection of the terminal. Errors are logged.
     *
     * @param aControl - terminal control
     */
    static void clearSelection(ITerminalViewControl aControl) {

        try {
            Field field = TextCanvas.class.getDeclaredField(FIELD_CELL_CANVAS_MODEL);
            field.setAccessible(true);
            ((ITextCanvasModel)field.get(aControl.getControl())).setSelection(-1, -1, -1, -1);
        } catch (Exception e) {
            IfsProjectsPlugin.logWarning("Could not clear the selection of the terminal: " + e.getLocalizedMessage()); //$NON-NLS-1$
        }
    }
}
