package hu.pagavcs.mug.gnome;

import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JMenu;

import com.mucommander.extensions.ContextMenuExtension;
import com.mucommander.extensions.ContextMenuExtensionPositions;
import com.mucommander.file.AbstractArchiveEntryFile;
import com.mucommander.file.AbstractFile;
import com.mucommander.file.FileProtocols;
import com.mucommander.file.util.FileSet;
import com.mucommander.ui.dialog.InformationDialog;
import com.mucommander.ui.main.MainFrame;
import com.mucommander.ui.main.menu.TablePopupMenu;

public class OpenWithExtension implements ContextMenuExtension {

	private HashMap<String, List<ExecSlot>> mapMimeApp;

	private static class ExecSlot {

		String exec;
		String name;
	}

	private void loadMimeTypes(File dir) {
		for (File liFile : dir.listFiles()) {
			if (liFile.isDirectory()) {
				loadMimeTypes(liFile);
			} else {
				try {
					String exec = null;
					String mimeTypes = null;
					String name = null;
					BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(liFile)));
					String line;
					while ((line = br.readLine()) != null) {
						if (line.toLowerCase().startsWith("exec=")) {
							exec = line.substring("exec=".length());
						} else if (line.toLowerCase().startsWith("mimetype=")) {
							mimeTypes = line.substring("mimetype=".length());
						} else if (line.toLowerCase().startsWith("name=")) {
							name = line.substring("name=".length());
						}
					}
					br.close();
					if (exec != null && mimeTypes != null && name != null) {
						for (String mime : mimeTypes.split("\\;")) {
							List<ExecSlot> lstExec = mapMimeApp.get(mime);
							if (lstExec == null) {
								lstExec = new ArrayList<ExecSlot>();
								mapMimeApp.put(mime, lstExec);
							}
							ExecSlot slot = new ExecSlot();
							slot.exec = exec;
							slot.name = name;
							lstExec.add(slot);
						}
					}
				} catch (IOException ex) {
					// ignoring...
				}
			}
		}
	}

	private synchronized void loadAllMimeTypes() {
		if (mapMimeApp == null) {
			mapMimeApp = new HashMap<String, List<ExecSlot>>();
			loadMimeTypes(new File("/usr/share/applications/"));
		}
	}

	public int getPriority() {
		return 10;
	}

	public ContextMenuExtensionPositions getPosition() {
		return ContextMenuExtensionPositions.CAN_OPEN_IN_FILE_MANAGER;
	}

	public void addMenu(TablePopupMenu tablePopupMenu, MainFrame mainFrame, AbstractFile currentFolder, AbstractFile clickedFile, boolean parentFolderClicked,
	        FileSet markedFiles) {
		try {
			if (clickedFile != null && currentFolder.getURL().getScheme().equals(FileProtocols.FILE) && !currentFolder.isArchive()
			        && !currentFolder.hasAncestor(AbstractArchiveEntryFile.class)) {

				ProcessBuilder pb = new ProcessBuilder("xdg-mime", "query", "filetype", clickedFile.getPath());
				Process process = pb.start();
				process.waitFor();
				BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
				StringBuilder sb = new StringBuilder();
				String line;
				while ((line = br.readLine()) != null) {
					sb.append(line);
				}
				String mimeType = sb.toString();

				loadAllMimeTypes();

				List<ExecSlot> lstExec = mapMimeApp.get(mimeType);
				if (lstExec != null && !lstExec.isEmpty()) {
					JMenu subMenu = new JMenu("Open with gnome...");
					tablePopupMenu.add(subMenu);
					for (ExecSlot slot : lstExec) {
						subMenu.add(new OpenWithAction(mainFrame, slot, clickedFile));
					}
				}

				// tablePopupMenu.add(new OpenTerminalAction(clickedFile));
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private static class OpenWithAction extends AbstractAction {

		private final AbstractFile clickedFile;
		private final ExecSlot     slot;
		private final MainFrame    mainFrame;

		public OpenWithAction(MainFrame mainFrame, ExecSlot slot, AbstractFile clickedFile) {
			super(slot.name);
			this.mainFrame = mainFrame;
			this.slot = slot;
			this.clickedFile = clickedFile;
		}

		public void actionPerformed(ActionEvent e) {

			try {
				// fileName = fileName.replace(" ", "\\ ");
				List<String> lstCmd = new ArrayList<String>();
				for (String cmd : slot.exec.split("\\ ")) {
					if (cmd.equalsIgnoreCase("%u") || cmd.equalsIgnoreCase("%f")) {
						lstCmd.add(clickedFile.toString());
					} else {
						lstCmd.add(cmd);
					}
				}
				ProcessBuilder pb = new ProcessBuilder(lstCmd);
				pb.start();
			} catch (Exception ex) {
				InformationDialog.showErrorDialog(mainFrame);
			}
		}

	}

}
