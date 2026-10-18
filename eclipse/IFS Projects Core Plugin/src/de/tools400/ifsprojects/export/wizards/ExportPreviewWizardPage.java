/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.export.wizards;

import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.export.ExportPlan;
import de.tools400.ifsprojects.export.IncludeConverter;

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
    private static final int SUMMARY_WIDTH = 600;

    private ExportPlan plan;

    private Label summaryLabel;
    private CheckboxTableViewer viewer;
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

        summaryLabel = new Label(main, SWT.WRAP);
        GridData summaryData = new GridData(SWT.FILL, SWT.TOP, true, false);
        summaryData.widthHint = SUMMARY_WIDTH;
        summaryLabel.setLayoutData(summaryData);

        final Table table = new Table(main, SWT.CHECK | SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        GridData tableData = new GridData(SWT.FILL, SWT.FILL, true, true);
        tableData.widthHint = TABLE_WIDTH;
        tableData.heightHint = TABLE_HEIGHT;
        table.setLayoutData(tableData);

        String[] titles = { Messages.Label_Column_File, Messages.Label_Column_Member, Messages.Label_Column_Type, Messages.Label_Column_Status,
            Messages.Label_Column_Problem };
        for (int i = 0; i < titles.length; i++) {
            TableColumn column = new TableColumn(table, SWT.LEFT);
            column.setText(titles[i]);
            column.setWidth(COLUMN_WIDTHS[i]);
        }

        viewer = new CheckboxTableViewer(table);
        viewer.setContentProvider(ArrayContentProvider.getInstance());
        viewer.setLabelProvider(new ItemLabelProvider());

        viewer.addCheckStateListener(new ICheckStateListener() {
            public void checkStateChanged(CheckStateChangedEvent anEvent) {
                plan.setSelected((ExportPlan.Item)anEvent.getElement(), anEvent.getChecked());
                refresh();
            }
        });

        // The tool tip shows the full texts of the problems, that the column
        // shows only partly.
        table.addListener(SWT.MouseMove, new Listener() {
            public void handleEvent(Event anEvent) {
                TableItem tableItem = table.getItem(new Point(anEvent.x, anEvent.y));
                String toolTip = tableItem == null ? null : getProblemsText((ExportPlan.Item)tableItem.getData());
                if (toolTip == null ? table.getToolTipText() != null : !toolTip.equals(table.getToolTipText())) {
                    table.setToolTipText(toolTip);
                }
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
            viewer.setInput(plan.getItems());
            refresh();
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
            viewer.setInput(aPlan.getItems());
            refresh();
            showDetails(null);
        }
    }

    /**
     * Returns the plan of the export.
     */
    public ExportPlan getPlan() {
        return plan;
    }

    private void refresh() {

        viewer.refresh();
        for (ExportPlan.Item item : plan.getItems()) {
            viewer.setChecked(item, item.isSelected());
            viewer.setGrayed(item, item.hasErrors());
        }

        summaryLabel.setText(getSummary());
        summaryLabel.getParent().layout();

        boolean isEmpty = plan.getExportedItems().isEmpty();
        setErrorMessage(isEmpty ? Messages.Error_No_file_exported : null);
        setPageComplete(!isEmpty);

        IStructuredSelection selection = (IStructuredSelection)viewer.getSelection();
        showDetails((ExportPlan.Item)selection.getFirstElement());
    }

    private String getSummary() {

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

        StringBuilder summary = new StringBuilder();
        summary.append(Messages.bind(Messages.Label_A_of_B_files_exported_C_replaced, new Object[] { Integer.toString(exportedItems.size()),
            Integer.toString(plan.getItems().size()), Integer.toString(replaced) }));
        summary.append(' ').append(Messages.bind(Messages.Label_A_includes_converted, Integer.toString(includes)));

        if (plan.isCreateLibrary()) {
            summary.append(LINE_SEPARATOR).append(Messages.bind(Messages.Label_Library_A_is_created, plan.getSettings().getLibrary()));
        }

        Map<String, Integer> newSourceFiles = plan.getNewSourceFiles();
        if (!newSourceFiles.isEmpty()) {
            StringBuilder files = new StringBuilder();
            for (Map.Entry<String, Integer> entry : newSourceFiles.entrySet()) {
                if (files.length() > 0) {
                    files.append(LIST_SEPARATOR);
                }
                files.append(Messages.bind(Messages.Label_Source_file_A_record_length_B, entry.getKey(), entry.getValue().toString()));
            }
            summary.append(LINE_SEPARATOR).append(Messages.bind(Messages.Label_New_source_files_A_B,
                Integer.toString(plan.getSettings().getCcsid()), files.toString()));
        }

        if (plan.getOtherFileCount() > 0) {
            summary.append(LINE_SEPARATOR).append(Messages.bind(Messages.Label_A_other_files_not_exported, Integer.toString(plan.getOtherFileCount())));
        }

        return summary.toString();
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
                        include.getName(), include.getMember().getTarget() })).append(LINE_SEPARATOR);
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
