/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.iagentbridge.ui;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.osgi.service.prefs.BackingStoreException;

import de.tools400.ifsprojects.iagentbridge.AgentSettings;
import de.tools400.ifsprojects.iagentbridge.Messages;

/**
 * Preference page 'iAgentBridge &gt; IFS Projects': the switch, that allows AI
 * agents to compile on the IBM i, see {@link AgentSettings}.
 */
public class AgentPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

    /** ID of the page in <code>plugin.xml</code>. */
    public static final String ID = "de.tools400.ifsprojects.iagentbridge.ui.AgentPreferencePage"; //$NON-NLS-1$

    private static final int WIDTH_IN_CHARACTERS = 80;

    private Button allowButton;

    /**
     * Creates the page.
     */
    public AgentPreferencePage() {
        setDescription(Messages.Description_preference_page);
    }

    public void init(IWorkbench aWorkbench) {
        // Nothing to initialize
    }

    @Override
    protected Control createContents(Composite aParent) {

        Composite composite = new Composite(aParent, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        composite.setLayout(layout);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        allowButton = new Button(composite, SWT.CHECK);
        allowButton.setText(Messages.Label_Allow_compile);
        allowButton.setSelection(AgentSettings.isCompileAllowed());

        createWrappingLabel(composite, Messages.Label_Warning_commands);
        createWrappingLabel(composite, Messages.Label_Approve_projects_in_properties);

        return composite;
    }

    private void createWrappingLabel(Composite aParent, String aText) {

        Label label = new Label(aParent, SWT.WRAP);
        label.setText(aText);
        GridData data = new GridData(SWT.FILL, SWT.TOP, true, false);
        data.widthHint = convertWidthInCharsToPixels(WIDTH_IN_CHARACTERS);
        label.setLayoutData(data);
    }

    @Override
    protected void performDefaults() {
        allowButton.setSelection(false);
        super.performDefaults();
    }

    @Override
    public boolean performOk() {

        try {
            AgentSettings.setCompileAllowed(allowButton.getSelection());
        } catch (BackingStoreException e) {
            setErrorMessage(Messages.bind(Messages.Error_Could_not_save_A, e.getLocalizedMessage()));
            return false;
        }

        return true;
    }
}
