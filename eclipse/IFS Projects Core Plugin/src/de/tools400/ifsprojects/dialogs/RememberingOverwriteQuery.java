/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.dialogs;

/**
 * Passes the questions to another query, until the user answers 'Yes To All' or
 * 'No To All'. Afterwards it returns this answer without asking.
 */
public final class RememberingOverwriteQuery implements OverwriteQuery {

    private final OverwriteQuery query;

    /** Answer 'Yes To All' or 'No To All' of the user. */
    private Answer answerToAll;

    /**
     * Creates the query.
     *
     * @param aQuery - query, that asks the user
     */
    public RememberingOverwriteQuery(OverwriteQuery aQuery) {
        this.query = aQuery;
    }

    public Answer ask(String anObject) {

        if (answerToAll != null) {
            return answerToAll;
        }

        Answer answer = query.ask(anObject);
        if (answer == Answer.YES_TO_ALL || answer == Answer.NO_TO_ALL) {
            answerToAll = answer;
        }

        return answer;
    }
}
