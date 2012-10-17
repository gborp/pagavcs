package hu.pagavcs.client.operation;

import hu.pagavcs.client.bl.Manager;
import hu.pagavcs.client.gui.RevertGui;

import java.io.File;
import java.util.prefs.BackingStoreException;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.wc.SVNClientManager;
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
public class Revert {

	public enum RevertStatus {
		INIT, START, REVERTED, FAILED
	}

	private String  path;
	private boolean autoClose;

	public Revert(String path) throws BackingStoreException {
		this.path = path;
	}

	public void execute() throws Exception {
		RevertGui gui = new RevertGui(this);
		SVNClientManager mgrSvn = Manager.getSVNClientManagerForWorkingCopyOnly();
		try {
			gui.setStatus(RevertStatus.INIT);
			File wcFile = new File(path);
			SVNWCClient wcClient = mgrSvn.getWCClient();
			gui.setStatus(RevertStatus.START);
			wcClient.doRevert(new File[] { wcFile }, SVNDepth.INFINITY, null);
			Manager.invalidate(wcFile);
			gui.setStatus(RevertStatus.REVERTED);
			if (autoClose) {
				gui.close();
			}
		} catch (Exception ex) {
			gui.setStatus(RevertStatus.FAILED);
			throw ex;
		} finally {
			mgrSvn.dispose();
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

}
