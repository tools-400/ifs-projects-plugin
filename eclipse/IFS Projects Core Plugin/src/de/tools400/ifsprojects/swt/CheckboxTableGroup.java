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

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import de.tools400.ifsprojects.Messages;

/**
 * Table with a checkbox per row and the buttons 'Select All' and 'Deselect
 * All' to the right of the table. New elements are checked.
 *
 * @param <T> - type of the elements
 */
public class CheckboxTableGroup<T> {

    private final Composite composite;
    private final CheckboxTableViewer viewer;
    private final List<Runnable> listeners = new ArrayList<Runnable>();

    /**
     * Creates the group.
     *
     * @param aParent - parent with a grid layout
     * @param aColumnTitles - titles of the columns
     * @param aColumnWidths - widths of the columns
     * @param aLabelProvider - texts of the columns
     */
    public CheckboxTableGroup(Composite aParent, String[] aColumnTitles, int[] aColumnWidths, ITableLabelProvider aLabelProvider) {

        composite = new Composite(aParent, SWT.NONE);
        GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        composite.setLayout(layout);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Table table = new Table(composite, SWT.CHECK | SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        for (int i = 0; i < aColumnTitles.length; i++) {
            TableColumn column = new TableColumn(table, SWT.LEFT);
            column.setText(aColumnTitles[i]);
            column.setWidth(aColumnWidths[i]);
        }

        viewer = new CheckboxTableViewer(table);
        viewer.setContentProvider(ArrayContentProvider.getInstance());
        viewer.setLabelProvider(aLabelProvider);
        viewer.addCheckStateListener(new ICheckStateListener() {
            public void checkStateChanged(CheckStateChangedEvent anEvent) {
                notifyListeners();
            }
        });

        Composite buttons = new Composite(composite, SWT.NONE);
        GridLayout buttonsLayout = new GridLayout(1, true);
        buttonsLayout.marginWidth = 0;
        buttonsLayout.marginHeight = 0;
        buttons.setLayout(buttonsLayout);
        buttons.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
        createButton(buttons, Messages.Label_Select_all, true);
        createButton(buttons, Messages.Label_Deselect_all, false);
    }

    /**
     * Returns the composite of table and buttons, e.g. to change its layout
     * data, which fills the parent by default.
     */
    public Composite getControl() {
        return composite;
    }

    /**
     * Returns the table, e.g. to set its size hints.
     */
    public Table getTable() {
        return viewer.getTable();
    }

    /**
     * Shows elements, which are all checked.
     *
     * @param anElements - elements
     */
    public void setInput(List<T> anElements) {
        viewer.setInput(anElements);
        viewer.setAllChecked(true);
        notifyListeners();
    }

    /**
     * Returns the checked elements in the order of the table.
     */
    @SuppressWarnings("unchecked")
    public List<T> getCheckedElements() {

        List<T> elements = new ArrayList<T>();
        for (Object element : viewer.getCheckedElements()) {
            elements.add((T)element);
        }

        return elements;
    }

    /**
     * Adds a listener, that is called, when the checked elements change.
     *
     * @param aListener - listener
     */
    public void addCheckListener(Runnable aListener) {
        listeners.add(aListener);
    }

    private void createButton(Composite aParent, String aLabel, final boolean anIsChecked) {

        Button button = new Button(aParent, SWT.PUSH);
        button.setText(aLabel);
        button.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent anEvent) {
                viewer.setAllChecked(anIsChecked);
                notifyListeners();
            }
        });
    }

    private void notifyListeners() {
        for (Runnable listener : listeners) {
            listener.run();
        }
    }
}
