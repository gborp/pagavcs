/**
 * 
 */
package hu.pagavcs.client.operation;

import hu.pagavcs.client.bl.Cancelable;
import hu.pagavcs.client.bl.Manager;
import hu.pagavcs.client.gui.UpdateGui;
import hu.pagavcs.client.operation.Update.UpdateContentStatus;

import org.tmatesoft.svn.core.SVNCancelException;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.SVNEvent;
import org.tmatesoft.svn.core.wc.SVNEventAction;
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
public class UpdateEventHandler implements ISVNEventHandler {

	private int count;
	private UpdateGui gui;
	private final Cancelable cancelable;

	public UpdateEventHandler(Cancelable cancelable, UpdateGui gui) {
		this.cancelable = cancelable;
		this.gui = gui;
	}

	public void handleEvent(SVNEvent event, double progress)
			throws SVNException {
		try {

			SVNEventAction action = event.getAction();
			SVNStatusType contentStatus = event.getContentsStatus();
			SVNStatusType propertyStatus = event.getPropertiesStatus();

			if (contentStatus == SVNStatusType.INAPPLICABLE
					&& propertyStatus == SVNStatusType.UNKNOWN) {
				return;
			}

			count++;
			Manager.invalidate(event.getFile());

			UpdateContentStatus updateContentStatus = null;
			if (SVNStatusType.CONFLICTED.equals(contentStatus)) {
				updateContentStatus = UpdateContentStatus.CONFLICTED;
			} else if (SVNStatusType.MERGED.equals(contentStatus)) {
				updateContentStatus = UpdateContentStatus.MERGED;
			}

			String fileName = event.getFile() != null ? event.getFile()
					.getAbsolutePath() : null;
			if (SVNEventAction.UPDATE_NONE.equals(action)) {
				// gui.addItem(fileName, updateContentStatus,
				// ContentStatus.NONE,
				// event);
			} else if (SVNEventAction.UPDATE_ADD.equals(action)) {
				gui.addItem(fileName, updateContentStatus, ContentStatus.ADDED,
						event);
			} else if (SVNEventAction.RESTORE.equals(action)) {
				gui.addItem(fileName, updateContentStatus,
						ContentStatus.RESTORED, event);
			} else if (SVNEventAction.UPDATE_DELETE.equals(action)) {
				gui.addItem(fileName, updateContentStatus,
						ContentStatus.DELETED, event);
			} else if (SVNEventAction.UPDATE_EXISTS.equals(action)) {
				gui.addItem(fileName, updateContentStatus,
						ContentStatus.EXISTS, event);
			} else if (SVNEventAction.UPDATE_EXTERNAL.equals(action)) {
				gui.addItem(fileName, updateContentStatus,
						ContentStatus.EXTERNAL, event);
			} else if (SVNEventAction.UPDATE_REPLACE.equals(action)) {
				gui.addItem(fileName, updateContentStatus,
						ContentStatus.REPLACED, event);
			} else if (SVNEventAction.UPDATE_UPDATE.equals(action)) {
				gui.addItem(fileName, updateContentStatus,
						ContentStatus.UPDATE, event);
			} else if (SVNEventAction.UPDATE_COMPLETED.equals(action)) {
				gui.addItem(
						event.getFile() + " - Revision number: "
								+ event.getRevision(), updateContentStatus,
						ContentStatus.COMPLETED, event);
			} else if (SVNEventAction.TREE_CONFLICT.equals(action)) {
				gui.addItem("Tree conflict: " + event.getFile(),
						updateContentStatus, ContentStatus.MISSING, event);
			} else if (SVNEventAction.MERGE_COMPLETE.equals(action)) {
				count--;
			} else if (SVNEventAction.SKIP_CONFLICTED.equals(action)) {
				gui.addItem("Skip conflicted: " + event.getFile(),
						updateContentStatus, ContentStatus.CONFLICTED, event);
			}
		} catch (SVNException e) {
			throw e;
		} catch (Exception e) {
			Manager.handle(e);
		}

	}

	public int getCount() {
		return count;
	}

	public void checkCancelled() throws SVNCancelException {
		if (cancelable.isCancel()) {
			throw new SVNCancelException();
		}
	}
}
