/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.iagentbridge.ui;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.dialogs.PropertyPage;

import de.tools400.ifsprojects.iagentbridge.AgentSettings;
import de.tools400.ifsprojects.iagentbridge.Messages;

/**
 * Property page 'IFS Project &gt; iAgentBridge' of a local IFS project: the
 * approval of the project for AI agents, see {@link AgentSettings}. A link
 * opens the preference page with the switch of the workspace.
 */
public class AgentPropertyPage extends PropertyPage {

    private static final int WIDTH_IN_CHARACTERS = 80;

    private IProject project;
    private Button approveButton;

    /**
     * Creates the page.
     */
    public AgentPropertyPage() {
        setDescription(Messages.Description_property_page);
    }

    @Override
    protected Control createContents(Composite aParent) {

        IAdaptable element = getElement();
        Object resource = element == null ? null : element.getAdapter(IResource.class);
        project = resource instanceof IProject ? (IProject)resource : null;

        Composite composite = new Composite(aParent, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        composite.setLayout(layout);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        approveButton = new Button(composite, SWT.CHECK);
        approveButton.setText(Messages.Label_Approve_project);
        approveButton.setSelection(project != null && AgentSettings.isApproved(project));
        approveButton.setEnabled(project != null);

        Label warning = new Label(composite, SWT.WRAP);
        warning.setText(Messages.Label_Warning_commands);
        warning.setLayoutData(createWrappingData());

        Link link = new Link(composite, SWT.WRAP);
        link.setText(Messages.Link_Preference_page);
        link.setLayoutData(createWrappingData());
        link.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent anEvent) {
                PreferencesUtil.createPreferenceDialogOn(getShell(), AgentPreferencePage.ID, new String[] { AgentPreferencePage.ID }, null).open();
            }
        });

        return composite;
    }

    private GridData createWrappingData() {

        GridData data = new GridData(SWT.FILL, SWT.TOP, true, false);
        data.widthHint = convertWidthInCharsToPixels(WIDTH_IN_CHARACTERS);

        return data;
    }

    @Override
    protected void performDefaults() {
        approveButton.setSelection(false);
        super.performDefaults();
    }

    @Override
    public boolean performOk() {

        if (project == null) {
            return true;
        }

        try {
            AgentSettings.setApproved(project, approveButton.getSelection());
        } catch (CoreException e) {
            setErrorMessage(Messages.bind(Messages.Error_Could_not_save_A, e.getLocalizedMessage()));
            return false;
        }

        return true;
    }
}
