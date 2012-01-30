package hu.pagavcs;

import hu.pagavcs.client.bl.Manager;
import hu.pagavcs.client.operation.ApplyPatchOperation;
import hu.pagavcs.client.operation.BlameOperation;
import hu.pagavcs.client.operation.Checkout;
import hu.pagavcs.client.operation.Cleanup;
import hu.pagavcs.client.operation.Commit;
import hu.pagavcs.client.operation.CopyMoveRename;
import hu.pagavcs.client.operation.CreateRepo;
import hu.pagavcs.client.operation.Delete;
import hu.pagavcs.client.operation.ExportOperation;
import hu.pagavcs.client.operation.Ignore;
import hu.pagavcs.client.operation.LockOperation;
import hu.pagavcs.client.operation.Log;
import hu.pagavcs.client.operation.MergeOperation;
import hu.pagavcs.client.operation.PropertiesOperation;
import hu.pagavcs.client.operation.RepoBrowser;
import hu.pagavcs.client.operation.ResolveConflict;
import hu.pagavcs.client.operation.ResolveConflictUsingMine;
import hu.pagavcs.client.operation.ResolveConflictUsingTheirs;
import hu.pagavcs.client.operation.Revert;
import hu.pagavcs.client.operation.Settings;
import hu.pagavcs.client.operation.ShowChangesOperation;
import hu.pagavcs.client.operation.SwitchOperation;
import hu.pagavcs.client.operation.Unignore;
import hu.pagavcs.client.operation.UnlockOperation;
import hu.pagavcs.client.operation.Update;
import hu.pagavcs.client.operation.UpdateToRevisionOperation;
import hu.pagavcs.common.ResourceBundleAccessor;
import hu.pagavcs.server.FileStatusCache;
import hu.pagavcs.server.FileStatusCache.STATUS;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.swing.ImageIcon;

import org.tmatesoft.svn.core.SVNException;

import cx.ath.matthew.unix.UnixServerSocket;
import cx.ath.matthew.unix.UnixSocket;

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

public class Communication {

	private static final String SERVER_RUNNING_INDICATOR_FILE = System
			.getProperty("user.home") + "/.pagavcs/server-running.lock";
	private static final String UNIX_SOCKET = System.getProperty("user.home")
			+ "/.pagavcs/socket";

	private static final String COMMAND_UPDATE = "update";
	private static final String COMMAND_UPDATE2 = "up";
	private static final String COMMAND_LOG = "log";
	private static final String COMMAND_COMMIT = "commit";
	private static final String COMMAND_SHOWLOCALCHANGES = "showlocalchanges";
	private static final String COMMAND_IGNORE = "ignore";
	private static final String COMMAND_UNIGNORE = "unignore";
	private static final String COMMAND_REVERT = "revert";
	private static final String COMMAND_CLEANUP = "cleanup";
	private static final String COMMAND_LOCK = "lock";
	private static final String COMMAND_UNLOCK = "unlock";
	private static final String COMMAND_DELETE = "delete";
	private static final String COMMAND_MERGE = "merge";
	private static final String COMMAND_COPYMOVERENAME = "copymoverename";
	private static final String COMMAND_CHECKOUT = "checkout";
	private static final String COMMAND_SETTINGS = "settings";
	private static final String COMMAND_SWITCH = "switch";
	private static final String COMMAND_RESOLVE = "resolve";
	private static final String COMMAND_RESOLVEUSINGMINE = "resolveusingmine";
	private static final String COMMAND_RESOLVEUSINGTHEIRS = "resolveusingtheirs";
	private static final String COMMAND_REPOBROWSER = "repobrowser";
	private static final String COMMAND_CREATEREPO = "createrepo";
	private static final String COMMAND_STOP = "stop";
	private static final String COMMAND_PING = "ping";
	private static final String COMMAND_ADD = "add";
	private static final String COMMAND_APPLY_PATCH = "applypatch";
	private static final String COMMAND_BLAME = "blame";
	private static final String COMMAND_UPDATE_TO_REVISION = "updatetorevision";
	private static final String COMMAND_EXPORT = "export";
	private static final String COMMAND_PROPERTIES = "properties";

	private static final String CFG_DEBUG_MODE_KEY = "debug";

	private static Communication singleton;
	private boolean shutdown;
	private File running;
	private UnixServerSocket serverSocket;
	private FileStatusCache fileStatusCache;

	private Communication() {
	}

	private String getFileEmblem(STATUS status) {
		if (status == null) {
			return "";
		}
		switch (status) {
		case ADDED:
			return "pagavcs-added";
		case CONFLICTS:
			return "pagavcs-conflict";
		case DELETED:
			return "pagavcs-deleted";
		case IGNORED:
			return "pagavcs-ignored";
		case LOCKED:
			return "pagavcs-locked";
		case MODIFIED:
			return "pagavcs-modified";
		case NONE:
			return "";
		case NORMAL:
			return "pagavcs-normal";
		case OBSTRUCTED:
			return "pagavcs-obstructed";
		case READONLY:
			return "pagavcs-readonly";
		case SVNED:
			return "pagavcs-svn";
		case UNVERSIONED:
			return "pagavcs-unversioned";
		case UNKNOWN:
			return "pagavcs-unknown";
		default:
			return "";
		}
	}

	private void outComm(UnixSocket socket, String strOut) throws IOException {
		BufferedWriter outToClient = new BufferedWriter(new OutputStreamWriter(
				socket.getOutputStream()));
		outToClient.write(strOut);
		outToClient.close();
	}

	private void outCommEmblem(UnixSocket socket, String arg)
			throws IOException {

		String name = null;
		Integer width = null;
		Integer height = null;
		ImageIcon imageIcon = null;

		try {
			String[] args = arg.split(" ");

			name = args[0];
			width = Integer.valueOf(args[1]);
			height = Integer.valueOf(args[2]);

			ImageIcon ii = ResourceBundleAccessor.getImage(name + ".png");
			if (ii != null) {
				imageIcon = new ImageIcon(ii.getImage().getScaledInstance(
						width, height, Image.SCALE_SMOOTH));
			}
		} catch (Throwable t) {
		}

		if (imageIcon == null) {
			if (width == null) {
				width = 8;
			}
			if (height == null) {
				height = 8;
			}
			imageIcon = new ImageIcon(new BufferedImage(width, height,
					BufferedImage.TRANSLUCENT));
		}

		ObjectOutputStream objOut = new ObjectOutputStream(
				socket.getOutputStream());
		objOut.writeObject(imageIcon);
		objOut.close();
	}

	public void execute() throws Exception {
		running = new File(SERVER_RUNNING_INDICATOR_FILE);
		running.createNewFile();

		boolean debugMode = false;
		File cfgFile = new File(System.getProperty("user.home"),
				".pagavcs/config.properties");
		try {
			if (cfgFile.exists()) {
				Properties prop = new Properties();
				FileReader reader = new FileReader(cfgFile);
				prop.load(reader);
				reader.close();
				if (prop.containsKey(CFG_DEBUG_MODE_KEY)) {
					debugMode = Boolean.valueOf((String) prop
							.get(CFG_DEBUG_MODE_KEY));
					LogHelper.setDebugMode(debugMode);
				}
			}
		} catch (Exception ex) {
		}

		try {
			if (!cfgFile.getParentFile().isDirectory()) {
				cfgFile.getParentFile().mkdir();
			}

			Properties prop = new Properties();
			prop.put(CFG_DEBUG_MODE_KEY, Boolean.toString(debugMode));
			FileWriter writer = new FileWriter(cfgFile);
			prop.store(writer, null);
			writer.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		try {
			serverSocket = new UnixServerSocket(UNIX_SOCKET);
			new File(UNIX_SOCKET).deleteOnExit();
		} catch (IOException ex) {
			try {
				UnixSocket clientSocket = new UnixSocket(UNIX_SOCKET);
				clientSocket.setSoTimeout(2000);
				clientSocket.connect(UNIX_SOCKET);
				LogHelper.GENERAL.fatal("PagaVCS is already running");
				System.exit(-5);
			} catch (IOException ex2) {
				new File(UNIX_SOCKET).delete();
				serverSocket = new UnixServerSocket(UNIX_SOCKET);
				new File(UNIX_SOCKET).deleteOnExit();
			}
		}

		Manager.init();
		fileStatusCache = FileStatusCache.getInstance();
		LogHelper.GENERAL.warn("PagaVCS started.");

		while (!shutdown) {
			try {
				UnixSocket socket = serverSocket.accept();

				BufferedReader br = new BufferedReader(new InputStreamReader(
						socket.getInputStream()));

				String line = br.readLine();
				if (line == null || line.isEmpty()) {
					continue;
				}

				boolean waitSyncMode = false;
				boolean autoClose = false;
				String command;
				String arg = null;
				List<String> lstArg = null;
				if (line.startsWith("\"")) {

					String[] elements = line.split("\\\" \\\"");
					elements[0] = elements[0].substring(1);
					int eLastIndex = elements.length - 1;
					String eLast = elements[eLastIndex];
					elements[eLastIndex] = eLast.substring(0,
							eLast.length() - 2);

					ArrayList<String> lstElements = new ArrayList<String>(
							Arrays.asList(elements));

					if (lstElements.get(0).equals("-wait")
							|| lstElements.get(0).equals("-w")) {
						waitSyncMode = true;
						lstElements.remove(0);
					}
					if (lstElements.get(0).equals("-autoclose")
							|| lstElements.get(0).equals("-c")) {
						autoClose = true;
						lstElements.remove(0);
					}
					command = lstElements.get(0);
					lstElements.remove(0);
					lstArg = lstElements;
					if (lstElements.size() > 0) {
						arg = lstArg.get(0);
					}
				} else {
					int commandEndIndex = line.indexOf(' ');
					command = line.substring(
							0,
							commandEndIndex > -1 ? commandEndIndex : line
									.length());
					if (commandEndIndex > -1) {
						arg = line.substring(commandEndIndex + 1);
					}
				}

				if (command.equals("getfileinfo")) {
					String outStr = getFileEmblem(fileStatusCache
							.getStatus(new File(arg)));
					outComm(socket, outStr);
				} else if (command.equals("getfileinfonl")) {
					String outStr = getFileEmblem(fileStatusCache
							.getStatusFast(new File(arg))) + "\n";
					outComm(socket, outStr);
				} else if (command.equals("getemblem")) {
					outCommEmblem(socket, arg);
				} else {

					if (arg != null) {
						if (lstArg == null) {
							lstArg = new ArrayList<String>();
							boolean inQuote = false;
							boolean inBackslash = false;
							StringBuilder sb = new StringBuilder();
							for (char c : arg.toCharArray()) {

								if (!inBackslash) {
									if (c == '\"') {

										if (inQuote) {
											inQuote = false;
										} else {
											inQuote = true;
											sb = new StringBuilder();
										}
									} else if (c == ' ' && !inQuote) {
										lstArg.add(sb.toString());
										sb = null;
									} else {
										if (c == '\\') {
											inBackslash = true;
										} else {
											if (sb == null) {
												// wrong file url
												break;
											}
											sb.append(c);
										}
									}
								} else {
									sb.append(c);
									inBackslash = false;
								}
							}

							if (sb != null) {
								lstArg.add(sb.toString());
							}
						}
					}

					if (command.equals("getmenuitems")) {
						outComm(socket, getMenuItems(lstArg));
					} else {
						new Thread(new ProcessInput(command, lstArg, socket,
								waitSyncMode, autoClose), line).start();
						if (!waitSyncMode) {
							outComm(socket, "Processing " + command + "...\n");
						}
					}
				}

			} catch (SocketTimeoutException ex) {
				shutdown = true;
			} catch (IOException ex) {
				shutdown = true;
			} catch (Exception ex) {
				Manager.handle(ex);
			}
		}
		running.delete();
	}

	private void makeMenuItem(StringBuilder sb, String label, String tooltip,
			String icon, String mode, String command) {
		sb.append("NautilusPython::PagaVCS::");
		sb.append(command);
		sb.append('\n');

		sb.append(label);
		sb.append('\n');

		sb.append(tooltip);
		sb.append('\n');

		sb.append(icon);
		sb.append('\n');

		sb.append(mode);
		sb.append('\n');

		sb.append(command);
		sb.append('\n');
	}

	private String getMenuItems(List<String> lstArg) throws SVNException {

		boolean hasSvned = false;
		boolean hasConflicted = false;
		boolean hasNotConflicted = false;
		boolean hasModified = false;
		boolean hasNotModified = false;
		boolean hasFile = false;
		boolean hasDir = false;
		for (String fileName : lstArg) {
			File file = new File(fileName);
			STATUS status = fileStatusCache.getStatus(file);
			if (hasSvned || status.equals(STATUS.ADDED)
					|| status.equals(STATUS.CONFLICTS)
					|| status.equals(STATUS.DELETED)
					|| status.equals(STATUS.IGNORED)
					|| status.equals(STATUS.LOCKED)
					|| status.equals(STATUS.MODIFIED)
					|| status.equals(STATUS.NORMAL)
					|| status.equals(STATUS.OBSTRUCTED)
					|| status.equals(STATUS.READONLY)
					|| status.equals(STATUS.SVNED)
					|| status.equals(STATUS.UNVERSIONED)) {
				hasSvned = true;
			}
			if (status.equals(STATUS.CONFLICTS)) {
				hasConflicted = true;
			} else {
				hasNotConflicted = true;
			}
			if (status.equals(STATUS.MODIFIED)) {
				hasModified = true;
			} else {
				hasNotModified = true;
			}
			if (file.isDirectory()) {
				hasDir = true;
			} else {
				hasFile = true;
			}
		}

		StringBuilder sb = new StringBuilder(512);
		boolean showShowChanges = false;
		if (hasSvned && !hasDir && hasFile && hasModified && !hasNotModified) {
			makeMenuItem(sb, "Show changes", "Show local changes",
					"pagavcs-difficon", "", COMMAND_SHOWLOCALCHANGES);
			showShowChanges = true;
		}
		if (hasSvned) {
			makeMenuItem(sb, "Update", "Update", "pagavcs-update", "tp"
					+ (showShowChanges ? "s" : ""), COMMAND_UPDATE);
		}
		if (hasSvned) {
			makeMenuItem(sb, "Commit", "Commit", "pagavcs-commit", "tp",
					COMMAND_COMMIT);
		}
		if (hasSvned) {
			makeMenuItem(sb, "Log", "Log", "pagavcs-log", "t", COMMAND_LOG);
		}

		makeMenuItem(sb, "Repo Browser", "Repo Browser", "pagavcs-drive", "",
				COMMAND_REPOBROWSER);
		if (!hasSvned && hasDir && !hasFile) {
			makeMenuItem(sb, "Create Repository here",
					"Create Repository here", "pagavcs-about", "",
					COMMAND_CREATEREPO);
		}

		if (hasSvned) {
			makeMenuItem(sb, "Update to revision", "Update to revision",
					"pagavcs-update", "", COMMAND_UPDATE_TO_REVISION);
			makeMenuItem(sb, "Blame", "Blame", "pagavcs-other", "",
					COMMAND_BLAME);
		}

		if (hasSvned) {
			makeMenuItem(sb, "Ignore", "Ignore", "pagavcs-ignore", "s",
					COMMAND_IGNORE);
		}
		if (hasSvned) {
			makeMenuItem(sb, "Unignore", "Unignore", "pagavcs-unignore", "",
					COMMAND_UNIGNORE);
		}
		if (hasSvned) {
			makeMenuItem(sb, "Copy-move-rename", "Copy-move-rename",
					"pagavcs-rename", "", COMMAND_COPYMOVERENAME);
			makeMenuItem(sb, "Delete", "Delete", "pagavcs-delete", "",
					COMMAND_DELETE);
		}
		if (hasSvned) {
			makeMenuItem(sb, "Revert", "Revert", "pagavcs-revert", "",
					COMMAND_REVERT);
		}
		if (!hasSvned) {
			makeMenuItem(sb, "Checkout", "Checkout", "pagavcs-checkout", "t",
					COMMAND_CHECKOUT);
		}
		if (hasSvned) {
			makeMenuItem(sb, "Cleanup", "Cleanup", "pagavcs-cleanup", "t",
					COMMAND_CLEANUP);
			makeMenuItem(sb, "Get lock", "Get lock", "pagavcs-lock", "",
					COMMAND_LOCK);
			makeMenuItem(sb, "Release lock", "Release lock", "pagavcs-unlock",
					"", COMMAND_UNLOCK);
		}
		if (hasConflicted && !hasNotConflicted) {
			makeMenuItem(sb, "Resolve using mine", "Resolve using mine",
					"pagavcs-resolve", "", COMMAND_RESOLVEUSINGMINE);
			makeMenuItem(sb, "Resolve using theirs", "Resolve using theirs",
					"pagavcs-resolve", "", COMMAND_RESOLVEUSINGTHEIRS);
			makeMenuItem(sb, "Resolve", "Resolve", "pagavcs-resolve", "",
					COMMAND_RESOLVE);
		}
		if (hasSvned) {
			makeMenuItem(sb, "Switch", "Switch", "pagavcs-switch", "t",
					COMMAND_SWITCH);
			makeMenuItem(sb, "Merge", "Merge", "pagavcs-merge", "t",
					COMMAND_MERGE);
			makeMenuItem(sb, "Export", "Export", "pagavcs-export", "",
					COMMAND_EXPORT);
			makeMenuItem(sb, "Apply patch", "Apply patch",
					"pagavcs-applypatch", "", COMMAND_APPLY_PATCH);
			makeMenuItem(sb, "Properties", "Properties", "pagavcs-properties",
					"", COMMAND_PROPERTIES);
		}

		makeMenuItem(sb, "Settings", "Settings", "pagavcs-settings", "s",
				COMMAND_SETTINGS);

		sb.append("--end--\n");

		return sb.toString();
	}

	public void shutdown() {
		shutdown = true;
		if (running != null) {
			running.delete();
		}
		if (serverSocket != null) {
			try {
				serverSocket.close();
			} catch (IOException e) {
			}
		}
	}

	public static Communication getInstance() throws Exception {
		if (singleton == null) {
			singleton = new Communication();
		}
		return singleton;
	}

	private class ProcessInput implements Runnable {

		private final String command;
		private final List<String> lstArg;
		private final boolean waitSyncMode;
		private final boolean autoClose;
		private final UnixSocket socket;

		public ProcessInput(String command, List<String> lstArg,
				UnixSocket socket, boolean waitSyncMode, boolean autoClose) {
			this.command = command;
			this.lstArg = lstArg;
			this.socket = socket;
			this.waitSyncMode = waitSyncMode;
			this.autoClose = autoClose;
		}

		public void run() {
			try {
				if (COMMAND_PROPERTIES.equals(command)) {
					for (String path : lstArg) {
						PropertiesOperation propertiesOperation = new PropertiesOperation(
								path);
						propertiesOperation.execute();
					}
				} else if (COMMAND_APPLY_PATCH.equals(command)) {
					for (String path : lstArg) {
						ApplyPatchOperation applyPatch = new ApplyPatchOperation(
								path);
						applyPatch.execute();
					}
				} else if (COMMAND_BLAME.equals(command)) {
					for (String path : lstArg) {
						BlameOperation blame = new BlameOperation(path);
						blame.execute();
					}
				} else if (COMMAND_EXPORT.equals(command)) {
					for (String path : lstArg) {
						ExportOperation export = new ExportOperation(path);
						export.execute();
					}
				} else if (COMMAND_UPDATE.equals(command)
						|| COMMAND_UPDATE2.equals(command)) {
					Update update = new Update(lstArg);
					update.setAutoClose(autoClose);
					update.execute();
				} else if (COMMAND_UPDATE_TO_REVISION.equals(command)) {
					for (String path : lstArg) {
						UpdateToRevisionOperation updateToRevision = new UpdateToRevisionOperation(
								path);
						updateToRevision.setAutoClose(autoClose);
						updateToRevision.execute();
					}
				} else if (COMMAND_LOG.equals(command)) {
					for (String path : lstArg) {
						Log showlog = new Log(path);
						showlog.execute();
					}
				} else if (COMMAND_COMMIT.equals(command)) {
					for (String path : lstArg) {
						Commit commit = new Commit(path);
						commit.execute();
					}
				} else if (COMMAND_SHOWLOCALCHANGES.equals(command)) {
					ShowChangesOperation showChanges = new ShowChangesOperation(
							lstArg);
					showChanges.execute();
				} else if (COMMAND_IGNORE.equals(command)) {
					for (String path : lstArg) {
						Ignore ignore = new Ignore(path);
						ignore.execute();
					}
				} else if (COMMAND_UNIGNORE.equals(command)) {
					for (String path : lstArg) {
						Unignore unignore = new Unignore(path);
						unignore.execute();
					}
				} else if (COMMAND_REVERT.equals(command)) {
					for (String path : lstArg) {
						Revert revert = new Revert(path);
						revert.execute();
					}
				} else if (COMMAND_CLEANUP.equals(command)) {
					for (String path : lstArg) {
						Cleanup cleanup = new Cleanup(path);
						cleanup.setAutoClose(autoClose);
						cleanup.execute();
					}
				} else if (COMMAND_LOCK.equals(command)) {
					for (String path : lstArg) {
						LockOperation lockOperation = new LockOperation(path);
						lockOperation.execute();
					}
				} else if (COMMAND_UNLOCK.equals(command)) {
					for (String path : lstArg) {
						UnlockOperation unlockOperation = new UnlockOperation(
								path);
						unlockOperation.execute();
					}
				} else if (COMMAND_COPYMOVERENAME.equals(command)) {
					for (String path : lstArg) {
						CopyMoveRename copyMoveReaname = new CopyMoveRename(
								path);
						copyMoveReaname.execute();
					}
				} else if (COMMAND_DELETE.equals(command)) {
					Delete delete = new Delete(lstArg);
					delete.execute();
				} else if (COMMAND_SWITCH.equals(command)) {
					for (String path : lstArg) {
						SwitchOperation switchOp = new SwitchOperation(path);
						switchOp.execute();
					}
				} else if (COMMAND_MERGE.equals(command)) {
					for (String path : lstArg) {
						MergeOperation merge = new MergeOperation(path);
						merge.execute();
					}
				} else if (COMMAND_CHECKOUT.equals(command)) {
					for (String path : lstArg) {
						Checkout checkout = new Checkout(path);
						checkout.execute();
					}
				} else if (COMMAND_SETTINGS.equals(command)) {
					Settings settings = new Settings();
					settings.execute();
				} else if (COMMAND_RESOLVE.equals(command)) {
					for (String path : lstArg) {
						ResolveConflict resolve = new ResolveConflict(null,
								path, false);
						resolve.execute();
					}
				} else if (COMMAND_RESOLVEUSINGMINE.equals(command)) {
					ResolveConflictUsingMine resolveUsingMine = new ResolveConflictUsingMine(
							lstArg);
					resolveUsingMine.execute();
				} else if (COMMAND_RESOLVEUSINGTHEIRS.equals(command)) {
					ResolveConflictUsingTheirs resolveUsingTheirs = new ResolveConflictUsingTheirs(
							lstArg);
					resolveUsingTheirs.execute();
				} else if (COMMAND_REPOBROWSER.equals(command)) {
					for (String path : lstArg) {
						RepoBrowser repoBrowser = new RepoBrowser(path);
						repoBrowser.execute();
					}
				} else if (COMMAND_CREATEREPO.equals(command)) {
					for (String path : lstArg) {
						CreateRepo createRepo = new CreateRepo(path);
						createRepo.execute();
					}
				} else if (COMMAND_STOP.equals(command)) {
					System.exit(0);
				} else if (COMMAND_PING.equals(command)) {
					// do nothing
				} else {
					throw new RuntimeException("Unimplemented command: "
							+ command);
				}
			} catch (Exception ex) {
				Manager.handle(ex);
			} finally {
				if (waitSyncMode) {
					try {
						outComm(socket, "Finished.\n");
					} catch (IOException ex2) {
						Manager.handle(ex2);
					}
				}
			}
		}
	}

}
