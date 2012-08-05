package hu.pagavcs.client.operation;

import hu.pagavcs.client.bl.Manager;

import java.io.File;
import java.util.List;

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
public class AddOperation {

	private final List<String> lstPath;

	public AddOperation(List<String> lstPath) {
		this.lstPath = lstPath;
	}

	public void execute() throws Exception {

		boolean addRecursively = true;
		for (String path : lstPath) {
			File wcFile = new File(path);
			SVNClientManager mgrSvn = Manager
					.getSVNClientManagerForWorkingCopyOnly();
			try {
				SVNWCClient wcClient = mgrSvn.getWCClient();
				SVNDepth svnDepth = addRecursively ? SVNDepth.INFINITY
						: SVNDepth.EMPTY;
				wcClient.doAdd(wcFile, true, false, true, svnDepth, false,
						false, true);
				Manager.invalidate(wcFile);
			} finally {
				mgrSvn.dispose();
			}
		}
	}

}
