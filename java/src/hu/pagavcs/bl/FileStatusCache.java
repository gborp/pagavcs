package hu.pagavcs.bl;

import java.io.File;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.SVNClientManager;
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
		ADDED, CONFLICTS, DELETED, IGNORED, LOCKED, MODIFIED, NORMAL, OBSTRUCTED, READONLY, SVNED, NONE,
	}

	private static final int                 CACHE_SIZE    = 303;
	/** in ms */
	private static final long                CACHE_TOO_OLD = 5 * 60 * 1000;

	private static FileStatusCache           singleton;

	private final LruCache<File, StatusSlot> mapCache;
	private SVNStatusClient                  statusClient;

	private FileStatusCache() {
		SVNClientManager clientMgr = Manager.getSVNClientManagerForWorkingCopyOnly();
		statusClient = clientMgr.getStatusClient();
		mapCache = new LruCache<File, StatusSlot>(CACHE_SIZE);
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

	public synchronized STATUS getStatus(File file) throws SVNException {

		StatusSlot slot = mapCache.get(file);

		if (slot != null && ((System.currentTimeMillis() - slot.timeInMs) < CACHE_TOO_OLD)) {
			return slot.status;
		}

		slot = new StatusSlot();
		slot.timeInMs = System.currentTimeMillis();

		File parent = file.getParentFile();
		File svnDir = new File(parent, ".svn");
		STATUS result = STATUS.NONE;
		if (svnDir.exists()) {
			SVNStatus status = statusClient.doStatus(file, false);
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
				result = STATUS.NONE;
			} else {
				result = STATUS.SVNED;
			}
			if (file.isDirectory() && !result.equals(STATUS.IGNORED)) {
				result = STATUS.SVNED;
			}
			if (status.isLocked()) {
				result = STATUS.LOCKED;
			}
		}

		slot.status = result;
		mapCache.put(file, slot);

		return result;
	}

	private static class StatusSlot {

		STATUS status;
		long   timeInMs;
	}

}
