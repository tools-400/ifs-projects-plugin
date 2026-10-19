/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.project.propertypages;

import org.eclipse.core.resources.IProject;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.ibm.etools.iseries.rse.ui.widgets.IBMiConnectionCombo;

import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.local.LocalProject;
import de.tools400.ifsprojects.rse.RseConnections;
import de.tools400.ifsprojects.swt.ToolTips;
import de.tools400.ifsprojects.utils.StringUtils;

/**
 * Controls of the settings of a local IFS project, see {@link LocalProject}:
 * whether the project is built on an IBM i, the RSE connection and the deploy
 * directory in the IFS. Used by the property page of local IFS projects and by
 * the wizard of new local IFS projects.
 * <p>
 * The connection is selected with the connection combo of RDi, which can also
 * create a new connection.
 */
public class LocalProjectSettingsGroup {

    /**
     * Notified, when the user changes the settings.
     */
    public interface IChangeListener {
        void settingsChanged();
    }

    private static final String HOME_DIRECTORY_LABEL = "<home>"; //$NON-NLS-1$
    private static final String EMPTY = ""; //$NON-NLS-1$

    private final IProject project;
    private final String missingConnectionName;

    private final Button buildButton;
    private final IBMiConnectionCombo connectionCombo;
    private final Text directoryText;
    private final Label defaultLabel;

    /**
     * Creates the controls.
     *
     * @param aParent - parent with a grid layout of two columns
     * @param aProject - project, whose settings are shown, or
     *        <code>null</code> for a new project, which is built on an IBM i
     * @param aListener - notified, when the user changes the settings
     */
    public LocalProjectSettingsGroup(Composite aParent, IProject aProject, final IChangeListener aListener) {

        this.project = aProject;

        String connectionName = aProject == null ? null : LocalProject.getConnectionName(aProject);
        IHost host = RseConnections.findIBMiHost(connectionName);
        missingConnectionName = connectionName != null && host == null ? connectionName : null;

        buildButton = new Button(aParent, SWT.CHECK);
        buildButton.setText(Messages.Label_Build_on_IBM_i);
        GridData buttonData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        buttonData.horizontalSpan = 2;
        buildButton.setLayoutData(buttonData);
        buildButton.setSelection(aProject == null || connectionName != null);
        ToolTips.set(Messages.Tooltip_Build_on_IBM_i, buildButton);

        Label connectionLabel = new Label(aParent, SWT.NONE);
        connectionLabel.setText(Messages.Label_Connection_colon);

        // The combo shows the IBM i connections and a 'New...' button. The
        // constructor without label cannot be used: it filters the connections
        // by subsystem category 'com.ibm.etools.iseries.subsystems.qsys.objects',
        // but the category of the QSYS subsystem is 'nativefiles' (RDi 9.8), so
        // that the combo is empty.
        connectionCombo = new IBMiConnectionCombo(aParent, SWT.NONE, host, true);
        connectionCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        hidePromptLabel(connectionCombo);
        ToolTips.set(Messages.Tooltip_Local_project_connection, connectionLabel, connectionCombo);

        Label directoryLabel = new Label(aParent, SWT.NONE);
        directoryLabel.setText(Messages.Label_Deploy_directory_colon);

        directoryText = new Text(aParent, SWT.BORDER | SWT.SINGLE);
        directoryText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        ToolTips.set(Messages.Tooltip_Deploy_directory, directoryLabel, directoryText);

        new Label(aParent, SWT.NONE);
        defaultLabel = new Label(aParent, SWT.WRAP);
        defaultLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        setProjectName(aProject == null ? EMPTY : aProject.getName());

        loadDirectory();

        buildButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent anEvent) {
                loadDirectory();
                aListener.settingsChanged();
            }
        });

        // Also called, when a new connection has been created and selected.
        connectionCombo.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent anEvent) {
                loadDirectory();
                aListener.settingsChanged();
            }
        });

        directoryText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent anEvent) {
                aListener.settingsChanged();
            }
        });
    }

    /**
     * Shows the default deploy directory of a project name, e.g. while the
     * name of a new project is entered.
     *
     * @param aProjectName - name of the project
     */
    public void setProjectName(String aProjectName) {
        defaultLabel.setText(Messages.bind(Messages.Label_Empty_A, LocalProject.getDefaultDeployDirectory(HOME_DIRECTORY_LABEL, aProjectName)));
        defaultLabel.getParent().layout(true);
    }

    /**
     * Returns the name of the configured connection, that does not exist.
     *
     * @return name or <code>null</code>, if the connection exists or the
     *         project is not configured
     */
    public String getMissingConnectionName() {
        return missingConnectionName;
    }

    /**
     * Validates the settings.
     *
     * @return error message or <code>null</code>, if the settings are valid
     */
    public String validate() {

        if (!buildButton.getSelection()) {
            return null;
        }

        if (connectionCombo.getHost() == null) {
            return Messages.Error_Select_IBM_i_connection;
        }

        if (LocalProject.normalizeDeployDirectory(directoryText.getText()) == null) {
            return Messages.Error_Deploy_directory_invalid;
        }

        return null;
    }

    /**
     * Returns the name of the selected connection.
     *
     * @return name or <code>null</code>, if the project is not built on an
     *         IBM i or no connection is selected
     */
    public String getConnectionName() {

        IHost host = connectionCombo.getHost();
        if (!buildButton.getSelection() || host == null) {
            return null;
        }

        return host.getAliasName();
    }

    /**
     * Returns the entered deploy directory, see
     * {@link LocalProject#normalizeDeployDirectory(String)}.
     *
     * @return directory, <code>""</code> for the default directory or
     *         <code>null</code>, if the directory is invalid
     */
    public String getDeployDirectory() {
        return LocalProject.normalizeDeployDirectory(directoryText.getText());
    }

    /**
     * Enables the fields, if the project is built on an IBM i, and shows the
     * deploy directory, that has been saved for the selected connection of an
     * existing project.
     */
    private void loadDirectory() {

        boolean isBuild = buildButton.getSelection();
        connectionCombo.setEnabled(isBuild);
        directoryText.setEnabled(isBuild);

        if (project == null) {
            return;
        }

        String connectionName = getConnectionName();
        String directory = connectionName == null ? null : LocalProject.getDeployDirectory(project, connectionName);

        directoryText.setText(StringUtils.nullToEmpty(directory));
    }

    /**
     * Hides the label of the connection combo, so that the combo is aligned
     * with the other fields, and lets the combo span two columns, like the
     * combo of a connection combo without label.
     */
    private void hidePromptLabel(IBMiConnectionCombo aCombo) {

        Label label = aCombo.getPromptLabel();
        if (label == null) {
            return;
        }

        GridData labelData = new GridData();
        labelData.exclude = true;
        label.setLayoutData(labelData);
        label.setVisible(false);

        Object comboData = aCombo.getCombo().getLayoutData();
        if (comboData instanceof GridData) {
            ((GridData)comboData).horizontalSpan = 2;
        }
    }
}
