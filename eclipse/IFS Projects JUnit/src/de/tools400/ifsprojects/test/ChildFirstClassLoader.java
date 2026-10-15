/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.test;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * Child-first class loader of {@link CheckNLSMessages}: loads a class fresh
 * from the given URLs, so that the static NLS initializer runs again for each
 * locale, while platform dependencies (e.g. org.eclipse.osgi.util.NLS) are
 * resolved via the parent class loader.
 */
public class ChildFirstClassLoader extends URLClassLoader {

    public ChildFirstClassLoader(URL[] anUrls, ClassLoader aParent) {
        super(anUrls, aParent);
    }

    @Override
    protected Class<?> loadClass(String aName, boolean aResolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(aName)) {
            Class<?> clazz = findLoadedClass(aName);
            if (clazz == null) {
                try {
                    clazz = findClass(aName);
                } catch (ClassNotFoundException e) {
                    clazz = getParent().loadClass(aName);
                }
            }
            if (aResolve) {
                resolveClass(clazz);
            }
            return clazz;
        }
    }
}
