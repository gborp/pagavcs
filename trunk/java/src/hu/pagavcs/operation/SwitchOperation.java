package hu.pagavcs.operation;

import hu.pagavcs.bl.Cancelable;
import hu.pagavcs.bl.Manager;
import hu.pagavcs.bl.PagaException;
import hu.pagavcs.bl.SvnHelper;
import hu.pagavcs.gui.SwitchGui;
import hu.pagavcs.gui.UpdateGui;

import java.io.File;
import java.util.Arrays;
import java.util.prefs.BackingStoreException;

import javax.swing.JOptionPane;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNInfo;
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
public class SwitchOperation implements Cancelable {

	public enum SwitchStatus {
		INIT, START, WORKING, COMPLETED, FAILED, CANCEL
	}

	private String    path;
	private SwitchGui gui;
	private boolean   autoClose;
	private boolean   cancel;

	public SwitchOperation(String path) throws BackingStoreException, SVNException {
		this.path = path;
	}

	public void execute() throws SVNException, BackingStoreException, PagaException {
		gui = new SwitchGui(this);
		gui.display();
		gui.setStatus(SwitchStatus.INIT);
		File wcFile = new File(path);
		SVNClientManager mgrSvn = Manager.getSVNClientManager(new File(path));
		SVNWCClient wcClient = mgrSvn.getWCClient();
		gui.setStatus(SwitchStatus.START);
		try {
			SVNInfo svnInfo = wcClient.doInfo(wcFile, SVNRevision.WORKING);

			gui.setURL(svnInfo.getURL().toDecodedString());

			gui.setStatus(SwitchStatus.COMPLETED);
		} catch (SVNException ex) {
			Manager.handle(ex);
			gui.setStatus(SwitchStatus.FAILED);
		} finally {
			mgrSvn.dispose();
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

	public void doSwitch(String wc, String toUrl, String toRevision) throws Exception {
		setCancel(false);
		UpdateGui updateGui = new UpdateGui(this, "Switching...");
		updateGui.setPaths(Arrays.asList(new File(wc)));
		updateGui.display();
		SVNClientManager clientMgr = null;
		try {
			updateGui.setStatus(ContentStatus.INIT);

			clientMgr = Manager.getSVNClientManager(new File(path));
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
							gui.setStatus(SwitchStatus.CANCEL);
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
		} finally {
			clientMgr.dispose();
		}
	}

	public void doShowLog(String pathToShowLog) throws SVNException, BackingStoreException, Exception {
		new Log(pathToShowLog).execute();
	}

	public void storeUrlForHistory(String url) {
		SvnHelper.storeUrlForHistory(url);
	}
}
