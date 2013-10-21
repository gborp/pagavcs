package hu.pagavcs.client.operation;

import hu.pagavcs.client.bl.Cancelable;
import hu.pagavcs.client.bl.Manager;
import hu.pagavcs.client.bl.PagaException;
import hu.pagavcs.client.bl.SvnHelper;
import hu.pagavcs.client.gui.CopyMoveRenameGui;

import java.io.File;
import java.util.prefs.BackingStoreException;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNCopyClient;
import org.tmatesoft.svn.core.wc.SVNCopySource;
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
public class CopyMoveRename implements Cancelable {

	public enum CopyMoveRenameStatus {
		INIT, START, WORKING, COMPLETED, FAILED, CANCEL
	}

	public enum WorkingMode {
		WORKING_COPY, REPOSITORY
	}

	private String path;
	private String fromUrl;
	private CopyMoveRenameGui gui;
	private boolean autoClose;
	private boolean cancel;
	private WorkingMode workingMode;

	public CopyMoveRename() {
	}

	public void setFromPath(String fromPath) {
		this.path = fromPath;
	}

	public void setFromUrl(String fromUrl) {
		this.fromUrl = fromUrl;
	}

	public void execute() throws SVNException, BackingStoreException,
			PagaException {
		gui = new CopyMoveRenameGui(this);
		gui.display();
		gui.setStatus(CopyMoveRenameStatus.INIT);
		File wcFile = new File(path);
		SVNClientManager mgrSvn = Manager.getSVNClientManager(new File(path));
		SVNWCClient wcClient = mgrSvn.getWCClient();
		gui.setStatus(CopyMoveRenameStatus.START);
		try {
			SVNInfo svnInfo = wcClient.doInfo(wcFile, SVNRevision.WORKING);

			gui.setURL(svnInfo.getURL().toDecodedString());

			gui.setStatus(CopyMoveRenameStatus.COMPLETED);
		} catch (SVNException ex) {
			Manager.handle(ex);
			gui.setStatus(CopyMoveRenameStatus.FAILED);
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

	public void copyMoveRename(String oldPath, String newPath, boolean copy)
			throws SVNException {
		SVNClientManager clientMgr = Manager
				.getSVNClientManagerForWorkingCopyOnly();
		try {
			SVNCopyClient copyClient = clientMgr.getCopyClient();
			SVNCopySource[] source = new SVNCopySource[] { new SVNCopySource(
					SVNRevision.UNDEFINED, SVNRevision.UNDEFINED, new File(
							oldPath)) };
			File dest = new File(newPath);
			copyClient.doCopy(source, dest, !copy, true, true);
		} finally {
			clientMgr.dispose();
		}
	}

	public void storeUrlForHistory(String url) {
		SvnHelper.storeUrlForHistory(url);
	}

	public WorkingMode getWorkingMode() {
		return workingMode;
	}

	public void setWorkingMode(WorkingMode workingMode) {
		this.workingMode = workingMode;
	}

}
