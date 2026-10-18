/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.preferencepages;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.export.ExportSettings;
import de.tools400.ifsprojects.preferences.Preferences;
import de.tools400.ifsprojects.swt.ToolTips;

/**
 * Preference page of the export to a library: CCSID and record length of new
 * source files, which the export dialog shows for the first export of a
 * project.
 */
public class ExportPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public ExportPreferencePage() {
        super(GRID);
        setPreferenceStore(Preferences.getInstance().getPreferenceStore());
        setDescription(Messages.Description_Export_preference_page);
    }

    public void init(IWorkbench aWorkbench) {
    }

    @Override
    protected void createFieldEditors() {

        CcsidFieldEditor ccsid = new CcsidFieldEditor(Preferences.EXPORT_CCSID, Messages.Label_CCSID_of_new_source_files_colon,
            getFieldEditorParent());
        ccsid.setToolTipText(Messages.Tooltip_Default_export_CCSID);
        addField(ccsid);

        IntegerFieldEditor recordLength = new IntegerFieldEditor(Preferences.EXPORT_RECORD_LENGTH,
            Messages.Label_Record_length_of_new_source_files_colon, getFieldEditorParent());
        recordLength.setValidRange(ExportSettings.MIN_RECORD_LENGTH, ExportSettings.MAX_RECORD_LENGTH);
        ToolTips.set(recordLength, getFieldEditorParent(), Messages.Tooltip_Default_export_record_length);
        addField(recordLength);
    }
}
