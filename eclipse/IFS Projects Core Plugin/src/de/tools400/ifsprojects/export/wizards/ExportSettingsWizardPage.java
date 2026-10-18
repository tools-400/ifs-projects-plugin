/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.export.wizards;

import java.util.Locale;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.export.ExportSettings;
import de.tools400.ifsprojects.export.SourceFileRule;
import de.tools400.ifsprojects.swt.CcsidCombo;
import de.tools400.ifsprojects.swt.ToolTips;
import de.tools400.ifsprojects.syntaxcheck.SyntaxCheckCommands;

/**
 * First page of the export: library, source files, CCSID and record length of
 * new source files and member text. 'Next' reads the project and the library
 * and shows the {@link ExportPreviewWizardPage}.
 */
public class ExportSettingsWizardPage extends WizardPage {

    private static final String PAGE_NAME = "ExportSettingsWizardPage"; //$NON-NLS-1$
    private static final String QTEMP = "QTEMP"; //$NON-NLS-1$
    private static final int NUMBER_WIDTH = 60;

    private final ExportSettings initialSettings;

    private Text libraryText;
    private Button directoryButton;
    private Button sourceTypeButton;
    private Button singleFileButton;
    private Text sourceFileText;
    private CcsidCombo ccsidCombo;
    private Text recordLengthText;
    private Button memberTextButton;

    /**
     * Creates the page.
     *
     * @param aProjectName - name of the project
     * @param aSettings - settings of the last export or defaults
     */
    public ExportSettingsWizardPage(String aProjectName, ExportSettings aSettings) {
        super(PAGE_NAME);
        this.initialSettings = aSettings;
        setTitle(Messages.Title_Export_to_library);
        setDescription(Messages.bind(Messages.Description_Export_settings_page, aProjectName));
    }

    public void createControl(Composite aParent) {

        Composite main = new Composite(aParent, SWT.NONE);
        main.setLayout(new GridLayout(2, false));

        Label libraryLabel = new Label(main, SWT.NONE);
        libraryLabel.setText(Messages.Label_Library_colon);
        libraryText = new Text(main, SWT.BORDER);
        libraryText.setTextLimit(10);
        libraryText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        ToolTips.set(Messages.Tooltip_Export_library, libraryLabel, libraryText);

        createSourceFileGroup(main);
        createNewSourceFilesGroup(main);

        memberTextButton = new Button(main, SWT.CHECK);
        memberTextButton.setText(Messages.Label_Member_text_from_TEXT);
        memberTextButton.setLayoutData(createSpanningGridData());
        ToolTips.set(Messages.Tooltip_Member_text_from_TEXT, memberTextButton);

        setValues(initialSettings);

        ModifyListener modifyListener = new ModifyListener() {
            public void modifyText(ModifyEvent anEvent) {
                validate();
            }
        };
        libraryText.addModifyListener(modifyListener);
        sourceFileText.addModifyListener(modifyListener);
        ccsidCombo.addModifyListener(modifyListener);
        recordLengthText.addModifyListener(modifyListener);

        SelectionAdapter ruleListener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent anEvent) {
                sourceFileText.setEnabled(singleFileButton.getSelection());
                validate();
            }
        };
        directoryButton.addSelectionListener(ruleListener);
        sourceTypeButton.addSelectionListener(ruleListener);
        singleFileButton.addSelectionListener(ruleListener);

        setControl(main);

        validate();
        setErrorMessage(null);

        libraryText.setFocus();
    }

    /**
     * Returns the settings, that have been entered. Must only be called, if
     * the page is complete.
     */
    public ExportSettings getSettings() {

        ExportSettings settings = new ExportSettings(getLibrary(), ccsidCombo.getCcsid().intValue(), getNumber(recordLengthText));
        if (singleFileButton.getSelection()) {
            settings.setSourceFileRule(SourceFileRule.SINGLE_FILE);
        } else if (sourceTypeButton.getSelection()) {
            settings.setSourceFileRule(SourceFileRule.SOURCE_TYPE);
        } else {
            settings.setSourceFileRule(SourceFileRule.DIRECTORY);
        }
        settings.setSourceFile(getSourceFile());
        settings.setMemberText(memberTextButton.getSelection());
        settings.setConvertIncludes(initialSettings.isConvertIncludes());

        return settings;
    }

    /**
     * Only checks the input, because the default implementation would read the
     * project, see {@link #getNextPage()}.
     */
    @Override
    public boolean canFlipToNextPage() {
        return isPageComplete();
    }

    /**
     * Reads the project and the library and returns the preview, or
     * <code>null</code>, if they could not be read or the user does not want
     * to create the library.
     */
    @Override
    public IWizardPage getNextPage() {
        return ((ExportToLibraryWizard)getWizard()).analyze();
    }

    private void createSourceFileGroup(Composite aParent) {

        Group group = new Group(aParent, SWT.NONE);
        group.setText(Messages.Label_Group_Source_files);
        group.setLayout(new GridLayout(2, false));
        group.setLayoutData(createSpanningGridData());

        directoryButton = createRadioButton(group, Messages.Label_Source_file_by_directory, Messages.Tooltip_Source_file_by_directory);
        directoryButton.setLayoutData(createSpanningGridData());

        sourceTypeButton = createRadioButton(group, Messages.Label_Source_file_by_source_type, Messages.Tooltip_Source_file_by_source_type);
        sourceTypeButton.setLayoutData(createSpanningGridData());

        singleFileButton = createRadioButton(group, Messages.Label_Single_source_file_colon, Messages.Tooltip_Single_source_file);
        sourceFileText = new Text(group, SWT.BORDER);
        sourceFileText.setTextLimit(10);
        sourceFileText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        ToolTips.set(Messages.Tooltip_Single_source_file, sourceFileText);
    }

    private void createNewSourceFilesGroup(Composite aParent) {

        Group group = new Group(aParent, SWT.NONE);
        group.setText(Messages.Label_Group_New_source_files);
        group.setLayout(new GridLayout(2, false));
        group.setLayoutData(createSpanningGridData());

        Label ccsidLabel = new Label(group, SWT.NONE);
        ccsidLabel.setText(Messages.Label_CCSID_colon);
        ccsidCombo = new CcsidCombo(group);
        ccsidCombo.getControl().setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        ToolTips.set(Messages.Tooltip_Export_CCSID, ccsidLabel, ccsidCombo.getControl());

        recordLengthText = createNumberField(group, Messages.Label_Record_length_colon, Messages.Tooltip_Export_record_length);
    }

    private void setValues(ExportSettings aSettings) {

        libraryText.setText(aSettings.getLibrary() == null ? "" : aSettings.getLibrary()); //$NON-NLS-1$
        directoryButton.setSelection(aSettings.getSourceFileRule() == SourceFileRule.DIRECTORY);
        sourceTypeButton.setSelection(aSettings.getSourceFileRule() == SourceFileRule.SOURCE_TYPE);
        singleFileButton.setSelection(aSettings.getSourceFileRule() == SourceFileRule.SINGLE_FILE);
        sourceFileText.setText(aSettings.getSourceFile() == null ? "" : aSettings.getSourceFile()); //$NON-NLS-1$
        sourceFileText.setEnabled(singleFileButton.getSelection());
        ccsidCombo.setCcsid(aSettings.getCcsid());
        recordLengthText.setText(Integer.toString(aSettings.getRecordLength()));
        memberTextButton.setSelection(aSettings.isMemberText());
    }

    private void validate() {

        String errorMessage = null;
        String ccsidErrorMessage = ccsidCombo.getErrorMessage();

        if (!SyntaxCheckCommands.isObjectName(getLibrary())) {
            errorMessage = Messages.Error_Library_name_invalid;
        } else if (QTEMP.equals(getLibrary())) {
            errorMessage = Messages.Error_QTEMP_not_allowed;
        } else if (singleFileButton.getSelection() && !SyntaxCheckCommands.isObjectName(getSourceFile())) {
            errorMessage = Messages.Error_Source_file_name_invalid;
        } else if (ccsidErrorMessage != null) {
            errorMessage = ccsidErrorMessage;
        } else if (ExportSettings.parseInt(recordLengthText.getText(), ExportSettings.MIN_RECORD_LENGTH, ExportSettings.MAX_RECORD_LENGTH) == null) {
            errorMessage = Messages.bind(Messages.Error_Record_length_invalid_A_B, Integer.toString(ExportSettings.MIN_RECORD_LENGTH),
                Integer.toString(ExportSettings.MAX_RECORD_LENGTH));
        }

        setErrorMessage(errorMessage);
        setPageComplete(errorMessage == null);
    }

    private String getLibrary() {
        return libraryText.getText().trim().toUpperCase(Locale.ROOT);
    }

    private String getSourceFile() {
        return sourceFileText.getText().trim().toUpperCase(Locale.ROOT);
    }

    private static int getNumber(Text aText) {
        return Integer.parseInt(aText.getText().trim());
    }

    private Button createRadioButton(Composite aParent, String aLabel, String aToolTip) {

        Button button = new Button(aParent, SWT.RADIO);
        button.setText(aLabel);
        ToolTips.set(aToolTip, button);

        return button;
    }

    private Text createNumberField(Composite aParent, String aLabel, String aToolTip) {

        Label label = new Label(aParent, SWT.NONE);
        label.setText(aLabel);

        Text text = new Text(aParent, SWT.BORDER);
        GridData gridData = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
        gridData.widthHint = NUMBER_WIDTH;
        text.setLayoutData(gridData);
        ToolTips.set(aToolTip, label, text);

        return text;
    }

    private GridData createSpanningGridData() {
        GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gridData.horizontalSpan = 2;
        return gridData;
    }
}
