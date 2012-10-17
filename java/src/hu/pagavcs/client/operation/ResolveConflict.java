package hu.pagavcs.client.operation;

import hu.pagavcs.client.bl.Manager;
import hu.pagavcs.client.gui.Refreshable;
import hu.pagavcs.client.gui.ResolveConflictGui;

import java.io.File;
import java.util.prefs.BackingStoreException;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.SVNInfo;

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
public class ResolveConflict {

	private String             path;
	private ResolveConflictGui gui;
	private final Refreshable  parentRefreshable;
	private final boolean      applyPatchConlict;

	public ResolveConflict(Refreshable parentRefreshable, String path, boolean applyPatchConlict) throws BackingStoreException, SVNException {
		this.parentRefreshable = parentRefreshable;
		this.path = path;
		this.applyPatchConlict = applyPatchConlict;
	}

	public void execute() throws Exception {
		File newFile = null;
		File oldFile = null;
		File wrkFile = null;
		File mixedFile = null;
		if (!applyPatchConlict) {
			SVNInfo info = Manager.getInfo(path);

			newFile = info.getConflictNewFile();
			oldFile = info.getConflictOldFile();
			wrkFile = info.getConflictWrkFile();
			mixedFile = info.getFile();
		} else {
			mixedFile = new File(path);
		}

		gui = new ResolveConflictGui(parentRefreshable, mixedFile, oldFile, newFile, wrkFile, applyPatchConlict);
		gui.display();
	}

}
