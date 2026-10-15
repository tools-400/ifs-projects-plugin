/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.subsystem;

import java.util.Map;

import org.eclipse.rse.core.filters.ISystemFilter;
import org.eclipse.rse.core.filters.ISystemFilterReference;

/**
 * An IFS project: a filter of the {@link IfsProjectSubSystem} and its project
 * directory.
 */
public final class IfsProject {

    private final IfsProjectSubSystem subSystem;
    private final ISystemFilter filter;
    private final String directory;

    IfsProject(IfsProjectSubSystem aSubSystem, ISystemFilter aFilter, String aDirectory) {
        this.subSystem = aSubSystem;
        this.filter = aFilter;
        this.directory = aDirectory;
    }

    public IfsProjectSubSystem getSubSystem() {
        return subSystem;
    }

    public ISystemFilter getFilter() {
        return filter;
    }

    /**
     * Returns the name of the project, i.e. the name of the filter.
     */
    public String getName() {
        return filter.getName();
    }

    /**
     * Returns the absolute project directory without trailing slash.
     */
    public String getDirectory() {
        return directory;
    }

    /**
     * Returns the filter reference of the project, which is the object shown
     * in the Remote Systems view.
     *
     * @return filter reference or <code>null</code>
     */
    public ISystemFilterReference getFilterReference() {
        return subSystem.getFilterPoolReferenceManager().getSystemFilterReference(subSystem, filter);
    }

    /**
     * Returns the environment variables, that are exported before the build and
     * compile commands of the project are executed: the default environment
     * variables of the preferences, overridden by the variables of the project.
     *
     * @return variables, never <code>null</code>
     */
    public Map<String, String> getEnvironmentVariables() {
        return IfsProjectEnvironment.getEffectiveVariables(filter);
    }
}
