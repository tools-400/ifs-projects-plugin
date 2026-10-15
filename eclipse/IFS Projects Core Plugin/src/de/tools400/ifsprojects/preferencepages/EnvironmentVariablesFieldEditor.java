/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.preferencepages;

import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.project.EnvironmentVariables;
import de.tools400.ifsprojects.swt.ToolTips;

/**
 * Field editor of environment variables: a multi-line text with one variable
 * per line, <code>NAME=value</code>. The label is placed above the text and
 * both span all columns of the preference page.
 * <p>
 * The field editor is implemented from scratch, because
 * <code>StringFieldEditor.createTextWidget()</code>, which would allow a
 * multi-line text, is not available in RDi 9.6.
 */
public class EnvironmentVariablesFieldEditor extends FieldEditor {

    private static final int TEXT_LINES = 6;

    private Text text;
    private String oldValue;
    private boolean isValid;

    public EnvironmentVariablesFieldEditor(String aName, String aLabelText, Composite aParent) {
        init(aName, aLabelText);
        isValid = true;
        createControl(aParent);
    }

    /**
     * Sets the tool tip of the label and the text.
     *
     * @param aToolTipText - tool tip text
     */
    public void setToolTipText(String aToolTipText) {
        ToolTips.set(aToolTipText, getLabelControl(), text);
    }

    @Override
    protected void adjustForNumColumns(int aNumColumns) {
        ((GridData)getLabelControl().getLayoutData()).horizontalSpan = aNumColumns;
        ((GridData)text.getLayoutData()).horizontalSpan = aNumColumns;
    }

    @Override
    protected void doFillIntoGrid(Composite aParent, int aNumColumns) {

        Label label = getLabelControl(aParent);
        GridData labelData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        labelData.horizontalSpan = aNumColumns;
        label.setLayoutData(labelData);

        text = new Text(aParent, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
        text.setFont(aParent.getFont());
        GridData textData = new GridData(SWT.FILL, SWT.FILL, true, true);
        textData.horizontalSpan = aNumColumns;
        textData.heightHint = text.getLineHeight() * TEXT_LINES;
        text.setLayoutData(textData);

        text.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent anEvent) {
                valueChanged();
            }
        });
    }

    @Override
    protected void doLoad() {
        setText(getPreferenceStore().getString(getPreferenceName()));
    }

    @Override
    protected void doLoadDefault() {
        setText(getPreferenceStore().getDefaultString(getPreferenceName()));
        valueChanged();
    }

    @Override
    protected void doStore() {
        getPreferenceStore().setValue(getPreferenceName(), text.getText());
    }

    @Override
    public int getNumberOfControls() {
        return 1;
    }

    @Override
    public boolean isValid() {
        return isValid;
    }

    @Override
    protected void refreshValidState() {

        int invalidLine = EnvironmentVariables.findInvalidLine(text.getText());
        isValid = invalidLine == 0;

        if (isValid) {
            clearErrorMessage();
        } else {
            showErrorMessage(Messages.bind(Messages.Error_Invalid_environment_variable_in_line_A, Integer.toString(invalidLine)));
        }
    }

    @Override
    public void setFocus() {
        if (text != null) {
            text.setFocus();
        }
    }

    @Override
    public void setEnabled(boolean anEnabled, Composite aParent) {
        super.setEnabled(anEnabled, aParent);
        text.setEnabled(anEnabled);
    }

    private void setText(String aValue) {
        text.setText(aValue == null ? "" : aValue); //$NON-NLS-1$
        oldValue = text.getText();
    }

    private void valueChanged() {

        setPresentsDefaultValue(false);

        boolean oldState = isValid;
        refreshValidState();
        if (isValid != oldState) {
            fireStateChanged(IS_VALID, oldState, isValid);
        }

        String newValue = text.getText();
        if (!newValue.equals(oldValue)) {
            fireValueChanged(VALUE, oldValue, newValue);
            oldValue = newValue;
        }
    }
}
