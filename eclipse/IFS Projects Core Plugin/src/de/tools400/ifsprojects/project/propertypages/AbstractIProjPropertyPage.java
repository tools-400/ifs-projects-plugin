/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.project.propertypages;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.project.IProjConfig;
import de.tools400.ifsprojects.project.IProjFileContent;
import de.tools400.ifsprojects.project.IProjFileException;
import de.tools400.ifsprojects.project.IProjParseException;
import de.tools400.ifsprojects.swt.ToolTips;
import de.tools400.ifsprojects.utils.StringUtils;

/**
 * Base class of the property pages of IFS projects and local IFS projects.
 * Edits the <code>iproj.json</code> file of the project. Unknown attributes of
 * the file are preserved. The subclasses add information about the project
 * above the attributes and read and write the file.
 */
public abstract class AbstractIProjPropertyPage extends PropertyPage {

    private static final String[] TEXT_KEYS = { IProjConfig.DESCRIPTION, IProjConfig.VERSION, IProjConfig.REPOSITORY, IProjConfig.OBJLIB,
        IProjConfig.CURLIB, IProjConfig.BUILD_COMMAND, IProjConfig.COMPILE_COMMAND };

    private static final String[] LIST_KEYS = { IProjConfig.INCLUDE_PATH, IProjConfig.PRE_USRLIBL, IProjConfig.POST_USRLIBL,
        IProjConfig.SET_IBMI_ENV_CMD };

    private static final String LINE_SEPARATOR_PATTERN = "\\r?\\n"; //$NON-NLS-1$
    private static final String LINE_SEPARATOR = "\n"; //$NON-NLS-1$
    private static final String COLON = ":"; //$NON-NLS-1$
    private static final int LIST_FIELD_LINES = 3;
    private static final int WRAP_WIDTH_CHARS = 60;

    private final Map<String, Text> textFields = new LinkedHashMap<String, Text>();
    private final Map<String, Text> listFields = new LinkedHashMap<String, Text>();

    private boolean isInitialized;
    private IProjFileContent fileContent;
    private IProjConfig config;
    private boolean isReadOnly;
    private String readOnlyMessage;
    private boolean isModified;

    private Label statusLabel;

    protected AbstractIProjPropertyPage() {
        noDefaultAndApplyButton();
    }

    /**
     * Resolves the project of the page.
     *
     * @return <code>true</code>, if the element of the page is a project
     */
    protected abstract boolean initialize();

    /**
     * Creates the information about the project above the attributes of
     * <code>iproj.json</code>.
     *
     * @param aParent - parent with two columns
     */
    protected abstract void createHeader(Composite aParent);

    /**
     * Reads the <code>iproj.json</code> file. Must be called on the UI thread.
     *
     * @return content of the file
     * @throws IProjFileException if the file could not be read
     */
    protected abstract IProjFileContent readFile() throws IProjFileException;

    /**
     * Writes the <code>iproj.json</code> file. Must be called on the UI
     * thread.
     *
     * @param aContent - text of the file
     * @param aPrevious - content read before or <code>null</code> to
     *        overwrite the file without checking for changes
     * @throws IProjFileException if the file could not be written or has
     *         been changed since it was read
     */
    protected abstract void writeFile(String aContent, IProjFileContent aPrevious) throws IProjFileException;

    /**
     * Saves the settings of the header, before <code>iproj.json</code> is
     * written.
     *
     * @return <code>false</code>, if the settings could not be saved
     */
    protected boolean performOkHeader() {
        return true;
    }

    @Override
    protected Control createContents(Composite aParent) {

        initializeDialogUnits(aParent);

        Composite main = new Composite(aParent, SWT.NONE);
        GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        main.setLayout(layout);

        isInitialized = initialize();
        if (!isInitialized) {
            new Label(main, SWT.NONE).setText(Messages.Error_Not_an_IFS_project);
            return main;
        }

        createHeader(main);

        statusLabel = new Label(main, SWT.WRAP);
        statusLabel.setLayoutData(createWrapGridData());

        Label separator = new Label(main, SWT.SEPARATOR | SWT.HORIZONTAL);
        separator.setLayoutData(createSpanningGridData());

        ModifyListener modifyListener = new ModifyListener() {
            public void modifyText(ModifyEvent anEvent) {
                isModified = true;
            }
        };

        for (String key : TEXT_KEYS) {
            Text text = createText(main, key, SWT.SINGLE);
            text.addModifyListener(modifyListener);
            textFields.put(key, text);
        }

        for (String key : LIST_KEYS) {
            Text text = createText(main, key, SWT.MULTI | SWT.V_SCROLL);
            text.addModifyListener(modifyListener);
            listFields.put(key, text);
        }

        loadConfig();

        return main;
    }

    @Override
    public boolean performOk() {

        if (!isInitialized) {
            return true;
        }

        if (!performOkHeader()) {
            return false;
        }

        if (isReadOnly || !isModified || config == null) {
            return true;
        }

        for (Map.Entry<String, Text> entry : textFields.entrySet()) {
            if (entry.getValue().getEditable()) {
                config.setString(entry.getKey(), entry.getValue().getText());
            }
        }

        for (Map.Entry<String, Text> entry : listFields.entrySet()) {
            if (entry.getValue().getEditable()) {
                config.setStringList(entry.getKey(), splitLines(entry.getValue().getText()));
            }
        }

        String content = config.toJson();

        try {
            writeFile(content, fileContent);
        } catch (IProjFileException e) {
            if (!e.isConflict()) {
                MessageDialog.openError(getShell(), Messages.Title_IFS_Projects, e.getLocalizedMessage());
                return false;
            }
            if (!MessageDialog.openQuestion(getShell(), Messages.Title_IFS_Projects, Messages.bind(Messages.Question_File_A_changed_overwrite,
                fileContent.getPath()))) {
                return false;
            }
            try {
                writeFile(content, null);
            } catch (IProjFileException e2) {
                MessageDialog.openError(getShell(), Messages.Title_IFS_Projects, e2.getLocalizedMessage());
                return false;
            }
        }

        isModified = false;

        return true;
    }

    /**
     * Returns the message, why <code>iproj.json</code> cannot be edited.
     *
     * @return message or <code>null</code>, if the file can be edited
     */
    protected String getReadOnlyMessage() {
        return readOnlyMessage;
    }

    /**
     * Creates a row with a label and a value.
     */
    protected void createInfo(Composite aParent, String aLabel, String aValue) {

        new Label(aParent, SWT.NONE).setText(aLabel);

        Label value = new Label(aParent, SWT.NONE);
        value.setText(StringUtils.nullToEmpty(aValue));
        value.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    }

    /**
     * Returns the layout data of a control, that spans both columns.
     */
    protected GridData createSpanningGridData() {
        GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gridData.horizontalSpan = 2;
        return gridData;
    }

    /**
     * Returns the layout data of a wrapping label, that spans both columns.
     * The width hint keeps a long text from widening the dialog.
     */
    protected GridData createWrapGridData() {
        GridData gridData = createSpanningGridData();
        gridData.widthHint = convertWidthInCharsToPixels(WRAP_WIDTH_CHARS);
        return gridData;
    }

    private void loadConfig() {

        try {
            fileContent = readFile();
        } catch (IProjFileException e) {
            setReadOnly(e.getLocalizedMessage());
            return;
        }

        try {
            config = IProjConfig.parse(fileContent.getContent());
        } catch (IProjParseException e) {
            setReadOnly(Messages.bind(Messages.Error_Invalid_iproj_json_A, e.getLocalizedMessage()));
            return;
        }

        if (fileContent.exists()) {
            statusLabel.setText(Messages.bind(Messages.Label_File_A_CCSID_B, fileContent.getPath(), Integer.toString(fileContent.getCcsid())));
        } else {
            statusLabel.setText(Messages.bind(Messages.Label_File_A_does_not_exist, fileContent.getPath()));
        }

        for (Map.Entry<String, Text> entry : textFields.entrySet()) {
            String key = entry.getKey();
            Text text = entry.getValue();
            if (config.isNotEditable(key, false)) {
                text.setText(config.getJsonText(key));
                text.setEditable(false);
            } else {
                text.setText(StringUtils.nullToEmpty(config.getString(key)));
            }
        }

        for (Map.Entry<String, Text> entry : listFields.entrySet()) {
            String key = entry.getKey();
            Text text = entry.getValue();
            List<String> values = config.getStringList(key);
            if (values == null) {
                text.setText(config.getJsonText(key));
                text.setEditable(false);
            } else {
                text.setText(joinLines(values));
            }
        }

        isModified = false;
    }

    private void setReadOnly(String aMessage) {

        isReadOnly = true;
        readOnlyMessage = aMessage;
        setErrorMessage(aMessage);
        statusLabel.setText(aMessage);

        for (Text text : textFields.values()) {
            text.setEditable(false);
        }

        for (Text text : listFields.values()) {
            text.setEditable(false);
        }
    }

    private Text createText(Composite aParent, String aKey, int aStyle) {

        Label label = new Label(aParent, SWT.NONE);
        label.setText(aKey + COLON);

        Text text = new Text(aParent, SWT.BORDER | aStyle);
        GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        if ((aStyle & SWT.MULTI) != 0) {
            label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
            gridData.heightHint = text.getLineHeight() * LIST_FIELD_LINES;
        }
        text.setLayoutData(gridData);

        ToolTips.set(IProjToolTips.get(aKey), label, text);

        return text;
    }

    private List<String> splitLines(String aText) {

        List<String> lines = new ArrayList<String>();
        for (String line : aText.split(LINE_SEPARATOR_PATTERN)) {
            String trimmedLine = StringUtils.trimToNull(line);
            if (trimmedLine != null) {
                lines.add(trimmedLine);
            }
        }

        return lines;
    }

    private String joinLines(List<String> aValues) {

        StringBuilder buffer = new StringBuilder();
        for (String value : aValues) {
            if (buffer.length() > 0) {
                buffer.append(LINE_SEPARATOR);
            }
            buffer.append(value);
        }

        return buffer.toString();
    }
}
