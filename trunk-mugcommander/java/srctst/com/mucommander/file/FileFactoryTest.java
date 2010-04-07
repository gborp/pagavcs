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

package com.mucommander.file;

import junit.framework.TestCase;

import java.io.IOException;

/**
 * A test case for {@link FileFactory}.
 *
 * @author Maxence Bernard
 */
public class FileFactoryTest extends TestCase {

    /**
     * Tests {@link com.mucommander.file.FileFactory#getTemporaryFolder()}.
     *
     * @throws IOException should not happen
     */
    public void testTemporaryFolder() throws IOException {
        // Assert that the returned file is a folder that exists
        AbstractFile temporaryFolder = FileFactory.getTemporaryFolder();
        assertNotNull(temporaryFolder);
        assertTrue(temporaryFolder.isDirectory());
        assertTrue(temporaryFolder.exists());

        // Assert that the temporary folder is the parent folder of temporary files
        AbstractFile temporaryFile = FileFactory.getTemporaryFile(false);
        assertTrue(temporaryFile.getParent().equals(temporaryFolder));
    }

    /**
     * Tests {@link com.mucommander.file.FileFactory#getTemporaryFile(String, boolean)}.
     *
     * @throws IOException should not happen
     */
    public void testTemporaryFiles() throws IOException {
        String desiredName = System.currentTimeMillis()+".ext";

        // Assert that #getTemporaryFile returns a non-existing file with the desired name
        AbstractFile temporaryFile1 = FileFactory.getTemporaryFile(desiredName, true);
        assertNotNull(temporaryFile1);
        assertFalse(temporaryFile1.exists());
        assertEquals(desiredName, temporaryFile1.getName());

        // Assert that #getTemporaryFile returns a new temporary file if the requested file already exists, and that the
        // extension matches the desired one.
        temporaryFile1.mkfile();

        AbstractFile temporaryFile2 = FileFactory.getTemporaryFile(desiredName, true);
        assertNotNull(temporaryFile2);
        assertFalse(temporaryFile2.exists());
        assertFalse(temporaryFile2.getName().equals(desiredName));
        assertEquals(temporaryFile1.getExtension(), temporaryFile2.getExtension());

        // Note: the temporary file should normally be deleted on VM shutdown, but we have no (easy) way to assert that

        // Perform some basic tests on #getTemporaryFile when called without a desired name
        temporaryFile1 = FileFactory.getTemporaryFile(true);
        assertNotNull(temporaryFile1);
        assertFalse(temporaryFile1.exists());
    }
}
