/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.export.wizards;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.export.SourceTypeProposals;
import de.tools400.ifsprojects.swt.CheckboxTableGroup;

/**
 * Selects the further source types of an export from the extensions of the
 * project, see {@link SourceTypeProposals}. The further source types of the
 * settings are checked. The proposed extensions are only hints: they are listed
 * first, the column 'Hint' tells, why they are proposed, and 'Select Proposed'
 * checks them.
 */
class SourceTypesDialog extends Dialog {

    private static final int COLUMN_EXTENSION = 0;
    private static final int COLUMN_FILES = 1;
    private static final int COLUMN_EXAMPLE = 2;
    private static final int COLUMN_HINT = 3;
    private static final int[] COLUMN_WIDTHS = { 90, 60, 260, 260 };
    private static final int TABLE_HEIGHT = 250;
    private static final int TEXT_WIDTH = 600;

    private final List<SourceTypeProposals.Proposal> proposals;
    private final List<String> selectedSourceTypes;

    private CheckboxTableGroup<SourceTypeProposals.Proposal> tableGroup;

    /**
     * Creates the dialog.
     *
     * @param aParentShell - parent shell
     * @param aProposals - extensions of the project, see
     *        {@link de.tools400.ifsprojects.export.ExportPlan#getSourceTypeProposals()}
     */
    SourceTypesDialog(Shell aParentShell, List<SourceTypeProposals.Proposal> aProposals) {
        super(aParentShell);
        this.proposals = aProposals;
        this.selectedSourceTypes = new ArrayList<>();
    }

    /**
     * Returns the selected source types, after the dialog has been closed with
     * 'OK'.
     *
     * @return source types in uppercase, e.g. <code>TBL</code>
     */
    List<String> getSelectedSourceTypes() {
        return selectedSourceTypes;
    }

    @Override
    protected void configureShell(Shell aShell) {
        super.configureShell(aShell);
        aShell.setText(Messages.Title_Further_source_types);
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    @Override
    protected Control createDialogArea(Composite aParent) {

        Composite area = (Composite)super.createDialogArea(aParent);

        Label label = new Label(area, SWT.WRAP);
        label.setText(Messages.Label_Further_source_types_dialog);
        GridData labelData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        labelData.widthHint = TEXT_WIDTH;
        label.setLayoutData(labelData);

        String[] titles = { Messages.Label_Column_Extension, Messages.Label_Column_Files, Messages.Label_Column_Example,
            Messages.Label_Column_Hint };
        tableGroup = new CheckboxTableGroup<>(area, titles, COLUMN_WIDTHS, new ProposalLabelProvider());
        ((GridData)tableGroup.getTable().getLayoutData()).heightHint = TABLE_HEIGHT;

        tableGroup.setInput(proposals);
        for (SourceTypeProposals.Proposal proposal : proposals) {
            tableGroup.getViewer().setChecked(proposal, proposal.isSelected());
        }

        Button proposedButton = tableGroup.addButton(Messages.Label_Select_proposed, new Runnable() {
            public void run() {
                for (SourceTypeProposals.Proposal proposal : proposals) {
                    if (proposal.getReason() != null) {
                        tableGroup.getViewer().setChecked(proposal, true);
                    }
                }
            }
        });
        proposedButton.setEnabled(hasProposals());

        return area;
    }

    /**
     * Returns <code>true</code>, if at least one extension is proposed.
     */
    private boolean hasProposals() {
        for (SourceTypeProposals.Proposal proposal : proposals) {
            if (proposal.getReason() != null) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void okPressed() {

        selectedSourceTypes.clear();
        for (SourceTypeProposals.Proposal proposal : tableGroup.getCheckedElements()) {
            selectedSourceTypes.add(proposal.getSourceType());
        }

        super.okPressed();
    }

    /**
     * Returns the text of the column 'Hint' of an extension.
     */
    private static String getHint(SourceTypeProposals.Proposal aProposal) {

        if (aProposal.getReason() == null) {
            return aProposal.isSelected() ? Messages.Label_Hint_Selected : ""; //$NON-NLS-1$
        }

        switch (aProposal.getReason()) {
        case IBM_SOURCE_TYPE:
            return Messages.Label_Hint_IBM_source_type;
        case CODE_FOR_IBM_I:
            return Messages.Label_Hint_Code_for_IBM_i;
        default:
            String directory = aProposal.getDirectory().length() == 0 ? Messages.Label_Project_directory : aProposal.getDirectory();
            return Messages.bind(Messages.Label_Hint_Source_directory_A, directory);
        }
    }

    /**
     * Texts of the columns.
     */
    private static final class ProposalLabelProvider extends LabelProvider implements ITableLabelProvider {

        public Image getColumnImage(Object anElement, int aColumnIndex) {
            return null;
        }

        public String getColumnText(Object anElement, int aColumnIndex) {

            SourceTypeProposals.Proposal proposal = (SourceTypeProposals.Proposal)anElement;
            switch (aColumnIndex) {
            case COLUMN_EXTENSION:
                return proposal.getExtension();
            case COLUMN_FILES:
                return Integer.toString(proposal.getFileCount());
            case COLUMN_EXAMPLE:
                return proposal.getExample();
            case COLUMN_HINT:
                return getHint(proposal);
            default:
                return ""; //$NON-NLS-1$
            }
        }
    }
}
