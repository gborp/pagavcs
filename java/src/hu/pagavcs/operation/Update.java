package hu.pagavcs.operation;

import hu.pagavcs.bl.Cancelable;
import hu.pagavcs.bl.FileStatusCache;
import hu.pagavcs.bl.Manager;
import hu.pagavcs.bl.FileStatusCache.STATUS;
import hu.pagavcs.gui.UpdateGui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import org.tmatesoft.svn.core.SVNCancelException;
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
		NONE, CONFLICTED, MERGED, RESOLVED
	}

	private List<File>  lstFile;
	private boolean     cancel;
	private UpdateGui   gui;
	private SVNRevision updateToRevision;
	private File        baseDir;
	private boolean     baseDirIsNotSvned;

	public Update(List<String> lstArg) throws Exception {
		lstFile = new ArrayList<File>();
		for (String fileName : lstArg) {
			lstFile.add(new File(fileName));
		}
		baseDir = Manager.getCommonBaseDir(lstFile);
		if (!FileStatusCache.getInstance().getStatus(baseDir).equals(STATUS.NONE)) {
			baseDirIsNotSvned = true;
		}
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
			SVNClientManager mgrSvn;
			if (!baseDirIsNotSvned) {
				mgrSvn = Manager.getSVNClientManager(baseDir);
			} else {
				mgrSvn = Manager.getSVNClientManager(lstFile.get(0));
			}

			gui.setWorkingCopy(baseDir.getPath());
			if (!baseDirIsNotSvned) {
				gui.setRepo(Manager.getInfo(baseDir).getURL().toDecodedString());
			} else {
				gui.setRepo("<Multiply repos>");
			}

			SVNUpdateClient updateClient = mgrSvn.getUpdateClient();
			updateClient.setEventHandler(new UpdateEventHandler(this, gui));
			gui.setStatus(ContentStatus.STARTED);
			boolean successOrExit = false;
			while (!successOrExit) {
				try {
					updateClient.doUpdate(lstFile.toArray(new File[0]), updateToRevision, SVNDepth.INFINITY, true, true);
					successOrExit = true;
				} catch (SVNCancelException ex) {
					// do nothing
				} catch (SVNException ex) {
					SVNErrorCode errorCode = ex.getErrorMessage().getErrorCode();
					if (SVNErrorCode.WC_LOCKED.equals(errorCode)) {
						int choosed = JOptionPane.showConfirmDialog(Manager.getRootFrame(), "Working copy is locked, do cleanup?", "Error",
						        JOptionPane.YES_NO_OPTION);
						if (choosed == JOptionPane.YES_OPTION) {
							Cleanup cleanup = new Cleanup(baseDir.getPath());
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
