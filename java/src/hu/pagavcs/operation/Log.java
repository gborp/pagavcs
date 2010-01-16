package hu.pagavcs.operation;

import hu.pagavcs.bl.Manager;
import hu.pagavcs.bl.PagaException;
import hu.pagavcs.gui.LogGui;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
import org.tmatesoft.svn.core.wc.SVNDiffClient;
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
public class Log {

	public enum ShowLogStatus {
		INIT, STARTED, CANCEL, COMPLETED

	}

	public static final long LIMIT    = 100;
	public static final long NO_LIMIT = 0;

	private String           path;
	private boolean          cancel;

	private LogGui           gui;
	private SVNURL           rootUrl;

	public Log(String path) throws SVNException, BackingStoreException {
		this.path = path;
		setCancel(false);
	}

	public void execute() throws Exception {
		gui = new LogGui(this);
		gui.display();
		gui.setStatus(ShowLogStatus.INIT);
		gui.setUrlLabel(getRootUrl().toString());

		doShowLog(SVNRevision.HEAD, LIMIT);
	}

	/**
	 * @param startRevision
	 * @param limit
	 *            zero means no limit
	 * @throws Exception
	 */
	public void doShowLog(SVNRevision startRevision, long limit) throws Exception {
		gui.workStarted();
		SVNClientManager mgrSvn = Manager.getSVNClientManager(new File(path));
		if (mgrSvn == null) {
			Manager.showFailedDialog();
			return;
		}
		SVNLogClient logClient = mgrSvn.getLogClient();
		logClient.setEventHandler(new LogEventHandler());
		gui.setStatus(ShowLogStatus.STARTED);
		File[] filePaths = new File[1];
		filePaths[0] = new File(path);

		SVNRevision endRevision = SVNRevision.create(0);
		SVNRevision pegRevision = SVNRevision.UNDEFINED;
		boolean stopOnCopy = false;
		boolean discoverChangedPaths = true;
		boolean includeMergedRevisions = false;
		String[] revisionProperties = null;
		ISVNLogEntryHandler handler = new LogEntryHandler();
		logClient.doLog(filePaths, startRevision, endRevision, pegRevision, stopOnCopy, discoverChangedPaths, includeMergedRevisions, limit,
		        revisionProperties, handler);
		gui.setStatus(ShowLogStatus.COMPLETED);
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

	public SVNURL getRootUrl() throws SVNException {
		if (rootUrl == null) {
			rootUrl = Manager.getInfo(path).getURL();
		}
		return rootUrl;
	}

	public void showChanges(String showChangesPath, long revision) throws Exception {

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
			long previousRevision = Manager.getPreviousRevisionNumber(svnUrl, revision);

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

			wcClient.doGetFileContents(svnUrl, SVNRevision.UNDEFINED, SVNRevision.create(revision), false, outNewRevision);

			wcClient.doGetFileContents(svnUrl, SVNRevision.UNDEFINED, SVNRevision.create(previousRevision), false, outOldRevision);

			outNewRevision.close();
			outOldRevision.close();

			fileNew.setReadOnly();
			fileNew.deleteOnExit();
			fileOld.setReadOnly();
			fileOld.deleteOnExit();

			Process process = Runtime.getRuntime().exec(
			        "meld -L " + fileNameOld + " " + tempPrefix + fileNameOld + " -L " + fileNameNew + " " + tempPrefix + fileNameNew);
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
			try {
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
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void revertChanges(String path, long revision) throws SVNException, PagaException {
		SVNClientManager mgrSvn = Manager.getSVNClientManager(new File(path));
		SVNDiffClient diffClient = mgrSvn.getDiffClient();
		// TODO revertChanges
		throw new RuntimeException("not implemented");
	}

	private class LogEntryHandler implements ISVNLogEntryHandler {

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
