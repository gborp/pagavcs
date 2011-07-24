package hu.pagavcs.operation;

import hu.pagavcs.bl.Manager;
import hu.pagavcs.gui.CleanupGui;

import java.io.File;
import java.util.prefs.BackingStoreException;

import org.tmatesoft.svn.core.SVNException;
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
public class Cleanup {

	public enum CleanupStatus {
		INIT, START, COMPLETED, FAILED
	}

	private String     path;
	private CleanupGui gui;
	private boolean    autoClose;

	public Cleanup(String path) throws BackingStoreException {
		this.path = path;
	}

	public void execute() throws SVNException, BackingStoreException {
		gui = new CleanupGui(this);
		gui.display();
		gui.setStatus(CleanupStatus.INIT);
		File wcFile = new File(path);
		SVNClientManager mgrSvn = Manager.getSVNClientManagerForWorkingCopyOnly();
		SVNWCClient wcClient = mgrSvn.getWCClient();
		gui.setStatus(CleanupStatus.START);
		try {
			wcClient.doCleanup(wcFile);
			gui.setStatus(CleanupStatus.COMPLETED);
			if (autoClose) {
				gui.close();
			}
		} catch (SVNException ex) {
			Manager.handle(ex);
			gui.setStatus(CleanupStatus.FAILED);
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
