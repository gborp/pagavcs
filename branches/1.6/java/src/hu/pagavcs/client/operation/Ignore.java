package hu.pagavcs.client.operation;

import hu.pagavcs.client.bl.Manager;
import hu.pagavcs.client.gui.IgnoreGui;

import java.io.File;
import java.util.prefs.BackingStoreException;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNPropertyData;
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
public class Ignore {

	private String    path;
	private IgnoreGui gui;

	public Ignore(String path) throws BackingStoreException {
		this.path = path;
	}

	public void execute() throws SVNException {
		gui = new IgnoreGui(this);
		gui.display();
		gui.setStatus(GeneralStatus.INIT);
		gui.setStatus(GeneralStatus.START);
		SVNClientManager mgrSvn = Manager.getSVNClientManagerForWorkingCopyOnly();
		try {
			File wcFile = new File(path);
			SVNWCClient wcClient = mgrSvn.getWCClient();
			File dir = wcFile.getParentFile().getAbsoluteFile();
			SVNPropertyData property = wcClient.doGetProperty(dir, SVNProperty.IGNORE, SVNRevision.WORKING, SVNRevision.WORKING);
			String alreadyIgnoredItems = "";
			if (property != null) {
				alreadyIgnoredItems = property.getValue().getString();
			}

			wcClient.doSetProperty(dir, SVNProperty.IGNORE, SVNPropertyValue.create(alreadyIgnoredItems + wcFile.getName() + "\n"), false, SVNDepth.EMPTY,
			        null, null);
			Manager.invalidate(wcFile);
			gui.setStatus(GeneralStatus.COMPLETED);
		} catch (SVNException ex) {
			Manager.handle(ex);
			gui.setStatus(GeneralStatus.FAILED);
		} finally {
			mgrSvn.dispose();
		}
	}

	public String getPath() {
		return path;
	}
}
