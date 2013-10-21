package hu.pagavcs.common;

import org.tmatesoft.svn.core.wc.SVNClientManager;

public class CommonHelper {

	public static SVNClientManager getSVNClientManagerForWorkingCopyOnly() {
		return SVNClientManager.newInstance();
	}

	public static void handle(Exception ex) {
		// TODO log exception
		ex.printStackTrace();
	}
}
