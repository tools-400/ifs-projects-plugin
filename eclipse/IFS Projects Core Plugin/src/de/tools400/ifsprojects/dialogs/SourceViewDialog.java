/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.dialogs;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ListResourceBundle;
import java.util.ResourceBundle;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.contentmergeviewer.TextMergeViewer;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.LineNumberRulerColumn;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.texteditor.FindReplaceAction;

import de.tools400.ifsprojects.Messages;

/**
 * Shows the content of a source, that the export or the migration would write,
 * without writing it: the file or the member does not exist yet, so it cannot
 * be opened in an editor.
 * <ul>
 * <li>Tab <i>Result</i>: the converted source in the viewer of the text
 * editors ({@link SourceViewer}) with line numbers and the text font of the
 * workbench, read-only. <i>Find/Replace</i> is the action of the editors
 * ({@link FindReplaceAction}), on the button and on Ctrl+F.</li>
 * <li>Tab <i>Comparison</i>: the source before and after the conversion in the
 * compare viewer of the platform, read-only. It is missing, if there is
 * nothing to compare. The compare viewer keeps its find/replace target to
 * itself and wires its own action to a workbench part, which a dialog does not
 * have; therefore the button is disabled on this tab.</li>
 * </ul>
 */
public final class SourceViewDialog extends Dialog {

    private static final Charset UTF_8 = Charset.forName("UTF-8"); //$NON-NLS-1$

    /** Label of the action of Find/Replace, which has its own texts. */
    private static final ResourceBundle FIND_REPLACE_BUNDLE = new ListResourceBundle() {
        @Override
        protected Object[][] getContents() {
            return new Object[][] { { "label", Messages.Label_Find } }; //$NON-NLS-1$
        }
    };

    private static final int FIND_ID = IDialogConstants.CLIENT_ID;
    private static final int RESULT_TAB = 0;
    private static final int WIDTH_IN_CHARS = 100;
    private static final int HEIGHT_IN_CHARS = 30;

    private final String title;
    private final String originalLabel;
    private final String originalContent;
    private final String resultLabel;
    private final String resultContent;

    private TabFolder tabs;
    private FindReplaceAction findReplace;

    /**
     * Creates the dialog.
     *
     * @param aParentShell - parent shell
     * @param aTitle - title of the dialog, e.g. the path of the stream file
     * @param anOriginalLabel - label of the source before the conversion, e.g.
     *        the member
     * @param anOriginalContent - source before the conversion or
     *        <code>null</code>
     * @param aResultLabel - label of the source after the conversion
     * @param aResultContent - source after the conversion or <code>null</code>,
     *        if it has not been converted
     */
    public SourceViewDialog(Shell aParentShell, String aTitle, String anOriginalLabel, String anOriginalContent, String aResultLabel,
        String aResultContent) {
        super(aParentShell);
        this.title = aTitle;
        this.originalLabel = anOriginalLabel;
        this.originalContent = anOriginalContent;
        this.resultLabel = aResultLabel;
        this.resultContent = aResultContent == null ? anOriginalContent : aResultContent;
    }

    /**
     * Opens the dialog, if there is a content to show.
     *
     * @see #SourceViewDialog(Shell, String, String, String, String, String)
     */
    public static void open(Shell aParentShell, String aTitle, String anOriginalLabel, String anOriginalContent, String aResultLabel,
        String aResultContent) {

        if (anOriginalContent == null && aResultContent == null) {
            return;
        }

        new SourceViewDialog(aParentShell, aTitle, anOriginalLabel, anOriginalContent, aResultLabel, aResultContent).open();
    }

    @Override
    protected void configureShell(Shell aShell) {
        super.configureShell(aShell);
        aShell.setText(title);
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    @Override
    protected Control createDialogArea(Composite aParent) {

        Composite area = (Composite)super.createDialogArea(aParent);

        tabs = new TabFolder(area, SWT.NONE);
        tabs.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        TabItem result = new TabItem(tabs, SWT.NONE);
        result.setText(Messages.Label_Tab_Result);
        result.setControl(createSourceViewer(tabs));

        if (originalContent != null && !originalContent.equals(resultContent)) {
            TabItem comparison = new TabItem(tabs, SWT.NONE);
            comparison.setText(Messages.Label_Tab_Comparison);
            comparison.setControl(createCompareViewer(tabs));
        }

        return area;
    }

    /**
     * Creates the viewer of the text editors with line numbers and the action
     * of Find/Replace, which is also started with Ctrl+F.
     */
    private Control createSourceViewer(Composite aParent) {

        Composite composite = new Composite(aParent, SWT.NONE);
        composite.setLayout(new FillLayout());

        CompositeRuler ruler = new CompositeRuler();
        LineNumberRulerColumn lineNumbers = new LineNumberRulerColumn();
        lineNumbers.setFont(JFaceResources.getTextFont());
        ruler.addDecorator(0, lineNumbers);

        SourceViewer viewer = new SourceViewer(composite, ruler, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
        viewer.setDocument(new Document(resultContent));
        viewer.setEditable(false);
        viewer.getTextWidget().setFont(JFaceResources.getTextFont());

        findReplace = new FindReplaceAction(FIND_REPLACE_BUNDLE, "", getShell(), viewer.getFindReplaceTarget()); //$NON-NLS-1$

        viewer.getTextWidget().addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent anEvent) {
                if (anEvent.stateMask == SWT.MOD1 && (anEvent.keyCode == 'f' || anEvent.keyCode == 'F')) {
                    findReplace.run();
                }
            }
        });

        return composite;
    }

    /**
     * Creates the compare viewer of the platform with the source before and
     * after the conversion.
     */
    private Control createCompareViewer(Composite aParent) {

        Composite composite = new Composite(aParent, SWT.NONE);
        composite.setLayout(new FillLayout());

        CompareConfiguration configuration = new CompareConfiguration();
        configuration.setLeftLabel(originalLabel);
        configuration.setRightLabel(resultLabel);
        configuration.setLeftEditable(false);
        configuration.setRightEditable(false);

        TextMergeViewer viewer = new TextMergeViewer(composite, SWT.NONE, configuration);
        viewer.setInput(new DiffNode(new Content(originalLabel, originalContent), new Content(resultLabel, resultContent)));

        return composite;
    }

    @Override
    protected void createButtonsForButtonBar(Composite aParent) {

        final Button find = createButton(aParent, FIND_ID, Messages.Label_Find, false);
        createButton(aParent, IDialogConstants.CLOSE_ID, IDialogConstants.CLOSE_LABEL, true);

        // The compare viewer wires its own Find/Replace to a workbench part,
        // which a dialog does not have, and keeps its target to itself.
        tabs.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent anEvent) {
                find.setEnabled(tabs.getSelectionIndex() == RESULT_TAB);
            }
        });
    }

    @Override
    protected void buttonPressed(int aButtonId) {

        if (aButtonId == FIND_ID) {
            findReplace.run();
            return;
        }

        setReturnCode(aButtonId);
        close();
    }

    @Override
    protected Point getInitialSize() {

        Point size = super.getInitialSize();

        return new Point(Math.max(size.x, convertWidthInCharsToPixels(WIDTH_IN_CHARS)), Math.max(size.y,
            convertHeightInCharsToPixels(HEIGHT_IN_CHARS)));
    }

    /**
     * Content of the compare viewer.
     */
    private static final class Content implements ITypedElement, IStreamContentAccessor {

        private final String name;
        private final String text;

        Content(String aName, String aText) {
            this.name = aName;
            this.text = aText;
        }

        public String getName() {
            return name;
        }

        public Image getImage() {
            return null;
        }

        public String getType() {
            return ITypedElement.TEXT_TYPE;
        }

        public InputStream getContents() {
            return new ByteArrayInputStream(text.getBytes(UTF_8));
        }
    }
}
