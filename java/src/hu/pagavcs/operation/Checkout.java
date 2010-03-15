package hu.pagavcs.operation;

import hu.pagavcs.bl.Cancelable;
import hu.pagavcs.bl.Manager;
import hu.pagavcs.bl.SvnHelper;
import hu.pagavcs.gui.CheckoutGui;
import hu.pagavcs.gui.UpdateGui;

import java.io.File;
import java.util.Arrays;
import java.util.prefs.BackingStoreException;

import org.tmatesoft.svn.core.SVNCancelException;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;

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
public class Checkout implements Cancelable {

	public static final long HEAD_REVISION = -1;

	public enum CleanupStatus {
		INIT, START, COMPLETED, FAILED
	}

	private String      path;
	private CheckoutGui gui;
	private boolean     cancel;

	public Checkout(String path) throws BackingStoreException {
		this.path = path;
	}

	public void execute() throws SVNException, BackingStoreException {
		gui = new CheckoutGui(this);
		gui.display();
		gui.setUrlHistory(SvnHelper.getRepoUrlHistory());

		// TODO check if directory is under version control
	}

	public String getPath() {
		return path;
	}

	public void doCheckout(String url, String dir, long revision) throws Exception {
		UpdateGui updateGui = new UpdateGui(this, "Checkout");
		updateGui.setPaths(Arrays.asList(new File(dir)));
		updateGui.display();
		try {
			updateGui.setStatus(ContentStatus.INIT);

			SVNRevision svnRevision = null;
			if (revision == HEAD_REVISION) {
				svnRevision = SVNRevision.HEAD;
			} else {
				svnRevision = SVNRevision.create(revision);
			}
			SVNURL svnUrl = SVNURL.parseURIDecoded(url);

			SVNClientManager mgrSvn = Manager.getSVNClientManager(svnUrl);
			SVNUpdateClient updateClient = mgrSvn.getUpdateClient();
			updateClient.setEventHandler(new UpdateEventHandler(this, updateGui));
			updateGui.setStatus(ContentStatus.STARTED);
			updateClient.doCheckout(svnUrl, new File(dir), SVNRevision.UNDEFINED, svnRevision, SVNDepth.INFINITY, true);
		} catch (SVNCancelException ex) {
			updateGui.setStatus(ContentStatus.CANCEL);
		} catch (Exception ex) {
			updateGui.setStatus(ContentStatus.FAILED);
			throw ex;
		}
	}

	public void setCancel(boolean cancel) {
		this.cancel = cancel;
	}

	public boolean isCancel() {
		return cancel;
	}

	public void storeUrlForHistory(String url) {
		SvnHelper.storeUrlForHistory(url);
	}
}