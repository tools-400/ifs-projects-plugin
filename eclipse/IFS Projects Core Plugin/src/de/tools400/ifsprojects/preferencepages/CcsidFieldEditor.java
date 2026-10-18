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

import de.tools400.ifsprojects.swt.CcsidCombo;
import de.tools400.ifsprojects.swt.ToolTips;

/**
 * Field editor of the CCSID of new source files, see {@link CcsidCombo}. The
 * value is stored as integer.
 */
public class CcsidFieldEditor extends FieldEditor {

    private CcsidCombo ccsidCombo;
    private boolean isValid;

    public CcsidFieldEditor(String aName, String aLabelText, Composite aParent) {
        init(aName, aLabelText);
        isValid = true;
        createControl(aParent);
    }

    /**
     * Sets the tool tip of the label and the combo box.
     *
     * @param aToolTipText - tool tip text
     */
    public void setToolTipText(String aToolTipText) {
        ToolTips.set(aToolTipText, getLabelControl(), ccsidCombo.getControl());
    }

    @Override
    protected void adjustForNumColumns(int aNumColumns) {
        ((GridData)ccsidCombo.getControl().getLayoutData()).horizontalSpan = aNumColumns - 1;
    }

    @Override
    protected void doFillIntoGrid(Composite aParent, int aNumColumns) {

        getLabelControl(aParent);

        ccsidCombo = new CcsidCombo(aParent);
        GridData comboData = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
        comboData.horizontalSpan = aNumColumns - 1;
        ccsidCombo.getControl().setLayoutData(comboData);

        ccsidCombo.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent anEvent) {
                valueChanged();
            }
        });
    }

    @Override
    protected void doLoad() {
        ccsidCombo.setCcsid(getPreferenceStore().getInt(getPreferenceName()));
    }

    @Override
    protected void doLoadDefault() {
        ccsidCombo.setCcsid(getPreferenceStore().getDefaultInt(getPreferenceName()));
        valueChanged();
    }

    @Override
    protected void doStore() {

        Integer ccsid = ccsidCombo.getCcsid();
        if (ccsid != null) {
            getPreferenceStore().setValue(getPreferenceName(), ccsid.intValue());
        }
    }

    @Override
    public int getNumberOfControls() {
        return 2;
    }

    @Override
    public boolean isValid() {
        return isValid;
    }

    @Override
    protected void refreshValidState() {

        String errorMessage = ccsidCombo.getErrorMessage();
        isValid = errorMessage == null;

        if (isValid) {
            clearErrorMessage();
        } else {
            showErrorMessage(errorMessage);
        }
    }

    @Override
    public void setFocus() {
        if (ccsidCombo != null) {
            ccsidCombo.getControl().setFocus();
        }
    }

    @Override
    public void setEnabled(boolean anEnabled, Composite aParent) {
        super.setEnabled(anEnabled, aParent);
        ccsidCombo.setEnabled(anEnabled);
    }

    private void valueChanged() {

        setPresentsDefaultValue(false);

        boolean oldState = isValid;
        refreshValidState();
        if (isValid != oldState) {
            fireStateChanged(IS_VALID, oldState, isValid);
        }
    }
}
