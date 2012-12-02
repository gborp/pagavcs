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

import com.mucommander.desktop.DesktopManager;
import com.mucommander.file.AbstractFile;
import com.mucommander.file.FileProtocols;
import com.mucommander.file.impl.local.LocalFile;
import com.mucommander.job.TempExecJob;
import com.mucommander.text.Translator;
import com.mucommander.ui.action.*;
import com.mucommander.ui.dialog.InformationDialog;
import com.mucommander.ui.dialog.file.ProgressDialog;
import com.mucommander.ui.main.FolderPanel;
import com.mucommander.ui.main.MainFrame;
import com.mucommander.ui.main.quicklist.RecentExecutedFilesQL;

import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.Hashtable;

/**
 * This action 'opens' the currently selected file or folder in the active FileTable.
 * This means different things depending on the kind of file that is currently selected:
 * <ul>
 * <li>For browsable files (directory, archive...): shows file contents
 * <li>For local file that are not an archive or archive entry: executes file with native file associations
 * <li>For any other file type, remote or local: copies file to a temporary local file and executes it with native file associations
 * </ul>
 *
 * @author Maxence Bernard, Nicolas Rinaudo
 */
public class OpenAction extends MuAction {
    /**
     * Creates a new <code>OpenAction</code> with the specified parameters.
     * @param mainFrame  frame to which the action is attached.
     * @param properties action's properties.
     */
    public OpenAction(MainFrame mainFrame, Hashtable<String,Object> properties) {
        super(mainFrame, properties);
    }

    /**
     * Opens the specified file in the specified folder panel.
     * <p>
     * <code>file</code> will be opened using the following rules:
     * <ul>
     *   <li>
     *     If <code>file</code> is {@link com.mucommander.file.AbstractFile#isBrowsable() browsable},
     *     it will be opened in <code>destination</code>.
     *   </li>
     *   <li>
     *     If <code>file</code> is local, it will be opened using its native associations.
     *   </li>
     *   <li>
     *     If <code>file</code> is remote, it will first be copied in a temporary local file and
     *     then opened using its native association.
     *   </li>
     * </ul>
     * </p>
     * @param file        file to open.
     * @param destination if <code>file</code> is browsable, folder panel in which to open the file.
     */
    protected void open(AbstractFile file, FolderPanel destination) {
        // Opens browsable files in the destination FolderPanel.
        if(file.isBrowsable())
            destination.tryChangeCurrentFolder(file);

        // Opens local files using their native associations.
        else if(file.getURL().getScheme().equals(FileProtocols.FILE) && (file.hasAncestor(LocalFile.class))) {
            try {
            	DesktopManager.open(file);
            	RecentExecutedFilesQL.addFile(file);
    		}
            catch(IOException e) {
                InformationDialog.showErrorDialog(mainFrame);
            }
        }

        // Copies non-local file in a temporary local file and opens them using their native association.
        else {
            ProgressDialog progressDialog = new ProgressDialog(mainFrame, Translator.get("copy_dialog.copying"));
            TempExecJob job = new TempExecJob(progressDialog, mainFrame, file);
            progressDialog.start(job);
        }
    }

    /**
     * Opens the currently selected file in the active folder panel.
     */
    @Override
    public void performAction() {
        AbstractFile file;

        // Retrieves the currently selected file, aborts if none.
        // Note: a CachedFile instance is retrieved to avoid blocking the event thread.
        if((file = mainFrame.getActiveTable().getSelectedFile(true, true)) == null)
            return;

        // Opens the currently selected file.
        open(file, mainFrame.getActivePanel());

    }
    
    public static class Factory implements ActionFactory {

		public MuAction createAction(MainFrame mainFrame, Hashtable<String,Object> properties) {
			return new OpenAction(mainFrame, properties);
		}
    }
    
    public static class Descriptor extends AbstractActionDescriptor {
    	public static final String ACTION_ID = "Open";
    	
		public String getId() { return ACTION_ID; }

		public ActionCategory getCategory() { return ActionCategories.NAVIGATION; }

		public KeyStroke getDefaultAltKeyStroke() { return KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0); }

		public KeyStroke getDefaultKeyStroke() { return KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0); }
    }
}
