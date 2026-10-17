/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.terminal.tm;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.ViewPart;

import de.tools400.ifsprojects.IfsProjectsPlugin;
import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.dialogs.ToolTipInputDialog;
import de.tools400.ifsprojects.preferences.Preferences;
import de.tools400.ifsprojects.terminal.ITerminalCommandLine;
import de.tools400.ifsprojects.terminal.TerminalException;
import de.tools400.ifsprojects.terminal.TerminalLaunchers;
import de.tools400.ifsprojects.terminal.TerminalRequest;

/**
 * View that hosts all SSH terminal sessions. The active session is shown on the
 * left side, the list of all sessions on the right side. The toolbar actions
 * apply to the active session. The view menu contains actions for all sessions,
 * the context menu of the list actions for the selected sessions.
 */
public class TmTerminalView extends ViewPart implements TerminalSession.Listener {

    public static final String ID = "de.tools400.ifsprojects.views.terminal"; //$NON-NLS-1$

    private static final int[] SASH_WEIGHTS = { 80, 20 };

    private static final String MEMENTO_SESSION = "session"; //$NON-NLS-1$
    private static final String MEMENTO_ACTIVE_SESSION = "activeSession"; //$NON-NLS-1$
    private static final String MEMENTO_NAME = "name"; //$NON-NLS-1$
    private static final String MEMENTO_CONNECTION_NAME = "connectionName"; //$NON-NLS-1$
    private static final String MEMENTO_HOST_NAME = "hostName"; //$NON-NLS-1$
    private static final String MEMENTO_PORT = "port"; //$NON-NLS-1$
    private static final String MEMENTO_USER_ID = "userId"; //$NON-NLS-1$
    private static final String MEMENTO_ENCODING = "encoding"; //$NON-NLS-1$
    private static final String MEMENTO_TIMEOUT = "timeout"; //$NON-NLS-1$
    private static final String MEMENTO_KEEP_ALIVE = "keepAlive"; //$NON-NLS-1$
    private static final String MEMENTO_DIRECTORY = "directory"; //$NON-NLS-1$
    private static final String MEMENTO_REUSE_KEY = "reuseKey"; //$NON-NLS-1$

    private final List<TerminalSession> sessions = new ArrayList<TerminalSession>();
    private TerminalSession activeSession;

    private IMemento restoredState;
    private String defaultPartName;

    private SashForm sashForm;
    private Composite terminalArea;
    private StackLayout terminalAreaLayout;
    private Label noTerminalLabel;
    private TableViewer sessionList;
    private boolean isUpdatingSelection;

    private Image connectedImage;
    private Image disconnectedImage;

    private Action connectionAction;
    private Action scrollLockAction;
    private Action clearAction;
    private Action commandInputFieldAction;
    private Action newTerminalAction;

    private Action copyAction;
    private Action pasteAction;

    private Action connectAllAction;
    private Action disconnectAllAction;
    private Action closeAllAction;

    private Action connectSelectedAction;
    private Action disconnectSelectedAction;
    private Action closeSelectedAction;
    private Action renameSelectedAction;

    @Override
    public void init(IViewSite aSite, IMemento aMemento) throws PartInitException {
        super.init(aSite, aMemento);
        defaultPartName = getPartName();
        if (Preferences.getInstance().isRestoreTerminals()) {
            restoredState = aMemento;
        }
    }

    @Override
    public void createPartControl(Composite aParent) {

        ImageDescriptor terminalImage = IfsProjectsPlugin.getImageDescriptor(IfsProjectsPlugin.IMAGE_TERMINAL);
        connectedImage = terminalImage.createImage();
        disconnectedImage = ImageDescriptor.createWithFlags(terminalImage, SWT.IMAGE_DISABLE).createImage();

        sashForm = new SashForm(aParent, SWT.HORIZONTAL);

        terminalArea = new Composite(sashForm, SWT.NONE);
        terminalAreaLayout = new StackLayout();
        terminalArea.setLayout(terminalAreaLayout);

        noTerminalLabel = new Label(terminalArea, SWT.WRAP);
        noTerminalLabel.setText(Messages.Label_No_terminal_open);

        createSessionList(sashForm);
        sashForm.setWeights(SASH_WEIGHTS);

        createActions();
        contributeToActionBars();
        hookSessionListContextMenu();

        restoreSessions();

        if (activeSession == null) {
            showNoTerminal();
        }

        updateActions();
    }

    /**
     * Opens a new session and connects it. The command line of the request is
     * sent to the shell, once it is ready.
     *
     * @param aRequest - terminal request
     * @return command line passed to the session
     */
    public ITerminalCommandLine openSession(TerminalRequest aRequest) {

        TerminalSession session = addSession(aRequest);
        setActiveSession(session);

        ITerminalCommandLine commandLine = session.connect(aRequest.getCommandLine());

        updateActions();
        setFocus();

        return commandLine;
    }

    /**
     * Returns the session with the specified session key. Connected sessions
     * are preferred.
     *
     * @param aSessionKey - session key, see
     *        {@link TerminalRequest#getSessionKey()}
     * @return session or <code>null</code>
     */
    TerminalSession findSession(String aSessionKey) {

        TerminalSession disconnectedSession = null;

        for (TerminalSession session : sessions) {
            if (session.isSession(aSessionKey) && session.isAlive()) {
                if (session.isConnected()) {
                    return session;
                }
                if (disconnectedSession == null) {
                    disconnectedSession = session;
                }
            }
        }

        return disconnectedSession;
    }

    /**
     * Returns a session with the specified reuse key (build), that is
     * connected and whose command is not running.
     *
     * @param aReuseKey - reuse key
     * @return session or <code>null</code>
     */
    TerminalSession findReusableSession(String aReuseKey) {

        for (TerminalSession session : sessions) {
            if (session.isReusableFor(aReuseKey)) {
                return session;
            }
        }

        return null;
    }

    /**
     * Returns a session with the specified reuse key (build), that is
     * disconnected and whose command is not running, e.g. a session restored
     * after a restart.
     *
     * @param aReuseKey - reuse key
     * @return session or <code>null</code>
     */
    TerminalSession findReconnectableSession(String aReuseKey) {

        for (TerminalSession session : sessions) {
            if (session.isReconnectableFor(aReuseKey)) {
                return session;
            }
        }

        return null;
    }

    /**
     * Brings a disconnected session to the front and connects it again. The
     * command line of the request is sent, once the shell is ready.
     *
     * @param aSession - disconnected session
     * @param aRequest - request with command (build)
     * @return command line passed to the session
     */
    ITerminalCommandLine reconnectSession(TerminalSession aSession, TerminalRequest aRequest) {

        setActiveSession(aSession);
        ITerminalCommandLine commandLine = aSession.connect(aRequest.getCommandLine());

        updateActions();
        setFocus();

        return commandLine;
    }

    /**
     * Brings a session to the front and connects it again, if it is
     * disconnected.
     *
     * @param aSession - session
     */
    void activateSession(TerminalSession aSession) {

        setActiveSession(aSession);

        if (aSession.isAlive() && !aSession.isConnectedOrConnecting()) {
            aSession.reconnect();
        }

        updateActions();
        setFocus();
    }

    /**
     * Brings a connected session to the front and sends the command line of the
     * request.
     *
     * @param aSession - connected session
     * @param aRequest - request with command (build)
     * @return command line sent to the session
     */
    ITerminalCommandLine sendCommandLine(TerminalSession aSession, TerminalRequest aRequest) {

        setActiveSession(aSession);
        ITerminalCommandLine commandLine = aSession.sendCommandLine(aRequest.getCommandLine());
        setFocus();

        return commandLine;
    }

    /**
     * Closes all sessions. The view stays open.
     */
    void closeAllSessions() {
        closeSessions(new ArrayList<TerminalSession>(sessions));
    }

    public void sessionStateChanged(TerminalSession aSession) {

        if (sessionList == null || sessionList.getControl().isDisposed()) {
            return;
        }

        sessionList.update(aSession, null);

        if (aSession == activeSession) {
            updateTitle();
            updateActions();
        }
    }

    @Override
    public void setFocus() {

        if (activeSession == null) {
            if (sessionList != null && !sessionList.getControl().isDisposed()) {
                sessionList.getControl().setFocus();
            }
            return;
        }

        activeSession.setFocus();

        // A click on the tab of the view gives the focus to the tab folder,
        // possibly after the view has been activated. Therefore the focus is
        // set again, once the click has been processed.
        sashForm.getDisplay().asyncExec(new Runnable() {
            public void run() {
                if (activeSession != null && activeSession.isAlive() && isActivePart() && !isFocusInView()) {
                    activeSession.setFocus();
                }
            }
        });
    }

    @Override
    public void saveState(IMemento aMemento) {
        super.saveState(aMemento);

        if (!Preferences.getInstance().isRestoreTerminals()) {
            return;
        }

        // Password and command are never saved.
        for (TerminalSession session : sessions) {
            TerminalRequest request = session.getRequest();
            IMemento child = aMemento.createChild(MEMENTO_SESSION);
            child.putString(MEMENTO_CONNECTION_NAME, request.getConnectionName());
            child.putString(MEMENTO_HOST_NAME, request.getHostName());
            child.putInteger(MEMENTO_PORT, request.getPort());
            child.putString(MEMENTO_USER_ID, request.getUserId());
            child.putString(MEMENTO_ENCODING, request.getEncoding());
            child.putInteger(MEMENTO_TIMEOUT, request.getTimeout());
            child.putInteger(MEMENTO_KEEP_ALIVE, request.getKeepAlive());
            if (request.getDirectory() != null) {
                child.putString(MEMENTO_DIRECTORY, request.getDirectory());
            }
            // A restored build terminal is reused, see findReconnectableSession().
            if (request.getReuseKey() != null) {
                child.putString(MEMENTO_REUSE_KEY, request.getReuseKey());
            }
            if (session.getCustomName() != null) {
                child.putString(MEMENTO_NAME, session.getCustomName());
            }
        }

        if (activeSession != null) {
            aMemento.putInteger(MEMENTO_ACTIVE_SESSION, sessions.indexOf(activeSession));
        }
    }

    @Override
    public void dispose() {

        for (TerminalSession session : sessions) {
            session.dispose();
        }
        sessions.clear();
        activeSession = null;

        if (connectedImage != null) {
            connectedImage.dispose();
        }
        if (disconnectedImage != null) {
            disconnectedImage.dispose();
        }

        super.dispose();
    }

    private TerminalSession addSession(TerminalRequest aRequest) {

        int instanceNumber = 1;
        for (TerminalSession session : sessions) {
            if (session.getRequest().getTitle().equals(aRequest.getTitle())) {
                instanceNumber++;
            }
        }

        TerminalSession session = new TerminalSession(aRequest, instanceNumber, this);
        session.create(terminalArea);
        sessions.add(session);

        sessionList.refresh();

        return session;
    }

    private void restoreSessions() {

        if (restoredState == null) {
            return;
        }

        for (IMemento child : restoredState.getChildren(MEMENTO_SESSION)) {
            TerminalRequest request = restoreRequest(child);
            if (request != null) {
                TerminalSession session = addSession(request);
                session.setCustomName(child.getString(MEMENTO_NAME));
            }
        }

        Integer activeIndex = restoredState.getInteger(MEMENTO_ACTIVE_SESSION);
        if (activeIndex != null && activeIndex.intValue() >= 0 && activeIndex.intValue() < sessions.size()) {
            setActiveSession(sessions.get(activeIndex.intValue()));
        } else if (!sessions.isEmpty()) {
            setActiveSession(sessions.get(0));
        }

        sessionList.refresh();
        restoredState = null;
    }

    private TerminalRequest restoreRequest(IMemento aMemento) {

        try {

            TerminalRequest.Builder builder = TerminalRequest.builder();
            builder.connectionName(aMemento.getString(MEMENTO_CONNECTION_NAME));
            builder.hostName(aMemento.getString(MEMENTO_HOST_NAME));
            builder.userId(aMemento.getString(MEMENTO_USER_ID));
            builder.directory(aMemento.getString(MEMENTO_DIRECTORY));
            builder.reuseKey(aMemento.getString(MEMENTO_REUSE_KEY));

            String encoding = aMemento.getString(MEMENTO_ENCODING);
            if (encoding != null) {
                builder.encoding(encoding);
            }

            Integer port = aMemento.getInteger(MEMENTO_PORT);
            if (port != null) {
                builder.port(port.intValue());
            }

            Integer timeout = aMemento.getInteger(MEMENTO_TIMEOUT);
            if (timeout != null) {
                builder.timeout(timeout.intValue());
            }

            Integer keepAlive = aMemento.getInteger(MEMENTO_KEEP_ALIVE);
            if (keepAlive != null) {
                builder.keepAlive(keepAlive.intValue());
            }

            return builder.build();

        } catch (IllegalStateException e) {
            IfsProjectsPlugin.logWarning("Could not restore terminal session: " + e.getLocalizedMessage()); //$NON-NLS-1$
            return null;
        }
    }

    private void setActiveSession(TerminalSession aSession) {

        activeSession = aSession;

        terminalAreaLayout.topControl = aSession.getPage();
        terminalArea.layout(true, true);

        isUpdatingSelection = true;
        try {
            sessionList.setSelection(new StructuredSelection(aSession), true);
        } finally {
            isUpdatingSelection = false;
        }

        updateTitle();
        updateActions();
    }

    private void showNoTerminal() {

        activeSession = null;
        terminalAreaLayout.topControl = noTerminalLabel;
        terminalArea.layout(true, true);

        updateTitle();
        updateActions();
    }

    private void closeSessions(List<TerminalSession> aSessions) {

        if (aSessions.isEmpty()) {
            return;
        }

        int activeIndex = sessions.indexOf(activeSession);
        boolean isActiveClosed = aSessions.contains(activeSession);

        for (TerminalSession session : aSessions) {
            session.dispose();
            sessions.remove(session);
        }

        sessionList.refresh();

        if (sessions.isEmpty()) {
            showNoTerminal();
        } else if (isActiveClosed) {
            int index = Math.min(Math.max(activeIndex, 0), sessions.size() - 1);
            setActiveSession(sessions.get(index));
            setFocus();
        }

        updateActions();
    }

    private List<TerminalSession> getSelectedSessions() {

        List<TerminalSession> selectedSessions = new ArrayList<TerminalSession>();

        IStructuredSelection selection = (IStructuredSelection)sessionList.getSelection();
        for (Object element : selection.toList()) {
            if (element instanceof TerminalSession) {
                selectedSessions.add((TerminalSession)element);
            }
        }

        return selectedSessions;
    }

    private void connect(List<TerminalSession> aSessions) {
        for (TerminalSession session : aSessions) {
            if (session.isAlive() && !session.isConnectedOrConnecting()) {
                session.reconnect();
            }
        }
        updateActions();
    }

    private void disconnect(List<TerminalSession> aSessions) {
        for (TerminalSession session : aSessions) {
            session.disconnect();
        }
        updateActions();
    }

    private void rename(TerminalSession aSession) {

        ToolTipInputDialog dialog = new ToolTipInputDialog(getSite().getShell(), Messages.Title_Rename_terminal, Messages.Label_New_name_colon,
            aSession.getName(), null, Messages.Tooltip_Terminal_name);
        if (dialog.open() != Window.OK) {
            return;
        }

        aSession.setCustomName(dialog.getValue());
        sessionList.update(aSession, null);

        if (aSession == activeSession) {
            updateTitle();
        }
    }

    /**
     * Opens another session with the same connection and directory as the
     * active session. The command of the request (build) is not repeated.
     */
    private void openNewTerminal() {

        if (activeSession == null) {
            return;
        }

        TerminalRequest newRequest = activeSession.getRequest().toBuilder().command(null).reuseKey(null).forceNew(true).build();

        try {
            TerminalLaunchers.getLauncher().launch(newRequest);
        } catch (TerminalException e) {
            IfsProjectsPlugin.logError("Could not open terminal: " + newRequest.getToolTip(), e); //$NON-NLS-1$
            MessageDialog.openError(getSite().getShell(), Messages.Title_IFS_Projects, e.getLocalizedMessage());
        }
    }

    private void updateTitle() {

        if (activeSession == null) {
            setPartName(defaultPartName);
            setTitleToolTip(null);
            return;
        }

        if (activeSession.isConnectedOrConnecting()) {
            setPartName(activeSession.getName());
        } else {
            setPartName(Messages.bind(Messages.Label_A_disconnected, activeSession.getName()));
        }

        setTitleToolTip(activeSession.getRequest().getToolTip());
    }

    private boolean isActivePart() {
        IWorkbenchPage page = getSite().getPage();
        return page != null && page.getActivePart() == this;
    }

    /**
     * Returns <code>true</code>, if the focus control is part of this view,
     * e.g. the terminal, its command input field or the session list.
     */
    private boolean isFocusInView() {

        Control focusControl = sashForm.getDisplay().getFocusControl();
        while (focusControl != null) {
            if (focusControl == sashForm) {
                return true;
            }
            focusControl = focusControl.getParent();
        }

        return false;
    }

    private void createSessionList(Composite aParent) {

        sessionList = new TableViewer(aParent, SWT.MULTI | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
        sessionList.setContentProvider(new ArrayContentProvider());
        sessionList.setLabelProvider(new LabelProvider() {
            @Override
            public String getText(Object anElement) {
                return ((TerminalSession)anElement).getName();
            }

            @Override
            public Image getImage(Object anElement) {
                TerminalSession session = (TerminalSession)anElement;
                if (session.getErrorMessage() != null) {
                    return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_ERROR_TSK);
                }
                if (session.isConnectedOrConnecting()) {
                    return connectedImage;
                }
                return disconnectedImage;
            }
        });
        sessionList.setInput(sessions);
        sessionList.getControl().setToolTipText(Messages.Tooltip_Terminal_list);

        sessionList.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent anEvent) {
                if (isUpdatingSelection) {
                    return;
                }
                List<TerminalSession> selectedSessions = getSelectedSessions();
                if (selectedSessions.size() == 1 && selectedSessions.get(0) != activeSession) {
                    setActiveSession(selectedSessions.get(0));
                }
                updateActions();
            }
        });

        // A double-click connects a disconnected session again.
        sessionList.addDoubleClickListener(new IDoubleClickListener() {
            public void doubleClick(DoubleClickEvent anEvent) {
                List<TerminalSession> selectedSessions = getSelectedSessions();
                if (selectedSessions.size() == 1) {
                    activateSession(selectedSessions.get(0));
                }
            }
        });
    }

    /**
     * Creates the actions. The toolbar actions and their icons are the same as
     * in the 'Terminal' view of RDi 9.8. Connect and disconnect share one
     * button.
     */
    private void createActions() {

        connectionAction = new Action() {
            @Override
            public void run() {
                if (activeSession == null) {
                    return;
                }
                if (activeSession.isConnectedOrConnecting()) {
                    activeSession.disconnect();
                } else {
                    activeSession.reconnect();
                    activeSession.setFocus();
                }
                updateActions();
            }
        };
        updateConnectionAction(false);

        scrollLockAction = new Action(Messages.Label_Scroll_lock, IAction.AS_CHECK_BOX) {
            @Override
            public void run() {
                if (activeSession != null) {
                    activeSession.setScrollLock(isChecked());
                }
            }
        };
        setupAction(scrollLockAction, Messages.Tooltip_Scroll_lock, IfsProjectsPlugin.IMAGE_SCROLL_LOCK);

        clearAction = new Action(Messages.Label_Clear) {
            @Override
            public void run() {
                if (activeSession != null) {
                    activeSession.clear();
                }
            }
        };
        setupAction(clearAction, Messages.Tooltip_Clear, IfsProjectsPlugin.IMAGE_CLEAR);

        commandInputFieldAction = new Action(Messages.Label_Command_input_field, IAction.AS_CHECK_BOX) {
            @Override
            public void run() {
                if (activeSession != null) {
                    activeSession.setCommandInputFieldVisible(isChecked());
                }
            }
        };
        setupAction(commandInputFieldAction, Messages.Tooltip_Command_input_field, IfsProjectsPlugin.IMAGE_COMMAND_INPUT_FIELD);

        newTerminalAction = new Action(Messages.Label_New_terminal) {
            @Override
            public void run() {
                openNewTerminal();
            }
        };
        setupAction(newTerminalAction, Messages.Tooltip_New_terminal, IfsProjectsPlugin.IMAGE_NEW_TERMINAL);

        // Handlers of 'Edit > Copy' and 'Edit > Paste'. Ctrl+C and Ctrl+V reach
        // them only, if the terminal does not capture the keys, e.g. while it
        // is disconnected. Otherwise the session handles the keys, see
        // TerminalSession.handleKey().
        copyAction = new Action() {
            @Override
            public void run() {
                Control focusControl = sashForm.getDisplay().getFocusControl();
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
                Control focusControl = sashForm.getDisplay().getFocusControl();
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
                connect(new ArrayList<TerminalSession>(sessions));
            }
        };
        connectAllAction.setToolTipText(Messages.Tooltip_Connect_all);

        disconnectAllAction = new Action(Messages.Label_Disconnect_all) {
            @Override
            public void run() {
                disconnect(new ArrayList<TerminalSession>(sessions));
            }
        };
        disconnectAllAction.setToolTipText(Messages.Tooltip_Disconnect_all);

        closeAllAction = new Action(Messages.Label_Close_all) {
            @Override
            public void run() {
                closeSessions(new ArrayList<TerminalSession>(sessions));
            }
        };
        closeAllAction.setToolTipText(Messages.Tooltip_Close_all);

        connectSelectedAction = new Action(Messages.Label_Connect) {
            @Override
            public void run() {
                connect(getSelectedSessions());
            }
        };
        connectSelectedAction.setImageDescriptor(IfsProjectsPlugin.getEnabledImageDescriptor(IfsProjectsPlugin.IMAGE_CONNECT));
        connectSelectedAction.setToolTipText(Messages.Tooltip_Connect_selected);

        disconnectSelectedAction = new Action(Messages.Label_Disconnect) {
            @Override
            public void run() {
                disconnect(getSelectedSessions());
            }
        };
        disconnectSelectedAction.setImageDescriptor(IfsProjectsPlugin.getEnabledImageDescriptor(IfsProjectsPlugin.IMAGE_DISCONNECT));
        disconnectSelectedAction.setToolTipText(Messages.Tooltip_Disconnect_selected);

        closeSelectedAction = new Action(Messages.Label_Close) {
            @Override
            public void run() {
                closeSessions(getSelectedSessions());
            }
        };
        closeSelectedAction.setToolTipText(Messages.Tooltip_Close_selected);

        renameSelectedAction = new Action(Messages.Label_Rename) {
            @Override
            public void run() {
                List<TerminalSession> selectedSessions = getSelectedSessions();
                if (selectedSessions.size() == 1) {
                    rename(selectedSessions.get(0));
                }
            }
        };
        renameSelectedAction.setToolTipText(Messages.Tooltip_Rename_selected);
    }

    private void setupAction(Action anAction, String aToolTip, String anImage) {
        anAction.setToolTipText(aToolTip);
        anAction.setImageDescriptor(IfsProjectsPlugin.getEnabledImageDescriptor(anImage));
        anAction.setDisabledImageDescriptor(IfsProjectsPlugin.getDisabledImageDescriptor(anImage));
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

    private void contributeToActionBars() {

        IActionBars actionBars = getViewSite().getActionBars();
        actionBars.setGlobalActionHandler(ActionFactory.COPY.getId(), copyAction);
        actionBars.setGlobalActionHandler(ActionFactory.PASTE.getId(), pasteAction);

        IToolBarManager toolBarManager = actionBars.getToolBarManager();
        toolBarManager.add(connectionAction);
        toolBarManager.add(new Separator());
        toolBarManager.add(scrollLockAction);
        toolBarManager.add(clearAction);
        toolBarManager.add(commandInputFieldAction);
        toolBarManager.add(new Separator());
        toolBarManager.add(newTerminalAction);

        IMenuManager menuManager = actionBars.getMenuManager();
        menuManager.add(connectAllAction);
        menuManager.add(disconnectAllAction);
        menuManager.add(new Separator());
        menuManager.add(closeAllAction);
    }

    private void hookSessionListContextMenu() {

        MenuManager menuManager = new MenuManager("#PopupMenu"); //$NON-NLS-1$
        menuManager.setRemoveAllWhenShown(true);
        menuManager.addMenuListener(new IMenuListener() {
            public void menuAboutToShow(IMenuManager aManager) {
                aManager.add(connectSelectedAction);
                aManager.add(disconnectSelectedAction);
                aManager.add(new Separator());
                aManager.add(renameSelectedAction);
                aManager.add(new Separator());
                aManager.add(closeSelectedAction);
                updateSelectionActions();
            }
        });

        Control control = sessionList.getControl();
        control.setMenu(menuManager.createContextMenu(control));
    }

    private void updateActions() {

        if (connectionAction == null) {
            return;
        }

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

        boolean hasSessions = !sessions.isEmpty();
        connectAllAction.setEnabled(hasSessions);
        disconnectAllAction.setEnabled(hasSessions);
        closeAllAction.setEnabled(hasSessions);

        updateSelectionActions();
    }

    private void updateSelectionActions() {

        List<TerminalSession> selectedSessions = getSelectedSessions();

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
}
