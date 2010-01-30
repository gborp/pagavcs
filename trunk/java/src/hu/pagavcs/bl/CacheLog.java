package hu.pagavcs.bl;

import java.io.File;

import org.tmatesoft.svn.core.ISVNLogEntryHandler;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.wc.SVNLogClient;
import org.tmatesoft.svn.core.wc.SVNRevision;

public class CacheLog {

	private static CacheLog singleton;

	public static CacheLog getInstance() {
		if (singleton == null) {
			singleton = new CacheLog();
		}
		return singleton;
	}

	// TODO make some kind of caching
	public void doLog(SVNLogClient logClient, File filePath, SVNRevision startRevision, SVNRevision endRevision, SVNRevision pegRevision, boolean stopOnCopy,
	        boolean discoverChangedPaths, boolean includeMergedRevisions, long limit, String[] revisionProperties, final ISVNLogEntryHandler handler)
	        throws SVNException {

		ISVNLogEntryHandler wrapCacheItems = new ISVNLogEntryHandler() {

			public void handleLogEntry(SVNLogEntry logEntry) throws SVNException {

				handler.handleLogEntry(logEntry);
			}
		};

		SvnFileList sfl = new SvnFileList(filePath);

		logClient.doLog(sfl.getSvnRoot(), sfl.getTargetPaths(), pegRevision, startRevision, endRevision, stopOnCopy, discoverChangedPaths,
		        includeMergedRevisions, limit, revisionProperties, wrapCacheItems);
	}
}
