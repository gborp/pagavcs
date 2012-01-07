package hu.pagavcs.client.bl;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.HashSet;

import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNClientManager;
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
public class FileRevisionCache {

	private static final int                                MAX_CACHED_FILE = 50;

	private static FileRevisionCache                        singleton;

	private static HashMap<Pair<SVNURL, SVNRevision>, File> mapFiles        = new HashMap<Pair<SVNURL, SVNRevision>, File>();
	private static HashSet<Pair<SVNURL, SVNRevision>>       lstUsedFiles    = new HashSet<Pair<SVNURL, SVNRevision>>();
	private static HashMap<File, File>                      lstBaseFiles    = new HashMap<File, File>();

	public static synchronized FileRevisionCache getInstance() {
		if (singleton == null) {
			singleton = new FileRevisionCache();
		}

		return singleton;
	}

	public synchronized void init() {
		String tempPrefixR = Manager.getTempDir() + "r/";
		File tempDirR = new File(tempPrefixR);
		File[] lstFilesR = tempDirR.listFiles();
		if (lstFilesR != null) {
			for (File f : lstFilesR) {
				f.setWritable(true);
				f.delete();
			}
		}
		tempDirR.mkdirs();

		String tempPrefixW = Manager.getTempDir() + "w/";
		File tempDirW = new File(tempPrefixW);
		File[] lstFilesW = tempDirW.listFiles();
		if (lstFilesW != null) {
			for (File f : lstFilesW) {
				f.setWritable(true);
				f.delete();
			}
		}
		tempDirW.mkdirs();
	}

	public synchronized File getFile(SVNURL svnUrl, SVNRevision revision) throws Exception {

		Pair<SVNURL, SVNRevision> key = new Pair<SVNURL, SVNRevision>(svnUrl, revision);
		File result = mapFiles.get(key);
		if (result != null) {
			lstUsedFiles.add(key);
			return result;
		}

		FileOutputStream outOldRevision = null;
		SVNClientManager mgrSvn = Manager.getSVNClientManager(svnUrl);
		try {
			SVNWCClient wcClient = mgrSvn.getWCClient();

			String path = svnUrl.getPath();
			String fileName = path.substring(path.lastIndexOf('/') + 1);
			String fileNameOld = "r" + revision + "-" + fileName;
			String tempPrefix = Manager.getTempDir() + "r/";
			String fileNameRoot = tempPrefix + fileNameOld;
			File file = new File(fileNameRoot);
			int counter = 0;
			while (file.exists()) {
				file = new File(fileNameRoot + "-" + counter);
				counter++;
			}

			outOldRevision = new FileOutputStream(file.getPath());
			wcClient.doGetFileContents(svnUrl, SVNRevision.UNDEFINED, revision, false, outOldRevision);
			outOldRevision.close();

			file.setReadOnly();
			result = file;
		} finally {
			mgrSvn.dispose();
		}

		if (lstUsedFiles.size() > MAX_CACHED_FILE) {
			for (Pair<SVNURL, SVNRevision> li : mapFiles.keySet()) {
				if (!lstUsedFiles.contains(li)) {
					File fileToRemove = mapFiles.get(key);
					fileToRemove.setWritable(true);
					fileToRemove.delete();
					mapFiles.remove(key);
				}
			}
		}

		lstUsedFiles.add(key);
		mapFiles.put(key, result);

		return result;
	}

	public synchronized void releaseFile(SVNURL svnUrl, SVNRevision revision) throws Exception {
		Pair<SVNURL, SVNRevision> key = new Pair<SVNURL, SVNRevision>(svnUrl, revision);
		lstUsedFiles.remove(key);
	}

	public File getBaseFile(File wcFile) throws Exception {
		File result = lstBaseFiles.get(wcFile);

		if (result == null) {
			String tempPrefix = Manager.getTempDir() + "w/";
			String fileNameRoot = tempPrefix + wcFile.getName();
			File file = new File(fileNameRoot);
			int counter = 0;
			while (file.exists()) {
				file = new File(fileNameRoot + "-" + counter);
				counter++;
			}

			SVNClientManager svnMgr = Manager.getSVNClientManagerForWorkingCopyOnly();
			try {
				SVNWCClient wcClient = svnMgr.getWCClient();

				FileOutputStream outOldRevision = new FileOutputStream(file.getPath());
				wcClient.doGetFileContents(wcFile, SVNRevision.BASE, SVNRevision.BASE, false, outOldRevision);
				outOldRevision.close();

				lstBaseFiles.put(wcFile, file);
				result = file;
			} finally {
				svnMgr.dispose();
			}
		}
		return result;
	}

	public void releaseBase(File wcFile) {
		File cacheFile = lstBaseFiles.get(wcFile);
		if (cacheFile != null) {
			lstBaseFiles.remove(wcFile);
			cacheFile.delete();
		}
	}
}
