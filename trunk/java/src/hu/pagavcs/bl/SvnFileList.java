package hu.pagavcs.bl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.util.SVNEncodingUtil;
import org.tmatesoft.svn.core.internal.util.SVNURLUtil;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNInfo;
import org.tmatesoft.svn.core.wc.SVNRevision;

public class SvnFileList {

	private SVNURL   baseURL;
	private String[] target;

	public SvnFileList(File... files) throws SVNException {
		List<SVNURL> lstInfo = new ArrayList<SVNURL>();
		SVNClientManager mgrSvn = Manager.getSVNClientManagerForWorkingCopyOnly();
		try {
			for (File file : files) {
				SVNInfo svnInfo = mgrSvn.getWCClient().doInfo(file, SVNRevision.WORKING);
				lstInfo.add(svnInfo.getURL());
			}
		} finally {
			mgrSvn.dispose();
		}

		String[] paths = new String[lstInfo.size()];
		for (int i = 0; i < lstInfo.size(); i++) {
			paths[i] = lstInfo.get(i).getPath();
		}

		// String rootWCPath = SVNPathUtil.condencePaths(paths, null, true);
		ArrayList<String> targetPaths = new ArrayList<String>();
		baseURL = SVNURLUtil.condenceURLs(lstInfo.toArray(new SVNURL[0]), targetPaths, true);
		if (targetPaths.isEmpty()) {
			targetPaths.add("");
		}

		target = new String[targetPaths.size()];

		for (int i = 0; i < target.length; i++) {
			target[i] = SVNEncodingUtil.uriDecode(targetPaths.get(i));
		}
	}

	public SVNURL getSvnRoot() {
		return baseURL;
	}

	public String[] getTargetPaths() {
		return target;
	}

}
