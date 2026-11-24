/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.iagentbridge;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runs an action once, when a time has passed, unless it has been stopped
 * before, e.g. to cancel the job of a tool after its timeout. The action runs
 * in a daemon thread of its own.
 * <p>
 * The class has no Eclipse dependencies, so that it can be tested outside of
 * RDi.
 */
public final class Watchdog {

    private final Timer timer;
    private final AtomicBoolean isExpired;

    private Watchdog() {
        this.timer = new Timer("IFS Projects iAgentBridge timeout", true); //$NON-NLS-1$
        this.isExpired = new AtomicBoolean();
    }

    /**
     * Starts a watchdog.
     *
     * @param anAction - action, that runs, when the time has passed
     * @param aMillis - time in milliseconds
     * @return watchdog, which is stopped with {@link #stop()}
     */
    public static Watchdog start(final Runnable anAction, long aMillis) {

        final Watchdog watchdog = new Watchdog();
        watchdog.timer.schedule(new TimerTask() {
            @Override
            public void run() {
                watchdog.isExpired.set(true);
                anAction.run();
            }
        }, aMillis);

        return watchdog;
    }

    /**
     * Stops the watchdog. The action does not run any more, unless it has
     * already started.
     */
    public void stop() {
        timer.cancel();
    }

    /**
     * Tells, whether the time has passed and the action has run or is running.
     *
     * @return <code>true</code>, if the watchdog has expired
     */
    public boolean isExpired() {
        return isExpired.get();
    }
}
