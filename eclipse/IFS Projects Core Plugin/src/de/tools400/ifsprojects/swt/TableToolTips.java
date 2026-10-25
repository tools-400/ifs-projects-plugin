/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.swt;

import org.eclipse.jface.window.DefaultToolTip;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

/**
 * Shows a tool tip per row of a table, e.g. the full texts of problems, that a
 * column shows only partly. It is a tool tip of JFace, because the native tool
 * tip text of the table does not show on Windows, where the table shows its own
 * tips of truncated cells.
 */
public final class TableToolTips {

    private TableToolTips() {
        // Utility class
    }

    /**
     * Provides the tool tip of a row.
     */
    public interface Provider {

        /**
         * Returns the tool tip of the element of a row.
         *
         * @param anElement - element of the row
         * @return tool tip or <code>null</code>
         */
        String getToolTip(Object anElement);
    }

    /**
     * Shows the tool tips of the rows of a table, while the mouse is over them.
     *
     * @param aTable - table, whose items hold the elements, e.g. the table of a
     *        table viewer
     * @param aProvider - provider of the tool tips
     */
    public static void install(Table aTable, Provider aProvider) {
        new RowToolTip(aTable, aProvider);
    }

    /**
     * Tool tip, that is created again for each row.
     */
    private static final class RowToolTip extends DefaultToolTip {

        private final Table table;
        private final Provider provider;

        RowToolTip(Table aTable, Provider aProvider) {
            super(aTable, ToolTip.RECREATE, false);
            this.table = aTable;
            this.provider = aProvider;
        }

        @Override
        protected Object getToolTipArea(Event anEvent) {
            return getItem(anEvent);
        }

        @Override
        protected boolean shouldCreateToolTip(Event anEvent) {
            return super.shouldCreateToolTip(anEvent) && getText(anEvent) != null;
        }

        @Override
        protected String getText(Event anEvent) {
            TableItem item = getItem(anEvent);
            return item == null ? null : provider.getToolTip(item.getData());
        }

        private TableItem getItem(Event anEvent) {
            return table.getItem(new Point(anEvent.x, anEvent.y));
        }
    }
}
