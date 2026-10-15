/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.dialogs;

import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import de.tools400.ifsprojects.swt.ToolTips;

/**
 * Input dialog with a tool tip for the input field and its label.
 */
public class ToolTipInputDialog extends InputDialog {

    private final String toolTip;

    /**
     * Creates an input dialog.
     *
     * @param aParentShell - parent shell
     * @param aTitle - title of the dialog
     * @param aMessage - message, which is the label of the input field
     * @param anInitialValue - initial value or <code>null</code>
     * @param aValidator - validator or <code>null</code>
     * @param aToolTip - tool tip of the input field and its label
     */
    public ToolTipInputDialog(Shell aParentShell, String aTitle, String aMessage, String anInitialValue, IInputValidator aValidator,
        String aToolTip) {
        super(aParentShell, aTitle, aMessage, anInitialValue, aValidator);
        this.toolTip = aToolTip;
    }

    @Override
    protected Control createDialogArea(Composite aParent) {

        Control area = super.createDialogArea(aParent);

        ToolTips.set(toolTip, getText());
        if (area instanceof Composite) {
            for (Control child : ((Composite)area).getChildren()) {
                if (child instanceof Label) {
                    ToolTips.set(toolTip, child);
                }
            }
        }

        return area;
    }
}
