/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.swt;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

/**
 * Group with the rows of a summary: a term in bold on the left, its value on
 * the right, e.g. "Files: 5 exported". The rows can be replaced, e.g. when a
 * preview is computed again.
 */
public class SummaryGroup {

    private final Group group;
    private final List<String[]> rows = new ArrayList<String[]>();

    /**
     * Creates the group.
     *
     * @param aParent - parent with a grid layout
     * @param aTitle - title of the group
     */
    public SummaryGroup(Composite aParent, String aTitle) {

        group = new Group(aParent, SWT.NONE);
        group.setText(aTitle);
        group.setLayout(new GridLayout(2, false));
        group.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    }

    /**
     * Returns the group, e.g. to change its layout data.
     */
    public Group getControl() {
        return group;
    }

    /**
     * Removes all rows. The new rows are shown by {@link #update()}.
     */
    public void clear() {
        rows.clear();
    }

    /**
     * Adds a row.
     *
     * @param aTerm - term, e.g. <code>Files:</code>
     * @param aValue - value, e.g. <code>5 exported</code>
     */
    public void addRow(String aTerm, String aValue) {
        rows.add(new String[] { aTerm, aValue });
    }

    /**
     * Shows the rows, that have been added since {@link #clear()}, and lays
     * out the parent.
     */
    public void update() {

        for (Control child : group.getChildren()) {
            child.dispose();
        }

        for (String[] row : rows) {
            Label term = new Label(group, SWT.NONE);
            term.setText(row[0]);
            term.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT));
            term.setLayoutData(new GridData(SWT.BEGINNING, SWT.TOP, false, false));

            Label value = new Label(group, SWT.WRAP);
            value.setText(row[1]);
            value.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        }

        group.getParent().layout(true, true);
    }
}
