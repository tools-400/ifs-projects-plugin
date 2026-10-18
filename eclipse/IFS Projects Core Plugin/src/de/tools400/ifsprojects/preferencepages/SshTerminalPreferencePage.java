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
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.preferences.Preferences;
import de.tools400.ifsprojects.swt.ToolTips;

/**
 * Preference page of the SSH terminals: connection, log-on and the restore of
 * the terminals after a restart.
 */
public class SshTerminalPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    private static final int MAX_PORT = 65535;
    private static final int MAX_SECONDS = 3600;
    private static final int NUM_COLUMNS = 2;

    public SshTerminalPreferencePage() {
        super(GRID);
        setPreferenceStore(Preferences.getInstance().getPreferenceStore());
        setDescription(Messages.Description_SSH_terminal_preference_page);
    }

    public void init(IWorkbench aWorkbench) {
    }

    @Override
    protected void createFieldEditors() {

        Group connection = createGroup(getFieldEditorParent(), Messages.Label_Group_Connection);

        IntegerFieldEditor port = new IntegerFieldEditor(Preferences.PORT, Messages.Label_SSH_port_colon, connection);
        port.setValidRange(1, MAX_PORT);
        ToolTips.set(port, connection, Messages.Tooltip_SSH_port);
        addField(port);

        IntegerFieldEditor timeout = new IntegerFieldEditor(Preferences.TIMEOUT, Messages.Label_Connection_timeout_colon, connection);
        timeout.setValidRange(1, MAX_SECONDS);
        ToolTips.set(timeout, connection, Messages.Tooltip_Connection_timeout);
        addField(timeout);

        IntegerFieldEditor keepAlive = new IntegerFieldEditor(Preferences.KEEP_ALIVE, Messages.Label_Keep_alive_interval_colon, connection);
        keepAlive.setValidRange(1, MAX_SECONDS);
        ToolTips.set(keepAlive, connection, Messages.Tooltip_Keep_alive_interval);
        addField(keepAlive);

        StringFieldEditor encoding = new StringFieldEditor(Preferences.ENCODING, Messages.Label_Encoding_colon, connection);
        encoding.setEmptyStringAllowed(false);
        ToolTips.set(encoding, connection, Messages.Tooltip_Encoding);
        addField(encoding);

        // FieldEditor.createControl() replaces the layout of its parent.
        connection.setLayout(new GridLayout(NUM_COLUMNS, false));

        Group logOn = createGroup(getFieldEditorParent(), Messages.Label_Group_Log_on);

        addBooleanField(logOn, Preferences.LOWERCASE_USER_ID, Messages.Label_Convert_user_ID_to_lowercase,
            Messages.Tooltip_Convert_user_ID_to_lowercase);
        addBooleanField(logOn, Preferences.USE_SAVED_PASSWORD, Messages.Label_Use_password_saved_in_RSE, Messages.Tooltip_Use_password_saved_in_RSE);

        logOn.setLayout(new GridLayout(NUM_COLUMNS, false));

        addBooleanField(getFieldEditorParent(), Preferences.RESTORE_TERMINALS, Messages.Label_Restore_terminals_after_restart,
            Messages.Tooltip_Restore_terminals_after_restart);
    }

    private Group createGroup(Composite aParent, String aText) {

        Group group = new Group(aParent, SWT.NONE);
        group.setText(aText);
        GridData groupData = new GridData(SWT.FILL, SWT.FILL, true, false);
        groupData.horizontalSpan = NUM_COLUMNS;
        group.setLayoutData(groupData);

        return group;
    }

    private void addBooleanField(Composite aParent, String aName, String aLabel, String aToolTip) {

        BooleanFieldEditor fieldEditor = new BooleanFieldEditor(aName, aLabel, aParent);
        ToolTips.set(fieldEditor, aParent, aToolTip);
        addField(fieldEditor);
    }
}
