package hu.pagavcs.mug.pagavcs;

import hu.pagavcs.mug.findfile.RealFileProvider;

import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JSeparator;

import com.mucommander.extensions.ContextMenuExtension;
import com.mucommander.extensions.ContextMenuExtensionPositions;
import com.mucommander.file.AbstractArchiveEntryFile;
import com.mucommander.file.AbstractFile;
import com.mucommander.file.ArchiveEntry;
import com.mucommander.file.util.FileSet;
import com.mucommander.ui.main.MainFrame;
import com.mucommander.ui.main.menu.TablePopupMenu;

public class PagaVcsContextMenuExtension implements ContextMenuExtension {

	public int getPriority() {
		return 0;
	}

	public ContextMenuExtensionPositions getPosition() {
		return ContextMenuExtensionPositions.TOP;
	}

	public void addMenu(TablePopupMenu tablePopupMenu, MainFrame mainFrame, AbstractFile currentFolder, AbstractFile clickedFile, boolean parentFolderClicked,
	        FileSet markedFiles) {
		try {
			ArrayList<String> lstFile = new ArrayList<String>();

			if (clickedFile == null) {
				String absPath = getRealFile(currentFolder).getAbsolutePath();
				if (!new File(absPath).exists()) {
					return;
				}
				lstFile.add(absPath);
			} else {

				if (!markedFiles.contains(clickedFile)) {
					markedFiles = new FileSet();
					markedFiles.add(clickedFile);
				}

				for (AbstractFile li : markedFiles) {
					String absPath = getRealFile(li).getAbsolutePath();
					if (!new File(absPath).exists()) {
						return;
					}
					lstFile.add(absPath);
				}
			}

			if (lstFile.isEmpty()) {
				return;
			}

			StringBuilder sb = new StringBuilder();
			for (String file : lstFile) {
				sb.append('"');
				sb.append(file);
				sb.append('"');
			}
			String fileParamsString = sb.toString();

			Socket socket = PagaVcsIntegration.getSocket();
			String strOut = "getmenuitems " + fileParamsString + "\n";
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
					String iconName = br.readLine();
					String hints = br.readLine();
					String command = br.readLine();

					if (hints.contains("s")) {
						menuPagaVcs.add(new JSeparator());
					}

					ImageIcon icon = PagaVcsIntegration.getIcon("actions/" + iconName);
					PagaVcsAction action = new PagaVcsAction(label, icon, command + " " + fileParamsString + "\n");
					if (hints.contains("p")) {
						tablePopupMenu.add(action);
					} else {
						menuPagaVcs.add(action);
					}

					hasMenuItem = true;
				}
			}
			socket.close();
			if (hasMenuItem) {
				tablePopupMenu.add(menuPagaVcs);
				tablePopupMenu.add(new JSeparator());
			}
		} catch (UnknownHostException ex) {
			ex.printStackTrace();
		} catch (IOException ex) {
			ex.printStackTrace();
		} catch (ClassNotFoundException ex) {
			ex.printStackTrace();
		}
	}

	private static AbstractFile getRealFile(AbstractFile file) {
		if (file instanceof AbstractArchiveEntryFile) {
			AbstractArchiveEntryFile a = ((AbstractArchiveEntryFile) file);
			ArchiveEntry entry = a.getEntry();
			if (entry instanceof RealFileProvider) {
				AbstractFile realFile = ((RealFileProvider) entry).getRealFile();
				return realFile;
			}
		}
		return file;
	}

	private static class PagaVcsAction extends AbstractAction {

		private final String command;

		public PagaVcsAction(String label, Icon icon, String command) {
			super(label, icon);
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

}
