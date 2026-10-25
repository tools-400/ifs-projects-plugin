/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.rse;

import java.util.Locale;

import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.window.Window;
import org.eclipse.rse.core.PasswordPersistenceManager;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.core.model.SystemSignonInformation;
import org.eclipse.swt.widgets.Shell;

import com.ibm.etools.iseries.subsystems.qsys.api.IBMiConnection;

import de.tools400.ifsprojects.IfsProjectsPlugin;
import de.tools400.ifsprojects.Messages;
import de.tools400.ifsprojects.dialogs.ToolTipInputDialog;
import de.tools400.ifsprojects.preferences.Preferences;
import de.tools400.ifsprojects.terminal.TerminalRequest;
import de.tools400.ifsprojects.utils.StringUtils;

/**
 * Creates terminal requests from RSE connections: user ID and saved password of
 * the connection, settings of the preferences. The user ID is prompted, if the
 * connection has none.
 */
public final class RseTerminalRequests {

    private RseTerminalRequests() {
        // Utility class
    }

    /**
     * Returns a terminal request builder that is initialized with the
     * connection data of the specified host and the preferences.
     *
     * @param aShell - shell used for prompting the user ID
     * @param aHost - RSE connection
     * @return builder or <code>null</code>, if the user canceled the user ID
     *         prompt
     */
    public static TerminalRequest.Builder createBuilder(Shell aShell, IHost aHost) {

        Preferences preferences = Preferences.getInstance();

        String rseUserId = getRseUserId(aHost);
        String userId = rseUserId;
        if (StringUtils.isBlank(userId)) {
            userId = promptUserId(aShell, aHost);
            if (userId == null) {
                return null;
            }
        }

        userId = toSshUserId(userId, preferences.isLowercaseUserId());

        String password = null;
        if (preferences.isUseSavedPassword() && !StringUtils.isBlank(rseUserId)) {
            password = findSavedPassword(aHost, rseUserId);
        }

        TerminalRequest.Builder builder = TerminalRequest.builder();
        builder.connectionName(aHost.getAliasName());
        builder.hostName(aHost.getHostName());
        builder.port(preferences.getPort());
        builder.userId(userId);
        builder.password(password);
        builder.encoding(preferences.getEncoding());
        builder.timeout(preferences.getTimeout());
        builder.keepAlive(preferences.getKeepAlive());

        return builder;
    }

    /**
     * Converts an IBM i user profile name to the user ID used for the SSH
     * login. IBM i user profiles are upper case, but the SSH login is performed
     * with the lower case name.
     *
     * @param aUserId - IBM i user profile, e.g. JSMITH
     * @param aLowercase - specifies whether to convert the user ID to lower
     *        case
     * @return user ID for SSH, e.g. jsmith
     */
    public static String toSshUserId(String aUserId, boolean aLowercase) {

        if (aUserId == null) {
            return null;
        }

        String userId = aUserId.trim();
        if (aLowercase) {
            userId = userId.toLowerCase(Locale.ROOT);
        }

        return userId;
    }

    private static String getRseUserId(IHost aHost) {

        try {
            IBMiConnection connection = IBMiConnection.getConnection(aHost);
            if (connection != null) {
                String userId = connection.getUserID();
                if (!StringUtils.isBlank(userId)) {
                    return userId;
                }
            }
        } catch (Throwable e) {
            IfsProjectsPlugin.logError("Could not retrieve user ID of connection: " + aHost.getAliasName(), e); //$NON-NLS-1$
        }

        return aHost.getDefaultUserId();
    }

    // getPassword() is deprecated in RDi 9.8+, but getPasswordAsCharArray()
    // does not exist in RDi 9.6.
    @SuppressWarnings("deprecation")
    private static String findSavedPassword(IHost aHost, String aUserId) {

        try {
            SystemSignonInformation signonInformation = PasswordPersistenceManager.getInstance().find(aHost.getSystemType(), aHost.getHostName(),
                aUserId);
            if (signonInformation != null) {
                return signonInformation.getPassword();
            }
        } catch (Throwable e) {
            IfsProjectsPlugin.logError("Could not retrieve saved password of connection: " + aHost.getAliasName(), e); //$NON-NLS-1$
        }

        return null;
    }

    private static String promptUserId(Shell aShell, IHost aHost) {

        ToolTipInputDialog dialog = new ToolTipInputDialog(aShell, Messages.Title_IFS_Projects,
            Messages.bind(Messages.Label_Enter_user_ID_for_A, aHost.getHostName()), "", new IInputValidator() { //$NON-NLS-1$
                public String isValid(String aNewText) {
                    if (StringUtils.isBlank(aNewText)) {
                        return Messages.Error_User_ID_must_not_be_empty;
                    }
                    return null;
                }
            }, Messages.Tooltip_User_ID);

        if (dialog.open() != Window.OK) {
            return null;
        }

        return dialog.getValue().trim();
    }
}
