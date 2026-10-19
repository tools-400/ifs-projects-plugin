/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.terminal.tm;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionGroup;

import de.tools400.ifsprojects.IfsProjectsPlugin;
import de.tools400.ifsprojects.Messages;

/**
 * Actions of the {@link TmTerminalView}. The toolbar actions apply to the
 * active session, the view menu contains actions for all sessions, the context
 * menu of the session list actions for the selected sessions.
 * <p>
 * The toolbar actions and their icons are the same as in the 'Terminal' view
 * of RDi 9.8. Connect and disconnect share one button.
 */
class TerminalActionGroup extends ActionGroup {

    private final TmTerminalView view;

    private final Action connectionAction;
    private final Action scrollLockAction;
    private final Action clearAction;
    private final Action commandInputFieldAction;
    private final Action newTerminalAction;

    private final Action copyAction;
    private final Action pasteAction;

    private final Action connectAllAction;
    private final Action disconnectAllAction;
    private final Action closeAllAction;

    private final Action connectSelectedAction;
    private final Action disconnectSelectedAction;
    private final Action closeSelectedAction;
    private final Action renameSelectedAction;

    /**
     * Creates the actions of a terminal view.
     *
     * @param aView - terminal view
     */
    TerminalActionGroup(TmTerminalView aView) {

        this.view = aView;

        connectionAction = new Action() {
            @Override
            public void run() {
                TerminalSession activeSession = view.getActiveSession();
                if (activeSession == null) {
                    return;
                }
                if (activeSession.isConnectedOrConnecting()) {
                    activeSession.disconnect();
                } else {
                    activeSession.reconnect();
                    activeSession.setFocus();
                }
                updateActionBars();
            }
        };
        updateConnectionAction(false);

        scrollLockAction = new Action(Messages.Label_Scroll_lock, IAction.AS_CHECK_BOX) {
            @Override
            public void run() {
                TerminalSession activeSession = view.getActiveSession();
                if (activeSession != null) {
                    activeSession.setScrollLock(isChecked());
                }
            }
        };
        setupAction(scrollLockAction, Messages.Tooltip_Scroll_lock, IfsProjectsPlugin.IMAGE_SCROLL_LOCK);

        clearAction = new Action(Messages.Label_Clear) {
            @Override
            public void run() {
                TerminalSession activeSession = view.getActiveSession();
                if (activeSession != null) {
                    activeSession.clear();
                }
            }
        };
        setupAction(clearAction, Messages.Tooltip_Clear, IfsProjectsPlugin.IMAGE_CLEAR);

        commandInputFieldAction = new Action(Messages.Label_Command_input_field, IAction.AS_CHECK_BOX) {
            @Override
            public void run() {
                TerminalSession activeSession = view.getActiveSession();
                if (activeSession != null) {
                    activeSession.setCommandInputFieldVisible(isChecked());
                }
            }
        };
        setupAction(commandInputFieldAction, Messages.Tooltip_Command_input_field, IfsProjectsPlugin.IMAGE_COMMAND_INPUT_FIELD);

        newTerminalAction = new Action(Messages.Label_New_terminal) {
            @Override
            public void run() {
                view.openNewTerminal();
            }
        };
        setupAction(newTerminalAction, Messages.Tooltip_New_terminal, IfsProjectsPlugin.IMAGE_NEW_TERMINAL);

        // Handlers of 'Edit > Copy' and 'Edit > Paste'. Ctrl+C and Ctrl+V reach
        // them only, if the terminal does not capture the keys, e.g. while it
        // is disconnected. Otherwise the key handler of the session handles the
        // keys, see TerminalKeyHandler.
        copyAction = new Action() {
            @Override
            public void run() {
                Control focusControl = Display.getCurrent().getFocusControl();
                TerminalSession activeSession = view.getActiveSession();
                if (focusControl instanceof Text) {
                    ((Text)focusControl).copy();
                } else if (activeSession != null && activeSession.contains(focusControl)) {
                    activeSession.copySelection();
                }
            }
        };

        pasteAction = new Action() {
            @Override
            public void run() {
                Control focusControl = Display.getCurrent().getFocusControl();
                TerminalSession activeSession = view.getActiveSession();
                if (focusControl instanceof Text) {
                    ((Text)focusControl).paste();
                } else if (activeSession != null && activeSession.contains(focusControl)) {
                    activeSession.paste();
                }
            }
        };

        connectAllAction = new Action(Messages.Label_Connect_all) {
            @Override
            public void run() {
                view.connect(view.getSessions());
            }
        };
        connectAllAction.setToolTipText(Messages.Tooltip_Connect_all);

        disconnectAllAction = new Action(Messages.Label_Disconnect_all) {
            @Override
            public void run() {
                view.disconnect(view.getSessions());
            }
        };
        disconnectAllAction.setToolTipText(Messages.Tooltip_Disconnect_all);

        closeAllAction = new Action(Messages.Label_Close_all) {
            @Override
            public void run() {
                view.closeSessions(view.getSessions());
            }
        };
        closeAllAction.setToolTipText(Messages.Tooltip_Close_all);

        connectSelectedAction = new Action(Messages.Label_Connect) {
            @Override
            public void run() {
                view.connect(view.getSelectedSessions());
            }
        };
        connectSelectedAction.setImageDescriptor(IfsProjectsPlugin.getEnabledImageDescriptor(IfsProjectsPlugin.IMAGE_CONNECT));
        connectSelectedAction.setToolTipText(Messages.Tooltip_Connect_selected);

        disconnectSelectedAction = new Action(Messages.Label_Disconnect) {
            @Override
            public void run() {
                view.disconnect(view.getSelectedSessions());
            }
        };
        disconnectSelectedAction.setImageDescriptor(IfsProjectsPlugin.getEnabledImageDescriptor(IfsProjectsPlugin.IMAGE_DISCONNECT));
        disconnectSelectedAction.setToolTipText(Messages.Tooltip_Disconnect_selected);

        closeSelectedAction = new Action(Messages.Label_Close) {
            @Override
            public void run() {
                view.closeSessions(view.getSelectedSessions());
            }
        };
        closeSelectedAction.setToolTipText(Messages.Tooltip_Close_selected);

        renameSelectedAction = new Action(Messages.Label_Rename) {
            @Override
            public void run() {
                List<TerminalSession> selectedSessions = view.getSelectedSessions();
                if (selectedSessions.size() == 1) {
                    view.rename(selectedSessions.get(0));
                }
            }
        };
        renameSelectedAction.setToolTipText(Messages.Tooltip_Rename_selected);
    }

    /**
     * Adds the handlers of copy and paste, the toolbar actions and the actions
     * of the view menu.
     */
    @Override
    public void fillActionBars(IActionBars anActionBars) {

        anActionBars.setGlobalActionHandler(ActionFactory.COPY.getId(), copyAction);
        anActionBars.setGlobalActionHandler(ActionFactory.PASTE.getId(), pasteAction);

        IToolBarManager toolBarManager = anActionBars.getToolBarManager();
        toolBarManager.add(connectionAction);
        toolBarManager.add(new Separator());
        toolBarManager.add(scrollLockAction);
        toolBarManager.add(clearAction);
        toolBarManager.add(commandInputFieldAction);
        toolBarManager.add(new Separator());
        toolBarManager.add(newTerminalAction);

        IMenuManager menuManager = anActionBars.getMenuManager();
        menuManager.add(connectAllAction);
        menuManager.add(disconnectAllAction);
        menuManager.add(new Separator());
        menuManager.add(closeAllAction);
    }

    /**
     * Adds the actions of the context menu of the session list.
     */
    @Override
    public void fillContextMenu(IMenuManager aMenuManager) {

        aMenuManager.add(connectSelectedAction);
        aMenuManager.add(disconnectSelectedAction);
        aMenuManager.add(new Separator());
        aMenuManager.add(renameSelectedAction);
        aMenuManager.add(new Separator());
        aMenuManager.add(closeSelectedAction);

        updateSelectionActions();
    }

    /**
     * Enables the actions for the active session, all sessions and the
     * selected sessions.
     */
    @Override
    public void updateActionBars() {

        TerminalSession activeSession = view.getActiveSession();
        boolean isAlive = activeSession != null && activeSession.isAlive();

        updateConnectionAction(isAlive && activeSession.isConnectedOrConnecting());
        connectionAction.setEnabled(isAlive);
        scrollLockAction.setEnabled(isAlive);
        scrollLockAction.setChecked(isAlive && activeSession.isScrollLock());
        clearAction.setEnabled(isAlive);
        commandInputFieldAction.setEnabled(isAlive);
        commandInputFieldAction.setChecked(isAlive && activeSession.isCommandInputFieldVisible());
        newTerminalAction.setEnabled(activeSession != null);
        copyAction.setEnabled(isAlive);
        pasteAction.setEnabled(isAlive);

        boolean hasSessions = !view.getSessions().isEmpty();
        connectAllAction.setEnabled(hasSessions);
        disconnectAllAction.setEnabled(hasSessions);
        closeAllAction.setEnabled(hasSessions);

        updateSelectionActions();
    }

    private void updateSelectionActions() {

        List<TerminalSession> selectedSessions = view.getSelectedSessions();

        boolean canConnect = false;
        boolean canDisconnect = false;
        for (TerminalSession session : selectedSessions) {
            if (session.isAlive()) {
                if (session.isConnectedOrConnecting()) {
                    canDisconnect = true;
                } else {
                    canConnect = true;
                }
            }
        }

        connectSelectedAction.setEnabled(canConnect);
        disconnectSelectedAction.setEnabled(canDisconnect);
        closeSelectedAction.setEnabled(!selectedSessions.isEmpty());
        renameSelectedAction.setEnabled(selectedSessions.size() == 1);
    }

    /**
     * Switches the connection button between 'Connect' and 'Disconnect'.
     *
     * @param anIsConnected - <code>true</code>, if the terminal is connected or
     *        connecting
     */
    private void updateConnectionAction(boolean anIsConnected) {

        if (anIsConnected) {
            connectionAction.setText(Messages.Label_Disconnect);
            setupAction(connectionAction, Messages.Tooltip_Disconnect, IfsProjectsPlugin.IMAGE_DISCONNECT);
        } else {
            connectionAction.setText(Messages.Label_Connect);
            setupAction(connectionAction, Messages.Tooltip_Connect, IfsProjectsPlugin.IMAGE_CONNECT);
        }
    }

    private void setupAction(Action anAction, String aToolTip, String anImage) {
        anAction.setToolTipText(aToolTip);
        anAction.setImageDescriptor(IfsProjectsPlugin.getEnabledImageDescriptor(anImage));
        anAction.setDisabledImageDescriptor(IfsProjectsPlugin.getDisabledImageDescriptor(anImage));
    }
}
