/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.migration.wizards;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;

import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.dialogs.SourceViewDialog;
import de.tools400.ifsprojects.migration.MigrationPlan;
import de.tools400.ifsprojects.sources.IncludeConverter;
import de.tools400.ifsprojects.swt.TableToolTips;
import de.tools400.ifsprojects.utils.StringUtils;

/**
 * Preview of a migration: the stream file of each member and the problems.
 */
public class MigrationPreviewWizardPage extends WizardPage {

    private static final String PAGE_NAME = "MigrationPreviewWizardPage"; //$NON-NLS-1$
    private static final String LINE_SEPARATOR = "\n"; //$NON-NLS-1$
    private static final String EMPTY = ""; //$NON-NLS-1$

    private static final int COLUMN_MEMBER = 0;
    private static final int COLUMN_TYPE = 1;
    private static final int COLUMN_FILE = 2;
    private static final int COLUMN_STATUS = 3;
    private static final int COLUMN_PROBLEM = 4;
    private static final int[] COLUMN_WIDTHS = { 170, 80, 220, 70, 360 };
    private static final int TABLE_WIDTH = 700;
    private static final int TABLE_HEIGHT = 250;
    private static final int DETAILS_HEIGHT = 60;
    private static final int SUMMARY_WIDTH = 600;

    private MigrationPlan plan;

    private Label summaryLabel;
    private TableViewer viewer;
    private Text detailsText;

    public MigrationPreviewWizardPage() {
        super(PAGE_NAME);
        setTitle(Messages.Title_Migrate_to_IFS_project);
        setDescription(Messages.Description_Migration_preview_page);
        setPageComplete(false);
    }

    public void createControl(Composite aParent) {

        Composite main = new Composite(aParent, SWT.NONE);
        main.setLayout(new GridLayout(1, false));

        summaryLabel = new Label(main, SWT.WRAP);
        GridData summaryData = new GridData(SWT.FILL, SWT.TOP, true, false);
        summaryData.widthHint = SUMMARY_WIDTH;
        summaryLabel.setLayoutData(summaryData);

        Table table = new Table(main, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        GridData tableData = new GridData(SWT.FILL, SWT.FILL, true, true);
        tableData.widthHint = TABLE_WIDTH;
        tableData.heightHint = TABLE_HEIGHT;
        table.setLayoutData(tableData);

        String[] titles = { Messages.Label_Column_Member, Messages.Label_Column_Type, Messages.Label_Column_File, Messages.Label_Column_Status,
            Messages.Label_Column_Problem };
        for (int i = 0; i < titles.length; i++) {
            TableColumn column = new TableColumn(table, SWT.LEFT);
            column.setText(titles[i]);
            column.setWidth(COLUMN_WIDTHS[i]);
        }

        viewer = new TableViewer(table);
        viewer.setContentProvider(ArrayContentProvider.getInstance());
        viewer.setLabelProvider(new ItemLabelProvider());
        viewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent anEvent) {
                showDetails((MigrationPlan.Item)((IStructuredSelection)anEvent.getSelection()).getFirstElement());
            }
        });

        viewer.addDoubleClickListener(new IDoubleClickListener() {
            public void doubleClick(DoubleClickEvent anEvent) {
                showSource((MigrationPlan.Item)((IStructuredSelection)anEvent.getSelection()).getFirstElement());
            }
        });

        TableToolTips.install(table, new TableToolTips.Provider() {
            public String getToolTip(Object anElement) {
                return getProblemsText((MigrationPlan.Item)anElement);
            }
        });

        detailsText = new Text(main, SWT.BORDER | SWT.MULTI | SWT.READ_ONLY | SWT.WRAP | SWT.V_SCROLL);
        GridData detailsData = new GridData(SWT.FILL, SWT.FILL, true, false);
        detailsData.heightHint = DETAILS_HEIGHT;
        detailsText.setLayoutData(detailsData);

        setControl(main);

        if (plan != null) {
            showPlan();
        }
    }

    /**
     * Shows the plan of the migration.
     *
     * @param aPlan - plan
     */
    public void setPlan(MigrationPlan aPlan) {

        this.plan = aPlan;

        if (viewer != null) {
            showPlan();
        }
    }

    /**
     * Returns the plan of the migration.
     */
    public MigrationPlan getPlan() {
        return plan;
    }

    private void showPlan() {

        viewer.setInput(plan.getItems());
        showDetails(null);

        int migrated = plan.getMigratedItems().size();
        int includes = 0;
        for (MigrationPlan.Item item : plan.getMigratedItems()) {
            if (item.getIncludes() != null) {
                for (IncludeConverter.Include include : item.getIncludes().getIncludes()) {
                    if (include.getTarget() != null) {
                        includes++;
                    }
                }
            }
        }

        summaryLabel.setText(Messages.bind(Messages.Label_A_of_B_members_migrated_C_includes, new Object[] { Integer.toString(migrated), Integer
            .toString(plan.getItems().size()), Integer.toString(includes) }));
        summaryLabel.getParent().layout();

        setErrorMessage(migrated == 0 ? Messages.Error_No_member_migrated : null);
        setPageComplete(migrated > 0);
    }

    /**
     * Shows the problems and the converted include directives of a member.
     */
    private void showDetails(MigrationPlan.Item anItem) {

        if (anItem == null) {
            detailsText.setText(EMPTY);
            return;
        }

        StringBuilder details = new StringBuilder();
        String problems = getProblemsText(anItem);
        if (problems != null) {
            details.append(problems).append(LINE_SEPARATOR);
        }

        if (anItem.getIncludes() != null) {
            for (IncludeConverter.Include include : anItem.getIncludes().getIncludes()) {
                if (include.getTarget() != null) {
                    details.append(Messages.bind(Messages.Label_Line_A_B_converted_to_C, new Object[] { Integer.toString(include.getLineNumber()),
                        include.getName(), include.getTarget() })).append(LINE_SEPARATOR);
                }
            }
        }

        if (anItem.getMember().getText() != null && plan.getSettings().isMemberText()) {
            details.append(Messages.bind(Messages.Label_Member_text_A, anItem.getMember().getText())).append(LINE_SEPARATOR);
        }

        detailsText.setText(details.toString().trim());
    }

    /**
     * Shows the content of the stream file of a member, which does not exist
     * yet, and the member, from which it is created.
     */
    private void showSource(MigrationPlan.Item anItem) {

        if (anItem == null) {
            return;
        }

        String member = anItem.getMember().getKey();
        String path = anItem.getPath() == null ? member : anItem.getPath();

        SourceViewDialog.open(getShell(), path, member, anItem.getOriginalContent(), path, anItem.getContent());
    }

    /**
     * Returns the texts of the problems of a member, one per line.
     *
     * @return texts or <code>null</code>, if the member has no problems
     */
    private static String getProblemsText(MigrationPlan.Item anItem) {

        if (anItem == null || anItem.getProblems().isEmpty()) {
            return null;
        }

        StringBuilder text = new StringBuilder();
        for (MigrationPlan.Problem problem : anItem.getProblems()) {
            if (text.length() > 0) {
                text.append(LINE_SEPARATOR);
            }
            text.append(MigrationProblems.getText(problem));
        }

        return text.toString();
    }

    /**
     * Labels of the table.
     */
    private static class ItemLabelProvider extends LabelProvider implements ITableLabelProvider {

        public Image getColumnImage(Object anElement, int aColumnIndex) {
            return null;
        }

        public String getColumnText(Object anElement, int aColumnIndex) {

            MigrationPlan.Item item = (MigrationPlan.Item)anElement;
            switch (aColumnIndex) {
            case COLUMN_MEMBER:
                return item.getMember().getKey();
            case COLUMN_TYPE:
                return StringUtils.nullToEmpty(item.getMember().getSourceType());
            case COLUMN_FILE:
                return StringUtils.nullToEmpty(item.getPath());
            case COLUMN_STATUS:
                return item.isMigrated() ? Messages.Label_Status_new : Messages.Label_Status_error;
            case COLUMN_PROBLEM:
                return item.getProblems().isEmpty() ? EMPTY : MigrationProblems.getText(item.getProblems().get(0));
            default:
                return EMPTY;
            }
        }
    }
}
