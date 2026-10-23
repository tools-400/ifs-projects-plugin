/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.tools400.ifsprojects.sources.headers.QuotedStrings;

public class QuotedStringsTest {

    @Test
    public void testSkip() {

        // Behind the string follows the parenthesis, that closes TEXT.
        String text = "TEXT('Order (new)') ACTGRP(*NEW)";

        assertEquals(") ACTGRP(*NEW)", text.substring(QuotedStrings.skip(text, text.indexOf(QuotedStrings.QUOTE))));
    }

    @Test
    public void testSkip_doubledQuotes() {

        String text = "TEXT('Don''t') X";

        assertEquals(") X", text.substring(QuotedStrings.skip(text, 5)));
        assertEquals(") X", text.substring(QuotedStrings.skip(new StringBuilder(text), 5)));
    }

    @Test
    public void testSkip_notClosed() {

        assertEquals(12, QuotedStrings.skip("TEXT('Orders", 5));
        assertEquals(1, QuotedStrings.skip("'", 0));
    }
}
