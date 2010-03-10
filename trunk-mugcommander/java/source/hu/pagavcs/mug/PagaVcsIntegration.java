package hu.pagavcs.mug;

import java.awt.Graphics;
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
import java.util.HashMap;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import com.mucommander.ui.icon.CustomFileIconProvider;
import com.mucommander.ui.main.MainFrame;
import com.mucommander.ui.main.WindowManager;
import com.mucommander.ui.main.table.FileTable;

public class PagaVcsIntegration {

	private static HashMap<String, ImageIcon> mapPagaVcsIcon;

	private static void initOnePagaVcsIcon(String name, int width, int height) throws UnknownHostException, IOException, ClassNotFoundException {
		Socket socket;
		String strOut = "getemblem " + name + " " + width + " " + height + "\n";

		socket = new Socket("localhost", 12905);

		BufferedWriter outToClient = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
		outToClient.write(strOut);
		outToClient.flush();
		ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
		ImageIcon imageIcon = (ImageIcon) input.readObject();

		mapPagaVcsIcon.put(name, imageIcon);
	}

	private static void initPagaVcsIcon(int width, int height) throws UnknownHostException, IOException, ClassNotFoundException {
		if (mapPagaVcsIcon == null) {
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
					Thread.sleep(500);
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

	public static synchronized Icon getSvnDecoratedFileIcon(Icon icon, File file) {

		try {
			String status = null;
			synchronized (CustomFileIconProvider.class) {

				initPagaVcsIcon(8, 8);

				Socket socket = new Socket("localhost", 12905);
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

		} catch (UnknownHostException ex) {
			ex.printStackTrace();
		} catch (IOException ex) {
			ex.printStackTrace();
		} catch (ClassNotFoundException ex) {
			ex.printStackTrace();
		}
		return icon;

	}
}
