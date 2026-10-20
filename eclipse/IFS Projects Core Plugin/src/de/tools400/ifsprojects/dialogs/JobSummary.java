/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.dialogs;

import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.utils.PlatformUtils;

/**
 * Summary of a job, that processes several objects, e.g. an export: a message
 * and the first errors.
 */
public final class JobSummary {

    /** Number of errors listed in the summary. */
    private static final int MAX_LISTED_ERRORS = 20;

    private static final String LINE_SEPARATOR = "\n"; //$NON-NLS-1$
    private static final String INDENTATION = "  "; //$NON-NLS-1$

    private JobSummary() {
        // Utility class
    }

    /**
     * Returns the text of the summary: the message followed by the errors.
     *
     * @param aMessage - message, e.g. the number of processed objects
     * @param anErrors - errors
     * @return text
     */
    public static String getText(String aMessage, List<String> anErrors) {

        StringBuilder text = new StringBuilder(aMessage);
        if (anErrors.isEmpty()) {
            return text.toString();
        }

        text.append(LINE_SEPARATOR).append(LINE_SEPARATOR);
        text.append(Messages.bind(Messages.Label_Errors_A, Integer.toString(anErrors.size()))).append(LINE_SEPARATOR);
        for (int i = 0; i < anErrors.size() && i < MAX_LISTED_ERRORS; i++) {
            text.append(INDENTATION).append(anErrors.get(i)).append(LINE_SEPARATOR);
        }
        if (anErrors.size() > MAX_LISTED_ERRORS) {
            text.append(INDENTATION).append(Messages.bind(Messages.Label_And_A_more_files, Integer.toString(anErrors.size() - MAX_LISTED_ERRORS)));
        }

        return text.toString();
    }

    /**
     * Shows the summary in a dialog: a warning, if there are errors,
     * otherwise an information. Can be called from any thread.
     *
     * @param aTitle - title of the dialog
     * @param aMessage - message
     * @param anErrors - errors
     */
    public static void open(final String aTitle, String aMessage, List<String> anErrors) {

        final String text = getText(aMessage, anErrors);
        final boolean isError = !anErrors.isEmpty();
        Display.getDefault().asyncExec(new Runnable() {
            public void run() {
                if (isError) {
                    MessageDialog.openWarning(PlatformUtils.getWorkbenchShell(), aTitle, text);
                } else {
                    MessageDialog.openInformation(PlatformUtils.getWorkbenchShell(), aTitle, text);
                }
            }
        });
    }
}
