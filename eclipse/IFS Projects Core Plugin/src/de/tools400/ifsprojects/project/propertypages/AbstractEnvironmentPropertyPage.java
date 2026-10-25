/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.project.propertypages;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;

import de.tools400.ifsprojects.IfsProjectsPlugin;
import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.preferences.Preferences;
import de.tools400.ifsprojects.project.EnvironmentVariables;
import de.tools400.ifsprojects.project.IProjConfig;
import de.tools400.ifsprojects.project.IProjFileException;
import de.tools400.ifsprojects.project.IProjParseException;
import de.tools400.ifsprojects.swt.ToolTips;
import de.tools400.ifsprojects.utils.ExceptionUtils;

/**
 * Base class of the property pages of the environment variables of IFS projects
 * and local IFS projects. The variables are exported before the build and
 * compile commands are executed.
 * <p>
 * Variables, that are used in <code>iproj.json</code>, but are not defined, are
 * added with an empty value, because TOBi stops, if a variable is not defined.
 * Variables with a default value in the preferences are not added, because the
 * variables of the project override the defaults.
 */
public abstract class AbstractEnvironmentPropertyPage extends PropertyPage {

    private static final int TEXT_LINES = 8;
    private static final int WRAP_WIDTH_CHARS = 60;
    private static final String LINE_SEPARATOR = "\n"; //$NON-NLS-1$
    private static final String ASSIGNMENT = "="; //$NON-NLS-1$
    private static final String NAME_SEPARATOR = ", "; //$NON-NLS-1$
    private static final String MESSAGE_SEPARATOR = ". "; //$NON-NLS-1$
    private static final String MNEMONIC = "&"; //$NON-NLS-1$
    private static final String ESCAPED_MNEMONIC = "&&"; //$NON-NLS-1$

    private final String toolTip;

    private boolean isInitialized;
    private Text variablesText;
    private Label statusLabel;
    private Label defaultsLabel;
    private Map<String, String> defaults;
    private boolean isModified;

    /* Message about the variables of iproj.json, see addVariablesOfIProj(). */
    private String message;
    private int messageType;

    /**
     * Creates the page.
     *
     * @param aDescription - description of the page
     * @param aToolTip - tool tip of the variables
     */
    protected AbstractEnvironmentPropertyPage(String aDescription, String aToolTip) {
        this.toolTip = aToolTip;

        noDefaultAndApplyButton();
        setDescription(aDescription.replace(MNEMONIC, ESCAPED_MNEMONIC));
    }

    /**
     * Resolves the project of the page.
     *
     * @return <code>true</code>, if the element of the page is a project, whose
     *         variables can be edited
     */
    protected abstract boolean initialize();

    /**
     * Returns the name of the project, e.g. for log messages.
     */
    protected abstract String getProjectName();

    /**
     * Returns the stored variables of the project.
     */
    protected abstract Map<String, String> loadVariables();

    /**
     * Stores the variables of the project.
     *
     * @param aVariables - variables, an empty map removes all variables
     * @throws Exception if the variables could not be stored
     */
    protected abstract void saveVariables(Map<String, String> aVariables) throws Exception;

    /**
     * Reads the <code>iproj.json</code> file of the project. Must be called on
     * the UI thread.
     *
     * @return configuration, empty, if the file does not exist
     * @throws IProjParseException if the file is invalid
     * @throws Exception if the file could not be read; the message is shown to
     *         the user
     */
    protected abstract IProjConfig readConfig() throws Exception;

    /**
     * Creates controls above the variables, e.g. information about the project.
     * The parent has one column.
     *
     * @param aParent - parent
     */
    protected void createInfo(Composite aParent) {
        // No information by default
    }

    @Override
    protected Control createContents(Composite aParent) {

        initializeDialogUnits(aParent);

        Composite main = new Composite(aParent, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        main.setLayout(layout);

        isInitialized = initialize();
        if (!isInitialized) {
            new Label(main, SWT.NONE).setText(Messages.Error_Not_an_IFS_project);
            return main;
        }

        createInfo(main);

        Label variablesLabel = new Label(main, SWT.NONE);
        variablesLabel.setText(Messages.Label_Environment_variables_colon);

        variablesText = new Text(main, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
        ToolTips.set(toolTip, variablesLabel, variablesText);
        GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        gridData.heightHint = variablesText.getLineHeight() * TEXT_LINES;
        variablesText.setLayoutData(gridData);
        variablesText.setText(EnvironmentVariables.format(loadVariables()));

        statusLabel = new Label(main, SWT.WRAP);
        statusLabel.setLayoutData(createWrapGridData());

        defaults = Preferences.getInstance().getDefaultEnvironmentVariables();
        if (!defaults.isEmpty()) {
            defaultsLabel = new Label(main, SWT.WRAP);
            defaultsLabel.setLayoutData(createWrapGridData());
            defaultsLabel.setText(Messages.bind(Messages.Label_Default_environment_variables_of_preferences_A, EnvironmentVariables.format(defaults))
                .replace(MNEMONIC, ESCAPED_MNEMONIC));
        }

        addVariablesOfIProj();

        variablesText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent anEvent) {
                isModified = true;
                validate();
            }
        });

        validate();

        return main;
    }

    @Override
    public boolean performOk() {

        if (!isModified || !isInitialized) {
            return true;
        }

        if (EnvironmentVariables.findInvalidLine(variablesText.getText()) > 0) {
            return false;
        }

        try {
            saveVariables(EnvironmentVariables.parse(variablesText.getText()));
        } catch (Exception e) {
            IfsProjectsPlugin.logError("Could not save environment variables of project: " + getProjectName(), e); //$NON-NLS-1$
            MessageDialog.openError(getShell(), Messages.Title_IFS_Projects,
                Messages.bind(Messages.Error_Could_not_save_environment_variables_A, ExceptionUtils.getLocalizedMessage(e)));
            return false;
        }

        isModified = false;

        return true;
    }

    /**
     * Returns the layout data of a wrapping label. The width hint keeps a long
     * text from widening the preferences dialog, which remembers its size.
     */
    protected GridData createWrapGridData() {

        GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gridData.widthHint = convertWidthInCharsToPixels(WRAP_WIDTH_CHARS);

        return gridData;
    }

    /**
     * Reads <code>iproj.json</code>, shows the variables used there and adds
     * the variables, that are not defined yet.
     */
    private void addVariablesOfIProj() {

        IProjConfig config;
        try {
            config = readConfig();
        } catch (IProjParseException e) {
            statusLabel.setText(Messages.bind(Messages.Error_Invalid_iproj_json_A, ExceptionUtils.getLocalizedMessage(e)));
            return;
        } catch (IProjFileException e) {
            statusLabel.setText(ExceptionUtils.getLocalizedMessage(e));
            return;
        } catch (Exception e) {
            IfsProjectsPlugin.logError("Could not read iproj.json of project: " + getProjectName(), e); //$NON-NLS-1$
            statusLabel.setText(Messages.bind(Messages.Error_Unexpected_error_A, ExceptionUtils.getLocalizedMessage(e)));
            return;
        }

        Set<String> references = EnvironmentVariables.findReferences(config);
        if (references.isEmpty()) {
            statusLabel.setText(Messages.Label_No_variables_used_in_iproj_json);
            return;
        }

        statusLabel.setText(Messages.bind(Messages.Label_Variables_used_in_iproj_json_A, join(references)));

        if (EnvironmentVariables.findInvalidLine(variablesText.getText()) > 0) {
            return;
        }

        Map<String, String> variables = EnvironmentVariables.parse(variablesText.getText());
        List<String> added = new ArrayList<>();
        List<String> unsupported = new ArrayList<>();
        StringBuilder text = new StringBuilder(variablesText.getText());
        for (String name : references) {
            if (!EnvironmentVariables.isValidName(name)) {
                unsupported.add(name);
            } else if (!variables.containsKey(name) && !defaults.containsKey(name)) {
                if (text.length() > 0) {
                    text.append(LINE_SEPARATOR);
                }
                text.append(name).append(ASSIGNMENT);
                added.add(name);
            }
        }

        if (!added.isEmpty()) {
            variablesText.setText(text.toString());
            isModified = true;
            message = Messages.bind(Messages.Info_Variables_of_iproj_json_added_A, join(added));
            messageType = IMessageProvider.INFORMATION;
        }

        if (!unsupported.isEmpty()) {
            String warning = Messages.bind(Messages.Warning_Variables_of_iproj_json_not_supported_A, join(unsupported));
            message = message == null ? warning : message + MESSAGE_SEPARATOR + warning;
            messageType = IMessageProvider.WARNING;
        }
    }

    private void validate() {

        int invalidLine = EnvironmentVariables.findInvalidLine(variablesText.getText());
        if (invalidLine > 0) {
            setErrorMessage(Messages.bind(Messages.Error_Invalid_environment_variable_in_line_A, Integer.toString(invalidLine)));
            setValid(false);
            return;
        }

        setErrorMessage(null);
        setValid(true);

        if (message != null) {
            setMessage(message, messageType);
        }
    }

    private String join(Iterable<String> aNames) {

        StringBuilder buffer = new StringBuilder();
        for (String name : aNames) {
            if (buffer.length() > 0) {
                buffer.append(NAME_SEPARATOR);
            }
            buffer.append(name);
        }

        return buffer.toString();
    }
}
