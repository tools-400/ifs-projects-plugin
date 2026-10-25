/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.local;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.rse.core.model.IHost;

import com.ibm.as400.access.AS400;

import de.tools400.ifsprojects.export.ExportSettings;
import de.tools400.ifsprojects.export.ExportSource;
import de.tools400.ifsprojects.project.IProjConfig;
import de.tools400.ifsprojects.remote.RemoteJobs;

/**
 * Local IFS project, that is exported. Its files are read from the workspace,
 * so that the export neither needs nor changes the deploy directory. The same
 * files as by the upload are exported, see {@link Deployment#getFiles}.
 * <p>
 * The deploy directory is the project directory of the include directives,
 * because the project is compiled there. The settings are saved per connection
 * as persistent property of the project, like the environment variables.
 */
public final class LocalProjectExportSource implements ExportSource {

    private final LocalProject localProject;
    private final IHost host;
    private final IProjConfig config;
    private final Map<String, String> variables;

    /**
     * Creates the source.
     *
     * @param aLocalProject - local IFS project
     * @param aHost - RSE connection of the project
     * @param aConfig - content of <code>iproj.json</code>
     * @param aVariables - effective environment variables of the project
     */
    public LocalProjectExportSource(LocalProject aLocalProject, IHost aHost, IProjConfig aConfig, Map<String, String> aVariables) {
        this.localProject = aLocalProject;
        this.host = aHost;
        this.config = aConfig;
        this.variables = aVariables;
    }

    public String getName() {
        return localProject.getName();
    }

    public IHost getHost() {
        return host;
    }

    public IProjConfig getConfig() {
        return config;
    }

    public Map<String, String> getVariables() {
        return variables;
    }

    public String getProjectDirectory(AS400 aSystem) {
        return localProject.getDeployDirectory(RemoteJobs.getHomeDirectory(aSystem));
    }

    /**
     * Refreshes the project, so that changes made outside of the workspace,
     * e.g. by git, are exported, and returns its files.
     */
    public List<String> listFiles(AS400 aSystem) throws Exception {

        localProject.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);

        return new ArrayList<>(Deployment.getFiles(localProject.getProject()).keySet());
    }

    public String readFile(AS400 aSystem, String aPath) throws Exception {

        IFile file = localProject.getProject().getFile(aPath);

        return LocalProject.readText(file);
    }

    public ExportSettings loadSettings(ExportSettings aDefaults) {

        Map<String, String> values = LocalProject.getExportSettings(localProject.getProject(), localProject.getConnectionName());
        if (values.isEmpty()) {
            return aDefaults;
        }

        return ExportSettings.fromMap(values, aDefaults);
    }

    public void saveSettings(ExportSettings aSettings) throws Exception {
        LocalProject.setExportSettings(localProject.getProject(), localProject.getConnectionName(), aSettings.toMap());
    }
}
