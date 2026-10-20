/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.export.wizards;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;

import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.export.ExportPlan;
import de.tools400.ifsprojects.sources.IncludeConverter;
import de.tools400.ifsprojects.swt.CheckboxTableGroup;
import de.tools400.ifsprojects.swt.SummaryGroup;
import de.tools400.ifsprojects.swt.TableToolTips;

/**
 * Second page of the export: the files of the project, the members, into
 * which they are exported, and the problems, that have been found. Files with
 * errors are not exported. The user can deselect files; then the include
 * directives, that refer to them, are not converted.
 */
public class ExportPreviewWizardPage extends WizardPage {

    private static final String PAGE_NAME = "ExportPreviewWizardPage"; //$NON-NLS-1$
    private static final String LINE_SEPARATOR = "\n"; //$NON-NLS-1$
    private static final String LIST_SEPARATOR = ", "; //$NON-NLS-1$
    private static final String EMPTY = ""; //$NON-NLS-1$

    private static final int COLUMN_FILE = 0;
    private static final int COLUMN_MEMBER = 1;
    private static final int COLUMN_TYPE = 2;
    private static final int COLUMN_STATUS = 3;
    private static final int COLUMN_PROBLEM = 4;
    private static final int[] COLUMN_WIDTHS = { 220, 170, 80, 80, 360 };
    private static final int TABLE_WIDTH = 700;
    private static final int TABLE_HEIGHT = 250;
    private static final int DETAILS_HEIGHT = 60;

    private ExportPlan plan;

    private SummaryGroup summaryGroup;
    private CheckboxTableGroup<ExportPlan.Item> filesGroup;
    private CheckboxTableViewer viewer;
    /** True, while the check states are set from the plan. */
    private boolean isRefreshing;
    private Text detailsText;

    public ExportPreviewWizardPage() {
        super(PAGE_NAME);
        setTitle(Messages.Title_Export_to_library);
        setDescription(Messages.Description_Export_preview_page);
        setPageComplete(false);
    }

    public void createControl(Composite aParent) {

        Composite main = new Composite(aParent, SWT.NONE);
        main.setLayout(new GridLayout(1, false));

        summaryGroup = new SummaryGroup(main, Messages.Label_Group_Summary);

        String[] titles = { Messages.Label_Column_File, Messages.Label_Column_Member, Messages.Label_Column_Type, Messages.Label_Column_Status,
            Messages.Label_Column_Problem };
        filesGroup = new CheckboxTableGroup<ExportPlan.Item>(main, titles, COLUMN_WIDTHS, new ItemLabelProvider());
        GridData tableData = (GridData)filesGroup.getTable().getLayoutData();
        tableData.widthHint = TABLE_WIDTH;
        tableData.heightHint = TABLE_HEIGHT;
        Table table = filesGroup.getTable();
        viewer = filesGroup.getViewer();

        filesGroup.addCheckListener(new Runnable() {
            public void run() {
                if (!isRefreshing) {
                    plan.setSelectedItems(filesGroup.getCheckedElements());
                    refresh();
                }
            }
        });
        // The tool tip shows the full texts of the problems, that the column
        // shows only partly.
        TableToolTips.install(table, new TableToolTips.Provider() {
            public String getToolTip(Object anElement) {
                return getProblemsText((ExportPlan.Item)anElement);
            }
        });

        viewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent anEvent) {
                showDetails((ExportPlan.Item)((IStructuredSelection)anEvent.getSelection()).getFirstElement());
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
     * Shows the plan of the export.
     *
     * @param aPlan - plan
     */
    public void setPlan(ExportPlan aPlan) {

        this.plan = aPlan;

        if (viewer != null) {
            showPlan();
            showDetails(null);
        }
    }

    /**
     * Returns the plan of the export.
     */
    public ExportPlan getPlan() {
        return plan;
    }

    private void showPlan() {
        isRefreshing = true;
        try {
            filesGroup.setInput(plan.getItems());
        } finally {
            isRefreshing = false;
        }
        refresh();
    }

    private void refresh() {

        viewer.refresh();
        for (ExportPlan.Item item : plan.getItems()) {
            viewer.setChecked(item, item.isSelected());
            viewer.setGrayed(item, item.hasErrors());
        }

        showSummary();

        boolean isEmpty = plan.getExportedItems().isEmpty();
        setErrorMessage(isEmpty ? Messages.Error_No_file_exported : null);
        setPageComplete(!isEmpty);

        IStructuredSelection selection = (IStructuredSelection)viewer.getSelection();
        showDetails((ExportPlan.Item)selection.getFirstElement());
    }

    /**
     * Shows the summary: files, include directives and, if any, the library
     * and the source files, that are created, and the files, that have no
     * known source type.
     */
    private void showSummary() {

        List<ExportPlan.Item> exportedItems = plan.getExportedItems();
        int replaced = 0;
        int includes = 0;
        for (ExportPlan.Item item : exportedItems) {
            if (item.isExistingMember()) {
                replaced++;
            }
            if (item.getIncludes() != null) {
                for (IncludeConverter.Include include : item.getIncludes().getIncludes()) {
                    if (include.getProblem() == null) {
                        includes++;
                    }
                }
            }
        }

        summaryGroup.clear();
        summaryGroup.addRow(Messages.Label_Summary_Files, Messages.bind(Messages.Label_Summary_A_of_B_exported_C_replace, new Object[] { Integer
            .toString(exportedItems.size()), Integer.toString(plan.getItems().size()), Integer.toString(replaced) }));
        if (plan.getOtherFileCount() > 0) {
            summaryGroup.addRow(Messages.Label_Summary_Not_exported, Messages.bind(Messages.Label_Summary_A_without_source_type, Integer
                .toString(plan.getOtherFileCount())));
        }
        summaryGroup.addRow(Messages.Label_Summary_Includes, Messages.bind(Messages.Label_Summary_A_converted, Integer.toString(includes)));
        if (plan.isCreateLibrary()) {
            summaryGroup.addRow(Messages.Label_Summary_Library, Messages.bind(Messages.Label_Summary_A_is_created, plan.getSettings().getLibrary()));
        }
        if (!plan.getNewSourceFiles().isEmpty()) {
            summaryGroup.addRow(Messages.Label_Summary_Source_files, getNewSourceFilesText(plan.getNewSourceFiles()));
        }
        summaryGroup.update();
    }

    /**
     * Returns the text of the new source files, e.g.
     * <code>QRPGLESRC, INC are created (CCSID 1141, record length 112)</code>.
     * If the record lengths differ, each source file shows its own.
     */
    private String getNewSourceFilesText(Map<String, Integer> aNewSourceFiles) {

        Set<Integer> recordLengths = new HashSet<Integer>(aNewSourceFiles.values());
        String ccsid = Integer.toString(plan.getSettings().getCcsid());

        StringBuilder files = new StringBuilder();
        for (Map.Entry<String, Integer> entry : aNewSourceFiles.entrySet()) {
            if (files.length() > 0) {
                files.append(LIST_SEPARATOR);
            }
            if (recordLengths.size() == 1) {
                files.append(entry.getKey());
            } else {
                files.append(Messages.bind(Messages.Label_Source_file_A_record_length_B, entry.getKey(), entry.getValue().toString()));
            }
        }

        if (recordLengths.size() == 1) {
            return Messages.bind(Messages.Label_Summary_A_created_ccsid_B_record_length_C, new Object[] { files.toString(), ccsid, recordLengths
                .iterator().next().toString() });
        }

        return Messages.bind(Messages.Label_Summary_A_created_ccsid_B, files.toString(), ccsid);
    }
    /**
     * Shows the problems and the converted include directives of a file.
     */
    private void showDetails(ExportPlan.Item anItem) {

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
                if (include.getProblem() == null) {
                    details.append(Messages.bind(Messages.Label_Line_A_B_converted_to_C, new Object[] { Integer.toString(include.getLineNumber()),
                        include.getName(), include.getTarget() })).append(LINE_SEPARATOR);
                }
            }
        }

        if (anItem.getText() != null) {
            details.append(Messages.bind(Messages.Label_Member_text_A, anItem.getText())).append(LINE_SEPARATOR);
        }

        detailsText.setText(details.toString().trim());
    }

    /**
     * Returns the texts of the problems of a file, one per line.
     *
     * @return texts or <code>null</code>, if the file has no problems
     */
    private static String getProblemsText(ExportPlan.Item anItem) {

        if (anItem == null || anItem.getProblems().isEmpty()) {
            return null;
        }

        StringBuilder text = new StringBuilder();
        for (ExportPlan.Problem problem : anItem.getProblems()) {
            if (text.length() > 0) {
                text.append(LINE_SEPARATOR);
            }
            text.append(ExportProblems.getText(problem));
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

            ExportPlan.Item item = (ExportPlan.Item)anElement;
            switch (aColumnIndex) {
            case COLUMN_FILE:
                return item.getPath();

            case COLUMN_MEMBER:
                return item.getMember() == null ? EMPTY : item.getSourceFile() + '(' + item.getMember() + ')';

            case COLUMN_TYPE:
                return item.getSourceType();

            case COLUMN_STATUS:
                return getStatus(item);

            case COLUMN_PROBLEM:
                List<ExportPlan.Problem> problems = item.getProblems();
                if (problems.isEmpty()) {
                    return EMPTY;
                }
                String text = ExportProblems.getText(problems.get(0));
                if (problems.size() > 1) {
                    text = Messages.bind(Messages.Label_A_and_B_more, text, Integer.toString(problems.size() - 1));
                }
                return text;

            default:
                return EMPTY;
            }
        }

        private static String getStatus(ExportPlan.Item anItem) {

            if (anItem.hasErrors()) {
                return Messages.Label_Status_error;
            }

            if (!anItem.isSelected()) {
                return Messages.Label_Status_not_selected;
            }

            if (anItem.isExistingMember()) {
                return Messages.Label_Status_replace;
            }

            return Messages.Label_Status_new;
        }
    }
}
