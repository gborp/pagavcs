package hu.pagavcs.bl;

import hu.pagavcs.gui.UpdateGui;
import hu.pagavcs.operation.Cleanup;
import hu.pagavcs.operation.ContentStatus;
import hu.pagavcs.operation.UpdateEventHandler;

import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.JOptionPane;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNDiffClient;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNRevisionRange;

public class SvnHelper {

	private static SVNRevision decodeSVNRevision(String str) {
		str = str.trim();
		if (str.equalsIgnoreCase("head")) {
			return SVNRevision.HEAD;
		}
		return SVNRevision.create(Long.valueOf(str));
	}

	public static void doMerge(Cancelable cancelable, String urlTo, String pathTo, String urlFrom, String revisionRange, boolean reverseMerge) throws Exception {

		cancelable.setCancel(false);
		UpdateGui updateGui = new UpdateGui(cancelable, "Merge");
		updateGui.display();
		try {
			updateGui.setStatus(ContentStatus.INIT);
			SVNClientManager clientMgr = Manager.getSVNClientManager(new File(pathTo));
			SVNDiffClient diffClient = clientMgr.getDiffClient();
			SVNDepth depth = SVNDepth.INFINITY;
			boolean useAncestry = true;
			boolean force = false;
			boolean dryRun = false;
			boolean recordOnly = false;
			Collection<SVNRevisionRange> rangesToMerge = new ArrayList<SVNRevisionRange>();
			String[] ranges = revisionRange.split(",");
			for (String range : ranges) {
				range = range.trim();
				String[] rangeSplitted = range.split("-");
				SVNRevision startRevision = decodeSVNRevision(rangeSplitted[0]);
				SVNRevision endRevision;
				if (rangeSplitted.length > 1) {
					endRevision = decodeSVNRevision(rangeSplitted[0]);
				} else {
					endRevision = SVNRevision.create(startRevision.getNumber());
				}
				if (reverseMerge) {
					rangesToMerge.add(new SVNRevisionRange(endRevision, SVNRevision.create(startRevision.getNumber() - 1)));
				} else {
					rangesToMerge.add(new SVNRevisionRange(SVNRevision.create(startRevision.getNumber() - 1), endRevision));
				}
			}

			diffClient.setEventHandler(new UpdateEventHandler(cancelable, updateGui));
			updateGui.setStatus(ContentStatus.STARTED);

			boolean successOrExit = false;
			while (!successOrExit) {
				try {
					diffClient.doMerge(SVNURL.parseURIDecoded(urlFrom), SVNRevision.HEAD, rangesToMerge, new File(pathTo), depth, useAncestry, force, dryRun,
					        recordOnly);
					successOrExit = true;
				} catch (SVNException ex) {
					if (SVNErrorCode.WC_LOCKED.equals(ex.getErrorMessage().getErrorCode())) {
						int choosed = JOptionPane.showConfirmDialog(Manager.getRootFrame(), "Working copy is locked, do cleanup?", "Error",
						        JOptionPane.YES_NO_OPTION);
						if (choosed == JOptionPane.YES_OPTION) {
							Cleanup cleanup = new Cleanup(pathTo);
							cleanup.setAutoClose(true);
							cleanup.execute();
						} else {
							cancelable.setCancel(true);
							successOrExit = true;
						}
					} else {
						throw ex;
					}
				}
			}
			updateGui.setStatus(ContentStatus.COMPLETED);
		} catch (Exception ex) {
			updateGui.setStatus(ContentStatus.FAILED);
			throw ex;
		}
	}

	public static void createPatch(File[] wcFiles, OutputStream out) throws SVNException {
		SVNClientManager mgrSvn = Manager.getSVNClientManagerForWorkingCopyOnly();
		SVNDiffClient diffClient = mgrSvn.getDiffClient();
		for (File wcFile : wcFiles) {
			diffClient.doDiff(wcFile, SVNRevision.BASE, wcFile, SVNRevision.WORKING, SVNDepth.INFINITY, true, out, null);
		}
	}

}
