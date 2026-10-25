/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import de.tools400.ifsprojects.dialogs.OverwriteQuery;
import de.tools400.ifsprojects.dialogs.OverwriteQuery.Answer;
import de.tools400.ifsprojects.dialogs.RememberingOverwriteQuery;

public class RememberingOverwriteQueryTest {

    @Test
    public void testAsk_yesAndNo() {

        ScriptedQuery user = new ScriptedQuery(Answer.YES, Answer.NO, Answer.YES);
        OverwriteQuery query = new RememberingOverwriteQuery(user);

        assertEquals(Answer.YES, query.ask("A"));
        assertEquals(Answer.NO, query.ask("B"));
        assertEquals(Answer.YES, query.ask("C"));
        assertEquals(Arrays.asList("A", "B", "C"), user.objects);
    }

    @Test
    public void testAsk_toAll() {

        ScriptedQuery user = new ScriptedQuery(Answer.NO, Answer.YES_TO_ALL);
        OverwriteQuery query = new RememberingOverwriteQuery(user);

        assertEquals(Answer.NO, query.ask("A"));
        assertEquals(Answer.YES_TO_ALL, query.ask("B"));
        assertEquals(Answer.YES_TO_ALL, query.ask("C"));
        assertEquals(Answer.YES_TO_ALL, query.ask("D"));
        assertEquals(Arrays.asList("A", "B"), user.objects);

        user = new ScriptedQuery(Answer.NO_TO_ALL);
        query = new RememberingOverwriteQuery(user);

        assertEquals(Answer.NO_TO_ALL, query.ask("A"));
        assertEquals(Answer.NO_TO_ALL, query.ask("B"));
        assertEquals(Arrays.asList("A"), user.objects);
    }

    @Test
    public void testAsk_cancel() {

        ScriptedQuery user = new ScriptedQuery(Answer.CANCEL, Answer.YES);
        OverwriteQuery query = new RememberingOverwriteQuery(user);

        assertEquals(Answer.CANCEL, query.ask("A"));
        assertEquals(Answer.YES, query.ask("B"));
    }

    @Test
    public void testIsReplace() {
        assertTrue(Answer.YES.isReplace());
        assertTrue(Answer.YES_TO_ALL.isReplace());
        assertFalse(Answer.NO.isReplace());
        assertFalse(Answer.NO_TO_ALL.isReplace());
        assertFalse(Answer.CANCEL.isReplace());
    }

    /**
     * Answers like a user, who gives the scripted answers in this order.
     */
    private static class ScriptedQuery implements OverwriteQuery {

        private final LinkedList<Answer> answers;
        private final List<String> objects = new ArrayList<>();

        ScriptedQuery(Answer... anAnswers) {
            this.answers = new LinkedList<>(Arrays.asList(anAnswers));
        }

        public Answer ask(String anObject) {
            objects.add(anObject);
            return answers.removeFirst();
        }
    }
}
