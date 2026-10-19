/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.terminal.tm;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.tm.internal.terminal.control.CommandInputFieldWithHistory;
import org.eclipse.tm.internal.terminal.control.ITerminalListener;
import org.eclipse.tm.internal.terminal.control.ITerminalViewControl;
import org.eclipse.tm.internal.terminal.control.TerminalViewControlFactory;
import org.eclipse.tm.internal.terminal.control.actions.AbstractTerminalAction;
import org.eclipse.tm.internal.terminal.control.actions.TerminalActionClearAll;
import org.eclipse.tm.internal.terminal.control.actions.TerminalActionCopy;
import org.eclipse.tm.internal.terminal.control.actions.TerminalActionPaste;
import org.eclipse.tm.internal.terminal.control.actions.TerminalActionSelectAll;
import org.eclipse.tm.internal.terminal.provisional.api.ITerminalConnector;
import org.eclipse.tm.internal.terminal.provisional.api.TerminalState;

import de.tools400.ifsprojects.IfsProjectsPlugin;
import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.terminal.ITerminalCommandLine;
import de.tools400.ifsprojects.terminal.ShellCommands;
import de.tools400.ifsprojects.terminal.TerminalException;
import de.tools400.ifsprojects.terminal.TerminalRequest;
import de.tools400.ifsprojects.utils.PlatformUtils;
import de.tools400.ifsprojects.utils.StringUtils;

/**
 * One SSH terminal session of the {@link TmTerminalView}: the terminal control,
 * its connector and the command line that is sent, when the shell is ready.
 * <p>
 * Notes on the supported TM terminal versions (RDi 9.6, 9.8, 9.9):
 * <ul>
 * <li>Calls, that differ between the versions, are made by reflection, see
 * {@link TmCompatibility}.</li>
 * <li><code>ITerminalListener2</code> does not exist in RDi 9.6.</li>
 * <li>The terminal listener is called on the connector thread.</li>
 * </ul>
 * The keys of the terminal are handled by the {@link TerminalKeyHandler}.
 */
@SuppressWarnings("restriction")
class TerminalSession {

    /**
     * Receives state changes of the session. Called on the UI thread.
     */
    interface Listener {
        void sessionStateChanged(TerminalSession aSession);
    }

    private static final String FONT_DEFINITION = "terminal.views.view.font.definition"; //$NON-NLS-1$

    /** History size of the command input field (same as RDi 9.8). */
    private static final int COMMAND_HISTORY_SIZE = 100;

    /*
     * Waiting for the shell before the initial command line is sent
     * (milliseconds): poll interval, time without output after the prompt,
     * maximum wait time without any output and maximum wait time at all.
     */
    private static final int READY_POLL_INTERVAL = 100;
    private static final long READY_QUIET_PERIOD = 400;
    private static final long READY_NO_OUTPUT_TIMEOUT = 3000;
    private static final long READY_MAX_WAIT = 10000;

    private final TerminalRequest request;
    private final int instanceNumber;
    private final Listener listener;

    private String customName;
    private Composite page;
    private ITerminalViewControl control;
    private TerminalOutputMonitor outputMonitor;
    private CommandInputFieldWithHistory commandInputField;
    private AbstractTerminalAction[] contextMenuActions;
    private TerminalKeyHandler keyHandler;
    private String errorMessage;

    private CommandLine pendingCommandLine;
    private int connectGeneration;

    /* Last command line passed to the session, see isReusableFor(). */
    private CommandLine lastCommandLine;

    /*
     * Number of times, the session has been disconnected or closed, see
     * ITerminalCommandLine. Read on any thread.
     */
    private volatile int closeCount;
    private TerminalState lastState;

    private boolean isControlDisposed;

    /**
     * Creates a new session.
     *
     * @param aRequest - terminal request
     * @param anInstanceNumber - number of the session among the sessions with
     *        the same title, starting at 1
     * @param aListener - receives state changes
     */
    TerminalSession(TerminalRequest aRequest, int anInstanceNumber, Listener aListener) {
        this.request = aRequest;
        this.instanceNumber = anInstanceNumber;
        this.listener = aListener;
    }

    /**
     * Creates the terminal control on a new page of the specified parent. If
     * the terminal cannot be created, the page shows an error message.
     *
     * @param aParent - parent composite (stack of pages)
     * @return <code>true</code>, if the terminal has been created
     */
    boolean create(Composite aParent) {

        page = new Composite(aParent, SWT.NONE);
        page.setLayout(new FillLayout());

        // Closing the view disposes the widgets, before the view disposes its
        // sessions. Therefore the terminal is disconnected, when its page is
        // disposed. Otherwise the SSH connection and the shell would stay
        // alive.
        page.addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent anEvent) {
                disposeControl();
            }
        });

        ITerminalConnector connector;
        try {
            connector = TmConnectorFactory.createSshConnector();
        } catch (TerminalException e) {
            showError(e.getLocalizedMessage(), e);
            return false;
        }

        MapSettingsStore settings = new MapSettingsStore();
        settings.put(TmConnectorFactory.SETTING_HOST, request.getHostName());
        settings.put(TmConnectorFactory.SETTING_USER, request.getUserId());
        settings.put(TmConnectorFactory.SETTING_PORT, Integer.toString(request.getPort()));
        settings.put(TmConnectorFactory.SETTING_TIMEOUT, Integer.toString(request.getTimeout()));
        settings.put(TmConnectorFactory.SETTING_KEEP_ALIVE, Integer.toString(request.getKeepAlive()));
        settings.put(TmConnectorFactory.SETTING_PASSWORD, request.getPassword());
        connector.load(settings);

        String initializationError = connector.getInitializationErrorMessage();
        if (initializationError != null) {
            showError(initializationError, null);
            return false;
        }

        // Observe the output of the shell, see sendPendingCommandLineWhenReady().
        outputMonitor = new TerminalOutputMonitor();
        ITerminalConnector observedConnector = outputMonitor.observe(connector);

        // useCommonPrefs = true: colors, inverted colors, buffer lines and
        // font follow 'Preferences > Terminal'.
        control = TerminalViewControlFactory.makeControl(new TerminalListener(), page, new ITerminalConnector[] { observedConnector }, true);
        control.setConnector(observedConnector);
        control.setFont(FONT_DEFINITION);

        try {
            control.setEncoding(request.getEncoding());
        } catch (UnsupportedEncodingException e) {
            IfsProjectsPlugin.logWarning("Unsupported terminal encoding: " + request.getEncoding()); //$NON-NLS-1$
        }

        hookContextMenu();

        keyHandler = new TerminalKeyHandler(this, control);
        keyHandler.install();

        return true;
    }

    /**
     * Connects the terminal. The command line is sent to the shell, once it is
     * ready.
     *
     * @param aCommandLine - command line or <code>null</code>
     * @return command line passed to the session
     */
    ITerminalCommandLine connect(String aCommandLine) {

        CommandLine commandLine = createCommandLine(aCommandLine);

        if (!isAlive()) {
            closeCount++;
            return commandLine;
        }

        pendingCommandLine = aCommandLine == null ? null : commandLine;
        connectGeneration++;

        try {
            TmCompatibility.connectTerminal(control);
        } catch (InvocationTargetException e) {
            pendingCommandLine = null;
            closeCount++;
            IfsProjectsPlugin.logError(Messages.bind(Messages.Error_Could_not_connect_terminal_A, e.getCause().getLocalizedMessage()),
                e.getCause());
        } catch (Exception e) {
            pendingCommandLine = null;
            closeCount++;
            IfsProjectsPlugin.logError(Messages.bind(Messages.Error_Could_not_connect_terminal_A, e.getLocalizedMessage()), e);
        }

        return commandLine;
    }

    /**
     * Connects the terminal again and changes to the directory of the request.
     * The command of the request is never repeated.
     */
    void reconnect() {
        connect(ShellCommands.toCommandLine(request.getDirectory(), null));
    }

    /**
     * Connects the terminal again, if it is disconnected, see
     * {@link #reconnect()}.
     */
    void reconnectIfDisconnected() {
        if (isAlive() && !isConnectedOrConnecting()) {
            reconnect();
        }
    }

    void disconnect() {

        if (!isAlive()) {
            return;
        }

        pendingCommandLine = null;
        if (control.getState() != TerminalState.CLOSED) {
            control.disconnectTerminal();
        }
    }

    /**
     * Sends a command line to the connected shell, e.g. the build command.
     *
     * @param aCommandLine - command line
     * @return command line sent to the session
     */
    ITerminalCommandLine sendCommandLine(String aCommandLine) {

        CommandLine commandLine = createCommandLine(aCommandLine);

        if (isConnected() && aCommandLine != null) {
            control.pasteString(aCommandLine);
            commandLine.setSent();
        } else {
            closeCount++;
        }

        return commandLine;
    }

    void dispose() {

        disposeControl();

        if (page != null && !page.isDisposed()) {
            page.dispose();
        }

        control = null;
        pendingCommandLine = null;
        contextMenuActions = null;
        commandInputField = null;
    }

    TerminalRequest getRequest() {
        return request;
    }

    Composite getPage() {
        return page;
    }

    /**
     * Returns the name of the session as shown in the session list.
     */
    String getName() {

        if (customName != null) {
            return customName;
        }

        if (instanceNumber > 1) {
            return request.getTitle() + " (" + instanceNumber + ")"; //$NON-NLS-1$ //$NON-NLS-2$
        }

        return request.getTitle();
    }

    String getCustomName() {
        return customName;
    }

    /**
     * Sets the name of the session. An empty name restores the default name.
     */
    void setCustomName(String aName) {
        customName = StringUtils.trimToNull(aName);
    }

    String getErrorMessage() {
        return errorMessage;
    }

    boolean isAlive() {
        return control != null && !control.isDisposed();
    }

    boolean isConnected() {
        return isAlive() && control.isConnected();
    }

    /**
     * Returns <code>true</code>, if the terminal is connected or connecting.
     */
    boolean isConnectedOrConnecting() {
        return isAlive() && control.getState() != TerminalState.CLOSED;
    }

    boolean hasSessionKey(String aSessionKey) {
        return aSessionKey != null && aSessionKey.equals(request.getSessionKey());
    }

    /**
     * Returns <code>true</code>, if the session receives the command line of a
     * request with the specified reuse key: it is connected and its last
     * command is not running, see {@link ITerminalCommandLine#setRunning}.
     */
    boolean isReusableFor(String aReuseKey) {
        return isIdleFor(aReuseKey) && isConnected();
    }

    /**
     * Returns <code>true</code>, if the session can be connected again for the
     * command line of a request with the specified reuse key: it is
     * disconnected and its last command is not running.
     */
    boolean isReconnectableFor(String aReuseKey) {
        return isIdleFor(aReuseKey) && isAlive() && !isConnectedOrConnecting();
    }

    void setFocus() {
        if (isAlive()) {
            control.setFocus();
        } else if (page != null && !page.isDisposed()) {
            page.setFocus();
        }
    }

    /**
     * Returns <code>true</code>, if the control is part of this session, e.g.
     * the terminal or its command input field.
     */
    boolean contains(Control aControl) {
        return PlatformUtils.contains(page, aControl);
    }

    boolean isScrollLock() {
        return isAlive() && control.isScrollLock();
    }

    void setScrollLock(boolean anIsScrollLock) {
        if (isAlive()) {
            control.setScrollLock(anIsScrollLock);
        }
    }

    void clear() {
        if (isAlive()) {
            control.clearTerminal();
        }
    }

    /**
     * Copies the selected text to the clipboard and clears the selection, so
     * that the next Ctrl+C goes to the shell again.
     *
     * @return <code>true</code>, if text was selected
     */
    boolean copySelection() {

        if (!isAlive()) {
            return false;
        }

        String selection = control.getSelection();
        if (selection == null || selection.length() == 0) {
            return false;
        }

        control.copy();
        TmCompatibility.clearSelection(control);

        return true;
    }

    /**
     * Sends the text of the clipboard to the shell.
     */
    void paste() {
        if (isAlive()) {
            control.paste();
        }
    }

    boolean isCommandInputFieldVisible() {
        return isAlive() && control.getCommandInputField() != null;
    }

    void setCommandInputFieldVisible(boolean anIsVisible) {

        if (!isAlive()) {
            return;
        }

        if (anIsVisible) {
            if (commandInputField == null) {
                commandInputField = new CommandInputFieldWithHistory(COMMAND_HISTORY_SIZE);
            }
            control.setCommandInputField(commandInputField);
        } else {
            control.setCommandInputField(null);
        }

        page.layout(true, true);
    }

    /**
     * Sends the pending command line, when the remote shell is ready: it has
     * written its prompt and is quiet. Sending the command line earlier makes
     * the shell echo it twice (before and after executing its profile).
     */
    private void sendPendingCommandLineWhenReady() {

        final int generation = connectGeneration;
        final long connectedTime = System.currentTimeMillis();

        page.getDisplay().timerExec(READY_POLL_INTERVAL, new Runnable() {
            public void run() {

                if (generation != connectGeneration || pendingCommandLine == null || !isAlive() || control.getState() != TerminalState.CONNECTED) {
                    return;
                }

                long waited = System.currentTimeMillis() - connectedTime;
                boolean isReady = outputMonitor.isQuiet(READY_QUIET_PERIOD) || (!outputMonitor.hasOutput() && waited >= READY_NO_OUTPUT_TIMEOUT)
                    || waited >= READY_MAX_WAIT;

                if (isReady) {
                    CommandLine commandLine = pendingCommandLine;
                    pendingCommandLine = null;
                    control.pasteString(commandLine.getText());
                    commandLine.setSent();
                } else {
                    page.getDisplay().timerExec(READY_POLL_INTERVAL, this);
                }
            }
        });
    }

    /**
     * Disconnects and disposes the terminal control. Called, when the page is
     * disposed, and by {@link #dispose()}, whichever comes first.
     */
    private void disposeControl() {

        if (isControlDisposed) {
            return;
        }
        isControlDisposed = true;
        closeCount++;

        if (keyHandler != null) {
            keyHandler.dispose();
            keyHandler = null;
        }

        if (control != null && !control.isDisposed()) {
            if (control.getState() != TerminalState.CLOSED) {
                control.disconnectTerminal();
            }
            control.disposeTerminal();
        }
    }

    /**
     * Returns the command line, that is passed to the session now, and
     * remembers it as the last command line of the session.
     *
     * @param aText - command line or <code>null</code>
     */
    private CommandLine createCommandLine(String aText) {

        lastCommandLine = new CommandLine(aText, closeCount);

        return lastCommandLine;
    }

    private void onStateChanged(TerminalState aState) {

        // The initial state is CLOSED, before the session is connected.
        if (aState == TerminalState.CLOSED && lastState != null && lastState != TerminalState.CLOSED) {
            closeCount++;
        }
        lastState = aState;

        if (!isAlive()) {
            return;
        }

        keyHandler.stateChanged();

        if (aState == TerminalState.CONNECTED) {
            TmCompatibility.restartDisplayUpdates(control);
            if (pendingCommandLine != null) {
                sendPendingCommandLineWhenReady();
            }
        } else if (aState == TerminalState.CLOSED) {
            pendingCommandLine = null;
        }

        listener.sessionStateChanged(this);
    }

    /**
     * Returns <code>true</code>, if the session has been opened for a request
     * with the specified reuse key and its last command is not running.
     */
    private boolean isIdleFor(String aReuseKey) {
        return aReuseKey != null && aReuseKey.equals(request.getReuseKey()) && (lastCommandLine == null || !lastCommandLine.isRunning());
    }

    private void showError(String aMessage, Throwable aCause) {

        IfsProjectsPlugin.logError(aMessage, aCause);
        errorMessage = aMessage;

        Label label = new Label(page, SWT.WRAP);
        label.setText(aMessage);
    }

    private void hookContextMenu() {

        contextMenuActions = new AbstractTerminalAction[] { new TerminalActionCopy(control), new TerminalActionPaste(control),
            new TerminalActionSelectAll(control), new TerminalActionClearAll(control) };

        MenuManager menuManager = new MenuManager("#PopupMenu"); //$NON-NLS-1$
        menuManager.setRemoveAllWhenShown(true);
        menuManager.addMenuListener(new IMenuListener() {
            public void menuAboutToShow(IMenuManager aManager) {
                fillContextMenu(aManager);
            }
        });

        Control textControl = control.getControl();
        Menu menu = menuManager.createContextMenu(textControl);
        textControl.setMenu(menu);
    }

    private void fillContextMenu(IMenuManager aManager) {

        if (contextMenuActions == null) {
            return;
        }

        for (int i = 0; i < contextMenuActions.length; i++) {
            AbstractTerminalAction action = contextMenuActions[i];
            action.updateAction(true);
            aManager.add(action);
            if (i == 1) {
                aManager.add(new Separator());
            }
        }
    }

    /**
     * Command line passed to the session. It reports the session as closed,
     * once the session is disconnected or closed.
     */
    private class CommandLine implements ITerminalCommandLine {

        private final String text;
        private final int count;

        private volatile boolean isSent;
        private volatile boolean isRunning;

        CommandLine(String aText, int aCount) {
            this.text = aText;
            this.count = aCount;
        }

        String getText() {
            return text;
        }

        void setSent() {
            isSent = true;
        }

        boolean isRunning() {
            return isRunning;
        }

        public boolean isSessionClosed() {
            return closeCount != count;
        }

        public boolean isSent() {
            return isSent;
        }

        public void setRunning(boolean anIsRunning) {
            isRunning = anIsRunning;
        }
    }

    /**
     * Receives state changes of the terminal. The listener is called on the
     * connector thread.
     */
    private class TerminalListener implements ITerminalListener {

        public void setState(final TerminalState aState) {

            if (page == null || page.isDisposed()) {
                return;
            }

            Display display = page.getDisplay();
            display.asyncExec(new Runnable() {
                public void run() {
                    onStateChanged(aState);
                }
            });
        }

        public void setTerminalTitle(String aTitle) {
            // The name of the session is controlled by the request.
        }
    }
}
