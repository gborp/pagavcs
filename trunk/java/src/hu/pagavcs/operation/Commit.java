package hu.pagavcs.operation;

import hu.pagavcs.bl.Manager;
import hu.pagavcs.bl.OnSwing;
import hu.pagavcs.bl.SvnHelper;
import hu.pagavcs.gui.commit.CommitGui;
import hu.pagavcs.gui.platform.MessagePane;
import hu.pagavcs.gui.platform.StringHelper;

import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.prefs.BackingStoreException;

import javax.swing.JOptionPane;

import org.tmatesoft.svn.core.SVNCancelException;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.ISVNStatusHandler;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNCommitClient;
import org.tmatesoft.svn.core.wc.SVNEvent;
import org.tmatesoft.svn.core.wc.SVNEventAction;
import org.tmatesoft.svn.core.wc.SVNPropertyData;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNStatus;
import org.tmatesoft.svn.core.wc.SVNStatusClient;
import org.tmatesoft.svn.core.wc.SVNStatusType;
import org.tmatesoft.svn.core.wc.SVNTreeConflictDescription;
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
public class Commit {

	public enum CommitStatus {
		INIT, CANCEL, FILE_LIST_GATHERING_COMPLETED, COMMIT_STARTED, COMMIT_COMPLETED, COMMIT_FAILED,
	}

	public enum CommittedItemStatus {
		ADDED, DELETED, COMPLETED, MODIFIED, REPLACED, DELTA_SENT
	}

	private String                  path;
	private boolean                 cancel;
	private CommitGui               gui;
	private SVNURL                  rootUrl;
	private RefreshISVNEventHandler refreshISVNEventHandler;

	public Commit(String path) throws SVNException, BackingStoreException {
		this.path = path;
	}

	public void execute() throws Exception {
		gui = new CommitGui(this);
		gui.display();
		File wcFile = new File(path);
		SVNClientManager mgrSvn = Manager.getSVNClientManagerForWorkingCopyOnly();
		SVNWCClient wcClient = mgrSvn.getWCClient();
		SVNPropertyData logTemplate = wcClient.doGetProperty(wcFile, "tsvn:logtemplate", SVNRevision.WORKING, SVNRevision.WORKING);
		SVNPropertyData logMinSize = wcClient.doGetProperty(wcFile, "tsvn:logminsize", SVNRevision.WORKING, SVNRevision.WORKING);

		String strLogTemplate = null;
		if (logTemplate != null) {
			strLogTemplate = SVNPropertyValue.getPropertyAsString(logTemplate.getValue());
			gui.setLogTemplate(strLogTemplate);
		}
		if (logMinSize != null) {
			String strlogMinSize = SVNPropertyValue.getPropertyAsString(logMinSize.getValue());
			try {
				int intLogMinSize = Integer.valueOf(strlogMinSize);
				gui.setLogMinSize(intLogMinSize);
			} catch (Exception ex) {}
		}

		refresh();
	}

	public String getPath() {
		return path;
	}

	public void cancelRefresh() {
		if (refreshISVNEventHandler != null) {
			refreshISVNEventHandler.setCancel(true);
			refreshISVNEventHandler = null;
		}
	}

	public void refresh() throws Exception {
		cancelRefresh();

		new OnSwing() {

			protected void process() throws Exception {
				gui.setStatus(CommitStatus.INIT, null);
				gui.setRecentMessages(getRecentMessages());
				gui.setUrlLabel(getRootUrl().toDecodedString());
				gui.setPath(path);
			}
		}.run();

		try {
			File wcFile = new File(path);
			SVNClientManager mgrSvn = Manager.getSVNClientManagerForWorkingCopyOnly();
			SVNStatusClient statusClient = mgrSvn.getStatusClient();

			refreshISVNEventHandler = new RefreshISVNEventHandler();
			statusClient.setEventHandler(refreshISVNEventHandler);

			statusClient.doStatus(wcFile, SVNRevision.WORKING, SVNDepth.INFINITY, false, true, false, true, new StatusEventHandler(), null);
		} catch (SVNCancelException ex) {
			// ignoring cancel
		} finally {
			new OnSwing() {

				protected void process() throws Exception {
					gui.setStatus(CommitStatus.FILE_LIST_GATHERING_COMPLETED, null);
				}
			}.run();
		}
	}

	private String[] getRecentMessages() {
		List<String> result = new ArrayList<String>(Manager.getSettings().getLstCommitMessages());
		result.add(null);
		Collections.reverse(result);
		return result.toArray(new String[0]);
	}

	public void setCancel(boolean cancel) {
		this.cancel = cancel;
		if (cancel) {
			cancelRefresh();
		}
	}

	public boolean isCancel() {
		return cancel;
	}

	private boolean doTryUpdateing(SVNException ex) {
		SVNErrorMessage errMsg = ex.getErrorMessage();
		while (errMsg != null && !SVNErrorCode.FS_TXN_OUT_OF_DATE.equals(errMsg.getErrorCode()) && !SVNErrorCode.FS_CONFLICT.equals(errMsg.getErrorCode())) {
			errMsg = errMsg.getChildErrorMessage();
		}
		if (errMsg != null) {
			return true;
		} else {
			return false;
		}
	}

	public void doCommit(List<File> lstCommit, String message) throws Exception {
		gui.setStatus(CommitStatus.COMMIT_STARTED, null);
		SVNClientManager mgrSvn = Manager.getSVNClientManager(new File(path));
		SVNCommitClient commitClient = mgrSvn.getCommitClient();
		commitClient.setEventHandler(new CommitEventHandler());

		boolean successOrExit = false;
		while (!successOrExit) {
			try {
				commitClient.doCommit(lstCommit.toArray(new File[] {}), true, message, null, null, true, true, SVNDepth.EMPTY);
				successOrExit = true;
			} catch (SVNCancelException ex) {
				successOrExit = true;
			} catch (SVNException ex) {
				SVNErrorCode errorCode = ex.getErrorMessage().getErrorCode();
				if (errorCode.isAuthentication()) {
					MessagePane.showError(gui.getFrame(), "Authentication failure", "" + errorCode.getDescription());
					Manager.setForceShowingLoginDialogNextTime(true);
					mgrSvn = Manager.getSVNClientManager(new File(path));
					commitClient = mgrSvn.getCommitClient();
					commitClient.setEventHandler(new CommitEventHandler());
				} else if (SVNErrorCode.WC_LOCKED.equals(errorCode)) {
					File file = (File) ex.getErrorMessage().getRelatedObjects()[0];
					int choosed = JOptionPane.showConfirmDialog(Manager.getRootFrame(), "Working copy " + file.getPath() + " is locked, do cleanup?",
					        "Working copy locked, cleanup?", JOptionPane.YES_NO_OPTION);
					if (choosed == JOptionPane.YES_OPTION) {
						Cleanup cleanup = new Cleanup(file.getPath());
						cleanup.setAutoClose(true);
						cleanup.execute();
					} else {
						gui.setStatus(CommitStatus.CANCEL, null);
						successOrExit = true;
					}
				} else if (doTryUpdateing(ex)) {
					int choosed = JOptionPane.showConfirmDialog(Manager.getRootFrame(), "An update is need, do update now?", "Update is needed",
					        JOptionPane.YES_NO_OPTION);
					if (choosed == JOptionPane.YES_OPTION) {
						Update update = new Update(Arrays.asList(path));
						update.execute();

						int choosedContinue = JOptionPane.showConfirmDialog(Manager.getRootFrame(), "It's updated, do you still want to commit?",
						        "Still commit?", JOptionPane.YES_NO_OPTION);
						if (choosedContinue == JOptionPane.NO_OPTION) {
							gui.setStatus(CommitStatus.CANCEL, null);
							successOrExit = true;
						}
					} else {
						gui.setStatus(CommitStatus.CANCEL, null);
						successOrExit = true;
					}
				} else {
					gui.setStatus(CommitStatus.CANCEL, null);
					throw ex;
				}

			}
		}
	}

	private class CommitEventHandler implements ISVNEventHandler {

		public void handleEvent(SVNEvent event, double progress) throws SVNException {
			try {
				SVNEventAction action = event.getAction();
				String fileName = null;
				if (event.getFile() != null) {
					Manager.invalidate(event.getFile());
					fileName = event.getFile().getAbsolutePath();
				}

				if (SVNEventAction.COMMIT_ADDED.equals(action)) {
					gui.addCommittedItem(fileName, CommittedItemStatus.ADDED);
				} else if (SVNEventAction.COMMIT_DELETED.equals(action)) {
					gui.addCommittedItem(fileName, CommittedItemStatus.DELETED);
				} else if (SVNEventAction.COMMIT_MODIFIED.equals(action)) {
					gui.addCommittedItem(fileName, CommittedItemStatus.MODIFIED);
				} else if (SVNEventAction.COMMIT_REPLACED.equals(action)) {
					gui.addCommittedItem(fileName, CommittedItemStatus.REPLACED);
				} else if (SVNEventAction.COMMIT_DELTA_SENT.equals(action)) {
					gui.addCommittedItem(fileName, CommittedItemStatus.DELTA_SENT);
				} else if (SVNEventAction.COMMIT_COMPLETED.equals(action)) {
					gui.addCommittedItem("Revision: " + event.getRevision(), CommittedItemStatus.COMPLETED);
					gui.setStatus(CommitStatus.COMMIT_COMPLETED, Long.toString(event.getRevision()));
				}
			} catch (Exception ex) {
				Manager.handle(ex);
			}
		}

		public void checkCancelled() throws SVNCancelException {}
	}

	public SVNURL getRootUrl() throws SVNException {
		if (rootUrl == null) {
			rootUrl = Manager.getInfo(new File(path)).getURL();
		}
		return rootUrl;
	}

	public void showChangesFromBase(File wcFile) throws Exception {
		SvnHelper.showChangesFromBase(gui, wcFile);
	}

	public void showPropertyChangesFromBase(File wcFile) throws Exception {
		SvnHelper.showPropertyChangesFromBase(gui, wcFile);
	}

	public void add(File wcFile, boolean addRecursively) throws SVNException {
		SVNClientManager mgrSvn = Manager.getSVNClientManagerForWorkingCopyOnly();
		SVNWCClient wcClient = mgrSvn.getWCClient();
		SVNDepth svnDepth = addRecursively ? SVNDepth.INFINITY : SVNDepth.EMPTY;
		wcClient.doAdd(wcFile, true, false, true, svnDepth, false, false, true);
		Manager.invalidate(wcFile);
	}

	public void createPatch(File[] wcFiles, OutputStream out) throws SVNException {
		SvnHelper.createPatch(new File(path), wcFiles, out);
	}

	public void ignore(File wcFile) throws SVNException {
		SVNClientManager mgrSvn = Manager.getSVNClientManagerForWorkingCopyOnly();
		SVNWCClient wcClient = mgrSvn.getWCClient();
		File dir = wcFile.getParentFile().getAbsoluteFile();
		SVNPropertyData property = wcClient.doGetProperty(dir, SVNProperty.IGNORE, SVNRevision.WORKING, SVNRevision.WORKING);
		String alreadyIgnoredItems = "";
		if (property != null) {
			alreadyIgnoredItems = property.getValue().getString();
		}
		wcClient.doSetProperty(dir, SVNProperty.IGNORE, SVNPropertyValue.create(alreadyIgnoredItems + wcFile.getName() + "\n"), false, SVNDepth.EMPTY, null,
		        null);
		Manager.invalidate(wcFile);
	}

	private class StatusEventHandler implements ISVNStatusHandler {

		public void handleStatus(SVNStatus status) throws SVNException {
			SVNStatusType svnContentStatus = status.getContentsStatus();
			SVNStatusType svnPropertiesStatus = status.getPropertiesStatus();
			ContentStatus contentStatus = null;

			if (SVNStatusType.STATUS_ADDED.equals(svnContentStatus)) {
				contentStatus = ContentStatus.ADDED;
			} else if (SVNStatusType.STATUS_CONFLICTED.equals(svnContentStatus)) {
				contentStatus = ContentStatus.CONFLICTED;
			} else if (SVNStatusType.STATUS_DELETED.equals(svnContentStatus)) {
				contentStatus = ContentStatus.DELETED;
			} else if (SVNStatusType.STATUS_EXTERNAL.equals(svnContentStatus)) {
				contentStatus = ContentStatus.EXTERNAL;
			} else if (SVNStatusType.STATUS_IGNORED.equals(svnContentStatus)) {
				contentStatus = ContentStatus.IGNORED;
			} else if (SVNStatusType.STATUS_INCOMPLETE.equals(svnContentStatus)) {
				contentStatus = ContentStatus.INCOMPLETE;
			} else if (SVNStatusType.MERGED.equals(svnContentStatus)) {
				contentStatus = ContentStatus.MERGED;
			} else if (SVNStatusType.STATUS_MISSING.equals(svnContentStatus)) {
				contentStatus = ContentStatus.MISSING;
			} else if (SVNStatusType.STATUS_MODIFIED.equals(svnContentStatus)) {
				contentStatus = ContentStatus.MODIFIED;
			} else if (SVNStatusType.STATUS_NONE.equals(svnContentStatus)) {
				contentStatus = ContentStatus.NONE;
			} else if (SVNStatusType.STATUS_NORMAL.equals(svnContentStatus)) {
				contentStatus = ContentStatus.NORMAL;
			} else if (SVNStatusType.STATUS_OBSTRUCTED.equals(svnContentStatus)) {
				contentStatus = ContentStatus.OBSTRUCTED;
			} else if (SVNStatusType.STATUS_REPLACED.equals(svnContentStatus)) {
				contentStatus = ContentStatus.REPLACED;
			} else if (SVNStatusType.STATUS_UNVERSIONED.equals(svnContentStatus)) {
				contentStatus = ContentStatus.UNVERSIONED;
			} else {
				throw new RuntimeException("not implemented: " + svnContentStatus);
			}

			ContentStatus propertyStatus = null;
			if (SVNStatusType.STATUS_ADDED.equals(svnPropertiesStatus)) {
				propertyStatus = ContentStatus.ADDED;
			} else if (SVNStatusType.STATUS_CONFLICTED.equals(svnPropertiesStatus)) {
				propertyStatus = ContentStatus.CONFLICTED;
			} else if (SVNStatusType.STATUS_DELETED.equals(svnPropertiesStatus)) {
				propertyStatus = ContentStatus.DELETED;
			} else if (SVNStatusType.STATUS_EXTERNAL.equals(svnPropertiesStatus)) {
				propertyStatus = ContentStatus.EXTERNAL;
			} else if (SVNStatusType.STATUS_IGNORED.equals(svnPropertiesStatus)) {
				propertyStatus = ContentStatus.IGNORED;
			} else if (SVNStatusType.STATUS_INCOMPLETE.equals(svnPropertiesStatus)) {
				propertyStatus = ContentStatus.INCOMPLETE;
			} else if (SVNStatusType.MERGED.equals(svnPropertiesStatus)) {
				propertyStatus = ContentStatus.MERGED;
			} else if (SVNStatusType.STATUS_MISSING.equals(svnPropertiesStatus)) {
				propertyStatus = ContentStatus.MISSING;
			} else if (SVNStatusType.STATUS_MODIFIED.equals(svnPropertiesStatus)) {
				propertyStatus = ContentStatus.MODIFIED;
			} else if (SVNStatusType.STATUS_NONE.equals(svnPropertiesStatus)) {
				propertyStatus = ContentStatus.NONE;
			} else if (SVNStatusType.STATUS_NORMAL.equals(svnPropertiesStatus)) {
				propertyStatus = ContentStatus.NORMAL;
			} else if (SVNStatusType.STATUS_OBSTRUCTED.equals(svnPropertiesStatus)) {
				propertyStatus = ContentStatus.OBSTRUCTED;
			} else if (SVNStatusType.STATUS_REPLACED.equals(svnPropertiesStatus)) {
				propertyStatus = ContentStatus.REPLACED;
			} else if (SVNStatusType.STATUS_UNVERSIONED.equals(svnPropertiesStatus)) {
				propertyStatus = ContentStatus.UNVERSIONED;
			} else {
				throw new RuntimeException("not implemented: " + svnContentStatus);
			}
			String conlictResult = null;
			SVNTreeConflictDescription treeConflict = status.getTreeConflict();
			if (treeConflict != null) {
				StringBuilder sb = new StringBuilder();
				sb.append("TREE CONFLICT\n");
				sb.append("Svn conflict action: ");
				sb.append(treeConflict.getConflictAction().getName());
				sb.append('\n');
				sb.append("Conflict reason: ");
				sb.append(treeConflict.getConflictReason().getName());
				sb.append('\n');
				sb.append("Operation: ");
				sb.append(treeConflict.getOperation());
				sb.append('\n');
				sb.append("Local " + treeConflict.getNodeKind().toString() + ": ");
				sb.append(treeConflict.getMergeFiles().getLocalFile());
				sb.append('\n');
				sb.append("Left: ");
				sb.append(treeConflict.getSourceLeftVersion().toString());
				sb.append('\n');
				sb.append("Right: ");
				sb.append(treeConflict.getSourceRightVersion().toString());
				sb.append('\n');

				conlictResult = StringHelper.convertMultilineTextToHtml(sb.toString());
			}

			try {
				gui.addItem(status.getFile(), contentStatus, propertyStatus, conlictResult, status.getKind());
			} catch (SVNException e) {
				throw e;
			} catch (Exception e) {
				Manager.handle(e);
			}
		}

	}

	public void revertChanges(File file) throws SVNException {
		SVNClientManager svnMgr = Manager.getSVNClientManagerForWorkingCopyOnly();
		SVNWCClient client = svnMgr.getWCClient();
		client.doRevert(new File[] { file }, SVNDepth.INFINITY, null);
		Manager.invalidate(file);
	}

	public void revertPropertyChanges(File file) throws SVNException {
		SVNClientManager svnMgr = Manager.getSVNClientManagerForWorkingCopyOnly();
		SVNWCClient client = svnMgr.getWCClient();
		client.doRevert(new File[] { file }, SVNDepth.EMPTY, null);
		Manager.invalidate(file);
	}

	public void delete(File file) throws SVNException, BackingStoreException {
		Delete delete = new Delete(file.getPath());
		delete.setAutoClose(true);
		delete.setIgnoreIfFileError(true);
		delete.execute();
		Manager.invalidate(file);
	}

	private static class RefreshISVNEventHandler implements ISVNEventHandler {

		private boolean cancel;

		public void handleEvent(SVNEvent event, double progress) throws SVNException {}

		public void checkCancelled() throws SVNCancelException {
			if (cancel) {
				throw new SVNCancelException();
			}

		}

		public void setCancel(boolean cancel) {
			this.cancel = cancel;
		}
	}

	// public void resolved(File file) throws SVNException {
	// SVNClientManager svnMgr =
	// Manager.getSVNClientManagerForWorkingCopyOnly();
	// SVNWCClient client = svnMgr.getWCClient();
	// client.doResolve(file, SVNDepth.INFINITY, true, true, true,
	// SVNConflictChoice.MERGED);
	// Manager.invalidate(file);
	// }

}
