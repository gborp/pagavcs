/**
 * 
 */
package hu.pagavcs.operation;

import hu.pagavcs.bl.Cancelable;
import hu.pagavcs.bl.Manager;
import hu.pagavcs.gui.UpdateGui;
import hu.pagavcs.operation.Update.UpdateContentStatus;

import java.io.File;

import org.tmatesoft.svn.core.SVNCancelException;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNEvent;
import org.tmatesoft.svn.core.wc.SVNEventAction;
import org.tmatesoft.svn.core.wc.SVNStatus;
import org.tmatesoft.svn.core.wc.SVNStatusClient;
import org.tmatesoft.svn.core.wc.SVNStatusType;

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
class UpdateEventHandler implements ISVNEventHandler {

	private UpdateGui        gui;
	private final Cancelable cancelable;
	private final File       path;

	public UpdateEventHandler(Cancelable cancelable, UpdateGui gui, File path) {
		this.cancelable = cancelable;
		this.gui = gui;
		this.path = path;
	}

	public void handleEvent(SVNEvent event, double progress) throws SVNException {
		try {
			SVNEventAction action = event.getAction();
			SVNStatusType contentStatus = event.getContentsStatus();
			UpdateContentStatus updateContentStatus = null;
			if (SVNStatusType.CONFLICTED.equals(contentStatus)) {
				updateContentStatus = UpdateContentStatus.CONFLICTED;
			} else if (SVNStatusType.MERGED.equals(contentStatus)) {
				updateContentStatus = UpdateContentStatus.MERGED;
			}

			String fileName = event.getFile() != null ? event.getFile().getAbsolutePath() : null;
			if (SVNEventAction.UPDATE_NONE.equals(action)) {

				gui.addItem(fileName, updateContentStatus, ContentStatus.NONE);

			} else if (SVNEventAction.UPDATE_ADD.equals(action)) {
				gui.addItem(fileName, updateContentStatus, ContentStatus.ADDED);
			} else if (SVNEventAction.UPDATE_DELETE.equals(action)) {
				gui.addItem(fileName, updateContentStatus, ContentStatus.DELETED);
			} else if (SVNEventAction.UPDATE_EXISTS.equals(action)) {
				gui.addItem(fileName, updateContentStatus, ContentStatus.EXISTS);
			} else if (SVNEventAction.UPDATE_EXTERNAL.equals(action)) {
				gui.addItem(fileName, updateContentStatus, ContentStatus.EXTERNAL);
			} else if (SVNEventAction.UPDATE_REPLACE.equals(action)) {
				gui.addItem(fileName, updateContentStatus, ContentStatus.REPLACED);
			} else if (SVNEventAction.UPDATE_UPDATE.equals(action)) {
				gui.addItem(fileName, updateContentStatus, ContentStatus.UPDATE);
			} else if (SVNEventAction.UPDATE_COMPLETED.equals(action)) {

				SVNClientManager mgrSvn = Manager.getSVNClientManagerForWorkingCopyOnly();
				SVNStatusClient statusClient = mgrSvn.getStatusClient();
				SVNStatus svnStatus = statusClient.doStatus(path, false);

				gui.addItem("Revision number: " + svnStatus.getRevision().getNumber(), updateContentStatus, ContentStatus.COMPLETED);
			}

			// TODO SVNEventAction.MERGE_COMPLETE
		} catch (SVNException e) {
			throw e;
		} catch (Exception e) {
			Manager.handle(e);
		}

	}

	public void checkCancelled() throws SVNCancelException {
		if (cancelable.isCancel()) {
			throw new SVNCancelException();
		}
	}
}
