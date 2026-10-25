/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.terminal.tm;

import java.util.Collection;

/**
 * Names of the sessions of the {@link TmTerminalView}: a session is named after
 * the title of its request, further sessions with the same title get their
 * instance number, e.g. <code>X (2)</code>. The view sorts its session list
 * with {@link #compare(String, int, String, int)}.
 */
public final class SessionNames {

    private SessionNames() {
        // Utility class
    }

    /**
     * Returns the smallest instance number, that is not used, so that the names
     * of the sessions are unique, also after sessions have been closed.
     *
     * @param anInstanceNumbers - instance numbers of the open sessions with the
     *        same title
     * @return instance number, starting at 1
     */
    public static int getFreeInstanceNumber(Collection<Integer> anInstanceNumbers) {

        int instanceNumber = 1;
        while (anInstanceNumbers.contains(Integer.valueOf(instanceNumber))) {
            instanceNumber++;
        }

        return instanceNumber;
    }

    /**
     * Returns the instance number of a new session: the preferred number, e.g.
     * the saved number of a restored session, if it is not used, otherwise the
     * smallest free number, see {@link #getFreeInstanceNumber(Collection)}. The
     * sessions are saved in the order of the session list, in which a renamed
     * session may precede the sessions with the same title. Without the saved
     * numbers, they would be numbered again in this order after a restart.
     *
     * @param anInstanceNumbers - instance numbers of the open sessions with the
     *        same title
     * @param aPreferredNumber - preferred instance number or <code>null</code>
     * @return instance number, starting at 1
     */
    public static int getInstanceNumber(Collection<Integer> anInstanceNumbers, Integer aPreferredNumber) {

        if (aPreferredNumber != null && aPreferredNumber.intValue() > 0 && !anInstanceNumbers.contains(aPreferredNumber)) {
            return aPreferredNumber.intValue();
        }

        return getFreeInstanceNumber(anInstanceNumbers);
    }

    /**
     * Compares two sessions for the session list: alphabetically by name,
     * ignoring case, sessions with the same name by their instance numbers, so
     * that <code>X (10)</code> follows <code>X (2)</code>.
     *
     * @param aName1 - custom name or title of the first session
     * @param anInstanceNumber1 - instance number of the first session
     * @param aName2 - custom name or title of the second session
     * @param anInstanceNumber2 - instance number of the second session
     * @return negative, zero or positive, as {@link java.util.Comparator}
     */
    public static int compare(String aName1, int anInstanceNumber1, String aName2, int anInstanceNumber2) {

        int result = aName1.compareToIgnoreCase(aName2);
        if (result != 0) {
            return result;
        }

        return anInstanceNumber1 - anInstanceNumber2;
    }
}
