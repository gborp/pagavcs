package hu.pagavcs.mug.comparefiles;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.Hashtable;

import javax.swing.KeyStroke;

import com.mucommander.file.AbstractFile;
import com.mucommander.file.impl.local.LocalFile;
import com.mucommander.file.util.FileSet;
import com.mucommander.ui.action.AbstractActionDescriptor;
import com.mucommander.ui.action.ActionCategories;
import com.mucommander.ui.action.ActionCategory;
import com.mucommander.ui.action.ActionFactory;
import com.mucommander.ui.action.InvokesDialog;
import com.mucommander.ui.action.MuAction;
import com.mucommander.ui.main.MainFrame;

public class CompareFilesAction extends MuAction implements InvokesDialog {

	public CompareFilesAction(MainFrame mainFrame, Hashtable<String, Object> properties) {
		super(mainFrame, properties);
	}

	// TODO display error messages
	@Override
	public void performAction() {
		try {
		AbstractFile file1 = null;
		AbstractFile file2 = null;
		FileSet activeSelectedFiles = mainFrame.getActiveTable().getSelectedFiles();
		if (activeSelectedFiles.size() > 2) {
			throw new RuntimeException("Can't compare more then 2 files");
		}
		if (activeSelectedFiles.size() == 2) {
			file1 = activeSelectedFiles.get(0);
				file2 = activeSelectedFiles.get(1);
		} else {
			file1 = mainFrame.getActiveTable().getSelectedFile();
			file2 = mainFrame.getInactiveTable().getSelectedFile();
		}
		if (file1 == null || file2 == null) {
			throw new RuntimeException("Cannot compare");
		}

		if (!file1.hasAncestor(LocalFile.class) || !file2.hasAncestor(LocalFile.class)) {
			throw new RuntimeException("Cannot compare non-local files");
		}

			ProcessBuilder pb = new ProcessBuilder("meld", file1.getAbsolutePath(), file2.getAbsolutePath());
			pb.start();
		} catch (IOException ex) {
			System.out.println(ex);
		}
	}

	public static class Factory implements ActionFactory {

		public MuAction createAction(MainFrame mainFrame, Hashtable<String, Object> properties) {
			return new CompareFilesAction(mainFrame, properties);
		}
	}

	public static class Descriptor extends AbstractActionDescriptor {

		public static final String ACTION_ID = "CompareFiles";

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
			return KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK | KeyEvent.ALT_DOWN_MASK);
		}
	}
}
