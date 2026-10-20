/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.migration.wizards;

import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.migration.MigrationPlan;

/**
 * Texts of the problems of a migration.
 */
final class MigrationProblems {

    private MigrationProblems() {
        // Utility class
    }

    /**
     * Returns the text of a problem.
     *
     * @param aProblem - problem
     * @return text
     */
    static String getText(MigrationPlan.Problem aProblem) {

        String lineNumber = Integer.toString(aProblem.getLineNumber());

        switch (aProblem.getType()) {
        case NOT_READ:
            return Messages.bind(Messages.Problem_Member_not_read_A, aProblem.getText());

        case NO_SOURCE_TYPE:
            return Messages.Problem_No_source_type;

        case UNKNOWN_SOURCE_TYPE:
            return Messages.bind(Messages.Problem_Unknown_source_type_A, aProblem.getText());

        case MAKE_CHARACTERS:
            return Messages.Problem_Make_characters;

        case INCLUDE:
            switch (aProblem.getIncludeProblem()) {
            case NOT_FOUND:
                return Messages.bind(Messages.Problem_Line_A_member_B_not_found, lineNumber, aProblem.getText());
            case NOT_TRANSFERRED:
                return Messages.bind(Messages.Problem_Line_A_member_B_not_migrated, lineNumber, aProblem.getText());
            case OTHER_LIBRARY:
                return Messages.bind(Messages.Problem_Line_A_member_B_other_library, lineNumber, aProblem.getText());
            case TOO_LONG:
                return Messages.bind(Messages.Problem_Line_A_include_B_too_long, lineNumber, aProblem.getText());
            default:
                return Messages.bind(Messages.Problem_Line_A_include_B_not_supported, lineNumber, aProblem.getText());
            }

        default:
            return aProblem.getType().toString();
        }
    }
}
