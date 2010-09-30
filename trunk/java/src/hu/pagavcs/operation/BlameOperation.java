package hu.pagavcs.operation;

import hu.pagavcs.bl.Cancelable;
import hu.pagavcs.bl.Manager;
import hu.pagavcs.bl.PagaException;
import hu.pagavcs.bl.SvnHelper;
import hu.pagavcs.gui.BlameGui;
import hu.pagavcs.gui.BlameListItem;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.prefs.BackingStoreException;

import org.tmatesoft.svn.core.SVNCancelException;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.ISVNAnnotateHandler;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNEvent;
import org.tmatesoft.svn.core.wc.SVNInfo;
import org.tmatesoft.svn.core.wc.SVNLogClient;
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
public class BlameOperation implements Cancelable {

	private String   path;
	private BlameGui gui;
	private boolean  cancel;

	public BlameOperation(String path) throws BackingStoreException, SVNException {
		this.path = path;
	}

	public void execute() throws SVNException, BackingStoreException, PagaException {
		gui = new BlameGui(this);
		gui.display();
		gui.setFile(path);
		gui.setStatus(GeneralStatus.INIT);
		File wcFile = new File(path);
		SVNClientManager mgrSvn = Manager.getSVNClientManager(new File(path));
		SVNWCClient wcClient = mgrSvn.getWCClient();
		gui.setStatus(GeneralStatus.START);
		try {
			SVNInfo svnInfo = wcClient.doInfo(wcFile, SVNRevision.WORKING);

			gui.setURL(svnInfo.getURL().toDecodedString());

			gui.setStatus(GeneralStatus.COMPLETED);
		} catch (SVNException ex) {
			Manager.handle(ex);
			gui.setStatus(GeneralStatus.FAILED);
		}
	}

	public String getPath() {
		return path;
	}

	public List<BlameListItem> doBlame(String path, String revision) throws SVNException, PagaException {
		revision = revision.trim();
		SVNRevision blameRevision;
		if (revision.isEmpty()) {
			blameRevision = SVNRevision.HEAD;
		} else {
			blameRevision = SVNRevision.create(Long.valueOf(revision));
		}

		SVNClientManager clientMgr = Manager.getSVNClientManager(new File(path));
		SVNLogClient logClient = clientMgr.getLogClient();
		logClient.setEventHandler(new ISVNEventHandler() {

			public void checkCancelled() throws SVNCancelException {
				if (isCancel()) {
					throw new SVNCancelException();
				}
			}

			public void handleEvent(SVNEvent event, double progress) throws SVNException {}
		});
		final ArrayList<BlameListItem> lstBlame = new ArrayList<BlameListItem>();
		logClient.doAnnotate(new File(path), SVNRevision.HEAD, SVNRevision.create(1), blameRevision, new ISVNAnnotateHandler() {

			public void handleEOF() {}

			public void handleLine(Date date, long revision, String author, String line, Date mergedDate, long mergedRevision, String mergedAuthor,
			        String mergedPath, int lineNumber) throws SVNException {
				BlameListItem li = new BlameListItem();
				li.setDate(date);
				li.setRevision(revision);
				li.setAuthor(author);
				// convert tabulators to 4 spaces
				line = line.replace("\t", "    ");
				li.setLine(line);
				li.setLineNumber(lstBlame.size() + 1);
				lstBlame.add(li);
			}

			public void handleLine(Date date, long revision, String author, String line) throws SVNException {
				throw new RuntimeException("Not implemented");
			}

			public boolean handleRevision(Date date, long revision, String author, File contents) throws SVNException {
				return false;
			}

		});

		return lstBlame;
	}

	public void storeUrlForHistory(String url) {
		SvnHelper.storeUrlForHistory(url);
	}

	public void setCancel(boolean cancel) {
		this.cancel = cancel;
	}

	public boolean isCancel() {
		return cancel;
	}
}
