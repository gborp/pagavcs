package hu.pagavcs.operation;

import hu.pagavcs.bl.Manager;
import hu.pagavcs.gui.CreateRepoGui;

import java.io.File;
import java.util.prefs.BackingStoreException;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;

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
public class CreateRepo {

	public enum CreateRepoStatus {
		INIT, START, COMPLETED, FAILED
	}

	private String        path;
	private CreateRepoGui gui;
	private boolean       autoClose;

	public CreateRepo(String path) throws BackingStoreException {
		this.path = path;
	}

	public void execute() throws SVNException, BackingStoreException {
		gui = new CreateRepoGui(this);
		gui.display();
		gui.setStatus(CreateRepoStatus.INIT);
		File wcFile = new File(path);
		gui.setStatus(CreateRepoStatus.START);
		try {
			SVNRepositoryFactory.createLocalRepository(wcFile, true, false);
			gui.setStatus(CreateRepoStatus.COMPLETED);
			if (autoClose) {
				gui.close();
			}
		} catch (SVNException ex) {
			Manager.handle(ex);
			gui.setStatus(CreateRepoStatus.FAILED);
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
