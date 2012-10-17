/*
 * This file is part of muCommander, http://www.mucommander.com
 * Copyright (C) 2002-2010 Maxence Bernard
 *
 * muCommander is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * muCommander is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mucommander.ant.util;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

import java.io.File;

/**
 * Task used to compute the size of a fileset.
 * @ant.task name="size" category="util"
 * @author Nicolas Rinaudo
 */
public class SizeTask extends Task {
    // - Task fields -----------------------------------------------------------
    // -------------------------------------------------------------------------
    /** Name of the property in which to store the size of the analysed fileset. */
    private String  propertyName;
    /** Fileset to analyse. */
    private FileSet files;

    public SizeTask() {}

    public void init() {
        propertyName = null;
        files        = null;
    }

    public void addConfiguredFileSet(FileSet f) {files = f;}
    public void setName(String s) {propertyName = s;}

    public void execute() throws BuildException {
        DirectoryScanner scanner;
        String[] selectedFiles;
        int      size;

        // Makes sure the task has been properly initialised.
        if(propertyName == null)
            throw new BuildException("Unspecified name - please fill in the name attribute.");
        if(files == null)
            throw new BuildException("Unspecified fileset.");

        scanner = files.getDirectoryScanner(getProject());
        scanner.scan();
        selectedFiles = scanner.getIncludedFiles();
        size          = 0 ;

        for (String selectedFile : selectedFiles)
            size += new File(files.getDir(getProject()), selectedFile).length();

        getProject().setProperty(propertyName, Integer.toString(size / 1024));
    }
}
