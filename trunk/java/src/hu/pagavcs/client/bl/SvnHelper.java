package hu.pagavcs.client.bl;

import hu.pagavcs.client.bl.PagaException.PagaExceptionType;
import hu.pagavcs.client.gui.UpdateGui;
import hu.pagavcs.client.gui.Working;
import hu.pagavcs.client.gui.platform.MessagePane;
import hu.pagavcs.client.gui.platform.StringHelper;
import hu.pagavcs.client.operation.Cleanup;
import hu.pagavcs.client.operation.ContentStatus;
import hu.pagavcs.client.operation.MergeDryRunEventHandler;
import hu.pagavcs.client.operation.UpdateEventHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.JOptionPane;

import org.tmatesoft.svn.core.SVNCancelException;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.ISVNPropertyHandler;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNDiffClient;
import org.tmatesoft.svn.core.wc.SVNDiffOptions;
import org.tmatesoft.svn.core.wc.SVNPropertyData;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNRevisionRange;
import org.tmatesoft.svn.core.wc.SVNWCClient;

public class SvnHelper {

	private static final String MERGE_URL_KEY_SEPARATOR = " -> ";

	private static SVNRevision decodeSVNRevision(String str) {
		str = str.trim();
		if (str.charAt(0) == '#') {
			str = str.substring(1);
		}
		if (str.equalsIgnoreCase("head")) {
			return SVNRevision.HEAD;
		} else if (str.equalsIgnoreCase("base")) {
			return SVNRevision.BASE;
		} else if (str.equalsIgnoreCase("committed")) {
			return SVNRevision.COMMITTED;
		} else if (str.equalsIgnoreCase("previous")) {
			return SVNRevision.PREVIOUS;
		} else if (str.equalsIgnoreCase("working")) {
			return SVNRevision.WORKING;
		}
		return SVNRevision.create(Long.valueOf(str));
	}

	public static Collection<SVNRevisionRange> getRevisionRanges(
			String revisionRange, boolean reverseMerge) throws PagaException {
		Collection<SVNRevisionRange> rangesToMerge = new ArrayList<SVNRevisionRange>();
		for (String range : revisionRange.split(",")) {
			range = range.trim();

			String[] rangeSplitted = range.split("-");
			SVNRevision startRevision = decodeSVNRevision(rangeSplitted[0]);
			SVNRevision endRevision;
			if (rangeSplitted.length > 2) {
				throw new PagaException(PagaExceptionType.INVALID_PARAMETERS);
			}
			if (rangeSplitted.length == 2) {
				endRevision = decodeSVNRevision(rangeSplitted[1]);
			} else {
				endRevision = SVNRevision.create(startRevision.getNumber());
			}
			if (reverseMerge) {
				rangesToMerge.add(new SVNRevisionRange(endRevision, SVNRevision
						.create(startRevision.getNumber() - 1)));
			} else {
				rangesToMerge.add(new SVNRevisionRange(SVNRevision
						.create(startRevision.getNumber() - 1), endRevision));
			}
		}

		return rangesToMerge;
	}

	public static void doMerge(Cancelable cancelable, String urlTo,
			String pathTo, String urlFrom, String pathFrom,
			String revisionRange, boolean reverseMerge, boolean ignoreEolStyle)
			throws Exception {

		cancelable.setCancel(false);
		UpdateGui updateGui = new UpdateGui(cancelable, "Merge");
		updateGui.setPaths(Arrays.asList(new File(pathTo)));
		updateGui.display();
		SVNClientManager clientMgr = null;
		try {
			updateGui.setStatus(ContentStatus.INIT);
			clientMgr = Manager.getSVNClientManager(new File(pathTo));
			SVNDiffClient diffClient = clientMgr.getDiffClient();
			diffClient.setMergeOptions(new SVNDiffOptions(false, false,
					ignoreEolStyle));
			SVNDepth depth = SVNDepth.INFINITY;
			boolean useAncestry = true;
			boolean force = true;
			boolean recordOnly = false;
			Collection<SVNRevisionRange> rangesToMerge = getRevisionRanges(
					revisionRange, reverseMerge);

			updateGui.setStatus(ContentStatus.STARTED);

			MergeDryRunEventHandler mergeDryRunEventHandler = new MergeDryRunEventHandler(
					cancelable);
			diffClient.setEventHandler(mergeDryRunEventHandler);

			boolean success = false;
			boolean exit = false;

			if (rangesToMerge.size() > 1) {
				while (!success && !exit) {
					try {
						if (pathFrom == null) {
							diffClient.doMerge(SVNURL.parseURIDecoded(urlFrom),
									SVNRevision.HEAD, rangesToMerge, new File(
											pathTo), depth, useAncestry, force,
									true, recordOnly);
						} else {
							diffClient.doMerge(new File(pathFrom),
									SVNRevision.HEAD, rangesToMerge, new File(
											pathTo), depth, useAncestry, force,
									true, recordOnly);
						}
						success = true;
					} catch (SVNCancelException ex) {
						exit = true;
					} catch (SVNException ex) {
						if (SVNErrorCode.WC_LOCKED.equals(ex.getErrorMessage()
								.getErrorCode())) {
							int choosed = JOptionPane.showConfirmDialog(
									Manager.getRootFrame(),
									"Working copy is locked, do cleanup?",
									"Error", JOptionPane.YES_NO_OPTION);
							if (choosed == JOptionPane.YES_OPTION) {
								Cleanup cleanup = new Cleanup(pathTo);
								cleanup.setAutoClose(true);
								cleanup.execute();
							} else {
								cancelable.setCancel(true);
								exit = true;
							}
						} else {
							throw ex;
						}
					}
				}

				List<File> multiConflictedFiles = mergeDryRunEventHandler
						.getMultiConflictedFiles();
				if (!multiConflictedFiles.isEmpty()) {
					StringBuilder sb = new StringBuilder();
					sb.append("Cannot perform multi merge,\nbecause the following files are conflicted and in a later revision would be updated:\n");
					for (File file : multiConflictedFiles) {
						sb.append(file.getPath());
						sb.append('\n');
					}
					sb.append("Please do the merge revision by revision.\n");
					MessagePane.showError(updateGui.getFrame(),
							"Cannot perform multi merge", sb.toString());
					cancelable.setCancel(true);
					exit = true;
				}
			}

			diffClient.setEventHandler(new UpdateEventHandler(cancelable,
					updateGui));
			success = false;
			while (!success && !exit) {
				try {
					if (pathFrom == null) {
						diffClient.doMerge(SVNURL.parseURIDecoded(urlFrom),
								SVNRevision.HEAD, rangesToMerge, new File(
										pathTo), depth, useAncestry, force,
								false, recordOnly);
					} else {
						diffClient.doMerge(new File(pathFrom),
								SVNRevision.HEAD, rangesToMerge, new File(
										pathTo), depth, useAncestry, force,
								false, recordOnly);
					}
					success = true;
				} catch (SVNCancelException ex) {
					exit = true;
				} catch (SVNException ex) {
					if (SVNErrorCode.WC_LOCKED.equals(ex.getErrorMessage()
							.getErrorCode())) {
						int choosed = JOptionPane.showConfirmDialog(
								Manager.getRootFrame(),
								"Working copy is locked, do cleanup?", "Error",
								JOptionPane.YES_NO_OPTION);
						if (choosed == JOptionPane.YES_OPTION) {
							Cleanup cleanup = new Cleanup(pathTo);
							cleanup.setAutoClose(true);
							cleanup.execute();
						} else {
							cancelable.setCancel(true);
							exit = true;
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
		} finally {
			if (clientMgr != null) {
				clientMgr.dispose();
			}
		}
	}

	public static void createPatch(File basePath, File[] wcFiles,
			OutputStream out) throws SVNException {
		SVNClientManager mgrSvn = Manager
				.getSVNClientManagerForWorkingCopyOnly();
		try {
			SVNDiffClient diffClient = mgrSvn.getDiffClient();
			diffClient.getDiffGenerator().setBasePath(basePath);
			for (File wcFile : wcFiles) {
				diffClient
						.doDiff(wcFile, SVNRevision.BASE, wcFile,
								SVNRevision.WORKING, SVNDepth.INFINITY, true,
								out, null);
			}
		} finally {
			mgrSvn.dispose();
		}
	}

	public static void showChangesFromBase(Working working, File wcFile)
			throws Exception {
		working.workStarted();
		try {
			File fileOld = Manager.getBaseFile(wcFile);

			String wcFilePath = wcFile.getPath();
			String fileName = wcFilePath
					.substring(wcFilePath.lastIndexOf('/') + 1);

			ProcessBuilder processBuilder = new ProcessBuilder(Manager.MELD,
					"-L " + fileOld.getName(), fileOld.getPath(), "-L "
							+ fileName, wcFilePath);
			Process process = processBuilder.start();
			working.workEnded();
			process.waitFor();

		} finally {
			Manager.releaseBaseFile(wcFile);
		}
	}

	public static void showChangesBetweenRevisions(Working working,
			String path, SVNRevision previousRevision, SVNRevision revision,
			ContentStatus contentStatus) throws Exception {

		FileOutputStream outNewRevision = null;
		FileOutputStream outOldRevision = null;
		File fileNew = null;
		File fileOld = null;
		SVNClientManager mgrSvn = null;
		try {
			working.workStarted();

			mgrSvn = Manager.getSVNClientManager(new File(path));
			SVNWCClient wcClient = mgrSvn.getWCClient();

			String fileName = path.substring(path.lastIndexOf('/') + 1);

			String fileNameNew = "r" + revision.toString() + "-" + fileName;
			String fileNameOld = "r" + previousRevision.toString() + "-"
					+ fileName;
			String tempPrefix = Manager.getTempDir();

			fileNew = new File(tempPrefix + fileNameNew);
			fileOld = new File(tempPrefix + fileNameOld);
			fileNew.delete();
			fileOld.delete();
			outNewRevision = new FileOutputStream(tempPrefix + fileNameNew);
			outOldRevision = new FileOutputStream(tempPrefix + fileNameOld);

			// FIXME it doesn't work for DELETED currently
			if (!contentStatus.equals(ContentStatus.DELETED)) {
				wcClient.doGetFileContents(new File(path), revision, revision,
						false, outNewRevision);
				if (!contentStatus.equals(ContentStatus.ADDED)) {
					wcClient.doGetFileContents(new File(path),
							previousRevision, previousRevision, false,
							outOldRevision);
				}
			} else {
				SVNURL svnUrl = Manager.getSvnUrlByFile(new File(path));
				wcClient.doGetFileContents(svnUrl, previousRevision,
						previousRevision, false, outOldRevision);
			}

			fileNew.setReadOnly();
			fileNew.deleteOnExit();
			fileOld.setReadOnly();
			fileOld.deleteOnExit();

			ProcessBuilder processBuilder;
			if (contentStatus.equals(ContentStatus.DELETED)) {
				processBuilder = new ProcessBuilder(Manager.GEDIT, tempPrefix
						+ fileNameOld);
			} else if (contentStatus.equals(ContentStatus.ADDED)) {
				processBuilder = new ProcessBuilder(Manager.GEDIT, tempPrefix
						+ fileNameNew);
			} else {
				processBuilder = new ProcessBuilder(Manager.MELD, "-L "
						+ fileNameOld, tempPrefix + fileNameOld, "-L "
						+ fileNameNew, tempPrefix + fileNameNew);
			}
			Process process = processBuilder.start();
			working.workEnded();
			process.waitFor();
		} catch (Exception e) {
			try {
				working.workEnded();
			} catch (Exception e1) {
				Manager.handle(e1);
			}
			throw e;
		} finally {
			if (outNewRevision != null) {
				outNewRevision.close();
			}
			if (outOldRevision != null) {
				outOldRevision.close();
			}
			if (fileNew != null) {
				fileNew.delete();
			}
			if (fileOld != null) {
				fileOld.delete();
			}
			if (mgrSvn != null) {
				mgrSvn.dispose();
			}
		}
	}

	private static List<SVNPropertyData> showPropertyChangesFromBase(
			Working working, File wcFile, SVNRevision pegRevision,
			SVNRevision revision) throws Exception {
		final List<SVNPropertyData> lstResult = new ArrayList<SVNPropertyData>();
		SVNClientManager svnMgr = Manager
				.getSVNClientManagerForWorkingCopyOnly();
		try {
			SVNWCClient wcClient = svnMgr.getWCClient();
			ISVNPropertyHandler handler = new ISVNPropertyHandler() {

				public void handleProperty(long revision,
						SVNPropertyData property) throws SVNException {
					lstResult.add(property);
				}

				public void handleProperty(SVNURL url, SVNPropertyData property)
						throws SVNException {
					lstResult.add(property);
				}

				public void handleProperty(File path, SVNPropertyData property)
						throws SVNException {
					lstResult.add(property);
				}
			};
			wcClient.doGetProperty(wcFile, null, pegRevision, revision,
					SVNDepth.EMPTY, handler, null);
			return lstResult;
		} finally {
			svnMgr.dispose();
		}
	}

	private static List<SVNPropertyData> showPropertyChangesFromBase(
			Working working, SVNURL svnUrl, SVNRevision pegRevision,
			SVNRevision revision) throws Exception {
		final List<SVNPropertyData> lstResult = new ArrayList<SVNPropertyData>();
		SVNClientManager svnMgr = Manager.getSVNClientManager(svnUrl);
		try {
			SVNWCClient wcClient = svnMgr.getWCClient();
			ISVNPropertyHandler handler = new ISVNPropertyHandler() {

				public void handleProperty(long revision,
						SVNPropertyData property) throws SVNException {
					lstResult.add(property);
				}

				public void handleProperty(SVNURL url, SVNPropertyData property)
						throws SVNException {
					lstResult.add(property);
				}

				public void handleProperty(File path, SVNPropertyData property)
						throws SVNException {
					lstResult.add(property);
				}
			};
			wcClient.doGetProperty(svnUrl, null, pegRevision, revision,
					SVNDepth.EMPTY, handler);
			return lstResult;
		} finally {
			svnMgr.dispose();
		}
	}

	private static String propertyListToString(
			List<SVNPropertyData> lstSvnProperty) {
		StringBuilder sb = new StringBuilder();
		for (SVNPropertyData li : lstSvnProperty) {
			String name = li.getName();
			String valueAll = SVNPropertyValue.getPropertyAsString(li
					.getValue());
			for (String value : valueAll.split("\n")) {
				sb.append(name);
				sb.append("=");
				sb.append(value);
				sb.append("\n");
			}
		}

		return sb.toString();
	}

	public static void showPropertyChangesFromBase(Working working, File wcFile)
			throws Exception {
		working.workStarted();
		String strBase = propertyListToString(showPropertyChangesFromBase(
				working, wcFile, SVNRevision.BASE, SVNRevision.BASE));
		String strWorking = propertyListToString(showPropertyChangesFromBase(
				working, wcFile, SVNRevision.WORKING, SVNRevision.WORKING));

		String prefix = wcFile.getName();
		if (prefix.length() < 3) {
			prefix = (prefix + "___").substring(0, 3);
		}
		File fileBase = File.createTempFile(prefix, ".base");
		File fileWorking = File.createTempFile(prefix, ".working");
		fileBase.deleteOnExit();
		fileWorking.deleteOnExit();
		Manager.saveStringToFile(fileBase, strBase);
		Manager.saveStringToFile(fileWorking, strWorking);

		ProcessBuilder processBuilder = new ProcessBuilder(Manager.MELD, "-L "
				+ fileBase.getName(), fileBase.getPath(), "-L "
				+ fileWorking.getName(), fileWorking.getPath());
		Process process = processBuilder.start();
		working.workEnded();
		process.waitFor();
		fileBase.delete();
		fileWorking.delete();
	}

	public static void showPropertyChangesFromRepo(Working working,
			SVNURL svnUrl, long previousRevision, long revision,
			ContentStatus contentStatus) throws Exception {
		working.workStarted();
		File file1 = null;
		File file2 = null;
		Process process = null;
		try {
			String strWorking = propertyListToString(showPropertyChangesFromBase(
					working, svnUrl, SVNRevision.HEAD,
					SVNRevision.create(revision)));
			file2 = File.createTempFile(
					"DirProp-" + StringHelper.depathString(svnUrl.toString())
							+ "_", ".r" + revision);
			Manager.saveStringToFile(file2, strWorking);

			ProcessBuilder processBuilder;

			if (previousRevision != -1) {
				String strBase = propertyListToString(showPropertyChangesFromBase(
						working, svnUrl, SVNRevision.HEAD,
						SVNRevision.create(previousRevision)));
				file1 = File.createTempFile(
						"DirProp-"
								+ StringHelper.depathString(svnUrl.toString())
								+ "_", ".r" + previousRevision);
				Manager.saveStringToFile(file1, strBase);
				processBuilder = new ProcessBuilder(Manager.MELD, "-L DirProp-"
						+ previousRevision, file1.getPath(), "-L DirProp-"
						+ revision, file2.getPath());
			} else {
				processBuilder = new ProcessBuilder(Manager.GEDIT,
						file2.getPath());
			}
			process = processBuilder.start();
		} finally {
			working.workEnded();
		}
		if (process != null) {
			process.waitFor();
		}
		if (file1 != null) {
			file1.delete();
		}
		if (file2 != null) {
			file2.delete();
		}
	}

	public synchronized static String[] getRepoUrlHistory() {
		return Manager.getSettings().getLstRepoUrl().toArray(new String[0]);
	}

	public synchronized static void storeUrlForHistory(String url) {
		List<String> lstRepoUrl = Manager.getSettings().getLstRepoUrl();
		lstRepoUrl.remove(url);
		lstRepoUrl.add(0, url);

		int maxNo = Manager.getMaxUrlHistoryItems();
		while (lstRepoUrl.size() > maxNo) {
			lstRepoUrl.remove(lstRepoUrl.size() - 1);
		}
	}

	public synchronized static void storeUrlForMergeHistory(String urlFrom,
			String urlTo) {
		ArrayList<String> lstUrls = new ArrayList<String>(Manager.getSettings()
				.getLstMergeUrlHistory());
		String key = urlTo + MERGE_URL_KEY_SEPARATOR + urlFrom;
		int oldIndex = lstUrls.indexOf(key);
		if (oldIndex != -1) {
			lstUrls.remove(oldIndex);
		}
		lstUrls.add(0, key);
		if (lstUrls.size() > 64) {
			lstUrls.remove(lstUrls.size() - 1);
		}
		Manager.getSettings().setLstMergeUrlHistory(lstUrls);
	}

	public synchronized static String[] getUrlHistoryForMerge(String urlTo) {
		ArrayList<String> lstResult = new ArrayList<String>(Manager
				.getSettings().getLstRepoUrl());

		String searchCriterionReverse = MERGE_URL_KEY_SEPARATOR + urlTo;
		String searchCriterion = urlTo + MERGE_URL_KEY_SEPARATOR;
		ArrayList<String> lstMergeUrlHistory = new ArrayList<String>(Manager
				.getSettings().getLstMergeUrlHistory());
		Collections.reverse(lstMergeUrlHistory);
		for (String mergeHistoryLine : lstMergeUrlHistory) {
			if (mergeHistoryLine.endsWith(searchCriterionReverse)) {
				String urlFrom = mergeHistoryLine.substring(0,
						mergeHistoryLine.indexOf(MERGE_URL_KEY_SEPARATOR));
				int indexInUrlHistory = lstResult.indexOf(urlFrom);
				if (indexInUrlHistory != -1) {
					lstResult.remove(indexInUrlHistory);
					lstResult.add(0, urlFrom);
				}
			}
		}
		for (String mergeHistoryLine : lstMergeUrlHistory) {
			if (mergeHistoryLine.startsWith(searchCriterion)) {
				String urlFrom = mergeHistoryLine.substring(searchCriterion
						.length());
				int indexInUrlHistory = lstResult.indexOf(urlFrom);
				if (indexInUrlHistory != -1) {
					lstResult.remove(indexInUrlHistory);
					lstResult.add(0, urlFrom);
				}
			}
		}

		return lstResult.toArray(new String[0]);
	}
}
