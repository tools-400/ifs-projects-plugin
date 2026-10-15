/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.terminal;

/**
 * Immutable description of a terminal session that is opened by an
 * {@link ITerminalLauncher}.
 */
public final class TerminalRequest {

    public static final int DEFAULT_PORT = 22;
    public static final int DEFAULT_TIMEOUT = 30;
    public static final int DEFAULT_KEEP_ALIVE = 300;
    public static final String DEFAULT_ENCODING = "UTF-8"; //$NON-NLS-1$

    private static final String HOME_DIRECTORY_LABEL = "~"; //$NON-NLS-1$

    private final String connectionName;
    private final String hostName;
    private final int port;
    private final String userId;
    private final String password;
    private final String encoding;
    private final int timeout;
    private final int keepAlive;
    private final String directory;
    private final String command;
    private final String reuseKey;
    private final boolean isForceNew;

    private TerminalRequest(Builder aBuilder) {
        this.connectionName = aBuilder.connectionName;
        this.hostName = aBuilder.hostName;
        this.port = aBuilder.port;
        this.userId = aBuilder.userId;
        this.password = aBuilder.password;
        this.encoding = aBuilder.encoding;
        this.timeout = aBuilder.timeout;
        this.keepAlive = aBuilder.keepAlive;
        this.directory = aBuilder.directory;
        this.command = aBuilder.command;
        this.reuseKey = aBuilder.reuseKey;
        this.isForceNew = aBuilder.isForceNew;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns a builder initialized with the values of this request.
     *
     * @return new builder
     */
    public Builder toBuilder() {
        return new Builder().connectionName(connectionName).hostName(hostName).port(port).userId(userId).password(password).encoding(encoding)
            .timeout(timeout).keepAlive(keepAlive).directory(directory).command(command).reuseKey(reuseKey).forceNew(isForceNew);
    }

    /**
     * Returns the name of the RSE connection.
     */
    public String getConnectionName() {
        return connectionName;
    }

    public String getHostName() {
        return hostName;
    }

    public int getPort() {
        return port;
    }

    public String getUserId() {
        return userId;
    }

    /**
     * Returns the password or <code>null</code>, if the terminal implementation
     * shall prompt for it.
     */
    public String getPassword() {
        return password;
    }

    public String getEncoding() {
        return encoding;
    }

    /**
     * Returns the connection timeout in seconds.
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * Returns the keep alive interval in seconds.
     */
    public int getKeepAlive() {
        return keepAlive;
    }

    /**
     * Returns the initial directory or <code>null</code> for the home
     * directory of the user.
     */
    public String getDirectory() {
        return directory;
    }

    /**
     * Returns the command that is executed after changing to the directory or
     * <code>null</code>.
     */
    public String getCommand() {
        return command;
    }

    /**
     * Returns the key that is used to find a connected terminal that can be
     * reused or <code>null</code>, if a new terminal must be opened.
     */
    public String getReuseKey() {
        return reuseKey;
    }

    /**
     * Returns <code>true</code>, if a new terminal must be opened, even if a
     * terminal of the same session is already open.
     */
    public boolean isForceNew() {
        return isForceNew;
    }

    /**
     * Returns the key of the terminal session. Two requests with the same
     * session key describe the same terminal: same connection, host, port,
     * user and directory.
     *
     * @return session key
     */
    public String getSessionKey() {

        String path = HOME_DIRECTORY_LABEL;
        if (directory != null) {
            path = directory;
            while (path.length() > 1 && path.endsWith("/")) { //$NON-NLS-1$
                path = path.substring(0, path.length() - 1);
            }
        }

        return connectionName + "|" + hostName + "|" + port + "|" + userId + "|" + path; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    }

    /**
     * Returns the command line that is sent to the shell after the terminal
     * has been connected.
     *
     * @return command line or <code>null</code>
     */
    public String getCommandLine() {
        return ShellCommands.toCommandLine(directory, command);
    }

    /**
     * Returns the title of the terminal, e.g. <code>MYIBMI: src</code>.
     */
    public String getTitle() {
        return connectionName + ": " + getDirectoryLabel(); //$NON-NLS-1$
    }

    /**
     * Returns the tool tip of the terminal, e.g.
     * <code>jsmith@ibmi.example.com:/home/jsmith/src</code>.
     */
    public String getToolTip() {
        String path;
        if (directory == null) {
            path = HOME_DIRECTORY_LABEL;
        } else {
            path = directory;
        }

        return userId + "@" + hostName + ":" + path; //$NON-NLS-1$ //$NON-NLS-2$
    }

    private String getDirectoryLabel() {

        if (directory == null) {
            return HOME_DIRECTORY_LABEL;
        }

        String path = directory;
        while (path.length() > 1 && path.endsWith("/")) { //$NON-NLS-1$
            path = path.substring(0, path.length() - 1);
        }

        int index = path.lastIndexOf('/');
        if (index < 0 || path.length() == 1) {
            return path;
        }

        return path.substring(index + 1);
    }

    public static final class Builder {

        private String connectionName;
        private String hostName;
        private int port = DEFAULT_PORT;
        private String userId;
        private String password;
        private String encoding = DEFAULT_ENCODING;
        private int timeout = DEFAULT_TIMEOUT;
        private int keepAlive = DEFAULT_KEEP_ALIVE;
        private String directory;
        private String command;
        private String reuseKey;
        private boolean isForceNew;

        private Builder() {
        }

        public Builder connectionName(String aConnectionName) {
            this.connectionName = aConnectionName;
            return this;
        }

        public Builder hostName(String aHostName) {
            this.hostName = aHostName;
            return this;
        }

        public Builder port(int aPort) {
            this.port = aPort;
            return this;
        }

        public Builder userId(String aUserId) {
            this.userId = aUserId;
            return this;
        }

        public Builder password(String aPassword) {
            this.password = aPassword;
            return this;
        }

        public Builder encoding(String anEncoding) {
            this.encoding = anEncoding;
            return this;
        }

        public Builder timeout(int aTimeout) {
            this.timeout = aTimeout;
            return this;
        }

        public Builder keepAlive(int aKeepAlive) {
            this.keepAlive = aKeepAlive;
            return this;
        }

        public Builder directory(String aDirectory) {
            this.directory = aDirectory;
            return this;
        }

        public Builder command(String aCommand) {
            this.command = aCommand;
            return this;
        }

        public Builder reuseKey(String aReuseKey) {
            this.reuseKey = aReuseKey;
            return this;
        }

        public Builder forceNew(boolean anIsForceNew) {
            this.isForceNew = anIsForceNew;
            return this;
        }

        public TerminalRequest build() {
            if (hostName == null || hostName.trim().length() == 0) {
                throw new IllegalStateException("Host name must not be empty."); //$NON-NLS-1$
            }
            if (userId == null || userId.trim().length() == 0) {
                throw new IllegalStateException("User ID must not be empty."); //$NON-NLS-1$
            }
            if (connectionName == null) {
                connectionName = hostName;
            }
            return new TerminalRequest(this);
        }
    }
}
