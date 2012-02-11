package hu.pagavcs.mug.pagavcs;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import com.mucommander.extensions.FileEmblemExtension;
import com.mucommander.file.AbstractFile;
import com.mucommander.ui.icon.CustomFileIconProvider;
import com.mucommander.ui.main.MainFrame;
import com.mucommander.ui.main.WindowManager;
import com.mucommander.ui.main.table.FileTable;

import cx.ath.matthew.unix.UnixSocket;

public class PagaVcsFileEmblemExtension implements FileEmblemExtension {

	private static PagaVcsFileEmblemExtension singleton;

	private Object doPanelRefresh;

	private PagaVcsFileEmblemExtension() {
		doPanelRefresh = new Object();
		Thread refreshThread = new Thread(new RefreshPanels());
		refreshThread.setName("PagaVCS emblem refresh job");
		refreshThread.setDaemon(true);
		refreshThread.start();
	}

	public synchronized static PagaVcsFileEmblemExtension getInstance() {
		if (singleton == null) {
			singleton = new PagaVcsFileEmblemExtension();
		}
		return singleton;
	}

	public synchronized Icon getDecoratedAbstractFileIcon(Icon icon,
			AbstractFile file) {
		File f = new File(file.getAbsolutePath());
		if (f.exists()) {
			return getDecoratedFileIcon(icon, f);
		}
		return icon;
	}

	public synchronized Icon getDecoratedFileIcon(Icon icon, File file) {
		try {
			String status = null;
			synchronized (CustomFileIconProvider.class) {

				UnixSocket socket = PagaVcsIntegration.getSocket();
				String strOut = "getfileinfonl " + file.getPath() + "\n";
				BufferedReader br = new BufferedReader(new InputStreamReader(
						socket.getInputStream()));
				BufferedWriter outToClient = new BufferedWriter(
						new OutputStreamWriter(socket.getOutputStream()));
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
					ImageIcon overlayIcon = PagaVcsIntegration
							.getIcon("emblems/"
									+ status.substring("pagavcs-".length()));

					if (overlayIcon != null) {
						BufferedImage bi = new BufferedImage(
								icon.getIconWidth(), icon.getIconHeight(),
								BufferedImage.TYPE_INT_ARGB);

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

	private class RefreshPanels implements Runnable {

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

}
