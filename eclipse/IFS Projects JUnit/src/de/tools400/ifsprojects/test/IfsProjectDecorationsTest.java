/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import de.tools400.ifsprojects.decorators.IfsProjectDecorations;

public class IfsProjectDecorationsTest {

    @Test
    public void testGetLocalSuffix() {
        assertEquals(" [GFD400]", IfsProjectDecorations.getLocalSuffix("GFD400"));
    }

    @Test
    public void testGetRemoteSuffixSameName() {
        assertNull(IfsProjectDecorations.getRemoteSuffix("/home/jsmith/peek400", Arrays.asList("peek400")));
        assertNull(IfsProjectDecorations.getRemoteSuffix("/home/jsmith/peek400/", Arrays.asList("PEEK400")));
        assertNull(IfsProjectDecorations.getRemoteSuffix("/home/jsmith/peek400", Arrays.asList("peek400", "Peek400")));
    }

    @Test
    public void testGetRemoteSuffixOtherName() {
        assertEquals(" [Peek/400]", IfsProjectDecorations.getRemoteSuffix("/home/jsmith/peek400", Arrays.asList("Peek/400")));
        assertEquals(" [peek400, Peek test]", IfsProjectDecorations.getRemoteSuffix("/home/jsmith/peek400", Arrays.asList("peek400", "Peek test")));
        assertEquals(" [Root]", IfsProjectDecorations.getRemoteSuffix("/", Arrays.asList("Root")));
    }

    @Test
    public void testGetRemoteSuffixNoProject() {
        assertNull(IfsProjectDecorations.getRemoteSuffix("/home/jsmith/peek400", Collections.<String> emptyList()));
        assertNull(IfsProjectDecorations.getRemoteSuffix("/home/jsmith/peek400", null));
    }
}
