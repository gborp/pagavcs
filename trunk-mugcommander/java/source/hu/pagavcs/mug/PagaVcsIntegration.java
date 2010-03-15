package hu.pagavcs.mug;

import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JSeparator;

import com.mucommander.file.AbstractArchiveEntryFile;
import com.mucommander.file.AbstractFile;
import com.mucommander.file.ArchiveEntry;
import com.mucommander.file.util.FileSet;
import com.mucommander.ui.icon.CustomFileIconProvider;
import com.mucommander.ui.main.MainFrame;
import com.mucommander.ui.main.WindowManager;
import com.mucommander.ui.main.menu.TablePopupMenu;
import com.mucommander.ui.main.table.FileTable;

public class PagaVcsIntegration {

	private static boolean                    initialized;
	private static HashMap<String, ImageIcon> mapPagaVcsIcon;

	private static Socket getSocket() throws IOException {
		try {
			Socket socket = new Socket("localhost", 12905);
			// 10 secundum
			socket.setSoTimeout(10 * 1000);
			return socket;
		} catch (Exception ex) {
			return createAndGetSocket();
		}
	}

	private static Socket createAndGetSocket() throws IOException {
		Runtime.getRuntime().exec("/usr/bin/pagavcs ping");
		try {
			Thread.sleep(500);
		} catch (InterruptedException ex) {}
		return new Socket("localhost", 12905);
	}

	private static void initOnePagaVcsIcon(String name, int width, int height) throws UnknownHostException, IOException, ClassNotFoundException {
		String strOut = "getemblem " + name + " " + width + " " + height + "\n";

		Socket socket = getSocket();

		BufferedWriter outToClient = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
		outToClient.write(strOut);
		outToClient.flush();
		ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
		ImageIcon imageIcon = (ImageIcon) input.readObject();

		mapPagaVcsIcon.put(name, imageIcon);
	}

	private static void initPagaVcsIcon(int width, int height) throws UnknownHostException, IOException, ClassNotFoundException {
		if (!initialized) {
			mapPagaVcsIcon = new HashMap<String, ImageIcon>();
			initOnePagaVcsIcon("added", width, height);
			initOnePagaVcsIcon("conflict", width, height);
			initOnePagaVcsIcon("deleted", width, height);
			initOnePagaVcsIcon("locked", width, height);
			initOnePagaVcsIcon("modified", width, height);
			initOnePagaVcsIcon("normal", width, height);
			initOnePagaVcsIcon("obstructed", width, height);
			initOnePagaVcsIcon("readonly", width, height);
			initOnePagaVcsIcon("svn", width, height);
			initOnePagaVcsIcon("unversioned", width, height);
			initialized = true;
		}
	}

	static {
		doPanelRefresh = new Object();
		Thread refreshThread = new Thread(new RefreshPanels());
		refreshThread.setName("PagaVCS emblem refresh job");
		refreshThread.setDaemon(true);
		refreshThread.start();
	}

	private static Object doPanelRefresh;

	private static class RefreshPanels implements Runnable {

		public void run() {

			try {

				while (true) {
					synchronized (doPanelRefresh) {
						doPanelRefresh.wait();
					}
					Thread.sleep(100);
					MainFrame mainFrame = WindowManager.getCurrentMainFrame();
					if (mainFrame != null) {
						FileTable activeTable = mainFrame.getActiveTable();
						FileTable inactiveTable = mainFrame.getInactiveTable();
						activeTable.getFolderPanel().getFileTable().repaint();
						inactiveTable.getFolderPanel().getFileTable().repaint();
					}
				}
			} catch (InterruptedException ex) {
				ex.printStackTrace();
			}

		}

	}

	public static synchronized Icon getSvnDecoratedFileIcon(Icon icon, AbstractFile file) {
		File f = new File(file.getAbsolutePath());
		if (f.exists()) {
			return getSvnDecoratedFileIcon(icon, f);
		}
		return icon;
	}

	public static synchronized Icon getSvnDecoratedFileIcon(Icon icon, File file) {

		try {
			String status = null;
			synchronized (CustomFileIconProvider.class) {

				initPagaVcsIcon(8, 8);

				Socket socket = getSocket();
				String strOut = "getfileinfonl " + file.getPath() + "\n";
				BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				BufferedWriter outToClient = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
				outToClient.write(strOut);
				outToClient.flush();

				status = br.readLine();
				socket.close();
			}

			if (status != null && status.startsWith("pagavcs-")) {

				if (status.equals("pagavcs-unknown")) {
					synchronized (doPanelRefresh) {
						doPanelRefresh.notifyAll();
					}
				} else {
					ImageIcon overlayIcon = mapPagaVcsIcon.get(status.substring("pagavcs-".length()));

					if (overlayIcon != null) {
						BufferedImage bi = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);

						Graphics g = bi.getGraphics();
						icon.paintIcon(null, g, 0, 0);
						overlayIcon.paintIcon(null, g, 0, 8);

						icon = new ImageIcon(bi);
					}
				}
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return icon;

	}

	private static AbstractFile getRealFile(AbstractFile file) {
		if (file instanceof AbstractArchiveEntryFile) {
			AbstractArchiveEntryFile a = ((AbstractArchiveEntryFile) file);
			ArchiveEntry entry = a.getEntry();
			if (entry instanceof FindFileArchiveEntry) {
				AbstractFile realFile = ((FindFileArchiveEntry) entry).getRealFile();
				return realFile;
			}
		}
		return file;
	}

	public static void pagaVcsMenu(TablePopupMenu tablePopupMenu, AbstractFile currentFolder, AbstractFile clickedFile, boolean parentFolderClicked,
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

			Socket socket = getSocket();
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
					br.readLine();
					br.readLine();
					String command = br.readLine();

					menuPagaVcs.add(new PagaVcsAction(label, command + " " + fileParamsString + "\n"));
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
		}

	}

	private static class PagaVcsAction extends AbstractAction {

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

}
