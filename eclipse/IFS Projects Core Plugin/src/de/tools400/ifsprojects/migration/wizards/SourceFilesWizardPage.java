/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.migration.wizards;

import java.util.List;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.migration.IncludeStyle;
import de.tools400.ifsprojects.migration.MigrationSettings;
import de.tools400.ifsprojects.migration.SourceFile;
import de.tools400.ifsprojects.swt.CcsidCombo;
import de.tools400.ifsprojects.swt.CheckboxTableGroup;
import de.tools400.ifsprojects.swt.ToolTips;
import de.tools400.ifsprojects.utils.StringUtils;

/**
 * First page of the migration: the source files, whose members are migrated,
 * and the options.
 */
public class SourceFilesWizardPage extends WizardPage {

    private static final String PAGE_NAME = "SourceFilesWizardPage"; //$NON-NLS-1$

    private static final int COLUMN_NAME = 0;
    private static final int COLUMN_TEXT = 1;
    private static final int COLUMN_MEMBERS = 2;
    private static final int COLUMN_CCSID = 3;
    private static final int[] COLUMN_WIDTHS = { 110, 300, 80, 70 };
    private static final int TABLE_HEIGHT = 200;

    private final List<SourceFile> sourceFiles;
    private final int defaultHexCcsid;

    private CheckboxTableGroup<SourceFile> sourceFilesGroup;
    private Button memberTextButton;
    private Button conditionalButton;
    private CcsidCombo hexCcsidCombo;

    /**
     * Creates the page.
     *
     * @param aLibrary - library of the source files
     * @param aSourceFiles - source files, which are all selected
     * @param aDefaultHexCcsid - default CCSID of source files with CCSID
     *        65535
     */
    public SourceFilesWizardPage(String aLibrary, List<SourceFile> aSourceFiles, int aDefaultHexCcsid) {
        super(PAGE_NAME);
        this.sourceFiles = aSourceFiles;
        this.defaultHexCcsid = aDefaultHexCcsid;
        setTitle(Messages.Title_Migrate_to_IFS_project);
        setDescription(Messages.bind(Messages.Description_Migration_source_files_page, aLibrary));
    }

    public void createControl(Composite aParent) {

        Composite main = new Composite(aParent, SWT.NONE);
        main.setLayout(new GridLayout(2, false));

        String[] titles = { Messages.Label_Column_Source_file, Messages.Label_Column_Text, Messages.Label_Column_Members,
            Messages.Label_Column_CCSID };
        sourceFilesGroup = new CheckboxTableGroup<SourceFile>(main, titles, COLUMN_WIDTHS, new SourceFileLabelProvider());
        ((GridData)sourceFilesGroup.getTable().getLayoutData()).heightHint = TABLE_HEIGHT;
        ((GridData)sourceFilesGroup.getControl().getLayoutData()).horizontalSpan = 2;

        memberTextButton = createCheckbox(main, Messages.Label_Member_text_as_comment, Messages.Tooltip_Member_text_as_comment, true);
        conditionalButton = createCheckbox(main, Messages.Label_Keep_member_directives, Messages.Tooltip_Keep_member_directives, false);

        Label hexCcsidLabel = new Label(main, SWT.NONE);
        hexCcsidLabel.setText(Messages.Label_CCSID_of_hex_source_files_colon);
        hexCcsidCombo = new CcsidCombo(main);
        hexCcsidCombo.getControl().setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        hexCcsidCombo.setCcsid(defaultHexCcsid);
        ToolTips.set(Messages.Tooltip_CCSID_of_hex_source_files, hexCcsidLabel, hexCcsidCombo.getControl());

        sourceFilesGroup.addCheckListener(new Runnable() {
            public void run() {
                validate();
            }
        });
        hexCcsidCombo.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent anEvent) {
                validate();
            }
        });

        sourceFilesGroup.setInput(sourceFiles);

        setControl(main);

        validate();
    }

    /**
     * Returns the selected source files.
     */
    public List<SourceFile> getSelectedSourceFiles() {
        return sourceFilesGroup.getCheckedElements();
    }

    /**
     * Returns the settings, that have been entered. Must only be called, if
     * the page is complete.
     */
    public MigrationSettings getSettings() {

        Integer hexCcsid = hexCcsidCombo.getCcsid();
        IncludeStyle style = conditionalButton.getSelection() ? IncludeStyle.CONDITIONAL : IncludeStyle.REPLACE;

        return new MigrationSettings(style, memberTextButton.getSelection(), hexCcsid == null ? defaultHexCcsid : hexCcsid.intValue());
    }

    /**
     * Only checks the input, because the default implementation would read the
     * members, see {@link #getNextPage()}.
     */
    @Override
    public boolean canFlipToNextPage() {
        return isPageComplete();
    }

    /**
     * Reads the members and returns the preview, or <code>null</code>, if
     * they could not be read.
     */
    @Override
    public IWizardPage getNextPage() {
        return ((MigrateToIfsProjectWizard)getWizard()).analyze();
    }

    private void validate() {

        boolean isHex = false;
        for (SourceFile sourceFile : getSelectedSourceFiles()) {
            isHex |= sourceFile.isHex();
        }
        hexCcsidCombo.setEnabled(isHex);

        String errorMessage = null;
        if (getSelectedSourceFiles().isEmpty()) {
            errorMessage = Messages.Error_Select_at_least_one_source_file;
        } else if (isHex) {
            errorMessage = hexCcsidCombo.getErrorMessage();
        }

        setErrorMessage(errorMessage);
        setPageComplete(errorMessage == null);
    }

    private Button createCheckbox(Composite aParent, String aLabel, String aToolTip, boolean anIsSelected) {

        Button button = new Button(aParent, SWT.CHECK);
        button.setText(aLabel);
        button.setSelection(anIsSelected);
        GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gridData.horizontalSpan = 2;
        button.setLayoutData(gridData);
        ToolTips.set(aToolTip, button);

        return button;
    }

    /**
     * Labels of the table.
     */
    private static class SourceFileLabelProvider extends LabelProvider implements ITableLabelProvider {

        public Image getColumnImage(Object anElement, int aColumnIndex) {
            return null;
        }

        public String getColumnText(Object anElement, int aColumnIndex) {

            SourceFile sourceFile = (SourceFile)anElement;
            switch (aColumnIndex) {
            case COLUMN_NAME:
                return sourceFile.getName();
            case COLUMN_TEXT:
                return StringUtils.nullToEmpty(sourceFile.getText());
            case COLUMN_MEMBERS:
                return Integer.toString(sourceFile.getMemberCount());
            case COLUMN_CCSID:
                return Integer.toString(sourceFile.getCcsid());
            default:
                return ""; //$NON-NLS-1$
            }
        }
    }
}
