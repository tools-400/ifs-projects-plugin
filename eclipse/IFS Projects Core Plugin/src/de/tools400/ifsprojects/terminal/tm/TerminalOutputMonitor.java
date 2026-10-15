/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.terminal.tm;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.eclipse.tm.internal.terminal.provisional.api.ITerminalConnector;
import org.eclipse.tm.internal.terminal.provisional.api.ITerminalControl;

/**
 * Observes the data that the terminal receives from the remote shell. It is
 * used to send the initial command line, when the shell is ready, i.e. when
 * the shell has written its prompt and is quiet. Otherwise the shell would
 * echo the command line before it has executed its profile.
 * <p>
 * The monitor sits between the terminal control and the connector. Dynamic
 * proxies are used, because the interfaces of the TM terminal differ between
 * the RDi versions. The SSH connectors of RDi 9.6, 9.8 and 9.9 only call the
 * interface methods of ITerminalControl.
 */
@SuppressWarnings("restriction")
final class TerminalOutputMonitor {

    private static final String METHOD_CONNECT = "connect"; //$NON-NLS-1$
    private static final String METHOD_GET_REMOTE_TO_TERMINAL_OUTPUT_STREAM = "getRemoteToTerminalOutputStream"; //$NON-NLS-1$

    private volatile boolean hasOutput;
    private volatile long lastOutputTime;

    /**
     * Returns a connector that delegates to the specified connector and
     * observes the data received from the remote shell.
     *
     * @param aConnector - terminal connector
     * @return observed terminal connector
     */
    ITerminalConnector observe(final ITerminalConnector aConnector) {

        return (ITerminalConnector)createProxy(ITerminalConnector.class, aConnector, new Interceptor() {
            public Object[] beforeInvoke(Method aMethod, Object[] anArgs) {
                if (METHOD_CONNECT.equals(aMethod.getName()) && anArgs != null && anArgs.length == 1 && anArgs[0] instanceof ITerminalControl) {
                    reset();
                    return new Object[] { observeControl((ITerminalControl)anArgs[0]) };
                }
                return anArgs;
            }

            public Object afterInvoke(Method aMethod, Object aResult) {
                return aResult;
            }
        });
    }

    /**
     * Returns <code>true</code>, if the remote shell has written some data and
     * has been quiet for the specified time.
     *
     * @param aQuietMillis - time without output in milliseconds
     * @return <code>true</code>, if the shell is quiet
     */
    boolean isQuiet(long aQuietMillis) {
        return hasOutput && System.currentTimeMillis() - lastOutputTime >= aQuietMillis;
    }

    /**
     * Returns <code>true</code>, if the remote shell has written some data
     * since the terminal has been connected.
     */
    boolean hasOutput() {
        return hasOutput;
    }

    void reset() {
        hasOutput = false;
        lastOutputTime = 0;
    }

    private void outputReceived() {
        lastOutputTime = System.currentTimeMillis();
        hasOutput = true;
    }

    private ITerminalControl observeControl(ITerminalControl aControl) {

        return (ITerminalControl)createProxy(ITerminalControl.class, aControl, new Interceptor() {
            public Object[] beforeInvoke(Method aMethod, Object[] anArgs) {
                return anArgs;
            }

            public Object afterInvoke(Method aMethod, Object aResult) {
                if (METHOD_GET_REMOTE_TO_TERMINAL_OUTPUT_STREAM.equals(aMethod.getName()) && aResult instanceof OutputStream) {
                    return new ObservingOutputStream((OutputStream)aResult);
                }
                return aResult;
            }
        });
    }

    private static Object createProxy(Class<?> anInterface, final Object aDelegate, final Interceptor anInterceptor) {

        return Proxy.newProxyInstance(anInterface.getClassLoader(), new Class<?>[] { anInterface }, new InvocationHandler() {
            public Object invoke(Object aProxy, Method aMethod, Object[] anArgs) throws Throwable {

                if (aMethod.getDeclaringClass() == Object.class) {
                    String name = aMethod.getName();
                    if ("equals".equals(name)) { //$NON-NLS-1$
                        return Boolean.valueOf(aProxy == anArgs[0]);
                    }
                    if ("hashCode".equals(name)) { //$NON-NLS-1$
                        return Integer.valueOf(System.identityHashCode(aProxy));
                    }
                }

                try {
                    Object result = aMethod.invoke(aDelegate, anInterceptor.beforeInvoke(aMethod, anArgs));
                    return anInterceptor.afterInvoke(aMethod, result);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            }
        });
    }

    private interface Interceptor {
        Object[] beforeInvoke(Method aMethod, Object[] anArgs);

        Object afterInvoke(Method aMethod, Object aResult);
    }

    /**
     * Output stream that passes the data to the terminal and records the time
     * of the last output.
     */
    private class ObservingOutputStream extends OutputStream {

        private final OutputStream delegate;

        public ObservingOutputStream(OutputStream aDelegate) {
            this.delegate = aDelegate;
        }

        @Override
        public void write(int aByte) throws IOException {
            delegate.write(aByte);
            outputReceived();
        }

        @Override
        public void write(byte[] aBuffer, int anOffset, int aLength) throws IOException {
            delegate.write(aBuffer, anOffset, aLength);
            if (aLength > 0) {
                outputReceived();
            }
        }

        @Override
        public void flush() throws IOException {
            delegate.flush();
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }
    }
}
