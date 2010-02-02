package hu.pagavcs.bl;

import hu.pagavcs.gui.UpdateGui;
import hu.pagavcs.gui.Working;
import hu.pagavcs.operation.Cleanup;
import hu.pagavcs.operation.ContentStatus;
import hu.pagavcs.operation.UpdateEventHandler;

import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JOptionPane;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.ISVNPropertyHandler;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNDiffClient;
import org.tmatesoft.svn.core.wc.SVNPropertyData;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNRevisionRange;
import org.tmatesoft.svn.core.wc.SVNWCClient;

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

	public static void createPatch(File basePath, File[] wcFiles, OutputStream out) throws SVNException {
		SVNClientManager mgrSvn = Manager.getSVNClientManagerForWorkingCopyOnly();
		SVNDiffClient diffClient = mgrSvn.getDiffClient();
		diffClient.getDiffGenerator().setBasePath(basePath);
		for (File wcFile : wcFiles) {
			diffClient.doDiff(wcFile, SVNRevision.BASE, wcFile, SVNRevision.WORKING, SVNDepth.INFINITY, true, out, null);
		}
	}

	public static void showChangesFromBase(Working working, File wcFile) throws Exception {
		working.workStarted();
		try {
			File fileOld = Manager.getBaseFile(wcFile);

			String wcFilePath = wcFile.getPath();
			String fileName = wcFilePath.substring(wcFilePath.lastIndexOf('/') + 1);

			ProcessBuilder processBuilder = new ProcessBuilder("meld", "-L " + fileOld.getName(), fileOld.getPath(), "-L " + fileName, wcFilePath);
			Process process = processBuilder.start();
			working.workEnded();
			process.waitFor();

		} finally {
			Manager.releaseBaseFile(wcFile);
		}
	}

	private static List<SVNPropertyData> showPropertyChangesFromBase(Working working, File wcFile, SVNRevision pegRevision, SVNRevision revision)
	        throws Exception {
		final List<SVNPropertyData> lstResult = new ArrayList<SVNPropertyData>();
		SVNClientManager svnMgr = Manager.getSVNClientManagerForWorkingCopyOnly();
		SVNWCClient wcClient = svnMgr.getWCClient();
		ISVNPropertyHandler handler = new ISVNPropertyHandler() {

			public void handleProperty(long revision, SVNPropertyData property) throws SVNException {
				lstResult.add(property);
			}

			public void handleProperty(SVNURL url, SVNPropertyData property) throws SVNException {
				lstResult.add(property);
			}

			public void handleProperty(File path, SVNPropertyData property) throws SVNException {
				lstResult.add(property);
			}
		};
		wcClient.doGetProperty(wcFile, null, pegRevision, revision, SVNDepth.EMPTY, handler, null);
		return lstResult;
	}

	private static List<SVNPropertyData> showPropertyChangesFromBase(Working working, SVNURL svnUrl, SVNRevision pegRevision, SVNRevision revision)
	        throws Exception {
		final List<SVNPropertyData> lstResult = new ArrayList<SVNPropertyData>();
		SVNClientManager svnMgr = Manager.getSVNClientManager(svnUrl);
		SVNWCClient wcClient = svnMgr.getWCClient();
		ISVNPropertyHandler handler = new ISVNPropertyHandler() {

			public void handleProperty(long revision, SVNPropertyData property) throws SVNException {
				lstResult.add(property);
			}

			public void handleProperty(SVNURL url, SVNPropertyData property) throws SVNException {
				lstResult.add(property);
			}

			public void handleProperty(File path, SVNPropertyData property) throws SVNException {
				lstResult.add(property);
			}
		};
		wcClient.doGetProperty(svnUrl, null, pegRevision, revision, SVNDepth.EMPTY, handler);
		return lstResult;
	}

	private static String propertyListToString(List<SVNPropertyData> lstSvnProperty) {
		StringBuilder sb = new StringBuilder();
		for (SVNPropertyData li : lstSvnProperty) {
			String name = li.getName();
			String valueAll = SVNPropertyValue.getPropertyAsString(li.getValue());
			for (String value : valueAll.split("\n")) {
				sb.append(name);
				sb.append("=");
				sb.append(value);
				sb.append("\n");
			}
		}

		return sb.toString();
	}

	public static void showPropertyChangesFromBase(Working working, File wcFile) throws Exception {
		working.workStarted();
		String strBase = propertyListToString(showPropertyChangesFromBase(working, wcFile, SVNRevision.BASE, SVNRevision.BASE));
		String strWorking = propertyListToString(showPropertyChangesFromBase(working, wcFile, SVNRevision.WORKING, SVNRevision.WORKING));
		File fileBase = File.createTempFile(wcFile.getName(), ".base");
		File fileWorking = File.createTempFile(wcFile.getName(), ".working");
		fileBase.deleteOnExit();
		fileWorking.deleteOnExit();
		Manager.saveStringToFile(fileBase, strBase);
		Manager.saveStringToFile(fileWorking, strWorking);

		ProcessBuilder processBuilder = new ProcessBuilder("meld", "-L " + fileBase.getName(), fileBase.getPath(), "-L " + fileWorking.getName(), fileWorking
		        .getPath());
		Process process = processBuilder.start();
		working.workEnded();
		process.waitFor();
		fileBase.delete();
		fileWorking.delete();
	}

	public static void showPropertyChangesFromRepo(Working working, SVNURL svnUrl, long revision1, long revision2) throws Exception {
		working.workStarted();
		String strBase = propertyListToString(showPropertyChangesFromBase(working, svnUrl, SVNRevision.HEAD, SVNRevision.create(revision1)));
		String strWorking = propertyListToString(showPropertyChangesFromBase(working, svnUrl, SVNRevision.HEAD, SVNRevision.create(revision2)));
		File file1 = File.createTempFile("DirProp", "." + revision1);
		File file2 = File.createTempFile("DirProp", "." + revision2);
		file1.deleteOnExit();
		file2.deleteOnExit();
		Manager.saveStringToFile(file1, strBase);
		Manager.saveStringToFile(file2, strWorking);

		ProcessBuilder processBuilder = new ProcessBuilder("meld", "-L DirProp-" + revision1, file1.getPath(), "-L DirProp-" + revision2, file2.getPath());
		Process process = processBuilder.start();
		working.workEnded();

		process.waitFor();
		file1.delete();
		file2.delete();
	}
}
