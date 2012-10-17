/**
 * 
 */
package hu.pagavcs.client.operation;

import hu.pagavcs.client.bl.Cancelable;
import hu.pagavcs.client.bl.Manager;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.tmatesoft.svn.core.SVNCancelException;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.SVNEvent;
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
public class MergeDryRunEventHandler implements ISVNEventHandler {

	private final Cancelable cancelable;
	private List<File>       lstConflictedFiles;

	public MergeDryRunEventHandler(Cancelable cancelable) {
		this.cancelable = cancelable;
		lstConflictedFiles = new ArrayList<File>();
	}

	public void handleEvent(SVNEvent event, double progress) throws SVNException {
		try {
			SVNStatusType contentStatus = event.getContentsStatus();
			if (SVNStatusType.CONFLICTED.equals(contentStatus)) {
				lstConflictedFiles.add(event.getFile());
			}

		} catch (Exception e) {
			Manager.handle(e);
		}
	}

	public List<File> getMultiConflictedFiles() {
		HashSet<File> setConflicted = new HashSet<File>();
		List<File> lstMultiConflictedFiles = new ArrayList<File>();
		for (File file : lstConflictedFiles) {
			if (setConflicted.contains(file)) {
				lstMultiConflictedFiles.add(file);
			} else {
				setConflicted.add(file);
			}
		}
		return lstMultiConflictedFiles;
	}

	public void checkCancelled() throws SVNCancelException {
		if (cancelable.isCancel()) {
			throw new SVNCancelException();
		}
	}
}
