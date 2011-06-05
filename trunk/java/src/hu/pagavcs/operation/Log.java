package hu.pagavcs.operation;

import hu.pagavcs.bl.CacheLog;
import hu.pagavcs.bl.Cancelable;
import hu.pagavcs.bl.Manager;
import hu.pagavcs.bl.SettingsStore;
import hu.pagavcs.bl.SvnHelper;
import hu.pagavcs.gui.log.LogGui;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.prefs.BackingStoreException;

import org.tmatesoft.svn.core.ISVNLogEntryHandler;
import org.tmatesoft.svn.core.SVNCancelException;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNLogEntryPath;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNEvent;
import org.tmatesoft.svn.core.wc.SVNLogClient;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNWCClient;

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
public class Log implements Cancelable {

	public enum ShowLogStatus {
		INIT, STARTED, CANCEL, COMPLETED, CANCELLED

	}

	public static final long LIMIT    = 100;
	public static final long NO_LIMIT = 0;

	private String           path;
	private boolean          cancel;

	private LogGui           gui;
	private SVNURL           rootUrl;

	public Log(String path) throws SVNException, BackingStoreException {
		this(path, true);
	}

	public Log(String pathOrUrl, boolean isPath) throws SVNException, BackingStoreException {
		if (isPath) {
			this.path = pathOrUrl;
		} else {
			this.rootUrl = SVNURL.parseURIDecoded(pathOrUrl);
		}
		setCancel(false);
	}

	public void execute() throws Exception {
		gui = new LogGui(this);
		gui.display();
		gui.setStatus(ShowLogStatus.INIT);
		if (path != null) {
			gui.setLogRootsFiles(Arrays.asList(new File(path)));
			gui.setUrlLabel(getUrl().toDecodedString());
		} else {
			gui.setUrlLabel(getRootUrl().toDecodedString());
		}
		gui.setSvnRepoRootUrl(getRootUrl());
		// Manager.getSvnRootUrlByFile(new File(path)));

		gui.doShowLog(SVNRevision.HEAD, LIMIT);
	}

	/**
	 * @param startRevision
	 * @param limit
	 *            zero means no limit
	 * @throws Exception
	 */
	public void doShowLog(SVNRevision startRevision, long limit) throws Exception {
		gui.workStarted();
		SVNClientManager mgrSvn;
		if (path != null) {
			mgrSvn = Manager.getSVNClientManager(new File(path));
		} else {
			mgrSvn = Manager.getSVNClientManager(getRootUrl());
		}
		SVNLogClient logClient = mgrSvn.getLogClient();
		logClient.setEventHandler(new LogEventHandler());
		gui.setStatus(ShowLogStatus.STARTED);

		SVNRevision endRevision = SVNRevision.create(0);
		SVNRevision pegRevision = SVNRevision.UNDEFINED;
		boolean stopOnCopy = false;
		boolean discoverChangedPaths = true;
		boolean includeMergedRevisions = false;
		String[] revisionProperties = null;
		final ISVNLogEntryHandler handler = new LogEntryHandler();
		try {
			if (path != null) {
				CacheLog.getInstance().doLog(logClient, new File(path), startRevision, endRevision, pegRevision, stopOnCopy, discoverChangedPaths,
				        includeMergedRevisions, limit, revisionProperties, handler);
			} else {

				ISVNLogEntryHandler wrapCacheItems = new ISVNLogEntryHandler() {

					public void handleLogEntry(SVNLogEntry logEntry) throws SVNException {

						handler.handleLogEntry(logEntry);
					}
				};

				logClient.doLog(getRootUrl(), new String[] { "" }, pegRevision, startRevision, endRevision, stopOnCopy, discoverChangedPaths,
				        includeMergedRevisions, limit, revisionProperties, wrapCacheItems);
			}

			gui.setStatus(ShowLogStatus.COMPLETED);
		} catch (SVNCancelException ex) {
			gui.setStatus(ShowLogStatus.CANCELLED);
		}
		gui.workEnded();
	}

	public void setCancel(boolean cancel) {
		if (cancel) {
			gui.setStatus(ShowLogStatus.CANCEL);
		}
		this.cancel = cancel;
	}

	public boolean isCancel() {
		return cancel;
	}

	public String getPath() {
		return path;
	}

	public SVNURL getRootUrl() throws SVNException {
		if (rootUrl == null) {
			rootUrl = Manager.getInfo(path).getRepositoryRootURL();
		}
		return rootUrl;
	}

	public SVNURL getUrl() throws SVNException {
		return Manager.getInfo(path).getURL();
	}

	public void showDirChanges(String showChangesPath, long revision, ContentStatus contentStatus) throws Exception {
		SVNURL repoRoot = Manager.getSvnRootUrlByFile(new File(path));
		SVNURL svnUrl = SVNURL.create(repoRoot.getProtocol(), repoRoot.getUserInfo(), repoRoot.getHost(), repoRoot.getPort(), repoRoot.getPath()
		        + showChangesPath, true);
		long previousRevision = -1;
		if (!contentStatus.equals(ContentStatus.ADDED)) {
			previousRevision = Manager.getPreviousRevisionNumber(svnUrl, revision);
		}
		SvnHelper.showPropertyChangesFromRepo(gui, svnUrl, revision, previousRevision, contentStatus);
	}

	public void showFile(String showChangesPath, long revision) throws Exception {
		String fileName = showChangesPath.substring(showChangesPath.lastIndexOf('/') + 1);
		String tempPrefix = Manager.getTempDir();
		String fileNameNew = "r" + revision + "-" + fileName;
		File fileNew = new File(tempPrefix + fileNameNew);
		try {
			gui.workStarted();
			saveRevisionTo(showChangesPath, revision, fileNew);
			ProcessBuilder processBuilder = new ProcessBuilder("gedit", tempPrefix + fileNameNew);
			Process process = processBuilder.start();
			gui.workEnded();
			process.waitFor();
		} catch (Exception e) {
			try {
				gui.workEnded();
			} catch (Exception e1) {
				Manager.handle(e1);
			}
			throw e;
		} finally {
			fileNew.delete();
		}
	}

	public void saveRevisionTo(String showChangesPath, long revision, File destination) throws Exception {
		FileOutputStream outNewRevision = null;
		try {
			gui.workStarted();

			outNewRevision = new FileOutputStream(destination);

			SVNURL repoRoot = Manager.getSvnRootUrlByFile(new File(path));
			SVNURL svnUrl = SVNURL.create(repoRoot.getProtocol(), repoRoot.getUserInfo(), repoRoot.getHost(), repoRoot.getPort(), repoRoot.getPath()
			        + showChangesPath, true);
			SVNClientManager mgrSvn = Manager.getSVNClientManager(repoRoot);
			SVNWCClient wcClient = mgrSvn.getWCClient();

			wcClient.doGetFileContents(svnUrl, SVNRevision.create(revision), SVNRevision.create(revision), false, outNewRevision);

		} catch (Exception e) {
			throw e;
		} finally {
			gui.workEnded();
			if (outNewRevision != null) {
				outNewRevision.close();
			}
		}
	}

	public void showChanges(String showChangesPath, long revision, ContentStatus contentStatus) throws Exception {

		FileOutputStream outNewRevision = null;
		FileOutputStream outOldRevision = null;
		File fileNew = null;
		File fileOld = null;
		try {
			gui.workStarted();
			SVNURL repoRoot = Manager.getSvnRootUrlByFile(new File(path));
			SVNURL svnUrl = SVNURL.create(repoRoot.getProtocol(), repoRoot.getUserInfo(), repoRoot.getHost(), repoRoot.getPort(), repoRoot.getPath()
			        + showChangesPath, true);
			SVNClientManager mgrSvn = Manager.getSVNClientManager(repoRoot);
			SVNWCClient wcClient = mgrSvn.getWCClient();
			long previousRevision = -1;
			if (!contentStatus.equals(ContentStatus.ADDED)) {
				previousRevision = Manager.getPreviousRevisionNumber(svnUrl, revision);
			}

			String fileName = showChangesPath.substring(showChangesPath.lastIndexOf('/') + 1);

			String fileNameNew = "r" + revision + "-" + fileName;
			String fileNameOld = "r" + previousRevision + "-" + fileName;
			String tempPrefix = Manager.getTempDir();

			fileNew = new File(tempPrefix + fileNameNew);
			fileOld = new File(tempPrefix + fileNameOld);
			fileNew.delete();
			fileOld.delete();
			outNewRevision = new FileOutputStream(tempPrefix + fileNameNew);
			outOldRevision = new FileOutputStream(tempPrefix + fileNameOld);

			if (!contentStatus.equals(ContentStatus.DELETED)) {
				wcClient.doGetFileContents(svnUrl, SVNRevision.create(revision), SVNRevision.create(revision), false, outNewRevision);
			}

			if (!contentStatus.equals(ContentStatus.ADDED)) {
				wcClient.doGetFileContents(svnUrl, SVNRevision.create(previousRevision), SVNRevision.create(previousRevision), false, outOldRevision);
			}

			fileNew.setReadOnly();
			fileNew.deleteOnExit();
			fileOld.setReadOnly();
			fileOld.deleteOnExit();

			ProcessBuilder processBuilder;
			if (contentStatus.equals(ContentStatus.DELETED)) {
				processBuilder = new ProcessBuilder("gedit", tempPrefix + fileNameOld);
			} else if (contentStatus.equals(ContentStatus.ADDED)) {
				processBuilder = new ProcessBuilder("gedit", tempPrefix + fileNameNew);
			} else {
				processBuilder = new ProcessBuilder("meld", "-L " + fileNameOld, tempPrefix + fileNameOld, "-L " + fileNameNew, tempPrefix + fileNameNew);
			}
			Process process = processBuilder.start();
			gui.workEnded();
			process.waitFor();
		} catch (Exception e) {
			try {
				gui.workEnded();
			} catch (Exception e1) {
				Manager.handle(e1);
			}
			throw e;
		} finally {
			if (outNewRevision != null) {
				outNewRevision.close();
			}
			if (outOldRevision != null) {
				outOldRevision.close();
			}
			if (fileNew != null) {
				fileNew.delete();
			}
			if (fileOld != null) {
				fileOld.delete();
			}
		}
	}

	public void compareWithWorkingCopy(File file, SVNURL svnurl, long revision, ContentStatus contentStatus) throws Exception {
		FileOutputStream outNewRevision = null;
		File fileNew = null;
		try {
			gui.workStarted();
			// SVNClientManager mgrSvn = Manager.getSVNClientManager(repoRoot);
			SVNClientManager mgrSvn = Manager.getSVNClientManager(new File(path));
			SVNWCClient wcClient = mgrSvn.getWCClient();
			// File f = wcClient.doInfo(svnurl2, SVNRevision.WORKING,
			// SVNRevision.WORKING).getFile();

			// SVNURL repoRoot = Manager.getSvnRootUrlByFile(new File(path));
			// SVNURL svnUrl = SVNURL.create(repoRoot.getProtocol(),
			// repoRoot.getUserInfo(), repoRoot.getHost(), repoRoot.getPort(),
			// repoRoot.getPath() + svnurl2,
			// true);

			String svnurl2Str = svnurl.toDecodedString();
			String fileName = svnurl2Str.substring(svnurl2Str.lastIndexOf('/') + 1);

			String fileNameNew = "r" + revision + "-" + fileName;
			String tempPrefix = Manager.getTempDir();

			fileNew = new File(tempPrefix + fileNameNew);
			fileNew.delete();
			outNewRevision = new FileOutputStream(tempPrefix + fileNameNew);

			wcClient.doGetFileContents(svnurl, SVNRevision.create(revision), SVNRevision.create(revision), false, outNewRevision);

			fileNew.setReadOnly();
			fileNew.deleteOnExit();

			ProcessBuilder processBuilder;
			if (contentStatus.equals(ContentStatus.DELETED)) {
				processBuilder = new ProcessBuilder("gedit", file.toString());
			} else if (contentStatus.equals(ContentStatus.ADDED)) {
				processBuilder = new ProcessBuilder("gedit", file.toString());
			} else {
				processBuilder = new ProcessBuilder("meld", "-L " + file.getName(), file.toString(), "-L " + fileNameNew, tempPrefix + fileNameNew);
			}
			Process process = processBuilder.start();
			gui.workEnded();
			process.waitFor();
		} catch (Exception e) {
			try {
				gui.workEnded();
			} catch (Exception e1) {
				Manager.handle(e1);
			}
			throw e;
		} finally {
			if (outNewRevision != null) {
				outNewRevision.close();
			}
			if (fileNew != null) {
				fileNew.delete();
			}
		}
	}

	public void revertChanges(String revertPath, long revision) throws Exception {

		String pathToUrl = Manager.getSVNClientManagerForWorkingCopyOnly().getWCClient().doInfo(new File(path), SVNRevision.WORKING).getURL().toDecodedString();
		String rootUrlDecoded = getRootUrl().toDecodedString();

		SVNURL repoRoot = Manager.getSvnRootUrlByFile(new File(path));
		SVNURL svnUrl = SVNURL.create(repoRoot.getProtocol(), repoRoot.getUserInfo(), repoRoot.getHost(), repoRoot.getPort(), repoRoot.getPath() + revertPath,
		        true);
		String pathRevertWc = path + revertPath.substring(pathToUrl.substring(rootUrlDecoded.length()).length());

		SvnHelper.doMerge(this, svnUrl.toDecodedString(), pathRevertWc, svnUrl.toDecodedString(), pathRevertWc, Long.toString(revision), true, Boolean.TRUE
		        .equals(SettingsStore.getInstance().getGlobalIgnoreEol()));
	}

	public void revertChangesExact(long revision) throws Exception {
		String svnPath = Manager.getSVNClientManagerForWorkingCopyOnly().getWCClient().doInfo(new File(path), SVNRevision.WORKING).getURL().toDecodedString();
		SvnHelper.doMerge(this, svnPath, path, svnPath, path, Long.toString(revision), true, Boolean.TRUE.equals(SettingsStore.getInstance()
		        .getGlobalIgnoreEol()));
	}

	public void revertChangesToThisRevisionExact(String revisionRange) throws Exception {
		String svnPath = Manager.getSVNClientManagerForWorkingCopyOnly().getWCClient().doInfo(new File(path), SVNRevision.WORKING).getURL().toDecodedString();
		SvnHelper.doMerge(this, svnPath, path, svnPath, path, revisionRange, true, Boolean.TRUE.equals(SettingsStore.getInstance().getGlobalIgnoreEol()));
	}

	private class LogEntryHandler implements ISVNLogEntryHandler {

		@SuppressWarnings("unchecked")
		public void handleLogEntry(SVNLogEntry logEntry) throws SVNException {
			long revision = logEntry.getRevision();
			String author = logEntry.getAuthor();
			Date date = logEntry.getDate();
			String message = logEntry.getMessage();

			Map<String, SVNLogEntryPath> mapChanges = logEntry.getChangedPaths();

			gui.addItem(revision, author, date, message, mapChanges);
		}

	}

	private class LogEventHandler implements ISVNEventHandler {

		public void handleEvent(SVNEvent event, double progress) throws SVNException {}

		public void checkCancelled() throws SVNCancelException {
			if (isCancel()) {
				throw new SVNCancelException();
			}
		}
	}

}
