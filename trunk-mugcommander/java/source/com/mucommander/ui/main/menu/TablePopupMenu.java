/*
 * This file is part of muCommander, http://www.mucommander.com Copyright (C)
 * 2002-2010 Maxence Bernard muCommander is free software; you can redistribute
 * it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 3 of the License,
 * or (at your option) any later version. muCommander is distributed in the hope
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details. You should have received a copy of
 * the GNU General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 */

package com.mucommander.ui.main.menu;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;

import com.mucommander.desktop.DesktopManager;
import com.mucommander.file.AbstractFile;
import com.mucommander.file.util.FileSet;
import com.mucommander.ui.action.ActionManager;
import com.mucommander.ui.main.MainFrame;

/**
 * Contextual popup menu invoked by FileTable when right-clicking on a file or a
 * group of files. The following items are displayed (see constructor code for
 * conditions) : Open Open natively Open with... Rename Reveal in Finder ----
 * Copy name(s) Copy path(s) ---- Mark all Unmark all Mark / Unmark ---- Delete
 * ---- Properties
 * 
 * @author Maxence Bernard, Nicolas Rinaudo
 */
public class TablePopupMenu extends JPopupMenu {

	/** Parent MainFrame instance */
	private MainFrame mainFrame;

	private class PagaVcsAction extends AbstractAction {

		private final String command;

		public PagaVcsAction(String label, String command) {
			super(label);
			this.command = command;
		}

		public void actionPerformed(ActionEvent e) {
			try {
				Socket socket = new Socket("localhost", 12905);
				BufferedWriter outToClient = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
				outToClient.write(command);
				outToClient.flush();
				socket.close();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

	}

	private void pagaVcsMenu(AbstractFile currentFolder, AbstractFile clickedFile, boolean parentFolderClicked, FileSet markedFiles) {
		try {
			AbstractFile abstractFile = clickedFile;
			if (abstractFile == null) {
				abstractFile = currentFolder;
			}

			Socket socket = new Socket("localhost", 12905);
			String strOut = "getmenuitems " + abstractFile.getAbsolutePath() + "\n";
			BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			BufferedWriter outToClient = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			outToClient.write(strOut);
			outToClient.flush();

			boolean end = false;
			boolean hasMenuItem = false;
			JMenu menuPagaVcs = new JMenu("PagaVcs");

			while (!end) {
				String line = br.readLine();
				if ("--end--".equals(line)) {
					end = true;
				} else {
					String label = br.readLine();
					br.readLine();
					br.readLine();
					br.readLine();
					String command = br.readLine();

					menuPagaVcs.add(new PagaVcsAction(label, command + " \"" + abstractFile.getAbsolutePath() + "\""));
					hasMenuItem = true;
				}
			}
			socket.close();
			if (hasMenuItem) {
				add(menuPagaVcs);
			}
		} catch (UnknownHostException ex) {
			ex.printStackTrace();
		} catch (IOException ex) {
			ex.printStackTrace();
		}

	}

	/**
	 * Creates a new TablePopupMenu.
	 * 
	 * @param mainFrame
	 *            parent MainFrame instance
	 * @param currentFolder
	 *            current folder in table
	 * @param clickedFile
	 *            right-clicked file, can be null if user clicked on the folder
	 *            table background
	 * @param parentFolderClicked
	 *            true if user right-clicked on the parent '..' folder
	 * @param markedFiles
	 *            list of marked files, can be empty but never null
	 */
	public TablePopupMenu(MainFrame mainFrame, AbstractFile currentFolder, AbstractFile clickedFile, boolean parentFolderClicked, FileSet markedFiles) {
		this.mainFrame = mainFrame;

		pagaVcsMenu(currentFolder, clickedFile, parentFolderClicked, markedFiles);

		// 'Open' displayed if a single file was clicked
		if (clickedFile != null || parentFolderClicked) {
			addAction(com.mucommander.ui.action.impl.OpenAction.Descriptor.ACTION_ID);
			addAction(com.mucommander.ui.action.impl.OpenNativelyAction.Descriptor.ACTION_ID);

			// Creates the 'Open with...' menu.
			add(new OpenWithMenu(mainFrame));
		}

		// 'Rename' displayed if a single file was clicked
		if (clickedFile != null)
			addAction(com.mucommander.ui.action.impl.RenameAction.Descriptor.ACTION_ID);

		// 'Reveal in desktop' displayed only if clicked file is a local file
		// and the OS is capable of doing this
		if (DesktopManager.canOpenInFileManager(currentFolder))
			addAction(com.mucommander.ui.action.impl.RevealInDesktopAction.Descriptor.ACTION_ID);

		add(new JSeparator());

		// 'Copy name(s)' and 'Copy path(s)' are displayed only if a single file
		// was clicked or files are marked
		if (clickedFile != null || markedFiles.size() > 0) {
			addAction(com.mucommander.ui.action.impl.CopyFilesToClipboardAction.Descriptor.ACTION_ID);
			addAction(com.mucommander.ui.action.impl.CopyFileNamesAction.Descriptor.ACTION_ID);
			addAction(com.mucommander.ui.action.impl.CopyFilePathsAction.Descriptor.ACTION_ID);

			add(new JSeparator());
		}

		// Those following items are displayed in all cases
		addAction(com.mucommander.ui.action.impl.MarkAllAction.Descriptor.ACTION_ID);
		addAction(com.mucommander.ui.action.impl.UnmarkAllAction.Descriptor.ACTION_ID);
		addAction(com.mucommander.ui.action.impl.MarkSelectedFileAction.Descriptor.ACTION_ID);

		add(new JSeparator());
		addAction(com.mucommander.ui.action.impl.DeleteAction.Descriptor.ACTION_ID);

		add(new JSeparator());
		addAction(com.mucommander.ui.action.impl.ShowFilePropertiesAction.Descriptor.ACTION_ID);
		addAction(com.mucommander.ui.action.impl.ChangePermissionsAction.Descriptor.ACTION_ID);
		addAction(com.mucommander.ui.action.impl.ChangeDateAction.Descriptor.ACTION_ID);
	}

	/**
	 * Adds the MuAction denoted by the given ID to this popup menu, as a
	 * <code>JMenuItem</code>.
	 * <p>
	 * No icon will be displayed, regardless of whether the action has one or
	 * not.
	 * </p>
	 * <p>
	 * If the action has a keyboard shortcut that conflicts with the menu's
	 * internal ones (enter, space and escape), they will not be used.
	 * </p>
	 * 
	 * @param actionId
	 *            action ID
	 */
	private void addAction(String actionId) {
		JMenuItem item;
		KeyStroke stroke;

		item = add(ActionManager.getActionInstance(actionId, mainFrame));
		item.setIcon(null);

		stroke = item.getAccelerator();
		if (stroke != null)
			if (stroke.getModifiers() == 0
			        && (stroke.getKeyCode() == KeyEvent.VK_ENTER || stroke.getKeyCode() == KeyEvent.VK_SPACE || stroke.getKeyCode() == KeyEvent.VK_ESCAPE))
				item.setAccelerator(null);
	}
}