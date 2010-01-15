package hu.pagavcs.bl;

import hu.pagavcs.Communication;
import hu.pagavcs.bl.PagaException.PagaExceptionType;
import hu.pagavcs.gui.LoginGui;
import hu.pagavcs.gui.MessagePane;
import hu.pagavcs.operation.ContentStatus;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.prefs.BackingStoreException;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNConflictChoice;
import org.tmatesoft.svn.core.wc.SVNInfo;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNWCClient;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

/**
 * PagaVCS is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.<br>
 * <br>
 * PagaVCS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.<br>
 * <br>
 * You should have received a copy of the GNU General Public License along with
 * PagaVCS; If not, see http://www.gnu.org/licenses/.
 */
public class Manager {

	public static final long        REVALIDATE_DELAY = 500;

	public static Icon              ICON_ERROR       = Manager.loadIcon("/hu/pagavcs/resources/dialog-error.png");
	public static Icon              ICON_INFORMATION = Manager.loadIcon("/hu/pagavcs/resources/dialog-information.png");
	public static Icon              ICON_PASSWORD    = Manager.loadIcon("/hu/pagavcs/resources/dialog-password.png");
	public static Icon              ICON_QUESTION    = Manager.loadIcon("/hu/pagavcs/resources/dialog-question.png");
	public static Icon              ICON_WARNING     = Manager.loadIcon("/hu/pagavcs/resources/dialog-warning.png");

	private static final Color      COLOR_PURPLE     = new Color(100, 0, 100);

	private static String           tempDir;
	private static boolean          inited           = false;
	private static ExceptionHandler exceptionHandler;

	public static void init() throws BackingStoreException {
		if (!inited) {
			SVNRepositoryFactoryImpl.setup();
			DAVRepositoryFactory.setup();
			FileRevisionCache.getInstance().init();
			inited = true;
			getSettings().load();
			Runtime.getRuntime().addShutdownHook(new Thread(new ShutDownRunnable()));
			exceptionHandler = new ExceptionHandler();
			Thread.setDefaultUncaughtExceptionHandler(exceptionHandler);
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (Exception e) {}
		}
	}

	public static String getApplicationRootName() {
		return "PagaVCS";
	}

	public static String getTempDir() {
		if (tempDir == null) {
			tempDir = System.getProperty("java.io.tmpdir") + "/pagavcs/";
			new File(tempDir).mkdirs();
		}
		return tempDir;
	}

	public static SVNClientManager getSVNClientManagerForWorkingCopyOnly() {
		return SVNClientManager.newInstance();
	}

	public static SVNClientManager getSVNClientManager(File path) throws SVNException, PagaException {
		return getSVNClientManager(getSVNClientManagerForWorkingCopyOnly().getWCClient().doInfo(path, SVNRevision.WORKING).getRepositoryRootURL());
	}

	public static SVNClientManager getSVNClientManager(SVNURL repositoryUrl) throws SVNException, PagaException {

		String repoid = repositoryUrl.getHost() + ":" + repositoryUrl.getPort();

		SVNClientManager result = null;
		boolean reTryLogin = false;
		while (result == null) {

			String username = getSettings().getUsername(repoid);
			String password = getSettings().getPassword(repoid);
			if (password == null || username == null || "".equals(username) || "".equals(password) || reTryLogin) {
				final LoginGui loginGui = new LoginGui(username, password);
				SwingUtilities.invokeLater(new Runnable() {

					public void run() {
						loginGui.display();
					}

				});

				boolean doLogin = loginGui.waitForLoginButton();
				if (!doLogin) {
					return null;
				}
				username = loginGui.getPredefinedUsername();
				password = loginGui.getPredefinedPassword();
				getSettings().setUsername(repoid, username);
				getSettings().setPassword(repoid, password);
			}

			ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(username, password);
			// ISVNOptions options = SVNWCUtil.createDefaultOptions(true);
			result = SVNClientManager.newInstance(null, authManager);
			reTryLogin = false;
			try {
				result.getRepositoryPool().createRepository(repositoryUrl, false).testConnection();
			} catch (SVNException ex) {
				ex.printStackTrace();
				// if (ex.getErrorMessage().getType()== 0) {
				//				 
				// }
				result = null;
				reTryLogin = true;
			}
		}
		if (result == null) {
			throw new PagaException(PagaExceptionType.LOGIN_FAILED);
		}
		return result;
	}

	/**
	 * If a string is on the system clipboard, this method returns it; otherwise
	 * it returns null.
	 */
	public static String getClipboard() {
		Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);

		try {
			if (t != null && t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
				String text = (String) t.getTransferData(DataFlavor.stringFlavor);
				return text;
			}
		} catch (UnsupportedFlavorException e) {} catch (IOException e) {}
		return null;
	}

	/**
	 * This method writes a string to the system clipboard.
	 */
	public static void setClipboard(String str) {
		StringSelection ss = new StringSelection(str);
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);
	}

	public static Long getPreviousRevisionNumber(SVNURL path, long revision) throws SVNException, PagaException {
		PreviousRevisionFetcher fetcher = new PreviousRevisionFetcher();
		fetcher.execute(path, revision);
		return fetcher.getPreviousRevision();
	}

	public static SVNInfo getInfo(File path) throws SVNException {
		SVNClientManager mgrSvn = Manager.getSVNClientManagerForWorkingCopyOnly();
		SVNWCClient wcClient = mgrSvn.getWCClient();
		return wcClient.doInfo(path, SVNRevision.WORKING);
	}

	private static void centerScreen(Window window) {
		Dimension dim = window.getToolkit().getScreenSize();
		Rectangle bounds = window.getBounds();
		window.setLocation((dim.width - bounds.width) / 2, (dim.height - bounds.height) / 2);
	}

	public static Icon loadIcon(String path) {
		return new ImageIcon(Toolkit.getDefaultToolkit().getImage(Manager.class.getResource(path)));
	}

	public static Window createAndShowFrame(JComponent main, String applicationName) {

		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.getContentPane().add(main);
		frame.setTitle(applicationName + " [" + Manager.getApplicationRootName() + "]");
		frame.setIconImage(Toolkit.getDefaultToolkit().getImage(Manager.class.getResource("/hu/pagavcs/resources/icon.png")));
		frame.pack();

		Rectangle bounds = getSettings().getWindowBounds(applicationName);
		if (bounds != null) {
			frame.setBounds(bounds);
		} else {
			centerScreen(frame);
		}

		frame.setVisible(true);
		frame.addWindowListener(new WindowPreferencesSaverOnClose(applicationName));
		frame.addWindowListener(new WindowAdapter() {

			public void windowClosed(WindowEvent e) {
				try {
					getSettings().save();
				} catch (Exception ex) {
					Manager.handle(ex);
				}
			}
		});

		return frame;
	}

	public static JDialog createDialog(Window parent, JComponent main, String title) {
		JDialog dialog = new JDialog(parent);
		dialog.getContentPane().add(main);
		dialog.setTitle(title);
		dialog.setIconImage(Toolkit.getDefaultToolkit().getImage(Manager.class.getResource("/hu/pagavcs/resources/icon.png")));
		dialog.pack();

		Rectangle bounds = getSettings().getWindowBounds(title);
		if (bounds != null) {
			dialog.setBounds(bounds);
		} else {
			centerScreen(dialog);
		}

		return dialog;
	}

	public static JFrame getRootFrame() {
		return null;
	}

	public static SettingsStore getSettings() {
		return SettingsStore.getInstance();
	}

	public static int getMaxUrlHistoryItems() {
		// TODO set it from preferences
		return 20;
	}

	public static int getMaxMessageHistoryItems() {
		// TODO set it from preferences
		return 20;
	}

	public static void handle(Exception ex) {
		exceptionHandler.handle(ex);
	}

	public static void shutdown() throws Exception {
		getSettings().save();
		Communication.getInstance().shutdown();
	}

	private static class ShutDownRunnable implements Runnable {

		public void run() {
			try {
				shutdown();
			} catch (Exception e) {
				Manager.handle(e);
			}
		}

	}

	public static void resolveConflictUsingTheirs(String path) throws SVNException {
		getSVNClientManagerForWorkingCopyOnly().getWCClient().doResolve(new File(path), SVNDepth.INFINITY, SVNConflictChoice.THEIRS_FULL);
	}

	public static void resolveConflictUsingMine(String path) throws SVNException {
		getSVNClientManagerForWorkingCopyOnly().getWCClient().doResolve(new File(path), SVNDepth.INFINITY, SVNConflictChoice.MINE_FULL);
	}

	public static SVNInfo getInfo(String path) throws SVNException {
		SVNInfo info = getSVNClientManagerForWorkingCopyOnly().getWCClient().doInfo(new File(path), SVNRevision.WORKING);
		return info;
	}

	public static Color getColorByContentStatus(ContentStatus status) {
		Color color = null;
		switch (status) {
			case ADDED:
				color = COLOR_PURPLE;
				break;
			case CONFLICTED:
				color = Color.RED;
				break;
			case DELETED:
				color = Color.RED;
				break;
			case EXTERNAL:
				color = Color.DARK_GRAY;
				break;
			case IGNORED:
				color = Color.ORANGE;
				break;
			case INCOMPLETE:
				color = Color.RED;
				break;
			case MERGED:
				color = Color.GREEN;
				break;

			case MISSING:
				color = Color.RED;
				break;
			case MODIFIED:
				color = Color.BLUE;
				break;
			case NONE:
				color = Color.BLACK;
				break;
			case NORMAL:
				color = Color.BLACK;
				break;
			case OBSTRUCTED:
				color = Color.RED;
				break;
			case REPLACED:
				color = Color.RED;
				break;
			case UNVERSIONED:
				color = Color.BLACK;
				break;
		}

		return color;
	}

	public static File getFile(SVNURL svnUrl, SVNRevision revision) throws Exception {
		return FileRevisionCache.getInstance().getFile(svnUrl, revision);
	}

	public static void releaseFile(SVNURL svnUrl, SVNRevision revision) throws Exception {
		FileRevisionCache.getInstance().releaseFile(svnUrl, revision);
	}

	public static File getWorkingCopyFile(File wcFile) throws Exception {
		return FileRevisionCache.getInstance().getWorkingCopyFile(wcFile);
	}

	public static void releaseWorkingCopyFile(File wcFile) throws Exception {
		FileRevisionCache.getInstance().releaseWorkingCopyFile(wcFile);
	}

	public static void showFailedDialog() {
		MessagePane.showError(null, "Failed", "Failed");
	}

}
