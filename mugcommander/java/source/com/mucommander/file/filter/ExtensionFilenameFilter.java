/*
 * This file is part of muCommander, http://www.mucommander.com
 * Copyright (C) 2002-2010 Maxence Bernard
 *
 * muCommander is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * muCommander is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mucommander.file.filter;

/**
 * This {@link FilenameFilter} matches files whose path end with one of several specified extensions.
 *
 * <p>The extension(s) may be any string, but when used in the traditional sense of a file extension (e.g. zip extension)
 * the '.' character must be included in the specified extension (e.g. ".zip" must be used, not just "zip").</p>
 * 
 * @author Maxence Bernard, Nicolas Rinaudo
 */
public class ExtensionFilenameFilter extends AbstractExtensionFilter implements FilenameFilter {

    /**
     * Creates a case-insensitive <code>ExtensionFilenameFilter</code> matching filenames ending with the specified
     * extension and operating in non-inverted mode.
     *
     * @param extension the extension to match
     */
    public ExtensionFilenameFilter(String extension) {
        this(extension, false, false);
    }

    /**
     * Creates a <code>ExtensionFilenameFilter</code> matching filenames ending with the specified extension
     * and operating in the specified modes.
     *
     * @param extension the extension to match
     * @param caseSensitive if true, this FilenameFilter will be case-sensitive
     * @param inverted if true, this filter will operate in inverted mode.
     */
    public ExtensionFilenameFilter(String extension, boolean caseSensitive, boolean inverted) {
        this(new String[]{extension}, caseSensitive, inverted);
    }

    /**
     * Creates a case-insensitive <code>ExtensionFilenameFilter</code> matching filenames ending with one of the
     * specified extensions and operating in the specified mode.
     *
     * @param ext the extensions to match
     */
    public ExtensionFilenameFilter(String[] ext) {
        this(ext, false, false);
    }

    /**
     * Creates a new <code>ExtensionFilenameFilter</code> matching filenames ending with one of the specified
     * extensions and operating in the specified modes.
     *
     * @param ext the extensions to match
     * @param caseSensitive if true, this FilenameFilter will be case-sensitive
     * @param inverted if true, this filter will operate in inverted mode.
     */
    public ExtensionFilenameFilter(String[] ext, boolean caseSensitive, boolean inverted) {
        super(new FilenameGenerator(), ext, caseSensitive, inverted);
    }
}
