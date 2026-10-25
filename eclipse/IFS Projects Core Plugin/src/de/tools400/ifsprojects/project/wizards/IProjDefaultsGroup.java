/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.project.wizards;

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
import de.tools400.ifsprojects.preferences.Preferences;
import de.tools400.ifsprojects.project.BuildTool;
import de.tools400.ifsprojects.project.IProjConfig;
import de.tools400.ifsprojects.project.propertypages.IProjToolTips;
import de.tools400.ifsprojects.swt.ToolTips;
import de.tools400.ifsprojects.utils.StringUtils;

/**
 * Group of the wizards of new projects, that defines the attributes of a new
 * <code>iproj.json</code> file: description, build and compile command and
 * repository. An existing <code>iproj.json</code> file is used as it is; its
 * attributes are shown read-only.
 * <p>
 * The build tool buttons set the build and compile command. The button of the
 * build tool, whose commands are entered, is selected. For an existing file,
 * the button of the build tool, that its commands use, is selected, see
 * {@link BuildTool#findExisting(String, String)}.
 */
public class IProjDefaultsGroup {

    private final Button createIProjButton;
    private final Text descriptionText;
    private final Button[] buildToolButtons;
    private final Text buildCommandText;
    private final Text compileCommandText;
    private final Text repositoryText;

    private boolean hasExistingIProj;

    /**
     * Creates the group.
     *
     * @param aParent - parent with a grid layout of two columns
     */
    public IProjDefaultsGroup(Composite aParent) {

        Group group = new Group(aParent, SWT.NONE);
        group.setText(Messages.Label_Group_iproj_json);
        group.setLayout(new GridLayout(2, false));
        GridData groupData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        groupData.horizontalSpan = 2;
        group.setLayoutData(groupData);

        createIProjButton = new Button(group, SWT.CHECK);
        createIProjButton.setText(Messages.Label_Create_iproj_json_if_it_does_not_exist);
        ToolTips.set(Messages.Tooltip_Create_iproj_json, createIProjButton);
        createIProjButton.setSelection(true);
        GridData buttonData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        buttonData.horizontalSpan = 2;
        createIProjButton.setLayoutData(buttonData);

        descriptionText = createText(group, Messages.Label_Description_colon, IProjToolTips.get(IProjConfig.DESCRIPTION));
        buildToolButtons = createBuildToolButtons(group);
        buildCommandText = createText(group, Messages.Label_Build_command_colon, IProjToolTips.get(IProjConfig.BUILD_COMMAND));
        compileCommandText = createText(group, Messages.Label_Compile_command_colon, IProjToolTips.get(IProjConfig.COMPILE_COMMAND));
        repositoryText = createText(group, Messages.Label_Repository_colon, IProjToolTips.get(IProjConfig.REPOSITORY));

        buildCommandText.setText(Preferences.getInstance().getDefaultBuildCommand());
        compileCommandText.setText(Preferences.getInstance().getDefaultCompileCommand());
        updateBuildToolButtons();

        ModifyListener commandListener = new ModifyListener() {
            public void modifyText(ModifyEvent anEvent) {
                updateBuildToolButtons();
            }
        };
        buildCommandText.addModifyListener(commandListener);
        compileCommandText.addModifyListener(commandListener);

        createIProjButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent anEvent) {
                updateFields();
            }
        });

        updateFields();
    }

    /**
     * Returns <code>true</code>, if a new <code>iproj.json</code> file shall be
     * created. An existing file is never replaced.
     */
    public boolean isCreateIProj() {
        return !hasExistingIProj && createIProjButton.getSelection();
    }

    public String getDescription() {
        return descriptionText.getText();
    }

    public String getBuildCommand() {
        return buildCommandText.getText();
    }

    public String getCompileCommand() {
        return compileCommandText.getText();
    }

    public String getRepository() {
        return repositoryText.getText();
    }

    /**
     * Returns the content of a new <code>iproj.json</code> file.
     *
     * @return configuration with the entered attributes
     */
    public IProjConfig createConfig() {

        IProjConfig config = new IProjConfig();
        config.setString(IProjConfig.DESCRIPTION, getDescription());
        config.setString(IProjConfig.REPOSITORY, getRepository());
        config.setString(IProjConfig.BUILD_COMMAND, getBuildCommand());
        config.setString(IProjConfig.COMPILE_COMMAND, getCompileCommand());

        return config;
    }

    /**
     * Shows the attributes of an existing <code>iproj.json</code> file
     * read-only.
     *
     * @param aConfig - content of the file or <code>null</code>, if the file is
     *        invalid
     */
    public void showExisting(IProjConfig aConfig) {

        hasExistingIProj = true;

        createIProjButton.setSelection(false);
        createIProjButton.setEnabled(false);

        setText(descriptionText, aConfig == null ? null : aConfig.getString(IProjConfig.DESCRIPTION));
        setText(buildCommandText, aConfig == null ? null : aConfig.getString(IProjConfig.BUILD_COMMAND));
        setText(compileCommandText, aConfig == null ? null : aConfig.getString(IProjConfig.COMPILE_COMMAND));
        setText(repositoryText, aConfig == null ? null : aConfig.getString(IProjConfig.REPOSITORY));

        updateBuildToolButtons();
        updateFields();
    }

    /**
     * Shows the attributes of a new <code>iproj.json</code> file. After an
     * existing file has been shown, the defaults are restored.
     */
    public void showNew() {

        if (hasExistingIProj) {
            hasExistingIProj = false;
            createIProjButton.setSelection(true);
            setText(descriptionText, null);
            setText(buildCommandText, Preferences.getInstance().getDefaultBuildCommand());
            setText(compileCommandText, Preferences.getInstance().getDefaultCompileCommand());
            setText(repositoryText, null);
        }

        createIProjButton.setEnabled(true);
        updateBuildToolButtons();
        updateFields();
    }

    private void setText(Text aText, String aValue) {
        aText.setText(StringUtils.nullToEmpty(aValue));
    }

    private void updateFields() {

        boolean isEditable = !hasExistingIProj && createIProjButton.getSelection();
        boolean isEnabled = hasExistingIProj || createIProjButton.getSelection();

        for (Text text : new Text[] { descriptionText, buildCommandText, compileCommandText, repositoryText }) {
            text.setEnabled(isEnabled);
            text.setEditable(isEditable);
        }

        for (Button button : buildToolButtons) {
            button.setEnabled(isEditable);
        }
    }

    /**
     * Selects the button of the build tool, whose build and compile command are
     * entered. No button is selected for other commands.
     */
    private void updateBuildToolButtons() {

        String buildCommand = buildCommandText.getText();
        String compileCommand = compileCommandText.getText();
        BuildTool buildTool = hasExistingIProj ? BuildTool.findExisting(buildCommand, compileCommand) : BuildTool.find(buildCommand, compileCommand);
        for (Button button : buildToolButtons) {
            button.setSelection(button.getData() == buildTool);
        }
    }

    private Button[] createBuildToolButtons(Composite aParent) {

        Label label = new Label(aParent, SWT.NONE);
        label.setText(Messages.Label_Build_tool_colon);
        ToolTips.set(Messages.Tooltip_Build_tool, label);

        BuildTool[] buildTools = BuildTool.values();

        Composite composite = new Composite(aParent, SWT.NONE);
        GridLayout layout = new GridLayout(buildTools.length, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        composite.setLayout(layout);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        Button[] buttons = new Button[buildTools.length];
        for (int i = 0; i < buildTools.length; i++) {
            final BuildTool buildTool = buildTools[i];
            Button button = new Button(composite, SWT.RADIO);
            button.setText(buildTool.getLabel());
            button.setToolTipText(Messages.bind(Messages.Tooltip_Preset_A_B, buildTool.getBuildCommand(), buildTool.getCompileCommand()));
            button.setData(buildTool);
            button.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent anEvent) {
                    if (((Button)anEvent.widget).getSelection()) {
                        buildCommandText.setText(buildTool.getBuildCommand());
                        compileCommandText.setText(buildTool.getCompileCommand());
                    }
                }
            });
            buttons[i] = button;
        }

        return buttons;
    }

    private Text createText(Composite aParent, String aLabel, String aToolTip) {

        Label label = new Label(aParent, SWT.NONE);
        label.setText(aLabel);

        Text text = new Text(aParent, SWT.BORDER);
        text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        ToolTips.set(aToolTip, label, text);

        return text;
    }
}
