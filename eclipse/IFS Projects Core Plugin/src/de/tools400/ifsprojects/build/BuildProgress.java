/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.build;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Follows the output of a TOBi build line by line: the step, that is running,
 * e.g. <code>Creating program [HELLO] from modules [HELLO]</code>, and the
 * number of created and failed objects.
 * <p>
 * The results are recognized like <code>makei</code> does (method
 * <code>make</code> of <code>build.py</code>): <code>was created
 * successfully!</code>, <code>was registered successfully!</code> and
 * <code>End of creating</code> count as created, <code>Failed to create</code>
 * as failed. An object, that is reported twice, e.g. by a custom recipe, is
 * counted once.
 */
public final class BuildProgress {

    private static final Pattern ESCAPE_SEQUENCE = Pattern.compile("\u001B\\[[0-9;]*m"); //$NON-NLS-1$
    private static final Pattern WHITESPACE = Pattern.compile("\\s+"); //$NON-NLS-1$
    private static final Pattern NAME_DECORATION = Pattern.compile("^['\"]+|['\"!]+$"); //$NON-NLS-1$

    private static final String STEP_PREFIX = "=== "; //$NON-NLS-1$
    private static final String CREATING = STEP_PREFIX + "Creating "; //$NON-NLS-1$
    private static final String CREATED = "was created successfully!"; //$NON-NLS-1$
    private static final String REGISTERED = "was registered successfully!"; //$NON-NLS-1$
    private static final String END_OF_CREATING = "End of creating"; //$NON-NLS-1$
    private static final String FAILED = "Failed to create"; //$NON-NLS-1$

    private final Set<String> createdObjects = new HashSet<>();
    private final Set<String> failedObjects = new HashSet<>();
    private String step;

    /**
     * Evaluates a line of the output.
     *
     * @param aLine - line without line end
     * @return <code>true</code>, if the step or the number of objects has
     *         changed
     */
    public boolean add(String aLine) {

        String line = ESCAPE_SEQUENCE.matcher(aLine).replaceAll("").trim(); //$NON-NLS-1$

        if (line.startsWith(CREATING)) {
            String newStep = line.substring(STEP_PREFIX.length());
            boolean isChanged = !newStep.equals(step);
            step = newStep;
            return isChanged;
        }

        String[] words = WHITESPACE.split(line);
        if (words.length < 2) {
            return false;
        }

        if (line.contains(FAILED)) {
            return failedObjects.add(getName(words[words.length - 1]));
        }

        if (line.contains(CREATED) || line.contains(REGISTERED)) {
            return createdObjects.add(getName(words[1]));
        }

        if (line.contains(END_OF_CREATING)) {
            return createdObjects.add(getName(words[words.length - 1]));
        }

        return false;
    }

    /**
     * Returns the step, that is running.
     *
     * @return step without the leading <code>===</code> or <code>null</code>,
     *         if no step has started yet
     */
    public String getStep() {
        return step;
    }

    /**
     * Returns the number of objects, that have been created so far.
     *
     * @return number of objects
     */
    public int getCreatedCount() {
        return createdObjects.size();
    }

    /**
     * Returns the number of objects, that could not be created so far.
     *
     * @return number of objects
     */
    public int getFailedCount() {
        return failedObjects.size();
    }

    private static String getName(String aWord) {
        return NAME_DECORATION.matcher(aWord).replaceAll(""); //$NON-NLS-1$
    }
}
