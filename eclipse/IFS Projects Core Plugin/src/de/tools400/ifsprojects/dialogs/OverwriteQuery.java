/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.dialogs;

/**
 * Asks the user, whether an existing object, e.g. a member or a stream file, is
 * replaced. See {@link OverwriteQueryDialog} and
 * {@link RememberingOverwriteQuery}.
 */
public interface OverwriteQuery {

    /**
     * Answers of the question.
     */
    enum Answer {
        YES,
        YES_TO_ALL,
        NO,
        NO_TO_ALL,
        CANCEL;

        /**
         * Returns <code>true</code>, if the object shall be replaced.
         */
        public boolean isReplace() {
            return this == YES || this == YES_TO_ALL;
        }
    }

    /**
     * Asks the user. Called by a job, not on the UI thread.
     *
     * @param anObject - object, e.g. <code>JSMITH#CL/QRPGLESRC(HELLO)</code>
     * @return answer
     */
    Answer ask(String anObject);
}
