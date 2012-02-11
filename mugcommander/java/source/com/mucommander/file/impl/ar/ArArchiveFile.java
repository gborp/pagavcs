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

package com.mucommander.file.impl.ar;

import com.mucommander.file.*;
import com.mucommander.io.BoundedInputStream;

import java.io.IOException;
import java.io.InputStream;

/**
 * ArArchiveFile provides read-only access to archives in the unix AR format. Both the BSD and GNU variants (which adds
 * support for extended filenames) are supported.
 *
 * @see com.mucommander.file.impl.ar.ArFormatProvider
 * @author Maxence Bernard
 */
public class ArArchiveFile extends AbstractROArchiveFile {

    /**
     * Creates a ArArchiveFile around the given file.
     *
     * @param file the underlying file to wrap this archive file around
     */
    public ArArchiveFile(AbstractFile file) {
        super(file);
    }


    ////////////////////////////////////////
    // AbstractArchiveFile implementation //
    ////////////////////////////////////////

    @Override
    public ArchiveEntryIterator getEntryIterator() throws IOException, UnsupportedFileOperationException {
        return new ArArchiveEntryIterator(getInputStream());
    }

    @Override
    public InputStream getEntryInputStream(ArchiveEntry entry, ArchiveEntryIterator entryIterator) throws IOException, UnsupportedFileOperationException {
        InputStream in = getInputStream();
        ArchiveEntryIterator iterator = new ArArchiveEntryIterator(in);

        ArchiveEntry currentEntry;
        while((currentEntry = iterator.nextEntry())!=null) {
            if(currentEntry.getName().equals(entry.getName())) {
                FileLogger.finest("found entry "+entry.getName());
                return new BoundedInputStream(in, entry.getSize(), false);
            }
        }

        // Entry not found, should not normally happen
        FileLogger.fine("Warning: entry not found, throwing IOException");
        throw new IOException();
    }
}
