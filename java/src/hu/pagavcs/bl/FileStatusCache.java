package hu.pagavcs.bl;

import java.io.File;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNPropertyData;
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

public class FileStatusCache {

	public enum STATUS {
		NONE, SVNED
	}

	private static final int                 CACHE_SIZE    = 5000;
	/** in ms */
	private static final long                CACHE_TOO_OLD = 5 * 60 * 1000;

	private static FileStatusCache           singleton;

	private final SVNWCClient                wcClient;
	private final LruCache<File, StatusSlot> mapCache;

	private FileStatusCache() {
		SVNClientManager clientMgr = Manager.getSVNClientManagerForWorkingCopyOnly();
		wcClient = clientMgr.getWCClient();
		mapCache = new LruCache<File, StatusSlot>(CACHE_SIZE);
	}

	public synchronized static FileStatusCache getInstance() {
		if (singleton == null) {
			singleton = new FileStatusCache();
		}

		return singleton;
	}

	public STATUS getStatus(File file) throws SVNException {

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
			SVNPropertyData ignoreProp = wcClient.doGetProperty(parent, SVNProperty.IGNORE, SVNRevision.WORKING, SVNRevision.WORKING);
			if (ignoreProp != null) {
				boolean ignored = false;
				String path = file.getAbsolutePath();
				for (String li : ignoreProp.getValue().getString().split("\n")) {
					if (li.equals(path)) {
						ignored = true;
						break;
					}
				}
				if (!ignored) {
					result = STATUS.SVNED;
				}
			} else {
				result = STATUS.SVNED;
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
