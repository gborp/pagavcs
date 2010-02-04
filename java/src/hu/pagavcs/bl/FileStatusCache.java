package hu.pagavcs.bl;

import java.io.File;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.ISVNStatusHandler;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
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

public class FileStatusCache {

	public enum STATUS {
		ADDED, CONFLICTS, DELETED, IGNORED, LOCKED, MODIFIED, NORMAL, OBSTRUCTED, READONLY, SVNED, NONE, UNVERSIONED,
	}

	private static final int                 CACHE_SIZE    = 1000;
	/** in ms */
	private static final long                CACHE_TOO_OLD = 5 * 60 * 1000;

	private static FileStatusCache           singleton;

	private final LruCache<File, StatusSlot> mapCache;
	private SVNStatusClient                  statusClient;
	private SvnStatusHandler                 svnStatusHandler;

	private FileStatusCache() {
		SVNClientManager clientMgr = Manager.getSVNClientManagerForWorkingCopyOnly();
		statusClient = clientMgr.getStatusClient();
		mapCache = new LruCache<File, StatusSlot>(CACHE_SIZE);
		svnStatusHandler = new SvnStatusHandler();
	}

	public static FileStatusCache getInstance() {
		if (singleton == null) {
			synchronized (FileStatusCache.class) {
				if (singleton == null) {
					singleton = new FileStatusCache();
				}
			}
		}

		return singleton;
	}

	public synchronized void invalidate(File file) {
		mapCache.remove(file);
	}

	public synchronized void invalidateAll() {
		mapCache.clear();
	}

	private STATUS getStatus(File file, SVNStatus status) {
		STATUS result;
		if (status == null) {
			return STATUS.NONE;
		}
		SVNStatusType contentStatus = status.getContentsStatus();
		if (contentStatus.equals(SVNStatusType.STATUS_ADDED)) {
			result = STATUS.IGNORED;
		} else if (contentStatus.equals(SVNStatusType.STATUS_CONFLICTED)) {
			result = STATUS.CONFLICTS;
		} else if (contentStatus.equals(SVNStatusType.STATUS_DELETED)) {
			result = STATUS.DELETED;
		} else if (contentStatus.equals(SVNStatusType.STATUS_IGNORED)) {
			result = STATUS.IGNORED;
		} else if (contentStatus.equals(SVNStatusType.STATUS_MODIFIED)) {
			result = STATUS.MODIFIED;
		} else if (contentStatus.equals(SVNStatusType.STATUS_NORMAL)) {
			result = STATUS.NORMAL;
		} else if (contentStatus.equals(SVNStatusType.STATUS_OBSTRUCTED)) {
			result = STATUS.OBSTRUCTED;
		} else if (contentStatus.equals(SVNStatusType.STATUS_UNVERSIONED)) {
			result = STATUS.UNVERSIONED;
		} else {
			result = STATUS.SVNED;
		}
		if (!result.equals(STATUS.IGNORED) && file.isDirectory()) {
			result = STATUS.SVNED;
		}
		if (status.isLocked()) {
			result = STATUS.LOCKED;
		}
		return result;
	}

	private class SvnStatusHandler implements ISVNStatusHandler {

		public void handleStatus(SVNStatus status) {

			File file = status.getFile();

			StatusSlot slot = new StatusSlot();
			slot.status = getStatus(file, status);
			slot.lastModified = file.lastModified();
			slot.timestamp = System.currentTimeMillis();
			slot.fileSize = file.length();
			synchronized (mapCache) {
				mapCache.put(file, slot);
			}
		}
	}

	public STATUS getStatus(File file) throws SVNException {

		synchronized (mapCache) {
			StatusSlot slot = mapCache.get(file);
			if (slot != null) {
				if (file.lastModified() != slot.lastModified || file.length() != slot.fileSize
				        || ((System.currentTimeMillis() - slot.timestamp) > CACHE_TOO_OLD)) {
					mapCache.remove(file);
				} else {
					return slot.status;
				}
			}
		}

		File parent = file.getParentFile();
		File svnDir = new File(parent, ".svn");
		if (svnDir.exists()) {
			synchronized (mapCache) {
				StatusSlot slot = mapCache.get(file);
				if (slot != null) {
					return slot.status;
				}
			}
			synchronized (svnStatusHandler) {
				try {
					statusClient.doStatus(file, SVNRevision.HEAD, SVNDepth.EMPTY, false, true, true, false, svnStatusHandler, null);
				} catch (Exception ex) {
					return STATUS.NONE;
				}
			}
			synchronized (mapCache) {
				return mapCache.get(file).status;

			}
		} else {
			StatusSlot slot = new StatusSlot();
			slot.timestamp = System.currentTimeMillis();
			slot.lastModified = file.lastModified();
			slot.fileSize = file.length();
			if (file.isDirectory() && new File(file, ".svn").exists()) {
				slot.status = STATUS.SVNED;
			} else {
				slot.status = STATUS.NONE;
			}

			mapCache.put(file, slot);
			return slot.status;
		}
	}

	private static class StatusSlot {

		STATUS status;
		long   lastModified;
		long   fileSize;
		long   timestamp;
	}

}
