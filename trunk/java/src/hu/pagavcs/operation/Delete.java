package hu.pagavcs.operation;

import hu.pagavcs.bl.Manager;
import hu.pagavcs.gui.DeleteGui;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.prefs.BackingStoreException;

import org.tmatesoft.svn.core.SVNErrorCode;
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
public class Delete {

	public enum DeleteStatus {
		INIT, START, COMPLETED, FAILED
	}

	private List<String> lstPath;
	private DeleteGui    gui;
	private boolean      autoClose;
	private boolean      ignoreIfFileError;

	public Delete(String path) throws BackingStoreException, SVNException {
		this(Collections.singletonList(path));
	}

	public Delete(List<String> lstPath) throws BackingStoreException, SVNException {
		this.lstPath = lstPath;
	}

	public void execute() throws SVNException {
		gui = new DeleteGui(this);
		gui.display();
		gui.setStatus(DeleteStatus.INIT);
		gui.setStatus(DeleteStatus.START);
		try {

			for (String path : lstPath) {
				File wcFile = new File(path);
				SVNClientManager mgrSvn = Manager.getSVNClientManagerForWorkingCopyOnly();
				SVNWCClient wcClient = mgrSvn.getWCClient();

				try {
					wcClient.doDelete(wcFile, true, true, false);
				} catch (SVNException ex) {
					SVNErrorCode errorCode = ex.getErrorMessage().getErrorCode();
					if (ignoreIfFileError && (SVNErrorCode.BAD_FILENAME.equals(errorCode) || SVNErrorCode.WC_NOT_DIRECTORY.equals(errorCode))) {
						// ignore exception
					} else {
						throw ex;
					}
				}
				Manager.invalidate(wcFile);
			}

			gui.setStatus(DeleteStatus.COMPLETED);
			if (autoClose) {
				gui.close();
			}
		} catch (SVNException ex) {
			Manager.handle(ex);
			gui.setStatus(DeleteStatus.FAILED);
		}

	}

	public List<String> getPath() {
		return lstPath;
	}

	public void setAutoClose(boolean autoClose) {
		this.autoClose = autoClose;
	}

	public boolean isAutoClose() {
		return autoClose;
	}

	public void setIgnoreIfFileError(boolean ignoreIfFileError) {
		this.ignoreIfFileError = ignoreIfFileError;

	}
}
