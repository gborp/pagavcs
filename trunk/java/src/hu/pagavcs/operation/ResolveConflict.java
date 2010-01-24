package hu.pagavcs.operation;

import hu.pagavcs.bl.Manager;
import hu.pagavcs.gui.Refreshable;
import hu.pagavcs.gui.ResolveConflictGui;

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

	public ResolveConflict(Refreshable parentRefreshable, String path) throws BackingStoreException, SVNException {
		this.parentRefreshable = parentRefreshable;
		this.path = path;
	}

	public void execute() throws Exception {

		SVNInfo info = Manager.getInfo(path);

		File newFile = info.getConflictNewFile();
		File oldFile = info.getConflictOldFile();
		File wrkFile = info.getConflictWrkFile();
		File mixedFile = info.getFile();

		gui = new ResolveConflictGui(parentRefreshable, mixedFile, oldFile, newFile, wrkFile);
		gui.display();
	}

}
