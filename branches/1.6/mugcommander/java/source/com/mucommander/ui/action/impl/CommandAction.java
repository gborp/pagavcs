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

package com.mucommander.ui.action.impl;

import com.mucommander.AppLogger;
import com.mucommander.command.Command;
import com.mucommander.file.FileProtocols;
import com.mucommander.file.impl.local.LocalFile;
import com.mucommander.file.util.FileSet;
import com.mucommander.job.TempOpenWithJob;
import com.mucommander.process.ProcessRunner;
import com.mucommander.text.Translator;
import com.mucommander.ui.action.MuAction;
import com.mucommander.ui.dialog.InformationDialog;
import com.mucommander.ui.dialog.file.ProgressDialog;
import com.mucommander.ui.main.MainFrame;

import java.util.Hashtable;

/**
 * @author Nicolas Rinaudo
 */
public class CommandAction extends MuAction {
    // - Instance fields -------------------------------------------------------
    // -------------------------------------------------------------------------
    /** Command to run. */
    private Command command;



    // - Initialization --------------------------------------------------------
    // -------------------------------------------------------------------------
    /**
     * Creates a new <code>CommandAction</code> initialized with the specified parameters.
     * @param mainFrame  frame that will be affected by this action.
     * @param properties ignored.
     * @param command    command to run when this action is called.
     */
    public CommandAction(MainFrame mainFrame, Hashtable<String,Object> properties, Command command) {
        super(mainFrame, properties);
        this.command = command;
        setLabel(command.getDisplayName());
    }



    // - Action code -----------------------------------------------------------
    // -------------------------------------------------------------------------
    @Override
    public void performAction() {
        FileSet selectedFiles;

        // Retrieves the current selection.
        selectedFiles = mainFrame.getActiveTable().getSelectedFiles();

        // If no files are either selected or marked, aborts.
        if(selectedFiles.size() == 0)
            return;

        // If we're working with local files, go ahead and runs the command.
        if(selectedFiles.getBaseFolder().getURL().getScheme().equals(FileProtocols.FILE) && (selectedFiles.getBaseFolder().hasAncestor(LocalFile.class))) {
            try {ProcessRunner.execute(command.getTokens(selectedFiles), selectedFiles.getBaseFolder());}
            catch(Exception e) {
                InformationDialog.showErrorDialog(mainFrame);

                AppLogger.fine("Failed to execute command: " + command.getCommand(), e);
            }
        }
        // Otherwise, copies the files locally before running the command.
        else {
            ProgressDialog progressDialog = new ProgressDialog(mainFrame, Translator.get("copy_dialog.copying"));
            progressDialog.start(new TempOpenWithJob(new ProgressDialog(mainFrame, Translator.get("copy_dialog.copying")), mainFrame, selectedFiles, command));
        }
    }
}
