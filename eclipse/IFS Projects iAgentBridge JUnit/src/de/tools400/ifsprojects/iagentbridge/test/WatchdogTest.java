/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.iagentbridge.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import de.tools400.ifsprojects.iagentbridge.Watchdog;

public class WatchdogTest {

    @Test
    public void testStart_actionRunsWhenTheTimeHasPassed() throws Exception {

        final CountDownLatch latch = new CountDownLatch(1);
        Watchdog watchdog = Watchdog.start(new Runnable() {
            public void run() {
                latch.countDown();
            }
        }, 10);

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertTrue(watchdog.isExpired());
        watchdog.stop();
    }

    @Test
    public void testStop_actionDoesNotRunAfterStop() throws Exception {

        final AtomicBoolean isRun = new AtomicBoolean();
        Watchdog watchdog = Watchdog.start(new Runnable() {
            public void run() {
                isRun.set(true);
            }
        }, 1000);
        watchdog.stop();

        Thread.sleep(1500);

        assertFalse(isRun.get());
        assertFalse(watchdog.isExpired());
    }
}
