package hu.pagavcs.client.operation;

import hu.pagavcs.client.bl.Manager;
import hu.pagavcs.client.bl.UpdateCancelable;
import hu.pagavcs.client.gui.UpdateGui;
import hu.pagavcs.server.FileStatusCache;
import hu.pagavcs.server.FileStatusCache.STATUS;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
public class Update implements UpdateCancelable {

	public enum UpdateContentStatus {
		NONE, CONFLICTED, MERGED, RESOLVED
	}

	private List<File> lstFile;
	private boolean cancel;
	private UpdateGui gui;
	private SVNRevision updateToRevision;
	private File baseDir;
	private boolean baseDirIsNotSvned;
	private boolean autoClose;
	private SVNRevision previousWorkingCopyRevision;

	public Update(List<String> lstArg) throws Exception {
		lstFile = new ArrayList<File>();
		for (String fileName : lstArg) {
			lstFile.add(new File(fileName));
		}
		baseDir = Manager.getCommonBaseDir(lstFile);
		if (FileStatusCache.getInstance().getStatus(baseDir)
				.equals(STATUS.NONE)) {
			baseDirIsNotSvned = true;
		}
		setCancel(false);
		setUpdateToRevision(SVNRevision.HEAD);
	}

	public void setAutoClose(boolean autoClose) {
		this.autoClose = autoClose;
	}

	public boolean isAutoClose() {
		return autoClose;
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

		ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(
				1);
		executor.scheduleAtFixedRate(new Runnable() {

			public void run() {
				gui.setBandwidth(Manager.getBandwidthMeter().getBandwidth());
			}

		}, 0, 1, TimeUnit.SECONDS);
		SVNClientManager mgrSvn = null;
		try {
			gui.setStatus(ContentStatus.INIT);
			File fileForSvnManager;
			if (!baseDirIsNotSvned) {
				fileForSvnManager = baseDir;
			} else {
				fileForSvnManager = lstFile.get(0);
			}
			mgrSvn = Manager.getSVNClientManager(fileForSvnManager);
			previousWorkingCopyRevision = mgrSvn.getStatusClient()
					.doStatus(fileForSvnManager, false).getRevision();

			gui.setWorkingCopy(baseDir.getPath());
			if (!baseDirIsNotSvned) {
				gui.setRepo(Manager.getInfo(baseDir).getURL().toDecodedString());
			} else {
				gui.setRepo("<Multiply repos>");
			}
			gui.setPaths(lstFile);

			SVNUpdateClient updateClient = mgrSvn.getUpdateClient();
			updateClient.setEventHandler(new UpdateEventHandler(this, gui));

			gui.setStatus(ContentStatus.STARTED);
			boolean successOrExit = false;
			while (!successOrExit) {
				try {
					updateClient.doUpdate(lstFile.toArray(new File[0]),
							updateToRevision, SVNDepth.INFINITY, true, true);
					successOrExit = true;
				} catch (SVNCancelException ex) {
					successOrExit = true;
				} catch (SVNException ex) {
					SVNErrorCode errorCode = ex.getErrorMessage()
							.getErrorCode();
					if (SVNErrorCode.WC_LOCKED.equals(errorCode)) {
						File file = (File) ex.getErrorMessage()
								.getRelatedObjects()[0];
						int choosed = JOptionPane.showConfirmDialog(
								Manager.getRootFrame(),
								"Working copy " + file.getPath()
										+ " is locked, do cleanup?",
								"Working copy locked, cleanup?",
								JOptionPane.YES_NO_OPTION);
						if (choosed == JOptionPane.YES_OPTION) {
							Cleanup cleanup = new Cleanup(file.getPath());
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
			if (autoClose && !gui.hasConflicted()) {
				gui.close();
			}
		} catch (Exception ex) {
			gui.setStatus(ContentStatus.FAILED);
			throw ex;
		} finally {
			if (mgrSvn != null) {
				mgrSvn.dispose();
			}
			executor.shutdown();
		}
	}

	public void setCancel(boolean cancel) throws Exception {
		if (cancel) {
			gui.addItem("", null, ContentStatus.CANCEL, null);
		}
		this.cancel = cancel;
	}

	public boolean isCancel() {
		return cancel;
	}

	public SVNRevision getPreviousWorkingCopyRevision() {
		return previousWorkingCopyRevision;
	}
}
