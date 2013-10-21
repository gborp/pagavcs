package hu.pagavcs.client.operation;

import hu.pagavcs.client.bl.Cancelable;
import hu.pagavcs.client.bl.Manager;
import hu.pagavcs.client.bl.PagaException;
import hu.pagavcs.client.gui.repobrowser.RepoBrowserGui;
import hu.pagavcs.client.operation.CopyMoveRename.WorkingMode;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.BackingStoreException;

import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNCommitClient;
import org.tmatesoft.svn.core.wc.SVNInfo;
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
public class RepoBrowser implements Cancelable {

	public enum RepoBrowserStatus {
		INIT, START, WORKING, COMPLETED, FAILED, CANCEL
	}

	private String path;
	private RepoBrowserGui gui;
	private boolean cancel;
	private SVNRepository repo;

	public RepoBrowser(String path) throws BackingStoreException, SVNException {
		this.path = path;
	}

	public SVNClientManager getSvnClientManager() throws Exception {
		SVNClientManager mgrSvn = Manager.getSVNClientManager(new File(path));
		return mgrSvn;
	}

	public void execute() throws Exception {
		gui = new RepoBrowserGui(this);
		gui.display();
		gui.setStatus(RepoBrowserStatus.INIT);

		gui.setStatus(RepoBrowserStatus.START);
		SVNClientManager mgrSvn = getSvnClientManager();

		if (mgrSvn != null) {
			SVNWCClient wcClient = mgrSvn.getWCClient();

			try {
				SVNInfo svnInfo = wcClient.doInfo(new File(path),
						SVNRevision.WORKING);
				repo = mgrSvn.getRepositoryPool().createRepository(
						svnInfo.getURL(), true);
				gui.setURL(svnInfo.getURL().toDecodedString());
				gui.setWorkingCopy(path);
				gui.setStatus(RepoBrowserStatus.COMPLETED);
			} catch (SVNException ex) {
				Manager.handle(ex);
				gui.setStatus(RepoBrowserStatus.FAILED);
			} finally {
				mgrSvn.dispose();
			}
		}
	}

	public String getPath() {
		return path;
	}

	public void setCancel(boolean cancel) {
		// if (cancel) {
		// gui.addItem("", UpdateStatus.CANCEL);
		// }
		this.cancel = cancel;
	}

	public boolean isCancel() {
		return cancel;
	}

	private String getRelativePath(SVNRepository repo, String fullPath)
			throws SVNException {
		return fullPath.substring(repo.getRepositoryRoot(true).getPath()
				.length());
	}

	public SVNDirEntry getDirEntry(String url) throws SVNException,
			PagaException {
		SVNURL svnUrl = SVNURL.parseURIDecoded(url);
		String relativePath = getRelativePath(repo, svnUrl.getPath());
		return repo.info(relativePath, SVNRevision.HEAD.getNumber());
	}

	public List<SVNDirEntry> getDirEntryChain(String url) throws SVNException,
			PagaException {
		SVNURL svnUrl2 = SVNURL.parseURIDecoded(url);
		SVNClientManager mgrSvn = Manager.getSVNClientManager(svnUrl2);
		try {
			repo = mgrSvn.getRepositoryPool().createRepository(svnUrl2, true);
			String relativePath = repo.getLocation().getPath();
			repo.setLocation(repo.getRepositoryRoot(false), false);
			String rootPath = repo.getLocation().getPath();
			List<SVNDirEntry> result = getDirEntryChain2(repo,
					relativePath.substring(rootPath.length()));
			return result;
		} finally {
			mgrSvn.dispose();
		}
	}

	private List<SVNDirEntry> getDirEntryChain2(SVNRepository repo2,
			String relativePath) throws SVNException, PagaException {
		ArrayList<SVNDirEntry> lstResult = new ArrayList<SVNDirEntry>();
		SVNDirEntry result = repo2.info(relativePath,
				SVNRevision.HEAD.getNumber());
		String parentRelativePath = SVNPathUtil.removeTail(relativePath);
		if (!parentRelativePath.equals(relativePath)) {
			lstResult.addAll(getDirEntryChain2(repo2, parentRelativePath));
		}
		lstResult.add(result);
		return lstResult;
	}

	public List<SVNDirEntry> getChilds(String url) throws SVNException,
			PagaException {
		SVNURL svnUrl = SVNURL.parseURIDecoded(url);
		String relativePath = getRelativePath(repo, svnUrl.getPath());

		ArrayList<SVNDirEntry> entries = new ArrayList<SVNDirEntry>();
		repo.getDir(relativePath, SVNRevision.HEAD.getNumber(), false, entries);
		return entries;
	}

	public void createFolder(SVNDirEntry svnDirEntry, String folderName,
			String logMessage) throws SVNException {
		ISVNEditor editor = repo.getCommitEditor(logMessage, null /* locks */,
				false /* keepLocks */, null /* mediator */);
		try {
			editor.openRoot(SVNRepository.INVALID_REVISION);

			editor.addDir(svnDirEntry.getRelativePath() + "/" + folderName,
					null, -1);
			editor.closeDir();
			editor.closeDir();

			// SVNCommitInfo info =
			editor.closeEdit();
		} catch (SVNException svne) {
			editor.abortEdit();
			throw svne;
		}

	}

	public void copy(String fromUrl) throws SVNException, PagaException,
			BackingStoreException {

		CopyMoveRename copyMoveRename = new CopyMoveRename();
		copyMoveRename.setWorkingMode(WorkingMode.REPOSITORY);
		copyMoveRename.setFromUrl(fromUrl);
		copyMoveRename.execute();
	}

	public void delete(SVNURL url, String message) throws Exception {
		SVNCommitClient commitClient = getSvnClientManager().getCommitClient();
		commitClient.doDelete(new SVNURL[] { url }, message);
	}
}
