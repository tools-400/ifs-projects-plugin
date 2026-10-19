/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.export;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.rse.core.filters.ISystemFilter;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.core.model.IPropertySet;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.IFSFile;

import de.tools400.ifsprojects.ibmi.IfsPaths;
import de.tools400.ifsprojects.project.IProjConfig;
import de.tools400.ifsprojects.project.IgnoreRules;
import de.tools400.ifsprojects.project.ProjectFiles;
import de.tools400.ifsprojects.remote.RemoteJobs;

/**
 * IFS project, that is exported. Its files are read from the project
 * directory. Like the upload of local projects, the export skips the files of
 * git, Eclipse, the plug-in and the output directories of TOBi and the files
 * of <code>.deployignore</code> or <code>.gitignore</code>, see
 * {@link ProjectFiles}.
 * <p>
 * The settings are saved as a property set of the filter of the project, like
 * its environment variables.
 */
public final class IfsProjectExportSource implements ExportSource {

    /** Name of the property set of the filter, that contains the settings. */
    public static final String PROPERTY_SET_NAME = "de.tools400.ifsprojects.export"; //$NON-NLS-1$

    private static final char SEPARATOR = '/';

    private final ISystemFilter filter;
    private final IHost host;
    private final String directory;
    private final IProjConfig config;
    private final Map<String, String> variables;

    /**
     * Creates the source.
     *
     * @param aFilter - filter of the IFS project
     * @param aHost - RSE connection
     * @param aDirectory - project directory
     * @param aConfig - content of <code>iproj.json</code>
     * @param aVariables - effective environment variables of the project
     */
    public IfsProjectExportSource(ISystemFilter aFilter, IHost aHost, String aDirectory, IProjConfig aConfig, Map<String, String> aVariables) {
        this.filter = aFilter;
        this.host = aHost;
        this.directory = aDirectory;
        this.config = aConfig;
        this.variables = aVariables;
    }

    public String getName() {
        return filter.getName();
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
        return directory;
    }

    public List<String> listFiles(AS400 aSystem) throws Exception {

        IgnoreRules ignoreRules = readIgnoreRules(aSystem);

        List<String> files = new ArrayList<String>();
        listFiles(new IFSFile(aSystem, directory), "", ignoreRules, files); //$NON-NLS-1$

        return files;
    }

    public String readFile(AS400 aSystem, String aPath) throws Exception {
        return RemoteJobs.readText(new IFSFile(aSystem, getPath(aPath)));
    }

    public ExportSettings loadSettings(ExportSettings aDefaults) {

        IPropertySet propertySet = filter.getPropertySet(PROPERTY_SET_NAME);
        if (propertySet == null) {
            return aDefaults;
        }

        Map<String, String> values = new HashMap<String, String>();
        for (String key : propertySet.getPropertyKeys()) {
            values.put(key, propertySet.getPropertyValue(key));
        }

        return ExportSettings.fromMap(values, aDefaults);
    }

    public void saveSettings(ExportSettings aSettings) {

        filter.removePropertySet(PROPERTY_SET_NAME);

        IPropertySet propertySet = filter.createPropertySet(PROPERTY_SET_NAME);
        for (Map.Entry<String, String> entry : aSettings.toMap().entrySet()) {
            propertySet.addProperty(entry.getKey(), entry.getValue());
        }

        filter.setDirty(true);
        filter.commit();
    }

    private void listFiles(IFSFile aDirectory, String aPrefix, IgnoreRules anIgnoreRules, List<String> aFiles) throws Exception {

        IFSFile[] children = RemoteJobs.listFiles(aDirectory);
        if (children == null) {
            return;
        }

        for (IFSFile child : children) {
            String path = aPrefix + child.getName();
            boolean isDirectory = child.isDirectory();
            if (ProjectFiles.isExcluded(path) || anIgnoreRules.isIgnored(path, isDirectory)) {
                continue;
            }
            if (isDirectory) {
                listFiles(child, path + SEPARATOR, anIgnoreRules, aFiles);
            } else if (child.isFile()) {
                aFiles.add(path);
            }
        }
    }

    /**
     * Reads the rules of the ignore file of the project directory, see
     * {@link ProjectFiles#readIgnoreRules(ProjectFiles.ProjectDirectory)}.
     */
    private IgnoreRules readIgnoreRules(final AS400 aSystem) throws Exception {

        return ProjectFiles.readIgnoreRules(new ProjectFiles.ProjectDirectory() {
            public String readFile(String aName) throws Exception {
                IFSFile file = new IFSFile(aSystem, getPath(aName));
                return file.exists() ? RemoteJobs.readText(file) : null;
            }
        });
    }

    private String getPath(String aRelativePath) {
        return IfsPaths.append(directory, aRelativePath);
    }
}
