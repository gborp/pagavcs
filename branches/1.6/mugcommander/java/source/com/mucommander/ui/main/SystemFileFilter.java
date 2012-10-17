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

package com.mucommander.ui.main;

import com.mucommander.file.AbstractFile;
import com.mucommander.file.filter.AbstractFileFilter;

/**
 * Filter used to filter out system files and folders that should not be displayed to inexperienced users.
 *
 * <p>At the moment, this filter only supports Mac OS X top-level system folders (those hidden by Finder)
 * and thus this filter should only be used under Mac OS X.
 *
 * @author Maxence Bernard
 */
public class SystemFileFilter extends AbstractFileFilter {

    /**
     * Top-level Mac OS X system folders hidden by Finder. For more info about those files:
     * http://www.westwind.com/reference/OS-X/invisibles.html
     */
    private final static String SYSTEM_FOLDERS[]= {
        // Mac OS X system folders
        "/.Trashes",
        "/.vol",
        "/dev",
        "/automount",
        "/bin",
        "/cores",
        "/etc",
        "/lost+found",    
        "/Network",
        "/private",
        "/sbin",
        "/tmp",
        "/usr",
        "/var",
//        "/Volumes",
        "/mach.sym",
        "/mach_kernel",
        "/mach",
        "/Desktop DB",
        "/Desktop DF",
        "/File Transfer Folder",
        "/.hotfiles.btree",
        "/.Spotlight-V100",
        "/.hidden",     // Used by Mac OS X up to 10.3, not in 10.4
        System.getProperty("user.home")+"/.Trash",  // User trash folder
        // Mac OS 9 system folders 
        "/AppleShare PDS",
        "/Cleanup At Startup",
        "/Desktop Folder",
        "/Network Trash Folder",
        "/Shutdown Check",
        "/Temporary Items",
        System.getProperty("user.home")+"/Temporary Items",  // User trash folder
        "/TheFindByContentFolder",
        "/TheVolumeSettingsFolder",
        "/Trash",
        "/VM Storage"
    };


    ///////////////////////////////
    // FileFilter implementation //
    ///////////////////////////////

    public boolean accept(AbstractFile file) {
        String path = file.getAbsolutePath(false);

        int nbSystemFolders = SYSTEM_FOLDERS.length;
        for(int i=0; i<nbSystemFolders; i++)
            if(path.equals(SYSTEM_FOLDERS[i]))
                return false;

        return true;
    }
}
