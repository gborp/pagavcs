package hu.pagavcs.operation;

import hu.pagavcs.bl.Manager;
import hu.pagavcs.bl.PagaException;
import hu.pagavcs.gui.LockGui;

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
public class LockOperation {

	private String  path;
	private LockGui gui;

	public LockOperation(String path) throws BackingStoreException {
		this.path = path;
	}

	public void execute() throws SVNException, PagaException {
		gui = new LockGui(this);
		gui.display();
		gui.setStatus(GeneralStatus.INIT);
		gui.setStatus(GeneralStatus.START);
		try {
			File wcFile = new File(path);
			SVNClientManager mgrSvn = Manager.getSVNClientManager(wcFile);
			SVNWCClient wcClient = mgrSvn.getWCClient();
			wcClient.doLock(new File[] { wcFile }, true, "Lock");
			Manager.invalidate(wcFile);
			gui.setStatus(GeneralStatus.COMPLETED);
		} catch (SVNException ex) {
			Manager.handle(ex);
			gui.setStatus(GeneralStatus.FAILED);
		}
	}

	public String getPath() {
		return path;
	}
}
