/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.swt;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Sets tool tips of controls. A field and its label get the same tool tip.
 */
public final class ToolTips {

    private ToolTips() {
        // Utility class
    }

    /**
     * Sets the tool tip of controls, e.g. of a field and its label.
     *
     * @param aToolTip - tool tip text
     * @param aControls - controls, <code>null</code> values are ignored
     */
    public static void set(String aToolTip, Control... aControls) {
        for (Control control : aControls) {
            if (control != null) {
                control.setToolTipText(aToolTip);
            }
        }
    }

    /**
     * Sets the tool tip of the label and the text of a string field editor,
     * e.g. also of an integer field editor.
     *
     * @param anEditor - field editor
     * @param aParent - parent of the field editor
     * @param aToolTip - tool tip text
     */
    public static void set(StringFieldEditor anEditor, Composite aParent, String aToolTip) {
        set(aToolTip, anEditor.getLabelControl(aParent), anEditor.getTextControl(aParent));
    }

    /**
     * Sets the tool tips of a radio group field editor: the tool tip of the
     * group and the tool tips of the radio buttons in the order of the
     * buttons.
     *
     * @param anEditor - field editor
     * @param aParent - parent of the field editor
     * @param aGroupToolTip - tool tip of the group
     * @param aButtonToolTips - tool tips of the radio buttons
     */
    public static void set(RadioGroupFieldEditor anEditor, Composite aParent, String aGroupToolTip, String... aButtonToolTips) {

        Composite radioBox = anEditor.getRadioBoxControl(aParent);
        set(aGroupToolTip, radioBox);

        int index = 0;
        for (Control control : radioBox.getChildren()) {
            if (control instanceof Button && index < aButtonToolTips.length) {
                control.setToolTipText(aButtonToolTips[index]);
                index++;
            }
        }
    }

    /**
     * Sets the tool tip of the check box of a boolean field editor.
     *
     * @param anEditor - field editor
     * @param aParent - parent of the field editor
     * @param aToolTip - tool tip text
     */
    public static void set(BooleanFieldEditor anEditor, Composite aParent, String aToolTip) {
        set(aToolTip, anEditor.getDescriptionControl(aParent));
    }
}
