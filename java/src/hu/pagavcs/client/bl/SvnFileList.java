package hu.pagavcs.client.bl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.util.SVNEncodingUtil;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNInfo;
import org.tmatesoft.svn.core.wc.SVNRevision;

public class SvnFileList {

	private SVNURL baseURL;
	private String[] target;

	public SvnFileList(File... files) throws SVNException {
		List<SVNURL> lstInfo = new ArrayList<SVNURL>();
		SVNClientManager mgrSvn = Manager
				.getSVNClientManagerForWorkingCopyOnly();
		try {
			for (File file : files) {
				SVNInfo svnInfo = mgrSvn.getWCClient().doInfo(file,
						SVNRevision.WORKING);
				baseURL = svnInfo.getRepositoryRootURL();
				lstInfo.add(svnInfo.getURL());
			}
		} finally {
			mgrSvn.dispose();
		}

		target = new String[lstInfo.size()];
		for (int i = 0; i < lstInfo.size(); i++) {
			target[i] = SVNEncodingUtil.uriDecode(lstInfo.get(i).getPath());
		}
	}

	public SVNURL getSvnRoot() {
		return baseURL;
	}

	public String[] getTargetPaths() {
		return target;
	}

}
