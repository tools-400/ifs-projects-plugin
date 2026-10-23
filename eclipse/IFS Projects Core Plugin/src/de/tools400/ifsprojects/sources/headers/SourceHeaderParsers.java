/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.sources.headers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

import de.tools400.ifsprojects.IfsProjectsPlugin;
import de.tools400.ifsprojects.preferences.Preferences;

/**
 * The parsers of the headers of the build tools, that are installed. Parsers
 * are contributed via extension point <code>sourceHeaderParsers</code>, see
 * {@link ISourceHeaderParser}; the parser with the highest priority, that finds
 * a header, wins.
 * <p>
 * {@link #getContributions()} returns all installed parsers, e.g. for the
 * preference page, {@link #getEnabled()} those, that the user has not switched
 * off there.
 * <p>
 * A source, whose header no parser knows, has no header,
 * {@link SourceHeader#NONE}: the object type then comes from the source type
 * and the SQL include file from the migrated source files. Without any parser
 * the migration therefore works as before, it just knows less about the
 * sources.
 */
public final class SourceHeaderParsers {

    private static final String EXTENSION_POINT_ID = IfsProjectsPlugin.PLUGIN_ID + ".sourceHeaderParsers"; //$NON-NLS-1$
    private static final String ELEMENT_PARSER = "parser"; //$NON-NLS-1$
    private static final String ATTRIBUTE_ID = "id"; //$NON-NLS-1$
    private static final String ATTRIBUTE_NAME = "name"; //$NON-NLS-1$
    private static final String ATTRIBUTE_CLASS = "class"; //$NON-NLS-1$
    private static final String ATTRIBUTE_PRIORITY = "priority"; //$NON-NLS-1$

    private static List<Contribution> contributions;

    private final List<ISourceHeaderParser> parsers;

    /**
     * Creates the parsers, that are used instead of the contributed ones, e.g.
     * for tests.
     *
     * @param aParsers - parsers in the order, in which they are called
     */
    public SourceHeaderParsers(ISourceHeaderParser... aParsers) {
        this(Arrays.asList(aParsers));
    }

    /**
     * Creates the parsers, that are used instead of the contributed ones.
     *
     * @param aParsers - parsers in the order, in which they are called
     */
    public SourceHeaderParsers(List<ISourceHeaderParser> aParsers) {
        this.parsers = Collections.unmodifiableList(new ArrayList<ISourceHeaderParser>(aParsers));
    }

    /**
     * Returns all contributed parsers, sorted by their priority, the highest
     * one first. They are read once, because creating them activates their
     * bundles.
     */
    public static synchronized List<Contribution> getContributions() {

        if (contributions == null) {
            contributions = Collections.unmodifiableList(findContributions());
        }

        return contributions;
    }

    /**
     * Returns the parsers, that are used: the contributed ones without those,
     * that are switched off on the preference page of the migration, see
     * {@link Preferences#getDisabledSourceHeaderParsers()}.
     */
    public static SourceHeaderParsers getEnabled() {

        List<Contribution> all = getContributions();
        if (all.isEmpty()) {
            return new SourceHeaderParsers();
        }

        Set<String> disabled = Preferences.getInstance().getDisabledSourceHeaderParsers();

        List<ISourceHeaderParser> enabled = new ArrayList<ISourceHeaderParser>();
        for (Contribution contribution : all) {
            if (!disabled.contains(contribution.getId())) {
                enabled.add(contribution.getParser());
            }
        }

        return new SourceHeaderParsers(enabled);
    }

    /**
     * Returns the parsers in the order, in which they are called.
     */
    public List<ISourceHeaderParser> getParsers() {
        return parsers;
    }

    /**
     * Returns the header of a source.
     *
     * @param aContent - content of the source with the line separator
     *        <code>\n</code> or <code>null</code>
     * @param aSourceType - source type of the source, e.g. <code>RPGLE</code>,
     *        or <code>null</code>, if it is unknown
     * @return header, {@link SourceHeader#NONE}, if the source has none
     */
    public SourceHeader parse(String aContent, String aSourceType) {

        if (aContent == null) {
            return SourceHeader.NONE;
        }

        for (ISourceHeaderParser parser : parsers) {
            try {
                List<CreateCommand> commands = parser.parse(aContent, aSourceType);
                if (commands != null && !commands.isEmpty()) {
                    return new SourceHeader(commands);
                }
            } catch (RuntimeException e) {
                IfsProjectsPlugin.logError("Source header parser failed: " + parser.getClass().getName(), e); //$NON-NLS-1$
            }
        }

        return SourceHeader.NONE;
    }

    private static List<Contribution> findContributions() {

        List<Contribution> found = new ArrayList<Contribution>();

        IExtensionRegistry registry = Platform.getExtensionRegistry();
        if (registry == null) {
            // The platform is not running, e.g. in a unit test.
            return found;
        }

        List<IConfigurationElement> elements = new ArrayList<IConfigurationElement>();
        for (IConfigurationElement element : registry.getConfigurationElementsFor(EXTENSION_POINT_ID)) {
            if (ELEMENT_PARSER.equals(element.getName())) {
                elements.add(element);
            }
        }

        Collections.sort(elements, new Comparator<IConfigurationElement>() {
            public int compare(IConfigurationElement anElement1, IConfigurationElement anElement2) {
                return getPriority(anElement2) - getPriority(anElement1);
            }
        });

        for (IConfigurationElement element : elements) {
            try {
                Object object = element.createExecutableExtension(ATTRIBUTE_CLASS);
                if (object instanceof ISourceHeaderParser) {
                    found.add(new Contribution(element.getAttribute(ATTRIBUTE_ID), element.getAttribute(ATTRIBUTE_NAME),
                        (ISourceHeaderParser)object));
                }
            } catch (CoreException e) {
                IfsProjectsPlugin.logError("Could not create source header parser: " + element.getAttribute(ATTRIBUTE_ID), e); //$NON-NLS-1$
            } catch (LinkageError e) {
                IfsProjectsPlugin.logError("Could not load source header parser: " + element.getAttribute(ATTRIBUTE_ID), e); //$NON-NLS-1$
            }
        }

        return found;
    }

    private static int getPriority(IConfigurationElement anElement) {
        try {
            return Integer.parseInt(anElement.getAttribute(ATTRIBUTE_PRIORITY));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * A parser of the extension point with the attributes, that identify it on
     * the preference page.
     */
    public static final class Contribution {

        private final String id;
        private final String name;
        private final ISourceHeaderParser parser;

        Contribution(String anId, String aName, ISourceHeaderParser aParser) {
            this.id = anId;
            this.name = aName;
            this.parser = aParser;
        }

        /**
         * Returns the unique ID of the parser, with which it is stored in the
         * preferences.
         */
        public String getId() {
            return id;
        }

        /**
         * Returns the name of the build tool, e.g. <code>STRPREPRC</code>.
         */
        public String getName() {
            return name;
        }

        public ISourceHeaderParser getParser() {
            return parser;
        }

        @Override
        public String toString() {
            return name + " (" + id + ")"; //$NON-NLS-1$ //$NON-NLS-2$
        }
    }
}
