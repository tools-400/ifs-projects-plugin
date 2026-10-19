/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.dialogs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.ibm.as400.access.AS400;

import de.tools400.ifsprojects.IfsProjectsPlugin;
import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.swt.ToolTips;
import de.tools400.ifsprojects.utils.StringUtils;

/**
 * Dialog for selecting a directory of the IFS. The tree is expanded to the
 * initial directory, which is selected.
 * <p>
 * Based on <code>SelectIFSDirectoryDialog</code> of iSphere.
 */
public class SelectIfsDirectoryDialog extends Dialog {

    private static final String DIALOG_SETTINGS_SECTION = "SelectIfsDirectoryDialog"; //$NON-NLS-1$
    private static final Point DEFAULT_SIZE = new Point(400, 500);
    private static final String SEPARATOR = "/"; //$NON-NLS-1$

    private final AS400 system;
    private String initialDirectory;

    private Label selectedPathLabel;
    private TreeViewer viewer;

    private IfsDirectoryItem selectedDirectory;

    /**
     * Creates the dialog.
     *
     * @param aParentShell - parent shell
     * @param aSystem - connected system
     */
    public SelectIfsDirectoryDialog(Shell aParentShell, AS400 aSystem) {
        super(aParentShell);
        this.system = aSystem;
    }

    /**
     * Sets the directory that is selected, when the dialog is opened.
     *
     * @param aDirectory - absolute IFS directory
     */
    public void setDirectory(String aDirectory) {
        this.initialDirectory = aDirectory;
    }

    /**
     * Returns the selected directory.
     *
     * @return absolute path of the selected directory or <code>null</code>
     */
    public String getSelectedDirectory() {

        if (selectedDirectory == null) {
            return null;
        }

        return selectedDirectory.getPath();
    }

    @Override
    protected void configureShell(Shell aShell) {
        super.configureShell(aShell);
        aShell.setText(Messages.Title_Select_project_directory);
    }

    @Override
    protected Control createDialogArea(Composite aParent) {

        Composite container = (Composite)super.createDialogArea(aParent);
        container.setLayout(new GridLayout());

        Label instructionLabel = new Label(container, SWT.NONE);
        instructionLabel.setText(Messages.Label_Select_a_directory_colon);

        Composite frame = new Composite(container, SWT.BORDER);
        GridLayout frameLayout = new GridLayout();
        frameLayout.marginWidth = 2;
        frameLayout.marginHeight = 2;
        frame.setLayout(frameLayout);
        frame.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        selectedPathLabel = new Label(frame, SWT.NONE);
        selectedPathLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        ToolTips.set(Messages.Tooltip_Selected_directory, selectedPathLabel);

        viewer = new TreeViewer(container, SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        viewer.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        ToolTips.set(Messages.Tooltip_Directory_tree, instructionLabel, viewer.getTree());
        viewer.setContentProvider(new TreeContentProvider());
        viewer.setLabelProvider(new TreeLabelProvider());

        viewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent anEvent) {
                showSelectedDirectory(((IStructuredSelection)anEvent.getSelection()).getFirstElement());
            }
        });

        viewer.addDoubleClickListener(new IDoubleClickListener() {
            public void doubleClick(DoubleClickEvent anEvent) {
                Object element = ((IStructuredSelection)anEvent.getSelection()).getFirstElement();
                if (element != null) {
                    viewer.setExpandedState(element, !viewer.getExpandedState(element));
                }
            }
        });

        BusyIndicator.showWhile(aParent.getDisplay(), new Runnable() {
            public void run() {
                setListItems();
            }
        });

        return container;
    }

    @Override
    protected void createButtonsForButtonBar(Composite aParent) {
        super.createButtonsForButtonBar(aParent);
        updateOkButton();
    }

    @Override
    protected void okPressed() {

        if (selectedDirectory == null) {
            return;
        }

        super.okPressed();
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    @Override
    protected Point getInitialSize() {

        IDialogSettings settings = getDialogBoundsSettings();
        if (settings != null && settings.get("DIALOG_WIDTH") == null) { //$NON-NLS-1$
            return DEFAULT_SIZE;
        }

        return super.getInitialSize();
    }

    @Override
    protected IDialogSettings getDialogBoundsSettings() {

        IDialogSettings settings = IfsProjectsPlugin.getDefault().getDialogSettings();
        IDialogSettings section = settings.getSection(DIALOG_SETTINGS_SECTION);
        if (section == null) {
            section = settings.addNewSection(DIALOG_SETTINGS_SECTION);
        }

        return section;
    }

    private void setListItems() {

        IfsDirectoryItem rootDirectory = IfsDirectoryItem.createRootDirectory(system);

        viewer.setInput(new IfsDirectoryItem[] { rootDirectory });
        viewer.setExpandedState(rootDirectory, true);

        expandAndSelectDirectory(rootDirectory, initialDirectory);
    }

    private void expandAndSelectDirectory(IfsDirectoryItem aRootDirectory, String aDirectory) {

        List<IfsDirectoryItem> directoryPath = findDirectoryPath(aRootDirectory, aDirectory);

        IfsDirectoryItem selectedItem;
        if (directoryPath == null) {
            selectedItem = aRootDirectory;
        } else {
            for (IfsDirectoryItem directoryItem : directoryPath) {
                viewer.setExpandedState(directoryItem, true);
            }
            selectedItem = directoryPath.get(directoryPath.size() - 1);
        }

        viewer.setSelection(new StructuredSelection(selectedItem), true);
        showSelectedDirectory(selectedItem);
    }

    /**
     * Returns the items from the root directory to the specified directory. If
     * the directory does not exist, the path to its deepest existing parent
     * directory is returned.
     */
    private List<IfsDirectoryItem> findDirectoryPath(IfsDirectoryItem aRootDirectory, String aDirectory) {

        if (StringUtils.isBlank(aDirectory)) {
            return null;
        }

        List<IfsDirectoryItem> directoryPath = new ArrayList<IfsDirectoryItem>();
        directoryPath.add(aRootDirectory);

        IfsDirectoryItem currentDirectory = aRootDirectory;
        for (String name : aDirectory.trim().split(SEPARATOR)) {
            if (name.length() == 0) {
                continue;
            }

            currentDirectory = findChildDirectory(currentDirectory, name);
            if (currentDirectory == null) {
                break;
            }

            directoryPath.add(currentDirectory);
        }

        return directoryPath;
    }

    private IfsDirectoryItem findChildDirectory(IfsDirectoryItem aParent, String aName) {

        IfsDirectoryItem caseInsensitiveMatch = null;

        for (IfsDirectoryItem child : aParent.getChildren()) {
            if (child.isError()) {
                continue;
            }
            if (child.getName().equals(aName)) {
                return child;
            }
            if (caseInsensitiveMatch == null && child.getName().equalsIgnoreCase(aName)) {
                caseInsensitiveMatch = child;
            }
        }

        return caseInsensitiveMatch;
    }

    private void showSelectedDirectory(Object anElement) {

        if (anElement instanceof IfsDirectoryItem && !((IfsDirectoryItem)anElement).isError()) {
            selectedDirectory = (IfsDirectoryItem)anElement;
            selectedPathLabel.setText(selectedDirectory.getPath());
        } else {
            selectedDirectory = null;
            selectedPathLabel.setText(""); //$NON-NLS-1$
        }

        updateOkButton();
    }

    private void updateOkButton() {
        if (getButton(IDialogConstants.OK_ID) != null) {
            getButton(IDialogConstants.OK_ID).setEnabled(selectedDirectory != null);
        }
    }

    private static class TreeLabelProvider extends LabelProvider {

        @Override
        public String getText(Object anElement) {
            return ((IfsDirectoryItem)anElement).getName();
        }

        @Override
        public Image getImage(Object anElement) {

            ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();
            if (((IfsDirectoryItem)anElement).isError()) {
                return sharedImages.getImage(ISharedImages.IMG_OBJS_ERROR_TSK);
            }

            return sharedImages.getImage(ISharedImages.IMG_OBJ_FOLDER);
        }
    }

    private static class TreeContentProvider implements ITreeContentProvider {

        public void dispose() {
        }

        public void inputChanged(Viewer aViewer, Object anOldInput, Object aNewInput) {
        }

        public Object[] getElements(Object anInputElement) {
            return ArrayContentProvider.getInstance().getElements(anInputElement);
        }

        public Object[] getChildren(Object aParentElement) {
            return ((IfsDirectoryItem)aParentElement).getChildren();
        }

        public Object getParent(Object anElement) {
            return null;
        }

        public boolean hasChildren(Object anElement) {
            return ((IfsDirectoryItem)anElement).hasChildren();
        }
    }
}
