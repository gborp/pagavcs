package com.mucommander.ui.action.impl;

import hu.pagavcs.mug.findfile.RealFileProvider;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.Hashtable;

import javax.swing.KeyStroke;

import com.mucommander.file.AbstractFile;
import com.mucommander.file.ArchiveEntry;
import com.mucommander.file.ROArchiveEntryFile;
import com.mucommander.ui.action.AbstractActionDescriptor;
import com.mucommander.ui.action.ActionCategories;
import com.mucommander.ui.action.ActionCategory;
import com.mucommander.ui.action.ActionFactory;
import com.mucommander.ui.action.MuAction;
import com.mucommander.ui.main.MainFrame;
import com.mucommander.ui.main.table.FileTable;

public class EditNativelyAction extends SelectedFileAction {

	public EditNativelyAction(MainFrame mainFrame, Hashtable<String, Object> properties) {
		super(mainFrame, properties);
	}

	public void performAction() {
		FileTable activeTable = mainFrame.getActiveTable();
		AbstractFile selectedFile = activeTable.getSelectedFile(false, false);

		if (selectedFile instanceof ROArchiveEntryFile) {
			ArchiveEntry entry = ((ROArchiveEntryFile) selectedFile).getEntry();
			if (entry instanceof RealFileProvider) {
				selectedFile = ((RealFileProvider) entry).getRealFile();
			}
		}

		if (selectedFile != null) {
			try {
				ProcessBuilder pb = new ProcessBuilder("gedit", selectedFile.getAbsolutePath());
				pb.start();
			} catch (IOException ex) {

			}
		}

	}

	// - Factory
	// -------------------------------------------------------------------------------------------------------
	// -----------------------------------------------------------------------------------------------------------------
	public static class Factory implements ActionFactory {

		public MuAction createAction(MainFrame mainFrame, Hashtable<String, Object> properties) {
			return new EditNativelyAction(mainFrame, properties);
		}
	}

	public static class Descriptor extends AbstractActionDescriptor {

		public static final String ACTION_ID = "EditNatively";

		public String getId() {
			return ACTION_ID;
		}

		public ActionCategory getCategory() {
			return ActionCategories.FILES;
		}

		public KeyStroke getDefaultAltKeyStroke() {
			return null;
		}

		public KeyStroke getDefaultKeyStroke() {
			return KeyStroke.getKeyStroke(KeyEvent.VK_F4, KeyEvent.SHIFT_DOWN_MASK);
		}
	}

}
