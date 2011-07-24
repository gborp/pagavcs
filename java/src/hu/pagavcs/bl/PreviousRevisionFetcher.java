package hu.pagavcs.bl;

import org.tmatesoft.svn.core.ISVNLogEntryHandler;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNLogClient;
import org.tmatesoft.svn.core.wc.SVNRevision;

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
public class PreviousRevisionFetcher {

	private long previousRevision = Long.MAX_VALUE;

	public void execute(SVNURL svnUrl, long revision) throws SVNException, PagaException {
		SVNClientManager mgrSvn = Manager.getSVNClientManager(svnUrl);
		try {
			SVNLogClient logClient = mgrSvn.getLogClient();
			SVNRevision startRevision = SVNRevision.create(revision - 1);
			SVNRevision endRevision = SVNRevision.create(0);
			SVNRevision pegRevision = startRevision;
			boolean stopOnCopy = false;
			boolean discoverChangedPaths = true;
			boolean includeMergedRevisions = false;
			long limit = 1;
			String[] revisionProperties = null;
			ISVNLogEntryHandler handler = new LogEntryHandler();
			logClient.doLog(svnUrl, new String[] { "" }, pegRevision, startRevision, endRevision, stopOnCopy, discoverChangedPaths, includeMergedRevisions,
			        limit, revisionProperties, handler);
		} finally {
			mgrSvn.dispose();
		}
	}

	private class LogEntryHandler implements ISVNLogEntryHandler {

		public void handleLogEntry(SVNLogEntry logEntry) throws SVNException {
			long revision = logEntry.getRevision();
			// String author = logEntry.getAuthor();
			// Date date = logEntry.getDate();
			// String message = logEntry.getMessage();
			//
			// Map<String, SVNLogEntryPath> mapChanges = logEntry
			// .getChangedPaths();
			previousRevision = revision;
		}

	}

	public long getPreviousRevision() {
		return previousRevision;
	}
}
