package hu.pagavcs.operation;

import hu.pagavcs.bl.Cancelable;
import hu.pagavcs.bl.Manager;
import hu.pagavcs.bl.PagaException;
import hu.pagavcs.bl.SvnHelper;
import hu.pagavcs.bl.PagaException.PagaExceptionType;
import hu.pagavcs.gui.BlameListItem;
import hu.pagavcs.gui.OtherGui;
import hu.pagavcs.gui.UpdateGui;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.prefs.BackingStoreException;

import javax.swing.JOptionPane;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.ISVNAnnotateHandler;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNCopyClient;
import org.tmatesoft.svn.core.wc.SVNCopySource;
import org.tmatesoft.svn.core.wc.SVNInfo;
import org.tmatesoft.svn.core.wc.SVNLogClient;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
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
public class Other implements Cancelable {

	public enum OtherStatus {
		INIT, START, WORKING, COMPLETED, FAILED, CANCEL
	}

	private String   path;
	private OtherGui gui;
	private boolean  autoClose;
	private boolean  cancel;

	public Other(String path) throws BackingStoreException, SVNException {
		this.path = path;
	}

	public void execute() throws SVNException, BackingStoreException, PagaException {
		gui = new OtherGui(this);
		gui.display();
		gui.setStatus(OtherStatus.INIT);
		gui.setUrlHistory(SvnHelper.getRepoUrlHistory());
		File wcFile = new File(path);
		SVNClientManager mgrSvn = Manager.getSVNClientManager(new File(path));
		SVNWCClient wcClient = mgrSvn.getWCClient();
		gui.setStatus(OtherStatus.START);
		try {
			SVNInfo svnInfo = wcClient.doInfo(wcFile, SVNRevision.WORKING);

			gui.setURL(svnInfo.getURL().toDecodedString());

			gui.setStatus(OtherStatus.COMPLETED);
		} catch (SVNException ex) {
			Manager.handle(ex);
			gui.setStatus(OtherStatus.FAILED);
		}
		if (autoClose) {
			gui.close();
		}
	}

	public String getPath() {
		return path;
	}

	public void setAutoClose(boolean autoClose) {
		this.autoClose = autoClose;
	}

	public boolean isAutoClose() {
		return autoClose;
	}

	public void setCancel(boolean cancel) throws Exception {
		if (this.cancel != cancel) {
			this.cancel = cancel;
			if (cancel) {
				gui.setCancel(true);
			}
		}
	}

	public boolean isCancel() {
		return cancel;
	}

	public void copyMoveRename(String oldPath, String newPath, boolean copy) throws SVNException {
		SVNClientManager clientMgr = Manager.getSVNClientManagerForWorkingCopyOnly();
		SVNCopyClient copyClient = clientMgr.getCopyClient();
		SVNCopySource[] source = new SVNCopySource[] { new SVNCopySource(SVNRevision.UNDEFINED, SVNRevision.UNDEFINED, new File(oldPath)) };
		File dest = new File(newPath);
		copyClient.doCopy(source, dest, !copy, true, true);
	}

	public void doMerge(String urlTo, String pathTo, String urlFrom, String revisionRange, boolean reverseMerge) throws Exception {

		SvnHelper.doMerge(this, urlTo, pathTo, urlFrom, revisionRange, reverseMerge);
	}

	public void doSwitch(String wc, String toUrl, String toRevision) throws Exception {
		setCancel(false);
		UpdateGui updateGui = new UpdateGui(this, "Switch");
		updateGui.setPaths(Arrays.asList(new File(wc)));
		updateGui.display();
		try {
			updateGui.setStatus(ContentStatus.INIT);

			SVNClientManager clientMgr = Manager.getSVNClientManager(new File(path));
			SVNUpdateClient updateClient = clientMgr.getUpdateClient();
			updateClient.setIgnoreExternals(false);
			SVNRevision revision = null;
			toUrl = toUrl.trim();
			toRevision = toRevision.trim();
			if (toRevision.isEmpty()) {
				revision = SVNRevision.HEAD;
			} else {
				revision = SVNRevision.create(Long.valueOf(toRevision));
			}

			updateClient.setEventHandler(new UpdateEventHandler(this, updateGui));
			updateGui.setStatus(ContentStatus.STARTED);

			boolean successOrExit = false;
			while (!successOrExit) {
				try {
					updateClient.doSwitch(new File(wc), SVNURL.parseURIDecoded(toUrl), SVNRevision.UNDEFINED, revision, SVNDepth.INFINITY, true, true);
					successOrExit = true;
				} catch (SVNException ex) {
					if (SVNErrorCode.WC_LOCKED.equals(ex.getErrorMessage().getErrorCode())) {
						int choosed = JOptionPane.showConfirmDialog(Manager.getRootFrame(), "Working copy is locked, do cleanup?", "Error",
						        JOptionPane.YES_NO_OPTION);
						if (choosed == JOptionPane.YES_OPTION) {
							Cleanup cleanup = new Cleanup(path);
							cleanup.setAutoClose(true);
							cleanup.execute();
						} else {
							gui.setStatus(OtherStatus.CANCEL);
							successOrExit = true;
						}
					} else {
						throw ex;
					}
				}
			}

		} catch (Exception ex) {
			updateGui.setStatus(ContentStatus.FAILED);
			throw ex;
		}
	}

	public List<BlameListItem> doBlame(String path, String revision) throws SVNException, PagaException {
		revision = revision.trim();
		SVNRevision blameRevision;
		if (revision.isEmpty()) {
			blameRevision = SVNRevision.HEAD;
		} else {
			blameRevision = SVNRevision.create(Long.valueOf(revision));
		}

		SVNClientManager clientMgr = Manager.getSVNClientManager(new File(path));
		SVNLogClient logClient = clientMgr.getLogClient();
		final ArrayList<BlameListItem> lstBlame = new ArrayList<BlameListItem>();
		logClient.doAnnotate(new File(path), SVNRevision.HEAD, SVNRevision.create(1), blameRevision, new ISVNAnnotateHandler() {

			public void handleEOF() {

			}

			public void handleLine(Date date, long revision, String author, String line, Date mergedDate, long mergedRevision, String mergedAuthor,
			        String mergedPath, int lineNumber) throws SVNException {
				BlameListItem li = new BlameListItem();
				li.setDate(date);
				li.setRevision(revision);
				li.setAuthor(author);
				// convert tabulators to 4 spaces
				line = line.replace("\t", "    ");
				li.setLine(line);
				li.setLineNumber(lstBlame.size() + 1);
				lstBlame.add(li);
			}

			public void handleLine(Date date, long revision, String author, String line) throws SVNException {
				throw new RuntimeException("Not implemented");
			}

			public boolean handleRevision(Date date, long revision, String author, File contents) throws SVNException {
				return false;
			}

		});

		return lstBlame;
	}

	public void doExport(String pathFrom, String pathExport) throws SVNException, PagaException {

		File filePathFrom = new File(pathFrom);
		File filePathExport = new File(pathExport);

		if (!pathExport.startsWith("/")) {
			// relative path
			filePathExport = new File(filePathFrom.getParent() + "/" + pathExport);
		}

		SVNClientManager clientMgr = Manager.getSVNClientManagerForWorkingCopyOnly();
		SVNUpdateClient updateClient = clientMgr.getUpdateClient();

		if (filePathExport.isFile()) {
			throw new PagaException(PagaExceptionType.NOT_DIRECTORY);
		}
		if (!filePathExport.exists() || gui.exportPathExistsOverride(pathExport)) {
			updateClient.doExport(filePathFrom, filePathExport, SVNRevision.WORKING, SVNRevision.WORKING, null, true, SVNDepth.INFINITY);
		}
	}

	public void doRepoBrowser(String wcFile) throws Exception {
		new RepoBrowser(wcFile).execute();
	}

	public void doUpdateToRevision(String wc, String toRevision) throws Exception {
		SVNRevision revision;
		if (toRevision.isEmpty()) {
			revision = SVNRevision.HEAD;
		} else {
			revision = SVNRevision.create(Long.valueOf(toRevision));
		}
		Update update = new Update(Arrays.asList(wc));
		update.setUpdateToRevision(revision);
		update.execute();
	}

	public void doShowLog(String pathToShowLog) throws SVNException, BackingStoreException, Exception {
		new Log(pathToShowLog).execute();
	}

	public void storeUrlForHistory(String url) {
		SvnHelper.storeUrlForHistory(url);
	}
}
