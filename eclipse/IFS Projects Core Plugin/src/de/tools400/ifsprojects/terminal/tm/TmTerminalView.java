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

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
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
import org.eclipse.ui.IMemento;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import de.tools400.ifsprojects.IfsProjectsPlugin;
import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.dialogs.ToolTipInputDialog;
import de.tools400.ifsprojects.preferences.Preferences;
import de.tools400.ifsprojects.terminal.ITerminalCommandLine;
import de.tools400.ifsprojects.terminal.TerminalRequest;
import de.tools400.ifsprojects.utils.PlatformUtils;

/**
 * View that hosts all SSH terminal sessions. The active session is shown on the
 * left side, the list of all sessions on the right side. The actions are
 * created by the {@link TerminalActionGroup}, the sessions are saved and
 * restored by the {@link TerminalRequestMemento}.
 */
public class TmTerminalView extends ViewPart implements TerminalSession.Listener {

    public static final String ID = "de.tools400.ifsprojects.views.terminal"; //$NON-NLS-1$

    private static final int[] SASH_WEIGHTS = { 80, 20 };

    private static final String MEMENTO_SESSION = "session"; //$NON-NLS-1$
    private static final String MEMENTO_ACTIVE_SESSION = "activeSession"; //$NON-NLS-1$
    private static final String MEMENTO_NAME = "name"; //$NON-NLS-1$

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

    private TerminalActionGroup actionGroup;

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

        actionGroup = new TerminalActionGroup(this);
        actionGroup.fillActionBars(getViewSite().getActionBars());
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
            if (session.hasSessionKey(aSessionKey) && session.isAlive()) {
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
        aSession.reconnectIfDisconnected();

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

        for (TerminalSession session : sessions) {
            IMemento child = aMemento.createChild(MEMENTO_SESSION);
            TerminalRequestMemento.save(child, session.getRequest());
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

        if (actionGroup != null) {
            actionGroup.dispose();
        }

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
            TerminalRequest request = TerminalRequestMemento.restore(child);
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

    /**
     * Returns the active session, which is shown in the view.
     *
     * @return session or <code>null</code>, if no session is open
     */
    TerminalSession getActiveSession() {
        return activeSession;
    }

    /**
     * Returns all sessions.
     *
     * @return copy of the sessions in the order of the list
     */
    List<TerminalSession> getSessions() {
        return new ArrayList<TerminalSession>(sessions);
    }

    void closeSessions(List<TerminalSession> aSessions) {

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

    List<TerminalSession> getSelectedSessions() {

        List<TerminalSession> selectedSessions = new ArrayList<TerminalSession>();

        IStructuredSelection selection = (IStructuredSelection)sessionList.getSelection();
        for (Object element : selection.toList()) {
            if (element instanceof TerminalSession) {
                selectedSessions.add((TerminalSession)element);
            }
        }

        return selectedSessions;
    }

    void connect(List<TerminalSession> aSessions) {
        for (TerminalSession session : aSessions) {
            session.reconnectIfDisconnected();
        }
        updateActions();
    }

    void disconnect(List<TerminalSession> aSessions) {
        for (TerminalSession session : aSessions) {
            session.disconnect();
        }
        updateActions();
    }

    void rename(TerminalSession aSession) {

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
    void openNewTerminal() {

        if (activeSession == null) {
            return;
        }

        openSession(activeSession.getRequest().toBuilder().command(null).reuseKey(null).forceNew(true).build());
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
        return PlatformUtils.contains(sashForm, sashForm.getDisplay().getFocusControl());
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

    private void hookSessionListContextMenu() {

        MenuManager menuManager = new MenuManager("#PopupMenu"); //$NON-NLS-1$
        menuManager.setRemoveAllWhenShown(true);
        menuManager.addMenuListener(new IMenuListener() {
            public void menuAboutToShow(IMenuManager aManager) {
                actionGroup.fillContextMenu(aManager);
            }
        });

        Control control = sessionList.getControl();
        control.setMenu(menuManager.createContextMenu(control));
    }

    private void updateActions() {
        if (actionGroup != null) {
            actionGroup.updateActionBars();
        }
    }
}
