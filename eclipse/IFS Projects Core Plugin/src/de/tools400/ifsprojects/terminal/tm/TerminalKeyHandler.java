/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.terminal.tm;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Scrollable;
import org.eclipse.tm.internal.terminal.control.ITerminalViewControl;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.contexts.IContextActivation;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.keys.IBindingService;

/**
 * Keys of a {@link TerminalSession}.
 * <p>
 * Keys handled by the session before the TM terminal, see
 * {@link #handleKey(Event)}: Ctrl+C copies the selected text, if there is any,
 * and Ctrl+V pastes, like in Windows Terminal (not on the Mac, where Cmd+C and
 * Cmd+V do so). Without a selection, the TM terminal sends Ctrl+C to the
 * shell. Shift+Page Up/Down and Ctrl+Shift+Home/End scroll the terminal, which
 * the TM terminal does not do.
 * <p>
 * The terminal receives all keys (instead of the key bindings of the
 * workbench), while it has the focus and is connected, see
 * {@link #updateKeyCapture()}. The TM terminal of RDi 9.6 and 9.8 does so
 * itself, but the TM terminal of RDi 9.9 captures the keys only while the mouse
 * pointer is over the terminal, even if the focus is elsewhere, e.g. in an
 * editor.
 */
@SuppressWarnings("restriction")
final class TerminalKeyHandler {

    /*
     * Contexts of the TM terminal: bindings, that disable the menu keys (Alt+F,
     * ...), and the bindings of copy and paste (Ctrl+Shift+C/V, ...).
     */
    private static final String CONTEXT_TERMINAL = "org.eclipse.tm.terminal.TerminalContext"; //$NON-NLS-1$
    private static final String CONTEXT_EDIT = "org.eclipse.tm.terminal.EditContext"; //$NON-NLS-1$

    private final TerminalSession session;
    private final ITerminalViewControl control;
    private final Display display;

    private Listener keyFilter;

    /* Key capture, see updateKeyCapture(). */
    private Listener mouseTrackFilter;
    private boolean isKeyFilterEnabledBeforeMouseTrack;
    private boolean hasFocus;
    private IContextActivation[] contextActivations;

    /**
     * Creates the key handler of a session.
     *
     * @param aSession - session
     * @param aControl - terminal control of the session
     */
    TerminalKeyHandler(TerminalSession aSession, ITerminalViewControl aControl) {
        this.session = aSession;
        this.control = aControl;
        this.display = aControl.getControl().getDisplay();
    }

    /**
     * Registers the key filter and the listeners of the key capture.
     */
    void install() {
        hookKeyFilter();
        hookKeyCapture();
    }

    /**
     * Removes the display filters and restores the key bindings of the
     * workbench. Called, before the terminal control is disposed, because a
     * disposed control does not receive <code>focusLost()</code>.
     */
    void dispose() {

        if (keyFilter != null) {
            display.removeFilter(SWT.KeyDown, keyFilter);
            keyFilter = null;
        }

        if (mouseTrackFilter != null) {
            display.removeFilter(SWT.MouseEnter, mouseTrackFilter);
            display.removeFilter(SWT.MouseExit, mouseTrackFilter);
            mouseTrackFilter = null;
        }

        if (hasFocus) {
            hasFocus = false;
            updateKeyCapture();
        }
    }

    /**
     * Updates the key capture, after the state of the terminal has changed.
     */
    void stateChanged() {
        if (hasFocus) {
            updateKeyCapture();
        }
    }

    /**
     * Registers a display filter, that receives the keys of the terminal
     * before the TM terminal and before the key bindings of the workbench, if
     * the TM terminal has disabled them.
     */
    private void hookKeyFilter() {

        keyFilter = new Listener() {
            public void handleEvent(Event anEvent) {
                if (session.isAlive() && anEvent.widget == control.getControl() && handleKey(anEvent)) {
                    anEvent.doit = false;
                    anEvent.type = SWT.None;
                }
            }
        };

        display.addFilter(SWT.KeyDown, keyFilter);
    }

    /**
     * Makes the key capture follow the focus of the terminal. The listeners
     * are called after the focus and mouse track listeners of the TM terminal,
     * because they are added later. They correct, what the TM terminal of RDi
     * 9.9 does on mouse track events: with the focus, the keys stay captured,
     * when the mouse pointer leaves the terminal; without the focus, the key
     * bindings of the workbench are restored, when the mouse pointer enters the
     * terminal.
     */
    private void hookKeyCapture() {

        final Control textControl = control.getControl();

        textControl.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent anEvent) {
                hasFocus = true;
                updateKeyCapture();
            }

            public void focusLost(FocusEvent anEvent) {
                hasFocus = false;
                updateKeyCapture();
            }
        });

        // Remembers the state of the key bindings, before the TM terminal
        // receives the mouse track event.
        mouseTrackFilter = new Listener() {
            public void handleEvent(Event anEvent) {
                if (anEvent.widget == textControl) {
                    isKeyFilterEnabledBeforeMouseTrack = getBindingService().isKeyFilterEnabled();
                }
            }
        };
        display.addFilter(SWT.MouseEnter, mouseTrackFilter);
        display.addFilter(SWT.MouseExit, mouseTrackFilter);

        textControl.addMouseTrackListener(new MouseTrackAdapter() {
            @Override
            public void mouseEnter(MouseEvent anEvent) {
                mouseTracked();
            }

            @Override
            public void mouseExit(MouseEvent anEvent) {
                mouseTracked();
            }
        });
    }

    private void mouseTracked() {

        if (hasFocus) {
            updateKeyCapture();
        } else {
            setKeyFilterEnabled(isKeyFilterEnabledBeforeMouseTrack);
        }
    }

    /**
     * Captures the keys for the terminal, while it has the focus and is
     * connected: the key bindings of the workbench are disabled and the
     * contexts of the TM terminal are active. Otherwise the key bindings of the
     * workbench are enabled.
     */
    private void updateKeyCapture() {

        boolean isCapture = hasFocus && session.isConnected();

        setKeyFilterEnabled(!isCapture);

        IContextService contextService = (IContextService)PlatformUI.getWorkbench().getService(IContextService.class);
        if (isCapture && contextActivations == null) {
            contextActivations = new IContextActivation[] { contextService.activateContext(CONTEXT_TERMINAL),
                contextService.activateContext(CONTEXT_EDIT) };
        } else if (!isCapture && contextActivations != null) {
            for (IContextActivation activation : contextActivations) {
                contextService.deactivateContext(activation);
            }
            contextActivations = null;
        }
    }

    private void setKeyFilterEnabled(boolean anIsEnabled) {

        IBindingService bindingService = getBindingService();
        if (bindingService.isKeyFilterEnabled() != anIsEnabled) {
            bindingService.setKeyFilterEnabled(anIsEnabled);
        }
    }

    private IBindingService getBindingService() {
        return (IBindingService)PlatformUI.getWorkbench().getService(IBindingService.class);
    }

    /**
     * Handles a key pressed in the terminal.
     *
     * @param anEvent - key down event
     * @return <code>true</code>, if the key has been handled and must not reach
     *         the TM terminal
     */
    private boolean handleKey(Event anEvent) {

        int modifiers = anEvent.stateMask & SWT.MODIFIER_MASK;

        if (modifiers == SWT.CTRL && SWT.MOD1 == SWT.CTRL) {
            if (anEvent.keyCode == 'c') {
                return session.copySelection();
            }
            if (anEvent.keyCode == 'v') {
                session.paste();
                return true;
            }
        } else if (modifiers == SWT.SHIFT) {
            if (anEvent.keyCode == SWT.PAGE_UP) {
                scrollBy(-1);
                return true;
            }
            if (anEvent.keyCode == SWT.PAGE_DOWN) {
                scrollBy(1);
                return true;
            }
        } else if (modifiers == (SWT.CTRL | SWT.SHIFT)) {
            if (anEvent.keyCode == SWT.HOME) {
                scrollTo(Integer.MIN_VALUE);
                return true;
            }
            if (anEvent.keyCode == SWT.END) {
                scrollTo(Integer.MAX_VALUE);
                return true;
            }
        }

        return false;
    }

    /**
     * Scrolls the terminal by the specified number of pages, like a click into
     * the scroll bar.
     */
    private void scrollBy(int aPages) {

        ScrollBar scrollBar = ((Scrollable)control.getControl()).getVerticalBar();
        if (scrollBar != null) {
            scrollTo(scrollBar.getSelection() + aPages * scrollBar.getPageIncrement());
        }
    }

    /**
     * Scrolls the terminal to the specified position of its scroll bar. The
     * position is limited to the range of the scroll bar.
     */
    private void scrollTo(int aSelection) {

        ScrollBar scrollBar = ((Scrollable)control.getControl()).getVerticalBar();
        if (scrollBar == null) {
            return;
        }

        int selection = Math.max(scrollBar.getMinimum(), Math.min(aSelection, scrollBar.getMaximum() - scrollBar.getThumb()));
        scrollBar.setSelection(selection);

        // The canvas scrolls, when its scroll bar reports the selection.
        scrollBar.notifyListeners(SWT.Selection, new Event());
    }
}
