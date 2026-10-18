/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.swt;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;

import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.export.ExportSettings;
import de.tools400.ifsprojects.export.SourceCheck;

/**
 * Editable combo box of the CCSID of new source files. The list contains the
 * common single-byte EBCDIC CCSIDs with a description, e.g.
 * <code>1141 - Germany, Austria (euro)</code>. Other CCSIDs can be entered as
 * number. Valid are the single-byte EBCDIC CCSIDs, see
 * {@link SourceCheck#isSingleByteEbcdic(int)}.
 */
public class CcsidCombo {

    /** CCSIDs of the list, euro variants first. All are single-byte EBCDIC. */
    private static final int[] CCSIDS = { 1141, 273, 1140, 37, 1148, 500, 1142, 1143, 1144, 1145, 1146, 1147, 1149, 1153, 1154, 1155 };

    /** CCSID entered as number, optionally followed by the description. */
    private static final Pattern CCSID_TEXT = Pattern.compile("^\\s*(\\d+)\\s*(?:-.*)?$"); //$NON-NLS-1$

    private final Combo combo;

    /**
     * Creates the combo box.
     *
     * @param aParent - parent composite
     */
    public CcsidCombo(Composite aParent) {

        combo = new Combo(aParent, SWT.DROP_DOWN);
        for (int ccsid : CCSIDS) {
            combo.add(getLabel(ccsid));
        }
        combo.setVisibleItemCount(CCSIDS.length);
    }

    /**
     * Returns the combo box, e.g. to set its layout data.
     */
    public Combo getControl() {
        return combo;
    }

    /**
     * Shows a CCSID: the item of the list or the number.
     *
     * @param aCcsid - CCSID
     */
    public void setCcsid(int aCcsid) {

        for (int i = 0; i < CCSIDS.length; i++) {
            if (CCSIDS[i] == aCcsid) {
                combo.select(i);
                return;
            }
        }

        combo.setText(Integer.toString(aCcsid));
    }

    /**
     * Returns the CCSID, that has been selected or entered.
     *
     * @return CCSID or <code>null</code>, if the text does not start with a
     *         number in the range of CCSIDs
     */
    public Integer getCcsid() {
        return parseCcsid(combo.getText());
    }

    /**
     * Returns the CCSID of the text of the combo box: a number, optionally
     * followed by the description of an item, e.g.
     * <code>1141 - Germany, Austria (euro)</code>.
     *
     * @param aText - text
     * @return CCSID or <code>null</code>, if the text does not start with a
     *         number in the range of CCSIDs
     */
    public static Integer parseCcsid(String aText) {

        Matcher matcher = CCSID_TEXT.matcher(aText);
        if (!matcher.matches()) {
            return null;
        }

        return ExportSettings.parseInt(matcher.group(1), ExportSettings.MIN_CCSID, ExportSettings.MAX_CCSID);
    }

    /**
     * Checks the CCSID.
     *
     * @return error message or <code>null</code>, if the CCSID is valid
     */
    public String getErrorMessage() {

        Integer ccsid = getCcsid();
        if (ccsid == null) {
            return Messages.bind(Messages.Error_CCSID_invalid_A_B, Integer.toString(ExportSettings.MIN_CCSID),
                Integer.toString(ExportSettings.MAX_CCSID));
        }

        if (!SourceCheck.isSingleByteEbcdic(ccsid.intValue())) {
            return Messages.bind(Messages.Error_CCSID_A_not_supported, ccsid.toString());
        }

        return null;
    }

    /**
     * Adds a listener, that is notified, when the text changes.
     */
    public void addModifyListener(ModifyListener aListener) {
        combo.addModifyListener(aListener);
    }

    /**
     * Enables or disables the combo box.
     */
    public void setEnabled(boolean anEnabled) {
        combo.setEnabled(anEnabled);
    }

    private static String getLabel(int aCcsid) {
        return Messages.bind(Messages.Label_CCSID_A_B, Integer.toString(aCcsid), getDescription(aCcsid));
    }

    private static String getDescription(int aCcsid) {

        switch (aCcsid) {
        case 1141:
            return withEuro(Messages.Label_CCSID_Germany_Austria);
        case 273:
            return Messages.Label_CCSID_Germany_Austria;
        case 1140:
            return withEuro(Messages.Label_CCSID_USA_Canada);
        case 37:
            return Messages.Label_CCSID_USA_Canada;
        case 1148:
            return withEuro(Messages.Label_CCSID_International);
        case 500:
            return Messages.Label_CCSID_International;
        case 1142:
            return withEuro(Messages.Label_CCSID_Denmark_Norway);
        case 1143:
            return withEuro(Messages.Label_CCSID_Finland_Sweden);
        case 1144:
            return withEuro(Messages.Label_CCSID_Italy);
        case 1145:
            return withEuro(Messages.Label_CCSID_Spain_Latin_America);
        case 1146:
            return withEuro(Messages.Label_CCSID_United_Kingdom);
        case 1147:
            return withEuro(Messages.Label_CCSID_France);
        case 1149:
            return withEuro(Messages.Label_CCSID_Iceland);
        case 1153:
            return withEuro(Messages.Label_CCSID_Central_Europe);
        case 1154:
            return withEuro(Messages.Label_CCSID_Cyrillic);
        case 1155:
            return withEuro(Messages.Label_CCSID_Turkey);
        default:
            return ""; //$NON-NLS-1$
        }
    }

    private static String withEuro(String aDescription) {
        return Messages.bind(Messages.Label_A_with_euro, aDescription);
    }
}
