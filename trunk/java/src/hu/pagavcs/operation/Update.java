package hu.pagavcs.operation;

import hu.pagavcs.bl.Cancelable;
import hu.pagavcs.bl.Manager;
import hu.pagavcs.gui.UpdateGui;

import java.io.File;

import javax.swing.JOptionPane;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;

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
public class Update implements Cancelable {

	public enum UpdateContentStatus {
		NONE, CONFLICTED, MERGED
	}

	String              path;
	private boolean     cancel;
	private UpdateGui   gui;
	private SVNRevision updateToRevision;

	public Update(String path) throws Exception {
		this.path = path;
		setCancel(false);
		setUpdateToRevision(SVNRevision.HEAD);
	}

	public SVNRevision getUpdateToRevision() {
		return updateToRevision;
	}

	public void setUpdateToRevision(SVNRevision updateToRevision) {
		this.updateToRevision = updateToRevision;
	}

	public void execute() throws Exception {
		gui = new UpdateGui(this);
		gui.display();
		try {
			gui.setStatus(ContentStatus.INIT);
			SVNClientManager mgrSvn = Manager.getSVNClientManager(new File(path));

			gui.setWorkingCopy(path);
			gui.setRepo(Manager.getInfo(path).getURL().toDecodedString());

			SVNUpdateClient updateClient = mgrSvn.getUpdateClient();
			updateClient.setEventHandler(new UpdateEventHandler(this, gui));
			gui.setStatus(ContentStatus.STARTED);
			boolean successOrExit = false;
			while (!successOrExit) {
				try {
					updateClient.doUpdate(new File(path), updateToRevision, SVNDepth.INFINITY, true, true);
					successOrExit = true;
				} catch (SVNException ex) {

					if (SVNErrorCode.WC_LOCKED.equals(ex.getErrorMessage().getErrorCode())) {
						int choosed = JOptionPane.showConfirmDialog(Manager.getRootFrame(), "Working copy is locked, do cleanup?", "Error",
						        JOptionPane.YES_NO_OPTION);
						if (choosed == JOptionPane.YES_OPTION) {
							Cleanup cleanup = new Cleanup(path);
							cleanup.setAutoClose(true);
							cleanup.execute();
						} else {
							gui.setStatus(ContentStatus.CANCEL);
							successOrExit = true;
						}
					} else {
						throw ex;
					}

				}
			}
		} catch (Exception ex) {
			gui.setStatus(ContentStatus.FAILED);
			throw ex;
		}
	}

	public void setCancel(boolean cancel) throws Exception {
		if (cancel) {
			gui.addItem("", null, ContentStatus.CANCEL);
		}
		this.cancel = cancel;
	}

	public boolean isCancel() {
		return cancel;
	}
}
