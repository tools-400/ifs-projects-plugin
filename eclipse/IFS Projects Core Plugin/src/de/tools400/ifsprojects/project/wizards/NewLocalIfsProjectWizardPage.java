/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.project.wizards;

import java.io.File;
import java.nio.file.Files;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import de.tools400.ifsprojects.IfsProjectsPlugin;
import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.project.IProjConfig;
import de.tools400.ifsprojects.project.IProjParseException;
import de.tools400.ifsprojects.project.propertypages.LocalProjectSettingsGroup;
import de.tools400.ifsprojects.swt.ToolTips;

/**
 * Last page of the wizards 'New Local IFS Project' and 'Import Local IFS
 * Project': the settings of the local IFS project (see
 * {@link LocalProjectSettingsGroup}), the encoding of the project and the
 * attributes of a new <code>iproj.json</code> file (see
 * {@link IProjDefaultsGroup}).
 * <p>
 * If the location of the project already contains an <code>iproj.json</code>
 * file, e.g. a git working tree, the file is used as it is.
 */
public class NewLocalIfsProjectWizardPage extends WizardPage {

    private static final String PAGE_NAME = "NewLocalIfsProjectWizardPage"; //$NON-NLS-1$
    private static final String UTF_8 = "UTF-8"; //$NON-NLS-1$

    /**
     * Name and directory of the project, entered on the first page of the
     * wizard.
     */
    public interface IProjectLocation {

        String getProjectName();

        /**
         * Returns the directory of the project or <code>null</code>, if it is
         * not known yet.
         */
        File getProjectDirectory();
    }

    private final IProjectLocation projectLocation;

    private LocalProjectSettingsGroup settingsGroup;
    private Button utf8Button;
    private IProjDefaultsGroup iprojGroup;

    /** Location of the project, whose iproj.json has been checked. */
    private File checkedDirectory;

    /**
     * Creates the page.
     *
     * @param aProjectLocation - name and directory of the project
     * @param aTitle - title of the wizard
     */
    public NewLocalIfsProjectWizardPage(IProjectLocation aProjectLocation, String aTitle) {
        super(PAGE_NAME);
        this.projectLocation = aProjectLocation;
        setTitle(aTitle);
        setDescription(Messages.Description_New_local_IFS_project_settings_page);
    }

    public void createControl(Composite aParent) {

        Composite main = new Composite(aParent, SWT.NONE);
        main.setLayout(new GridLayout(2, false));

        settingsGroup = new LocalProjectSettingsGroup(main, null, new LocalProjectSettingsGroup.IChangeListener() {
            public void settingsChanged() {
                validate();
            }
        });

        utf8Button = new Button(main, SWT.CHECK);
        utf8Button.setText(Messages.Label_Text_files_in_UTF8);
        utf8Button.setSelection(true);
        GridData utf8Data = new GridData(SWT.FILL, SWT.CENTER, true, false);
        utf8Data.horizontalSpan = 2;
        utf8Button.setLayoutData(utf8Data);
        ToolTips.set(Messages.Tooltip_Text_files_in_UTF8, utf8Button);

        iprojGroup = new IProjDefaultsGroup(main);

        setControl(main);

        validate();
    }

    @Override
    public void setVisible(boolean anIsVisible) {
        super.setVisible(anIsVisible);

        if (anIsVisible) {
            settingsGroup.setProjectName(projectLocation.getProjectName());
            checkIProjFile();
        }
    }

    /**
     * Returns the name of the selected connection.
     *
     * @return name or <code>null</code>, if the project is not built on an
     *         IBM i
     */
    public String getConnectionName() {
        return settingsGroup.getConnectionName();
    }

    /**
     * Returns the entered deploy directory.
     *
     * @return directory or <code>""</code> for the default directory
     */
    public String getDeployDirectory() {
        return settingsGroup.getDeployDirectory();
    }

    /**
     * Returns <code>true</code>, if the text file encoding of the project
     * shall be set to UTF-8.
     */
    public boolean isUtf8() {
        return utf8Button.getSelection();
    }

    /**
     * Returns <code>true</code>, if a new <code>iproj.json</code> file shall
     * be created. An existing file is never replaced.
     */
    public boolean isCreateIProj() {
        return iprojGroup.isCreateIProj();
    }

    /**
     * Returns the content of a new <code>iproj.json</code> file.
     */
    public IProjConfig createConfig() {
        return iprojGroup.createConfig();
    }

    /**
     * Checks, whether the location of the project contains an
     * <code>iproj.json</code> file. An existing file is shown read-only and is
     * used as it is.
     */
    private void checkIProjFile() {

        File directory = projectLocation.getProjectDirectory();
        if (directory == null || directory.equals(checkedDirectory)) {
            return;
        }

        checkedDirectory = directory;

        File file = new File(directory, IProjConfig.FILE_NAME);
        if (!file.isFile()) {
            iprojGroup.showNew();
            setMessage(null);
            return;
        }

        try {
            iprojGroup.showExisting(IProjConfig.parse(new String(Files.readAllBytes(file.toPath()), UTF_8)));
            setMessage(Messages.Info_Existing_iproj_json_is_used, IMessageProvider.INFORMATION);
        } catch (IProjParseException e) {
            iprojGroup.showExisting(null);
            setMessage(Messages.bind(Messages.Warning_Existing_iproj_json_is_invalid_A, e.getLocalizedMessage()), IMessageProvider.WARNING);
        } catch (Exception e) {
            IfsProjectsPlugin.logError("Could not check iproj.json of directory: " + directory, e); //$NON-NLS-1$
            iprojGroup.showExisting(null);
            setMessage(Messages.bind(Messages.Warning_Existing_iproj_json_is_invalid_A, e.getLocalizedMessage()), IMessageProvider.WARNING);
        }
    }

    private void validate() {

        String errorMessage = settingsGroup.validate();

        setErrorMessage(errorMessage);
        setPageComplete(errorMessage == null);
    }
}
