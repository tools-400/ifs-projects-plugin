/*******************************************************************************
 * Copyright (c) 2026 Thomas Raddatz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package de.tools400.ifsprojects.sources;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.tools400.ifsprojects.utils.StringUtils;

/**
 * A <code>/COPY</code> or <code>/INCLUDE</code> directive in a line of an RPG
 * source. Its name is either quoted (<code>'...'</code>, <code>"..."</code>),
 * which is a stream file, or unquoted, e.g. a member (<code>INC,CONSTS</code>,
 * <code>MYLIB/INC,CONSTS</code>, <code>CONSTS</code>) or a path.
 */
public final class RpgIncludeDirective {

    private static final Pattern DIRECTIVE = Pattern.compile("(/COPY|/INCLUDE)(?=\\s|$)", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$

    private final String line;
    private final String keyword;
    private final String name;
    private final boolean isQuoted;
    private final int nameStart;
    private final int nameEnd;
    private final int maxLength;

    private RpgIncludeDirective(String aLine, String aKeyword, String aName, boolean anIsQuoted, int aNameStart, int aNameEnd, int aMaxLength) {
        this.line = aLine;
        this.keyword = aKeyword;
        this.name = aName;
        this.isQuoted = anIsQuoted;
        this.nameStart = aNameStart;
        this.nameEnd = aNameEnd;
        this.maxLength = aMaxLength;
    }

    /**
     * Parses a directive.
     *
     * @param aLine - line, without the comment columns of fixed format sources
     * @param aStart - index, at which the directive must start, or -1
     * @param aMaxLength - maximum length of the line, see
     *        {@link #replaceName(String)}
     * @return directive or <code>null</code>, if the line has no directive at
     *         the index or the name of the directive is missing or not
     *         terminated
     */
    static RpgIncludeDirective parse(String aLine, int aStart, int aMaxLength) {

        if (aStart < 0) {
            return null;
        }

        Matcher matcher = DIRECTIVE.matcher(aLine);
        matcher.region(aStart, aLine.length());
        if (!matcher.lookingAt()) {
            return null;
        }

        String keyword = matcher.group(1);
        int nameStart = StringUtils.indexOfNonBlank(aLine, matcher.end());
        if (nameStart < 0) {
            return null;
        }

        char quote = aLine.charAt(nameStart);
        if (quote == '\'' || quote == '"') {
            int end = aLine.indexOf(quote, nameStart + 1);
            if (end < 0) {
                return null;
            }
            return new RpgIncludeDirective(aLine, keyword, aLine.substring(nameStart + 1, end), true, nameStart, end + 1, aMaxLength);
        }

        int nameEnd = nameStart;
        while (nameEnd < aLine.length() && !Character.isWhitespace(aLine.charAt(nameEnd))) {
            nameEnd++;
        }

        return new RpgIncludeDirective(aLine, keyword, aLine.substring(nameStart, nameEnd), false, nameStart, nameEnd, aMaxLength);
    }

    /**
     * Returns the keyword in the case of the source, e.g. <code>/copy</code>.
     */
    public String getKeyword() {
        return keyword;
    }

    /**
     * Returns the name without quotes, e.g. <code>inc/consts.rpgleinc</code>
     * or <code>INC,CONSTS</code>.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns <code>true</code>, if the name is quoted.
     */
    public boolean isQuoted() {
        return isQuoted;
    }

    /**
     * Returns the line with another name. The rest of the line is kept.
     *
     * @param aName - new name including the quotes, if it shall be quoted
     * @return line or <code>null</code>, if it is longer than the columns of
     *         the source allow
     */
    public String replaceName(String aName) {

        String converted = line.substring(0, nameStart) + aName + line.substring(nameEnd);
        if (converted.length() > maxLength) {
            return null;
        }

        return converted;
    }
}
