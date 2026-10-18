/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.preferencepages;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;
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
 * Main preference page of the IFS Projects plug-in: the builds of IFS projects
 * and local IFS projects. The SSH terminals and the export to a library have
 * pages of their own, see {@link SshTerminalPreferencePage} and
 * {@link ExportPreferencePage}.
 * <p>
 * The defaults of the IFS projects are placed in a group. The preset combo box
 * of the group fills the default build and compile command of a build tool and
 * shows the build tool, whose commands are entered.
 */
public class IfsProjectsPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    private static final int NUM_COLUMNS = 2;
    private static final String LINE_SEPARATOR = "\n"; //$NON-NLS-1$
    private static final String LABEL_SEPARATOR = ": "; //$NON-NLS-1$

    private RadioGroupFieldEditor buildMode;
    private BooleanFieldEditor reuseBuildTerminal;
    private Combo presetCombo;
    private StringFieldEditor buildCommand;
    private StringFieldEditor compileCommand;

    public IfsProjectsPreferencePage() {
        super(GRID);
        setPreferenceStore(Preferences.getInstance().getPreferenceStore());
        setDescription(Messages.Description_Build_preference_page);
    }

    public void init(IWorkbench aWorkbench) {
    }

    @Override
    protected void createFieldEditors() {

        buildMode = new RadioGroupFieldEditor(Preferences.BUILD_MODE, Messages.Label_Run_build_and_compile_commands, 1, new String[][] {
            { Messages.Label_Build_mode_terminal, BuildMode.TERMINAL.getValue() }, { Messages.Label_Build_mode_rse, BuildMode.RSE.getValue() } },
            getFieldEditorParent(), true);
        ToolTips.set(buildMode, getFieldEditorParent(), Messages.Tooltip_Build_mode, Messages.Tooltip_Build_mode_terminal,
            Messages.Tooltip_Build_mode_rse);
        addField(buildMode);

        reuseBuildTerminal = new BooleanFieldEditor(Preferences.REUSE_BUILD_TERMINAL, Messages.Label_Reuse_build_terminal, getFieldEditorParent());
        ToolTips.set(reuseBuildTerminal, getFieldEditorParent(), Messages.Tooltip_Reuse_build_terminal);
        addField(reuseBuildTerminal);

        createDefaultsGroup(getFieldEditorParent());
    }

    @Override
    protected void initialize() {
        super.initialize();
        updateReuseBuildTerminal(getPreferenceStore().getString(Preferences.BUILD_MODE));
    }

    @Override
    protected void performDefaults() {
        super.performDefaults();
        updateReuseBuildTerminal(getPreferenceStore().getDefaultString(Preferences.BUILD_MODE));
    }

    @Override
    public void propertyChange(PropertyChangeEvent anEvent) {
        super.propertyChange(anEvent);
        if (anEvent.getSource() == buildMode && FieldEditor.VALUE.equals(anEvent.getProperty())) {
            updateReuseBuildTerminal((String)anEvent.getNewValue());
        }
    }

    /**
     * Enables the option to reuse the build terminals, if builds run in the
     * terminal.
     */
    private void updateReuseBuildTerminal(String aBuildMode) {
        reuseBuildTerminal.setEnabled(BuildMode.fromValue(aBuildMode) == BuildMode.TERMINAL, getFieldEditorParent());
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
