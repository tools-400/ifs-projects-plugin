/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.export.wizards;

import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.export.ExportPlan;

/**
 * Texts of the problems of an export.
 */
final class ExportProblems {

    private ExportProblems() {
        // Utility class
    }

    /**
     * Returns the text of a problem.
     *
     * @param aProblem - problem
     * @return text
     */
    static String getText(ExportPlan.Problem aProblem) {

        String lineNumber = Integer.toString(aProblem.getLineNumber());

        switch (aProblem.getType()) {
        case INVALID_MEMBER_NAME:
            return Messages.Problem_Invalid_member_name;

        case DUPLICATE:
            return Messages.Problem_Duplicate_member;

        case NOT_READ:
            return Messages.bind(Messages.Problem_Not_read_A, aProblem.getText());

        case NOT_A_SOURCE_FILE:
            return Messages.bind(Messages.Problem_A_is_not_a_source_file, aProblem.getText());

        case LINE_TOO_LONG:
            return Messages.bind(Messages.Problem_Line_A_too_long_B_C, new Object[] { lineNumber, Integer.toString(aProblem.getLength()),
                Integer.toString(aProblem.getLimit()) });

        case UNMAPPABLE_CHARACTER:
            return Messages.bind(Messages.Problem_Line_A_unmappable_B, lineNumber, Integer.toString(aProblem.getLimit()));

        case INCLUDE:
            switch (aProblem.getIncludeProblem()) {
            case NOT_FOUND:
                return Messages.bind(Messages.Problem_Line_A_include_B_not_found, lineNumber, aProblem.getText());
            case NOT_TRANSFERRED:
                return Messages.bind(Messages.Problem_Line_A_include_B_not_exported, lineNumber, aProblem.getText());
            case TOO_LONG:
                return Messages.bind(Messages.Problem_Line_A_include_B_too_long, lineNumber, aProblem.getText());
            case INCLUDE_FILE:
                return Messages.bind(Messages.Problem_Line_A_include_B_other_source_file, lineNumber, aProblem.getText());
            default:
                return Messages.bind(Messages.Problem_Line_A_include_B_not_supported, lineNumber, aProblem.getText());
            }

        default:
            return aProblem.getType().toString();
        }
    }
}
