package hu.pagavcs.client.operation;

import hu.pagavcs.client.bl.Manager;
import hu.pagavcs.client.bl.PagaException;
import hu.pagavcs.client.gui.UnlockGui;

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
public class UnlockOperation {

	private String    path;
	private UnlockGui gui;

	public UnlockOperation(String path) throws BackingStoreException {
		this.path = path;
	}

	public void execute() throws SVNException, PagaException {
		gui = new UnlockGui(this);
		gui.display();
		gui.setStatus(GeneralStatus.INIT);
		gui.setStatus(GeneralStatus.START);
		SVNClientManager mgrSvn = null;
		try {
			File wcFile = new File(path);
			mgrSvn = Manager.getSVNClientManager(wcFile);
			SVNWCClient wcClient = mgrSvn.getWCClient();
			wcClient.doUnlock(new File[] { wcFile }, true);
			Manager.invalidate(wcFile);
			gui.setStatus(GeneralStatus.COMPLETED);
		} catch (SVNException ex) {
			Manager.handle(ex);
			gui.setStatus(GeneralStatus.FAILED);
		} finally {
			if (mgrSvn != null) {
				mgrSvn.dispose();
			}
		}
	}

	public String getPath() {
		return path;
	}
}
