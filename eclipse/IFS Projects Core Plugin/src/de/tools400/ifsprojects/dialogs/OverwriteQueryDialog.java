/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.dialogs;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.utils.PlatformUtils;

/**
 * Asks the user with Yes, Yes To All, No, No To All and Cancel, whether an
 * existing object is replaced. The dialog is opened on the UI thread, while
 * the calling job waits.
 */
public final class OverwriteQueryDialog implements OverwriteQuery {

    private static final Answer[] ANSWERS = { Answer.YES, Answer.YES_TO_ALL, Answer.NO, Answer.NO_TO_ALL, Answer.CANCEL };

    private final String title;
    private final String question;

    /**
     * Creates the query.
     *
     * @param aTitle - title of the dialog
     * @param aQuestion - question with the placeholder <code>{0}</code> for the
     *        object
     */
    public OverwriteQueryDialog(String aTitle, String aQuestion) {
        this.title = aTitle;
        this.question = aQuestion;
    }

    public Answer ask(final String anObject) {

        final Answer[] answer = { Answer.CANCEL };
        Display.getDefault().syncExec(new Runnable() {
            public void run() {
                MessageDialog dialog = new MessageDialog(PlatformUtils.getWorkbenchShell(), title, null, Messages.bind(question, anObject),
                    MessageDialog.QUESTION, new String[] { IDialogConstants.YES_LABEL, IDialogConstants.YES_TO_ALL_LABEL,
                        IDialogConstants.NO_LABEL, IDialogConstants.NO_TO_ALL_LABEL, IDialogConstants.CANCEL_LABEL }, 0);
                int index = dialog.open();
                if (index >= 0 && index < ANSWERS.length) {
                    answer[0] = ANSWERS[index];
                }
            }
        });

        return answer[0];
    }
}
