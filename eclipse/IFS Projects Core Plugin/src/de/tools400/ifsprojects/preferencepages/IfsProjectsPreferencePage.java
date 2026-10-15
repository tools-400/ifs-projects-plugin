/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.preferencepages;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.build.BuildMode;
import de.tools400.ifsprojects.preferences.Preferences;
import de.tools400.ifsprojects.project.BuildTool;
import de.tools400.ifsprojects.swt.ToolTips;

/**
 * Preference page of the IFS Projects plug-in.
 * <p>
 * The defaults of the IFS projects are placed in a group. The preset combo box
 * of the group fills the default build and compile command of a build tool and
 * shows the build tool, whose commands are entered.
 */
public class IfsProjectsPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    private static final int MAX_PORT = 65535;
    private static final int MAX_SECONDS = 3600;
    private static final int NUM_COLUMNS = 2;
    private static final String LINE_SEPARATOR = "\n"; //$NON-NLS-1$
    private static final String LABEL_SEPARATOR = ": "; //$NON-NLS-1$

    private Combo presetCombo;
    private StringFieldEditor buildCommand;
    private StringFieldEditor compileCommand;

    public IfsProjectsPreferencePage() {
        super(GRID);
        setPreferenceStore(Preferences.getInstance().getPreferenceStore());
        setDescription(Messages.Description_Preference_page);
    }

    public void init(IWorkbench aWorkbench) {
    }

    @Override
    protected void createFieldEditors() {

        IntegerFieldEditor port = new IntegerFieldEditor(Preferences.PORT, Messages.Label_SSH_port_colon, getFieldEditorParent());
        port.setValidRange(1, MAX_PORT);
        ToolTips.set(port, getFieldEditorParent(), Messages.Tooltip_SSH_port);
        addField(port);

        IntegerFieldEditor timeout = new IntegerFieldEditor(Preferences.TIMEOUT, Messages.Label_Connection_timeout_colon, getFieldEditorParent());
        timeout.setValidRange(1, MAX_SECONDS);
        ToolTips.set(timeout, getFieldEditorParent(), Messages.Tooltip_Connection_timeout);
        addField(timeout);

        IntegerFieldEditor keepAlive = new IntegerFieldEditor(Preferences.KEEP_ALIVE, Messages.Label_Keep_alive_interval_colon,
            getFieldEditorParent());
        keepAlive.setValidRange(1, MAX_SECONDS);
        ToolTips.set(keepAlive, getFieldEditorParent(), Messages.Tooltip_Keep_alive_interval);
        addField(keepAlive);

        StringFieldEditor encoding = new StringFieldEditor(Preferences.ENCODING, Messages.Label_Encoding_colon, getFieldEditorParent());
        encoding.setEmptyStringAllowed(false);
        ToolTips.set(encoding, getFieldEditorParent(), Messages.Tooltip_Encoding);
        addField(encoding);

        addBooleanField(Preferences.LOWERCASE_USER_ID, Messages.Label_Convert_user_ID_to_lowercase, Messages.Tooltip_Convert_user_ID_to_lowercase);

        addBooleanField(Preferences.USE_SAVED_PASSWORD, Messages.Label_Use_password_saved_in_RSE, Messages.Tooltip_Use_password_saved_in_RSE);

        addBooleanField(Preferences.RESTORE_TERMINALS, Messages.Label_Restore_terminals_after_restart,
            Messages.Tooltip_Restore_terminals_after_restart);

        RadioGroupFieldEditor buildMode = new RadioGroupFieldEditor(Preferences.BUILD_MODE, Messages.Label_Run_build_and_compile_commands, 1,
            new String[][] { { Messages.Label_Build_mode_terminal, BuildMode.TERMINAL.getValue() },
                { Messages.Label_Build_mode_rse, BuildMode.RSE.getValue() } },
            getFieldEditorParent(), true);
        ToolTips.set(buildMode, getFieldEditorParent(), Messages.Tooltip_Build_mode, Messages.Tooltip_Build_mode_terminal,
            Messages.Tooltip_Build_mode_rse);
        addField(buildMode);

        addBooleanField(Preferences.REUSE_BUILD_TERMINAL, Messages.Label_Reuse_build_terminal, Messages.Tooltip_Reuse_build_terminal);

        createDefaultsGroup(getFieldEditorParent());
    }

    private void addBooleanField(String aName, String aLabel, String aToolTip) {

        BooleanFieldEditor fieldEditor = new BooleanFieldEditor(aName, aLabel, getFieldEditorParent());
        ToolTips.set(fieldEditor, getFieldEditorParent(), aToolTip);
        addField(fieldEditor);
    }

    /**
     * Creates the group of the defaults of the IFS projects: preset, build
     * command, compile command and environment variables.
     */
    private void createDefaultsGroup(Composite aParent) {

        Group group = new Group(aParent, SWT.NONE);
        group.setText(Messages.Label_Group_Defaults_of_IFS_projects);
        GridData groupData = new GridData(SWT.FILL, SWT.FILL, true, true);
        groupData.horizontalSpan = NUM_COLUMNS;
        group.setLayoutData(groupData);

        createPresetCombo(group);

        buildCommand = new StringFieldEditor(Preferences.DEFAULT_BUILD_COMMAND, Messages.Label_Build_command_colon, group);
        buildCommand.setEmptyStringAllowed(false);
        ToolTips.set(buildCommand, group, Messages.Tooltip_Default_build_command);
        addField(buildCommand);

        compileCommand = new StringFieldEditor(Preferences.DEFAULT_COMPILE_COMMAND, Messages.Label_Compile_command_colon, group);
        compileCommand.setEmptyStringAllowed(false);
        ToolTips.set(compileCommand, group, Messages.Tooltip_Compile_command_placeholders);
        addField(compileCommand);

        EnvironmentVariablesFieldEditor environmentVariables = new EnvironmentVariablesFieldEditor(Preferences.DEFAULT_ENVIRONMENT_VARIABLES,
            Messages.Label_Environment_variables_colon, group);
        environmentVariables.setToolTipText(Messages.Tooltip_Default_environment_variables);
        addField(environmentVariables);

        // FieldEditor.createControl() replaces the layout of its parent.
        group.setLayout(new GridLayout(NUM_COLUMNS, false));

        ModifyListener commandListener = new ModifyListener() {
            public void modifyText(ModifyEvent anEvent) {
                updatePresetCombo();
            }
        };
        buildCommand.getTextControl(group).addModifyListener(commandListener);
        compileCommand.getTextControl(group).addModifyListener(commandListener);
    }

    /**
     * Creates the combo box, that fills the default build and compile command
     * of a build tool. The last item stands for other commands. The commands
     * are saved with 'Apply' or 'OK'.
     */
    private void createPresetCombo(Composite aParent) {

        Label label = new Label(aParent, SWT.NONE);
        label.setText(Messages.Label_Preset_colon);

        presetCombo = new Combo(aParent, SWT.DROP_DOWN | SWT.READ_ONLY);
        presetCombo.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

        StringBuilder toolTip = new StringBuilder();
        for (BuildTool buildTool : BuildTool.values()) {
            presetCombo.add(buildTool.getLabel());
            if (toolTip.length() > 0) {
                toolTip.append(LINE_SEPARATOR);
            }
            toolTip.append(buildTool.getLabel()).append(LABEL_SEPARATOR);
            toolTip.append(Messages.bind(Messages.Tooltip_Preset_A_B, buildTool.getBuildCommand(), buildTool.getCompileCommand()));
        }
        presetCombo.add(Messages.Label_Preset_custom);
        ToolTips.set(toolTip.toString(), label, presetCombo);

        presetCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent anEvent) {
                int index = presetCombo.getSelectionIndex();
                if (index >= 0 && index < BuildTool.values().length) {
                    BuildTool buildTool = BuildTool.values()[index];
                    buildCommand.setStringValue(buildTool.getBuildCommand());
                    compileCommand.setStringValue(buildTool.getCompileCommand());
                }
                updatePresetCombo();
            }
        });
    }

    /**
     * Selects the build tool, whose commands are entered, or the item for other
     * commands.
     */
    private void updatePresetCombo() {

        if (presetCombo == null || buildCommand == null || compileCommand == null) {
            return;
        }

        BuildTool buildTool = BuildTool.find(buildCommand.getStringValue(), compileCommand.getStringValue());
        presetCombo.select(buildTool == null ? BuildTool.values().length : buildTool.ordinal());
    }
}
