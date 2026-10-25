/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.preferencepages;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.preferences.Preferences;
import de.tools400.ifsprojects.sources.headers.SourceHeaderParsers;
import de.tools400.ifsprojects.sources.headers.SourceHeaderParsers.Contribution;
import de.tools400.ifsprojects.swt.CheckboxTableGroup;
import de.tools400.ifsprojects.swt.ToolTips;

/**
 * Preference page of the migration: the build tools, whose headers are read
 * when source members are migrated. The parsers are contributed by plug-ins of
 * their own, see extension point <code>sourceHeaderParsers</code>; a parser,
 * that is switched off here, is skipped, e.g. because the members still have
 * the headers of a build tool, that is no longer used.
 * <p>
 * The IDs of the switched off parsers are stored, so that a parser, that is
 * installed later, is used at once.
 */
public class MigrationPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

    private static final int COLUMN_NAME = 0;
    private static final int COLUMN_ID = 1;
    private static final int[] COLUMN_WIDTHS = { 160, 320 };
    private static final int TABLE_HEIGHT = 160;

    private List<Contribution> contributions;
    private CheckboxTableGroup<Contribution> parsersGroup;

    public MigrationPreferencePage() {
        setPreferenceStore(Preferences.getInstance().getPreferenceStore());
        setDescription(Messages.Description_Migration_preference_page);
    }

    public void init(IWorkbench aWorkbench) {
    }

    @Override
    protected Control createContents(Composite aParent) {

        Composite main = new Composite(aParent, SWT.NONE);
        main.setLayout(new GridLayout(1, false));
        main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Group group = new Group(main, SWT.NONE);
        group.setText(Messages.Label_Group_Build_tool_headers);
        group.setLayout(new GridLayout(1, false));
        group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        contributions = SourceHeaderParsers.getContributions();
        if (contributions.isEmpty()) {
            createHint(group);
        } else {
            createParsersGroup(group);
        }

        return main;
    }

    @Override
    protected void performDefaults() {

        if (parsersGroup != null) {
            parsersGroup.getViewer().setAllChecked(true);
        }

        super.performDefaults();
    }

    @Override
    public boolean performOk() {

        if (parsersGroup != null) {
            Preferences.getInstance().setDisabledSourceHeaderParsers(getDisabledIds());
        }

        return super.performOk();
    }

    private void createParsersGroup(Composite aParent) {

        String[] titles = new String[] { Messages.Label_Column_Build_tool, Messages.Label_Column_Id };

        parsersGroup = new CheckboxTableGroup<>(aParent, titles, COLUMN_WIDTHS, new ContributionLabelProvider());
        ((GridData)parsersGroup.getTable().getLayoutData()).heightHint = TABLE_HEIGHT;
        ToolTips.set(Messages.Tooltip_Build_tool_headers, parsersGroup.getTable());

        parsersGroup.setInput(contributions);

        Set<String> disabled = Preferences.getInstance().getDisabledSourceHeaderParsers();
        for (Contribution contribution : contributions) {
            if (disabled.contains(contribution.getId())) {
                parsersGroup.getViewer().setChecked(contribution, false);
            }
        }
    }

    private void createHint(Composite aParent) {

        Label hint = new Label(aParent, SWT.WRAP);
        hint.setText(Messages.Label_No_build_tool_headers_installed);
        GridData layoutData = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
        layoutData.widthHint = COLUMN_WIDTHS[COLUMN_NAME] + COLUMN_WIDTHS[COLUMN_ID];
        hint.setLayoutData(layoutData);
    }

    /**
     * Returns the IDs of the parsers, that are not checked.
     */
    private Set<String> getDisabledIds() {

        List<Contribution> checked = parsersGroup.getCheckedElements();

        Set<String> disabled = new HashSet<>();
        for (Contribution contribution : contributions) {
            if (!checked.contains(contribution)) {
                disabled.add(contribution.getId());
            }
        }

        return disabled;
    }

    private static class ContributionLabelProvider extends LabelProvider implements ITableLabelProvider {

        public Image getColumnImage(Object anElement, int aColumnIndex) {
            return null;
        }

        public String getColumnText(Object anElement, int aColumnIndex) {

            Contribution contribution = (Contribution)anElement;
            switch (aColumnIndex) {
            case COLUMN_NAME:
                return contribution.getName();
            case COLUMN_ID:
                return contribution.getId();
            default:
                return ""; //$NON-NLS-1$
            }
        }
    }
}
