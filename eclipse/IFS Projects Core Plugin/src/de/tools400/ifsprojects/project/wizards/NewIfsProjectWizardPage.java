/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.project.wizards;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.ibm.as400.access.AS400;

import de.tools400.ifsprojects.IfsProjectsPlugin;
import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.dialogs.SelectIfsDirectoryDialog;
import de.tools400.ifsprojects.ibmi.IfsPaths;
import de.tools400.ifsprojects.project.BuildTool;
import de.tools400.ifsprojects.project.IProjConfig;
import de.tools400.ifsprojects.project.IProjFileContent;
import de.tools400.ifsprojects.project.IProjFileException;
import de.tools400.ifsprojects.project.IProjFileStore;
import de.tools400.ifsprojects.project.IProjParseException;
import de.tools400.ifsprojects.project.IfsProjectFilters;
import de.tools400.ifsprojects.swt.ToolTips;
import de.tools400.ifsprojects.utils.StringUtils;

/**
 * Page of the 'New IFS Project' wizard.
 * <p>
 * If the project directory already contains an <code>iproj.json</code> file,
 * the file is used as it is. Its attributes are shown read-only, see
 * {@link IProjDefaultsGroup}.
 */
public class NewIfsProjectWizardPage extends WizardPage {

    private static final String PAGE_NAME = "NewIfsProjectWizardPage"; //$NON-NLS-1$

    private final IHost host;
    private final String initialDirectory;

    private Text nameText;
    private Text directoryText;
    private Button createDirectoryButton;
    private IProjDefaultsGroup iprojGroup;

    private boolean isNameModifiedByUser;
    private boolean isUpdatingName;

    /**
     * Directory, whose iproj.json is checked or has been checked, or
     * <code>null</code>, if the directory has been changed since.
     */
    private String checkedDirectory;

    public NewIfsProjectWizardPage(IHost aHost, String anInitialDirectory) {
        super(PAGE_NAME);
        this.host = aHost;
        this.initialDirectory = anInitialDirectory;
        setTitle(Messages.Title_New_IFS_project);
        setDescription(Messages.bind(Messages.Description_New_IFS_project_page, aHost.getAliasName()));
    }

    public void createControl(Composite aParent) {

        Composite main = new Composite(aParent, SWT.NONE);
        main.setLayout(new GridLayout(2, false));

        directoryText = createDirectoryField(main);
        nameText = createText(main, Messages.Label_Project_name_colon, Messages.Tooltip_Project_name);

        createDirectoryButton = createCheckbox(main, Messages.Label_Create_directory_if_it_does_not_exist, Messages.Tooltip_Create_directory);

        iprojGroup = new IProjDefaultsGroup(main);

        if (initialDirectory != null) {
            directoryText.setText(initialDirectory);
            updateDefaultName();
        }

        directoryText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent anEvent) {
                updateDefaultName();
                forgetIProjCheck();
                validate();
            }
        });

        directoryText.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent anEvent) {
                checkIProjFile();
            }
        });

        nameText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent anEvent) {
                if (!isUpdatingName) {
                    isNameModifiedByUser = true;
                }
                validate();
            }
        });

        setControl(main);

        // An empty page shows no error yet, but the error of a proposed
        // directory, e.g. a project name, that exists already.
        validate();

        if (initialDirectory == null) {
            setErrorMessage(null);
            directoryText.setFocus();
        } else {
            checkIProjFile();
            nameText.setFocus();
        }
    }

    public String getProjectName() {
        return nameText.getText().trim();
    }

    public String getDirectory() {
        return IfsPaths.normalizeDirectory(directoryText.getText());
    }

    public boolean isCreateDirectory() {
        return createDirectoryButton.getSelection();
    }

    /**
     * Returns <code>true</code>, if a new iproj.json file shall be created. An
     * existing file is never replaced.
     */
    public boolean isCreateIProj() {
        return iprojGroup.isCreateIProj();
    }

    /**
     * Returns the content of a new iproj.json file.
     *
     * @return configuration with the entered attributes
     */
    public IProjConfig createConfig() {
        return iprojGroup.createConfig();
    }

    /**
     * Returns the build tool of the project, that the build command of the new
     * or of the existing iproj.json file uses.
     *
     * @return build tool or <code>null</code>, if the commands use none or
     *         several build tools
     */
    public BuildTool getBuildTool() {
        return BuildTool.findExisting(iprojGroup.getBuildCommand(), iprojGroup.getCompileCommand());
    }

    /**
     * Checks, whether the project directory contains an iproj.json file. An
     * existing file is shown read-only and is used as it is.
     * <p>
     * The file is read in a job, so that the wizard stays responsive, if the
     * connection hangs, and a click, that has moved the focus, is not lost.
     * The connection is established before on the UI thread, because RSE may
     * prompt for the password.
     */
    private void checkIProjFile() {

        final String directory = getDirectory();
        if (directory == null || directory.equals(checkedDirectory)) {
            return;
        }

        try {
            IProjFileStore.connect(host);
        } catch (IProjFileException e) {
            IfsProjectsPlugin.logError("Could not check iproj.json of directory: " + directory, e); //$NON-NLS-1$
            showNewIProj(e.getLocalizedMessage());
            return;
        }

        checkedDirectory = directory;
        setMessage(Messages.bind(Messages.Task_Reading_A, directory), IMessageProvider.INFORMATION);

        final Display display = getShell().getDisplay();
        Job job = new Job(Messages.bind(Messages.Task_Reading_A, directory)) {
            @Override
            protected IStatus run(IProgressMonitor aMonitor) {

                IProjFileContent fileContent = null;
                IProjFileException error = null;
                try {
                    fileContent = IProjFileStore.read(host, directory);
                } catch (IProjFileException e) {
                    error = e;
                }

                final IProjFileContent result = fileContent;
                final IProjFileException resultError = error;
                display.asyncExec(new Runnable() {
                    public void run() {
                        showCheckResult(directory, result, resultError);
                    }
                });

                return Status.OK_STATUS;
            }
        };
        job.setSystem(true);
        job.schedule();
    }

    /**
     * Forgets the check of the previous directory, when the directory is
     * changed: the attributes of a new iproj.json file are shown again, and
     * the result of a check, that is still running, is ignored. Otherwise
     * Finish, e.g. with the Enter key or before the result of the new
     * directory is shown, would use the existing iproj.json file of the
     * previous directory and create the project without iproj.json. Finish
     * creates a new file only, if the directory does not contain one.
     */
    private void forgetIProjCheck() {

        if (checkedDirectory == null) {
            return;
        }

        checkedDirectory = null;
        showNewIProj(null);
    }

    /**
     * Shows the result of {@link #checkIProjFile()}, unless the page has been
     * closed or the directory has been changed meanwhile.
     */
    private void showCheckResult(String aDirectory, IProjFileContent aFileContent, IProjFileException anError) {

        Control control = getControl();
        if (control == null || control.isDisposed() || !aDirectory.equals(checkedDirectory)) {
            return;
        }

        if (anError != null) {
            IfsProjectsPlugin.logError("Could not check iproj.json of directory: " + aDirectory, anError); //$NON-NLS-1$
            // Checks the directory again, when the focus leaves the field.
            checkedDirectory = null;
            showNewIProj(anError.getLocalizedMessage());
            return;
        }

        if (!aFileContent.exists()) {
            showNewIProj(null);
            return;
        }

        try {
            showExistingIProj(IProjConfig.parse(aFileContent.getContent()), null);
        } catch (IProjParseException e) {
            showExistingIProj(null, Messages.bind(Messages.Warning_Existing_iproj_json_is_invalid_A, e.getLocalizedMessage()));
        }
    }

    private void showExistingIProj(IProjConfig aConfig, String aWarning) {

        iprojGroup.showExisting(aConfig);

        if (aWarning != null) {
            setMessage(aWarning, IMessageProvider.WARNING);
        } else {
            setMessage(Messages.Info_Existing_iproj_json_is_used, IMessageProvider.INFORMATION);
        }
    }

    private void showNewIProj(String aWarning) {

        iprojGroup.showNew();

        if (aWarning != null) {
            setMessage(aWarning, IMessageProvider.WARNING);
        } else {
            setMessage(null);
        }
    }

    private void updateDefaultName() {

        if (isNameModifiedByUser || nameText == null) {
            return;
        }

        String lastSegment = IfsPaths.getLastSegment(directoryText.getText());

        isUpdatingName = true;
        try {
            nameText.setText(StringUtils.nullToEmpty(lastSegment));
        } finally {
            isUpdatingName = false;
        }
    }

    private void validate() {

        String errorMessage = null;

        if (getDirectory() == null) {
            errorMessage = Messages.Error_Directory_must_be_absolute;
        } else if (BuildTool.hasMakeCharacters(getDirectory())) {
            errorMessage = Messages.Error_Directory_contains_make_characters;
        } else if (getProjectName().length() == 0) {
            errorMessage = Messages.Error_Project_name_must_not_be_empty;
        } else if (IfsProjectFilters.exists(host, getProjectName())) {
            errorMessage = Messages.bind(Messages.Error_A_project_named_A_already_exists, getProjectName());
        }

        setErrorMessage(errorMessage);
        setPageComplete(errorMessage == null);
    }

    private Text createDirectoryField(Composite aParent) {

        Label label = new Label(aParent, SWT.NONE);
        label.setText(Messages.Label_Directory_colon);

        Composite composite = new Composite(aParent, SWT.NONE);
        GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        composite.setLayout(layout);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        Text text = new Text(composite, SWT.BORDER);
        text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        ToolTips.set(Messages.Tooltip_Project_directory, label, text);

        Button browseButton = new Button(composite, SWT.PUSH);
        browseButton.setText(Messages.Label_Browse);
        ToolTips.set(Messages.Tooltip_Browse_project_directory, browseButton);
        browseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent anEvent) {
                browseDirectory();
            }
        });

        return text;
    }

    /**
     * Lets the user select the project directory from the IFS of the
     * connection.
     */
    private void browseDirectory() {

        AS400 system;
        try {
            system = IProjFileStore.getConnectedSystem(host);
        } catch (IProjFileException e) {
            setErrorMessage(e.getLocalizedMessage());
            return;
        }

        // Pre-selects the entered directory or its deepest existing parent.
        SelectIfsDirectoryDialog dialog = new SelectIfsDirectoryDialog(getShell(), system);
        dialog.setDirectory(directoryText.getText());

        if (dialog.open() != Window.OK) {
            return;
        }

        String selectedDirectory = dialog.getSelectedDirectory();
        if (selectedDirectory != null) {
            directoryText.setText(selectedDirectory);
            checkIProjFile();
            nameText.setFocus();
        }
    }

    private Text createText(Composite aParent, String aLabel, String aToolTip) {

        Label label = new Label(aParent, SWT.NONE);
        label.setText(aLabel);

        Text text = new Text(aParent, SWT.BORDER);
        text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        ToolTips.set(aToolTip, label, text);

        return text;
    }

    private Button createCheckbox(Composite aParent, String aLabel, String aToolTip) {

        Button button = new Button(aParent, SWT.CHECK);
        button.setText(aLabel);
        ToolTips.set(aToolTip, button);
        button.setSelection(true);
        button.setLayoutData(createSpanningGridData());

        return button;
    }

    private GridData createSpanningGridData() {
        GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gridData.horizontalSpan = 2;
        return gridData;
    }
}
