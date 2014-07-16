package hu.pagavcs.client.bl;

import hu.pagavcs.Communication;
import hu.pagavcs.client.bl.PagaException.PagaExceptionType;
import hu.pagavcs.client.gui.LoginGui;
import hu.pagavcs.client.gui.platform.GuiHelper;
import hu.pagavcs.client.operation.ContentStatus;
import hu.pagavcs.common.ResourceBundleAccessor;
import hu.pagavcs.server.FileStatusCache;

import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.prefs.BackingStoreException;

import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.dav.http.DefaultHTTPConnectionFactory;
import org.tmatesoft.svn.core.internal.io.dav.http.IHTTPConnectionFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.internal.util.SVNSocketFactory;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNMerger;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNOptions;
import org.tmatesoft.svn.core.internal.wc.SVNConflictVersion;
import org.tmatesoft.svn.core.internal.wc.SVNDiffConflictChoiceStyle;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc.admin.SVNAdminArea;
import org.tmatesoft.svn.core.internal.wc.admin.SVNLog;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.ISVNConflictHandler;
import org.tmatesoft.svn.core.wc.ISVNMerger;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNConflictChoice;
import org.tmatesoft.svn.core.wc.SVNDiffOptions;
import org.tmatesoft.svn.core.wc.SVNInfo;
import org.tmatesoft.svn.core.wc.SVNMergeFileSet;
import org.tmatesoft.svn.core.wc.SVNMergeResult;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNWCClient;
import org.tmatesoft.svn.core.wc.SVNWCUtil;
import org.tmatesoft.svn.core.wc2.ISvnMerger;
import org.tmatesoft.svn.core.wc2.SvnMergeResult;
import org.tmatesoft.svn.util.SVNDebugLog;

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

	static enum SslLoginType {
		DEFAULT, SSLv2Hello_SSLv3
	}

	public static final long REVALIDATE_DELAY = 500;
	public static final long TABLE_RESIZE_DELAY = 50;

	public static final String MELD = "meld";
	public static final String GEDIT = "gedit";

	private static Icon ICON_ERROR;
	private static Icon ICON_INFORMATION;
	private static Icon ICON_PASSWORD;
	private static Icon ICON_QUESTION;
	private static Icon ICON_WARNING;

	public static final String COMMIT_COMPLETED_TEMPLATE_SEPARATOR = ">>>";

	private static final Color COLOR_PURPLE = new Color(100, 0, 100);
	private static final Color COLOR_GREEN = new Color(20, 80, 25);

	private static File tempFileDirectory;

	private static String tempDir;
	private static boolean inited = false;
	private static ExceptionHandler exceptionHandler;
	private static boolean forceShowingLoginDialogNextTime;
	private static HashMap<String, SslLoginType> mapSslLoginType = new HashMap<String, SslLoginType>();

	private static boolean shutdown;

	private static BandwidthMeter bandwidthMeter;

	public static void init() throws BackingStoreException,
			GeneralSecurityException, IOException {
		if (!inited) {

			System.setProperty("svnkit.library.gnome-keyring.enabled", "false");

			bandwidthMeter = new BandwidthMeter();
			SVNDebugLog.setDefaultLog(bandwidthMeter);
			SVNRepositoryFactoryImpl.setup();
			SVNFileUtil.setSleepForTimestamp(false);
			// Enable full HTTP request spooling to prevent "svn: REPORT request
			// failed on '/svn/VSMRepo/!svn/vcc/default'"
			// http://old.nabble.com/REPORT-request-failed-accessing-Sourceforge-Subversion-td14733189.html
			IHTTPConnectionFactory factory = new DefaultHTTPConnectionFactory(
					null, true, null);
			DAVRepositoryFactory.setup(factory);
			FSRepositoryFactory.setup();
			tempFileDirectory = new File(Manager.getTempDir() + "f/");
			tempFileDirectory.mkdirs();
			inited = true;
			getSettings().load();
			Runtime.getRuntime().addShutdownHook(
					new Thread(new ShutDownRunnable()));
			exceptionHandler = new ExceptionHandler();
			Thread.setDefaultUncaughtExceptionHandler(exceptionHandler);
			GuiHelper.initGui();
		}
	}

	public static BandwidthMeter getBandwidthMeter() {
		return bandwidthMeter;
	}

	public static String getApplicationRootName() {
		return "PagaVCS";
	}

	public static String getTempDir() {
		if (tempDir == null) {
			tempDir = System.getProperty("java.io.tmpdir") + "/"
					+ System.getProperty("user.name") + "/pagavcs/";
			new File(tempDir).mkdirs();
			deleteAll(new File(tempDir));
			new File(tempDir).mkdirs();
		}
		return tempDir;
	}

	private static void deleteAll(File directory) {
		File[] lstFilesW = directory.listFiles();
		if (lstFilesW != null) {
			for (File f : lstFilesW) {
				f.setWritable(true);
				if (f.isDirectory()) {
					deleteAll(f);
				} else {
					f.delete();
				}
			}
		}
		directory.delete();
	}

	public static File getAbsoluteFile(String path, String relativeUrl)
			throws SVNException, PagaException {

		ArrayList<String> lstPath = new ArrayList<String>(Arrays.asList(path
				.split("/")));
		ArrayList<String> lstRelative = new ArrayList<String>(
				Arrays.asList(relativeUrl.split("/")));
		if (lstRelative.get(0).isEmpty()) {
			lstRelative.remove(0);
		}

		int offset1 = lstPath.size() - lstRelative.size();

		boolean success = false;

		while (!success && offset1 < lstPath.size()) {
			success = true;
			for (int i = 0; i < lstRelative.size()
					&& (offset1 + i) < lstPath.size(); i++) {
				if (!lstPath.get(i + offset1).equals(lstRelative.get(i))) {
					success = false;
					break;
				}
			}
			if (!success) {
				offset1++;
			}
		}
		lstPath.subList(offset1, lstPath.size()).clear();
		lstPath.addAll(lstRelative);

		StringBuilder sbResult = new StringBuilder();
		for (String li : lstPath) {
			sbResult.append(li);
			sbResult.append('/');
		}

		return new File(sbResult.toString());
	}

	public static SVNURL getAbsoluteUrl(SVNURL path, String relativeUrl,
			SVNRevision pegRevision, SVNRevision revision) throws SVNException,
			PagaException {

		SVNClientManager mgr = Manager.getSVNClientManager(path);
		try {
			SVNURL rootUrl = mgr.getWCClient()
					.doInfo(path, pegRevision, revision).getRepositoryRootURL();
			return rootUrl.appendPath(relativeUrl, true);
		} finally {
			mgr.dispose();
		}
	}

	public static SVNURL getAbsoluteUrl(SVNURL path, String relativeUrl)
			throws SVNException, PagaException {

		SVNClientManager mgr = Manager.getSVNClientManager(path);
		try {
			SVNURL rootUrl = mgr.getWCClient()
					.doInfo(path, SVNRevision.UNDEFINED, SVNRevision.UNDEFINED)
					.getRepositoryRootURL();
			return rootUrl.appendPath(relativeUrl, true);
		} finally {
			mgr.dispose();
		}
	}

	public static SVNClientManager getSVNClientManagerForWorkingCopyOnly() {
		return SVNClientManager.newInstance();
	}

	public static SVNURL getSvnUrlByFile(File path) throws SVNException {
		SVNClientManager mgrSvn = getSVNClientManagerForWorkingCopyOnly();
		try {
			return mgrSvn.getWCClient().doInfo(path, SVNRevision.WORKING)
					.getURL();
		} finally {
			mgrSvn.dispose();
		}
	}

	public static SVNURL getSvnRootUrlByFile(File path) throws SVNException {
		SVNClientManager mgrSvn = getSVNClientManagerForWorkingCopyOnly();
		try {
			return mgrSvn.getWCClient().doInfo(path, SVNRevision.WORKING)
					.getRepositoryRootURL();
		} finally {
			mgrSvn.dispose();
		}
	}

	public static SVNClientManager getSVNClientManager(File path)
			throws SVNException, PagaException {
		return getSVNClientManager(getSvnRootUrlByFile(path));
	}

	public static synchronized SVNClientManager getSVNClientManager(
			SVNURL repositoryUrl) throws SVNException, PagaException {

		String repoid = repositoryUrl.getHost() + ":" + repositoryUrl.getPort();
		SVNClientManager result = null;
		boolean reTryLogin = false;
		boolean reTestOnly = false;
		while (forceShowingLoginDialogNextTime || result == null || reTestOnly) {

			if (!reTestOnly) {
				String username = getSettings().getUsername(repoid);
				String password = getSettings().getPassword(repoid);
				if (forceShowingLoginDialogNextTime || password == null
						|| username == null || reTryLogin) {
					forceShowingLoginDialogNextTime = false;
					final LoginGui loginGui = new LoginGui(username, password);
					SwingUtilities.invokeLater(new Runnable() {

						public void run() {
							loginGui.display();
						}

					});

					boolean doLogin = loginGui.waitForLoginButton();
					if (!doLogin) {
						throw new PagaException(PagaExceptionType.LOGIN_FAILED);
					}
					username = loginGui.getPredefinedUsername();
					password = loginGui.getPredefinedPassword();

					if (loginGui.isRememberChecked()) {
						getSettings().setUsername(repoid, username);
						getSettings().setPassword(repoid, password);
					} else {
						getSettings().setUsername(repoid, null);
						getSettings().setPassword(repoid, null);
					}
				}

				int readTimeout = 3600 * 1000;
				int connectionTimeout = 5 * 1000;
				ISVNAuthenticationManager defAuthManager;
				if (username == null || username.isEmpty()) {
					defAuthManager = SVNWCUtil
							.createDefaultAuthenticationManager();
				} else {
					defAuthManager = SVNWCUtil
							.createDefaultAuthenticationManager(username,
									password);
				}
				ISVNAuthenticationManager authManager = new ShortTimeoutAuthenticationManager(
						defAuthManager, readTimeout, connectionTimeout);
				result = SVNClientManager.newInstance(null, authManager);
				reTryLogin = false;
			}
			reTestOnly = false;
			try {
				SslLoginType sslType = mapSslLoginType.get(repositoryUrl
						.getHost());
				if (sslType == SslLoginType.SSLv2Hello_SSLv3) {
					SVNSocketFactory.setSSLProtocols("SSLv2Hello,SSLv3");
				} else {
					SVNSocketFactory.setSSLProtocols(null);
				}
				SVNRepository svnRepo = result.getRepositoryPool()
						.createRepository(repositoryUrl, true);
				svnRepo.testConnection();
			} catch (SVNException ex) {
				ex.printStackTrace();

				SVNErrorCode errorCode = ex.getErrorMessage().getErrorCode();
				if (SVNErrorCode.RA_SVN_IO_ERROR.equals(errorCode)) {
					throw new PagaException(PagaExceptionType.CONNECTION_ERROR,
							ex.getErrorMessage().getMessage());
				} else if (SVNErrorCode.RA_DAV_REQUEST_FAILED.equals(errorCode)) {

					if ("svn: E175002: handshake alert:  unrecognized_name"
							.equals(ex.getErrorMessage().getMessage())) {
						mapSslLoginType.put(repositoryUrl.getHost(),
								SslLoginType.SSLv2Hello_SSLv3);
						reTestOnly = true;
						continue;
					} else {
						throw new PagaException(
								PagaExceptionType.CONNECTION_ERROR, ex
										.getErrorMessage().getMessage());
					}
				}
				result = null;
				reTryLogin = true;
			}
		}

		result.setOptions(new MergerFactory());

		return result;
	}

	/**
	 * If a string is on the system clipboard, this method returns it; otherwise
	 * it returns null.
	 */
	public static String getClipboard() {
		Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard()
				.getContents(null);

		try {
			if (t != null && t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
				String text = (String) t
						.getTransferData(DataFlavor.stringFlavor);
				return text;
			}
		} catch (UnsupportedFlavorException e) {
		} catch (IOException e) {
		}
		return null;
	}

	/**
	 * This method writes a string to the system clipboard.
	 */
	public static void setClipboard(String str) {
		StringSelection ss = new StringSelection(str);
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);
	}

	public static long getPreviousRevisionNumber(SVNURL svnUrl, long revision)
			throws SVNException, PagaException {
		PreviousRevisionFetcher fetcher = new PreviousRevisionFetcher();
		fetcher.execute(svnUrl, revision);
		return fetcher.getPreviousRevision();
	}

	public static SVNInfo getInfo(File path) throws SVNException {
		SVNClientManager mgrSvn = Manager
				.getSVNClientManagerForWorkingCopyOnly();
		try {
			SVNWCClient wcClient = mgrSvn.getWCClient();
			return wcClient.doInfo(path, SVNRevision.WORKING);
		} finally {
			mgrSvn.dispose();
		}
	}

	public static JFrame getRootFrame() {
		return null;
	}

	public static SettingsStore getSettings() {
		return SettingsStore.getInstance();
	}

	public static int getMaxUrlHistoryItems() {
		// TODO getMaxUrlHistoryItems set it from preferences
		return 30;
	}

	public static int getMaxMessageHistoryItems() {
		// TODO getMaxMessageHistoryItems set it from preferences
		return 30;
	}

	public static void handle(Exception ex) {
		exceptionHandler.handle(ex);
	}

	public static void shutdown() throws Exception {
		shutdown = true;
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

	public static void resolveConflictUsingTheirs(String path)
			throws SVNException {
		SVNClientManager mgrSvn = getSVNClientManagerForWorkingCopyOnly();
		try {
			mgrSvn.getWCClient().doResolve(new File(path), SVNDepth.INFINITY,
					SVNConflictChoice.THEIRS_FULL);
		} finally {
			mgrSvn.dispose();
		}

	}

	public static void resolveConflictUsingMine(String path)
			throws SVNException {
		SVNClientManager mgrSvn = getSVNClientManagerForWorkingCopyOnly();
		try {
			mgrSvn.getWCClient().doResolve(new File(path), SVNDepth.INFINITY,
					SVNConflictChoice.MINE_FULL);
		} finally {
			mgrSvn.dispose();
		}
	}

	public static void resolveTreeConflictUsingTheirs(String path)
			throws SVNException {
		SVNClientManager mgrSvn = getSVNClientManagerForWorkingCopyOnly();
		try {
			mgrSvn.getWCClient().doResolve(new File(path), SVNDepth.INFINITY,
					true, true, true, SVNConflictChoice.THEIRS_FULL);
		} finally {
			mgrSvn.dispose();
		}

	}

	public static void resolveTreeConflictUsingMine(String path)
			throws SVNException {
		SVNClientManager mgrSvn = getSVNClientManagerForWorkingCopyOnly();
		try {
			mgrSvn.getWCClient().doResolve(new File(path), SVNDepth.INFINITY,
					true, true, true, SVNConflictChoice.MINE_FULL);
		} finally {
			mgrSvn.dispose();
		}
	}

	public static void resolveTreeConflictUsingMerged(String path)
			throws SVNException {
		SVNClientManager mgrSvn = getSVNClientManagerForWorkingCopyOnly();
		try {
			mgrSvn.getWCClient().doResolve(new File(path), SVNDepth.INFINITY,
					true, true, true, SVNConflictChoice.MERGED);
		} finally {
			mgrSvn.dispose();
		}
	}

	public static SVNInfo getInfo(String path) throws SVNException {
		SVNClientManager mgrSvn = getSVNClientManagerForWorkingCopyOnly();
		try {
			SVNInfo info = mgrSvn.getWCClient().doInfo(new File(path),
					SVNRevision.WORKING);
			return info;
		} finally {
			mgrSvn.dispose();
		}
	}

	public static SVNInfo getInfo(SVNURL url, SVNRevision revision)
			throws SVNException, PagaException {
		SVNClientManager mgrSvn = getSVNClientManager(url);
		try {
			SVNInfo info = mgrSvn.getWCClient().doInfo(url, revision, revision);
			return info;
		} finally {
			mgrSvn.dispose();
		}
	}

	public static Color getColorByContentStatus(ContentStatus status) {
		switch (status) {
		case ADDED:
			return COLOR_PURPLE;
		case CONFLICTED:
			return Color.RED;
		case DELETED:
			return Color.RED;
		case EXTERNAL:
			return Color.DARK_GRAY;
		case IGNORED:
			return Color.ORANGE;
		case INCOMPLETE:
			return Color.RED;
		case MERGED:
			return COLOR_GREEN;
		case MISSING:
			return Color.RED;
		case MODIFIED:
			return Color.BLUE;
		case NONE:
			return Color.BLACK;
		case NORMAL:
			return Color.BLACK;
		case OBSTRUCTED:
			return Color.RED;
		case REPLACED:
			return Color.RED;
		case RESTORED:
			return Color.ORANGE;
		case UNVERSIONED:
			return Color.BLACK;
		case RESOLVED:
			return Color.decode("923700");
		default:
			return null;
		}
	}

	public static File getBaseFile(File wcFile) throws Exception {
		String tempPrefix = tempFileDirectory.getAbsolutePath() + "/";
		String fileNameRoot = tempPrefix + wcFile.getName();
		File file = new File(fileNameRoot);
		int counter = 0;
		while (file.exists()) {
			file = new File(fileNameRoot + "-" + counter);
			counter++;
		}

		SVNClientManager svnMgr = Manager
				.getSVNClientManagerForWorkingCopyOnly();
		try {
			SVNWCClient wcClient = svnMgr.getWCClient();

			FileOutputStream outOldRevision = new FileOutputStream(
					file.getPath());
			wcClient.doGetFileContents(wcFile, SVNRevision.BASE,
					SVNRevision.BASE, false, outOldRevision);
			outOldRevision.close();

			return file;
		} finally {
			svnMgr.dispose();
		}
	}

	public static void invalidate(File file) {
		FileStatusCache.getInstance().invalidate(file);
	}

	public static void invalidateAllFiles() {
		FileStatusCache.getInstance().invalidateAll();
	}

	public static String getOsCommandResult(File baseDir, String... args)
			throws IOException {

		ProcessBuilder pb = new ProcessBuilder(args);
		pb.directory(baseDir);
		pb.redirectErrorStream(true);
		Process process = pb.start();
		BufferedReader commandResult = new BufferedReader(
				new InputStreamReader(process.getInputStream(),
						Charset.defaultCharset()));
		String line = "";
		StringBuilder sb = new StringBuilder();
		while ((line = commandResult.readLine()) != null) {
			sb.append(line);
			sb.append("\n");
		}

		return sb.toString();
	}

	public static String loadFileToString(File file) throws IOException {
		Charset charSet = Charset.defaultCharset();

		StringBuilder text = new StringBuilder();
		InputStreamReader input = new InputStreamReader(new FileInputStream(
				file), charSet);
		char[] buffer = new char[16384];
		int length;
		while ((length = input.read(buffer)) != -1) {
			text.append(buffer, 0, length);
		}

		input.close();

		return text.toString();
	}

	public static void saveStringToFile(File file, String data)
			throws IOException {
		BufferedWriter out = new BufferedWriter(new FileWriter(file));
		out.write(data);
		out.close();
	}

	public static void setForceShowingLoginDialogNextTime(
			boolean forceShowingLoginDialogNextTime) {
		Manager.forceShowingLoginDialogNextTime = forceShowingLoginDialogNextTime;
	}

	public static boolean isShutdown() {
		return shutdown;
	}

	public static File getCommonBaseDir(List<File> lstFiles) {
		List<List<String>> lstPathLists = new ArrayList<List<String>>();
		for (File file : lstFiles) {
			if (!file.isDirectory()) {
				file = file.getParentFile();
			}

			LinkedList<String> lstPath = new LinkedList<String>();
			String li = file.getName();
			lstPath.add(li);
			File parent = file.getParentFile();
			while (parent != null) {
				lstPath.add(0, parent.getName());
				parent = parent.getParentFile();
			}
			lstPathLists.add(lstPath);
		}

		ArrayList<String> lstCommonPath = new ArrayList<String>();

		int i = 0;

		boolean match = true;
		while (match) {
			String pathLi = null;
			for (List<String> lstLi : lstPathLists) {
				if (lstLi.size() <= i) {
					match = false;
					break;
				}
				String newPathLi = lstLi.get(i);
				if (pathLi == null || pathLi.equals(newPathLi)) {
					pathLi = newPathLi;
				} else {
					match = false;
					break;
				}
			}
			if (match) {
				lstCommonPath.add(pathLi);
			}
			i++;
		}

		StringBuilder sb = new StringBuilder(128);
		sb.append(File.separatorChar);
		for (String li : lstCommonPath) {
			sb.append(li);
			sb.append(File.separatorChar);
		}

		return new File(sb.toString());
	}

	private static class MergerFactory extends DefaultSVNOptions {

		private ISVNConflictHandler myConflictResolver;

		public void setConflictHandler(ISVNConflictHandler resolver) {
			myConflictResolver = resolver;
		}

		public ISVNMerger createMerger(byte[] conflictStart,
				byte[] conflictSeparator, byte[] conflictEnd) {
			return new SVNMergerIgnoreEol(new DefaultSVNMerger(conflictStart,
					conflictSeparator, conflictEnd, myConflictResolver,
					SVNDiffConflictChoiceStyle.CHOOSE_MODIFIED_LATEST));

		}
	}

	private static class SVNMergerIgnoreEol implements ISVNMerger, ISvnMerger {

		private final ISVNMerger delegate;

		public SVNMergerIgnoreEol(ISVNMerger delegate) {
			this.delegate = delegate;
		}

		public SVNMergeResult mergeProperties(String localPath,
				SVNProperties workingProperties, SVNProperties baseProperties,
				SVNProperties serverBaseProps, SVNProperties propDiff,
				SVNAdminArea adminArea, SVNLog log, boolean baseMerge,
				boolean dryRun) throws SVNException {
			return this.delegate.mergeProperties(localPath, workingProperties,
					baseProperties, serverBaseProps, propDiff, adminArea, log,
					baseMerge, dryRun);
		}

		public SVNMergeResult mergeText(SVNMergeFileSet files, boolean dryRun,
				SVNDiffOptions options) throws SVNException {
			if (options == null
					&& Boolean.TRUE.equals(SettingsStore.getInstance()
							.getGlobalIgnoreEol())) {
				options = new SVNDiffOptions(false, false, true);
			}
			return this.delegate.mergeText(files, dryRun, options);
		}

		@Override
		public SvnMergeResult mergeText(ISvnMerger baseMerger, File resultFile,
				File targetAbspath, File detranslatedTargetAbspath,
				File leftAbspath, File rightAbspath, String targetLabel,
				String leftLabel, String rightLabel, SVNDiffOptions options,
				SVNDiffConflictChoiceStyle style) throws SVNException {

			if (options == null
					&& Boolean.TRUE.equals(SettingsStore.getInstance()
							.getGlobalIgnoreEol())) {
				options = new SVNDiffOptions(false, false, true);
			}
			return baseMerger.mergeText(baseMerger, resultFile, targetAbspath,
					detranslatedTargetAbspath, leftAbspath, rightAbspath,
					targetLabel, leftLabel, rightLabel, options, style);
		}

		@Override
		public SvnMergeResult mergeProperties(ISvnMerger baseMerger,
				File localAbsPath, SVNNodeKind kind,
				SVNConflictVersion leftVersion,
				SVNConflictVersion rightVersion,
				SVNProperties serverBaseProperties,
				SVNProperties pristineProperties,
				SVNProperties actualProperties, SVNProperties propChanges,
				boolean baseMerge, boolean dryRun,
				ISVNConflictHandler conflictResolver) throws SVNException {

			return baseMerger.mergeProperties(baseMerger, localAbsPath, kind,
					leftVersion, rightVersion, serverBaseProperties,
					pristineProperties, actualProperties, propChanges,
					baseMerge, dryRun, conflictResolver);
		}
	}

	public static Icon getIconError() {
		if (ICON_ERROR == null) {
			ICON_ERROR = ResourceBundleAccessor.getImage("dialog-error.png");
		}
		return ICON_ERROR;
	}

	public static Icon getIconInformation() {
		if (ICON_INFORMATION == null) {
			ICON_INFORMATION = ResourceBundleAccessor
					.getImage("dialog-information.png");
		}
		return ICON_INFORMATION;
	}

	public static Icon getIconPassword() {
		if (ICON_PASSWORD == null) {
			ICON_PASSWORD = ResourceBundleAccessor
					.getImage("dialog-password.png");
		}
		return ICON_PASSWORD;
	}

	public static Icon getIconQuestion() {
		if (ICON_QUESTION == null) {
			ICON_QUESTION = ResourceBundleAccessor
					.getImage("dialog-question.png");
		}
		return ICON_QUESTION;
	}

	public static Icon getIconWarning() {
		if (ICON_WARNING == null) {
			ICON_WARNING = ResourceBundleAccessor
					.getImage("dialog-warning.png");
		}
		return ICON_WARNING;
	}

	public static void viewFile(String filename) throws IOException {
		ProcessBuilder processBuilder = new ProcessBuilder(Manager.GEDIT,
				filename);
		processBuilder.start();
	}

	public static void compareTextFiles(String filename1, String filename2)
			throws IOException {

		String label1 = filename1;
		if (label1.lastIndexOf('/') != -1) {
			label1 = label1.substring(label1.lastIndexOf('/'));
		}
		String label2 = filename1;
		if (label2.lastIndexOf('/') != -1) {
			label2 = label2.substring(label2.lastIndexOf('/'));
		}

		ProcessBuilder processBuilder = new ProcessBuilder(Manager.MELD, "-L "
				+ label1, filename1, "-L " + label2, filename2);
		processBuilder.start();
	}
}
