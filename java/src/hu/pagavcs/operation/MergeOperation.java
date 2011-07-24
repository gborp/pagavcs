package hu.pagavcs.operation;

import hu.pagavcs.bl.Cancelable;
import hu.pagavcs.bl.Manager;
import hu.pagavcs.bl.PagaException;
import hu.pagavcs.bl.SvnHelper;
import hu.pagavcs.gui.MergeGui;

import java.io.File;
import java.util.prefs.BackingStoreException;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNInfo;
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
public class MergeOperation implements Cancelable {

	private String   path;
	private MergeGui gui;
	private boolean  cancel;
	private String   prefillMergeFromRevision;
	private String   prefillMergeFromUrl;
	private Boolean  prefillCommitToo;

	public MergeOperation(String path) throws BackingStoreException, SVNException {
		this.path = path;
	}

	public void setPrefillCommitNumber(String prefillMergeFromRevision) {
		this.prefillMergeFromRevision = prefillMergeFromRevision;
	}

	public void setPrefillMergeFromUrl(String prefillMergeFromUrl) {
		this.prefillMergeFromUrl = prefillMergeFromUrl;
	}

	public void setPrefillCommitToo(Boolean prefillCommitToo) {
		this.prefillCommitToo = prefillCommitToo;
	}

	public void execute() throws SVNException, BackingStoreException, PagaException {
		gui = new MergeGui(this);
		gui.display();
		gui.setUrlHistory(SvnHelper.getRepoUrlHistory());

		if (prefillMergeFromRevision != null) {
			gui.setPrefillMergeFromRevision(prefillMergeFromRevision);
		}
		if (prefillMergeFromUrl != null) {
			gui.setPrefillMergeFromUrl(prefillMergeFromUrl);
		}
		if (prefillCommitToo != null) {
			gui.setPrefillCommitToo(prefillCommitToo);
		}

		File wcFile = new File(path);
		SVNClientManager mgrSvn = Manager.getSVNClientManager(new File(path));
		SVNWCClient wcClient = mgrSvn.getWCClient();
		try {
			SVNInfo svnInfo = wcClient.doInfo(wcFile, SVNRevision.WORKING);
			gui.setURL(svnInfo.getURL().toDecodedString());
		} catch (SVNException ex) {
			Manager.handle(ex);
		} finally {
			mgrSvn.dispose();
		}
	}

	public String getPath() {
		return path;
	}

	public void setCancel(boolean cancel) throws Exception {
		this.cancel = cancel;
	}

	public boolean isCancel() {
		return cancel;
	}

	public void doMerge(String urlTo, String pathTo, String urlFrom, String revisionRange, boolean reverseMerge, boolean ignoreEolStyle) throws Exception {
		SvnHelper.doMerge(this, urlTo, pathTo, urlFrom, null, revisionRange, reverseMerge, ignoreEolStyle);
	}

	public void doShowLog(String pathToShowLog) throws SVNException, BackingStoreException, Exception {
		new Log(pathToShowLog).execute();
	}

	public void doShowLogUrl(String url) throws SVNException, BackingStoreException, Exception {
		new Log(url, false).execute();
	}

	public void storeUrlForHistory(String url) {
		SvnHelper.storeUrlForHistory(url);
	}

}
