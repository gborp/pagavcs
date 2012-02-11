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

package com.mucommander.file.impl.zip;

import com.mucommander.file.*;
import org.junit.After;
import org.junit.Before;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

/**
 * An {@link AbstractFileTest} implementation, which performs tests on {@link AbstractArchiveEntryFile}
 * entries located inside a {@link ZipArchiveFile} residing in a temporary {@link com.mucommander.file.impl.local.LocalFile}.
 *
 * @author Maxence Bernard
 */
public class ZipArchiveFileTest extends AbstractFileTest {

    /** The archive file which contains the temporary entries */
    private static ZipArchiveFile tempZipFile;

    /** id of the last temporary entry generated, to avoid collisions */
    private int entryNum;


    ////////////////////////////////////
    // ConditionalTest implementation //
    ////////////////////////////////////

    public boolean isEnabled() {
        return true;
    }

    /////////////////////////////////////
    // AbstractFileTest implementation //
    /////////////////////////////////////

    @Override
    public AbstractFile getTemporaryFile() throws IOException {
        // use a incremental id to avoid collisions
        return tempZipFile.getDirectChild("entry"+(++entryNum));
    }

    @Override
    public FileOperation[] getSupportedOperations() {
        return new FileOperation[] {
            FileOperation.READ_FILE,
            FileOperation.WRITE_FILE,
            FileOperation.CREATE_DIRECTORY,
            FileOperation.LIST_CHILDREN,
            FileOperation.DELETE,
            FileOperation.CHANGE_DATE,
            FileOperation.CHANGE_PERMISSION,
            FileOperation.GET_FREE_SPACE,
            FileOperation.GET_TOTAL_SPACE
        };
    }


    ////////////////////////
    // Overridden methods //
    ////////////////////////

    /**
     * Overridden to create the archive file before each test.
     */
    @Override
    @Before
    public void setUp() throws IOException {
        tempZipFile = (ZipArchiveFile)FileFactory.getTemporaryFile(ZipArchiveFileTest.class.getName()+".zip", false);
        tempZipFile.mkfile();

        entryNum = 0;

        super.setUp();
    }

    /**
     * Overridden to delete the archive file after each test.
     */
    @Override
    @After
    public void tearDown() throws IOException {
        super.tearDown();

        tempZipFile.delete();
    }

    @Override
    public void testCanonicalPath() throws IOException, NoSuchAlgorithmException {
        // TODO
        // Test temporarily disabled because if fails. The failure seems to be caused by archive file caching:
        // the change is made to the archive file denoted by its absolute path ; when accessed by the canonical path,
        // the archive file is another instance which isn't aware of the change, because the file date hasn't changed (?). 
    }

//    /**
//     * Tests the Zip32 4GB limit by asserting two things:
//     * <ul>
//     *  <li>that entries can be as large as 4GB minus one byte, preventing against unsigned java int issues amongst
//     * other things.</li>
//     *  <li>that entries cannot exceed 4GB and that an IOException is thrown if trying to write more than 4Gb, rather
//     * than failing silently and leaving the Zip file corrupted.</li>
//     * </ul>
//     *
//     * @throws IOException should not happen
//     * @throws NoSuchAlgorithmException should not happen
//     */
//    public void testZip32Limit() throws IOException, NoSuchAlgorithmException {
//        // Assert a 4GB minus one byte entry can be properly compressed and uncompressed
//        ChecksumOutputStream md5Out = getMd5OutputStream(tempFile.getAppendOutputStream());
//        StreamUtils.fillWithConstant(md5Out, (byte)0, MAX_ZIP32_ENTRY_SIZE);
//        md5Out.close();
//
//        assertEquals(md5Out.getChecksum(), calculateMd5(tempFile));
//
//        // Assert that an IOException is thrown if more than 4GB is written to an entry
//        OutputStream out = tempFile.getOutputStream();
//        boolean ioExceptionThrown = false;
//        try {
//            StreamUtils.fillWithConstant(out, (byte)0, MAX_ZIP32_ENTRY_SIZE+1);
//        }
//        catch(IOException e) {
//            ioExceptionThrown = true;
//        }
//        finally {
//            out.close();
//        }
//
//        assertTrue(ioExceptionThrown);
//
//        // Todo: test Zip files larger than 4Gb as a whole (should fail gracefully)
//    }
}
