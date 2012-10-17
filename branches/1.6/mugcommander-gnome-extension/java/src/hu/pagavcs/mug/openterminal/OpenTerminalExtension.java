package hu.pagavcs.mug.openterminal;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import com.mucommander.extensions.ContextMenuExtension;
import com.mucommander.extensions.ContextMenuExtensionPositions;
import com.mucommander.file.AbstractArchiveEntryFile;
import com.mucommander.file.AbstractFile;
import com.mucommander.file.FileProtocols;
import com.mucommander.file.util.FileSet;
import com.mucommander.ui.dialog.InformationDialog;
import com.mucommander.ui.main.MainFrame;
import com.mucommander.ui.main.menu.TablePopupMenu;

public class OpenTerminalExtension implements ContextMenuExtension {

	public int getPriority() {
		return 0;
	}

	public ContextMenuExtensionPositions getPosition() {
		return ContextMenuExtensionPositions.CAN_OPEN_IN_FILE_MANAGER;
	}

	public void addMenu(TablePopupMenu tablePopupMenu, MainFrame mainFrame, AbstractFile currentFolder, AbstractFile clickedFile, boolean parentFolderClicked,
	        FileSet markedFiles) {

		if (currentFolder.getURL().getScheme().equals(FileProtocols.FILE) && !currentFolder.isArchive()
		        && !currentFolder.hasAncestor(AbstractArchiveEntryFile.class)) {
			tablePopupMenu.add(new OpenTerminalAction(mainFrame));
		}
	}

	private static class OpenTerminalAction extends AbstractAction {

		private final MainFrame mainFrame;

		public OpenTerminalAction(MainFrame mainFrame) {
			super("Open Terminal");
			this.mainFrame = mainFrame;
		}

		public void actionPerformed(ActionEvent e) {

			try {
				ProcessBuilder pb = new ProcessBuilder("gnome-terminal", "--working-directory", mainFrame.getActivePanel().getCurrentFolder().getAbsolutePath());
				pb.start();
			} catch (Exception ex) {
				InformationDialog.showErrorDialog(mainFrame);
			}
		}

	}

}
