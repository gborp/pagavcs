package hu.pagavcs;

import hu.pagavcs.bl.FileStatusCache;
import hu.pagavcs.bl.Manager;
import hu.pagavcs.bl.FileStatusCache.STATUS;
import hu.pagavcs.operation.ApplyPatchOperation;
import hu.pagavcs.operation.BlameOperation;
import hu.pagavcs.operation.Checkout;
import hu.pagavcs.operation.Cleanup;
import hu.pagavcs.operation.Commit;
import hu.pagavcs.operation.CopyMoveRename;
import hu.pagavcs.operation.Delete;
import hu.pagavcs.operation.ExportOperation;
import hu.pagavcs.operation.Ignore;
import hu.pagavcs.operation.LockOperation;
import hu.pagavcs.operation.Log;
import hu.pagavcs.operation.MergeOperation;
import hu.pagavcs.operation.RepoBrowser;
import hu.pagavcs.operation.ResolveConflict;
import hu.pagavcs.operation.ResolveConflictUsingMine;
import hu.pagavcs.operation.ResolveConflictUsingTheirs;
import hu.pagavcs.operation.Revert;
import hu.pagavcs.operation.Settings;
import hu.pagavcs.operation.ShowChangesOperation;
import hu.pagavcs.operation.SwitchOperation;
import hu.pagavcs.operation.Unignore;
import hu.pagavcs.operation.UnlockOperation;
import hu.pagavcs.operation.Update;
import hu.pagavcs.operation.UpdateToRevisionOperation;

import java.awt.Image;
import java.awt.Toolkit;
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
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.net.ServerSocketFactory;
import javax.swing.ImageIcon;

import org.tmatesoft.svn.core.SVNException;

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

	private static final int     DEFAULT_PORT                  = 12905;
	private static final String  SERVER_RUNNING_INDICATOR_FILE = "server-running-indicator";

	private static final String  COMMAND_UPDATE                = "update";
	private static final String  COMMAND_LOG                   = "log";
	private static final String  COMMAND_COMMIT                = "commit";
	private static final String  COMMAND_SHOWLOCALCHANGES      = "showlocalchanges";
	private static final String  COMMAND_IGNORE                = "ignore";
	private static final String  COMMAND_UNIGNORE              = "unignore";
	private static final String  COMMAND_REVERT                = "revert";
	private static final String  COMMAND_CLEANUP               = "cleanup";
	private static final String  COMMAND_LOCK                  = "lock";
	private static final String  COMMAND_UNLOCK                = "unlock";
	private static final String  COMMAND_DELETE                = "delete";
	private static final String  COMMAND_MERGE                 = "merge";
	private static final String  COMMAND_COPYMOVERENAME        = "copymoverename";
	private static final String  COMMAND_CHECKOUT              = "checkout";
	private static final String  COMMAND_SETTINGS              = "settings";
	private static final String  COMMAND_SWITCH                = "switch";
	private static final String  COMMAND_RESOLVE               = "resolve";
	private static final String  COMMAND_RESOLVEUSINGMINE      = "resolveusingmine";
	private static final String  COMMAND_RESOLVEUSINGTHEIRS    = "resolveusingtheirs";
	private static final String  COMMAND_REPOBROWSER           = "repobrowser";
	private static final String  COMMAND_STOP                  = "stop";
	private static final String  COMMAND_PING                  = "ping";
	private static final String  COMMAND_ADD                   = "add";
	private static final String  COMMAND_APPLY_PATCH           = "applypatch";
	private static final String  COMMAND_BLAME                 = "blame";
	private static final String  COMMAND_UPDATE_TO_REVISION    = "updatetorevision";
	private static final String  COMMAND_EXPORT                = "export";

	private static final String  CFG_COMMUNICATION_PORT_KEY    = "port";

	private static Communication singleton;
	private boolean              shutdown;
	private File                 running;
	private ServerSocket         serverSocket;
	private FileStatusCache      fileStatusCache;

	private Communication() {}

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

	private void outComm(Socket socket, String strOut) throws IOException {
		BufferedWriter outToClient = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
		outToClient.write(strOut);
		outToClient.close();
	}

	private void outCommEmblem(Socket socket, String arg) throws IOException {

		String name = null;
		Integer width = null;
		Integer height = null;
		ImageIcon imageIcon = null;

		try {
			String[] args = arg.split(" ");

			name = args[0];
			width = Integer.valueOf(args[1]);
			height = Integer.valueOf(args[2]);

			URL url = Communication.class.getResource("/hu/pagavcs/resources/" + name + ".png");
			if (url != null) {
				ImageIcon ii = new ImageIcon(Toolkit.getDefaultToolkit().getImage(url));
				imageIcon = new ImageIcon(ii.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH));
			}
		} catch (Throwable t) {}

		if (imageIcon == null) {
			if (width == null) {
				width = 8;
			}
			if (height == null) {
				height = 8;
			}
			imageIcon = new ImageIcon(new BufferedImage(width, height, BufferedImage.TRANSLUCENT));
		}

		ObjectOutputStream objOut = new ObjectOutputStream(socket.getOutputStream());
		objOut.writeObject(imageIcon);
		objOut.close();
	}

	public void execute() throws Exception {
		String tempDir = Manager.getTempDir();
		running = new File(tempDir + SERVER_RUNNING_INDICATOR_FILE);
		running.createNewFile();

		int port = DEFAULT_PORT;
		File cfgFile = new File(System.getProperty("user.home"), ".pagavcs/config.properties");
		try {
			if (cfgFile.exists()) {
				Properties prop = new Properties();
				FileReader reader = new FileReader(cfgFile);
				prop.load(reader);
				reader.close();
				if (prop.containsKey(CFG_COMMUNICATION_PORT_KEY)) {
					port = Integer.valueOf((String) prop.get(CFG_COMMUNICATION_PORT_KEY));
				}
			}
		} catch (Exception ex) {}

		try {
			if (!cfgFile.getParentFile().isDirectory()) {
				cfgFile.getParentFile().mkdir();
			}

			Properties prop = new Properties();
			prop.put(CFG_COMMUNICATION_PORT_KEY, Integer.toString(port));
			FileWriter writer = new FileWriter(cfgFile);
			prop.store(writer, null);
			writer.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		try {
			serverSocket = ServerSocketFactory.getDefault().createServerSocket(port);
		} catch (IOException ex) {
			System.out.println("Port is not free, maybe PagaVCS is already running?");
			System.exit(-5);
		}

		Manager.init();
		fileStatusCache = FileStatusCache.getInstance();
		System.out.println("PagaVCS started.");

		while (!shutdown) {
			try {
				Socket socket = serverSocket.accept();

				BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

				String line = br.readLine();
				if (line == null || line.isEmpty()) {
					continue;
				}

				int commandEndIndex = line.indexOf(' ');
				String command = line.substring(0, commandEndIndex > -1 ? commandEndIndex : line.length());

				String arg = null;
				if (commandEndIndex > -1) {
					arg = line.substring(commandEndIndex + 1);
				}

				if (command.charAt(0) == '"') {
					command = command.substring(1, command.length() - 1);
				}

				if (command.equals("getfileinfo")) {
					String outStr = getFileEmblem(fileStatusCache.getStatus(new File(arg)));
					outComm(socket, outStr);
				} else if (command.equals("getfileinfonl")) {
					String outStr = getFileEmblem(fileStatusCache.getStatusFast(new File(arg))) + "\n";
					outComm(socket, outStr);
				} else if (command.equals("getemblem")) {
					outCommEmblem(socket, arg);
				} else {
					List<String> lstArg = new ArrayList<String>();
					if (arg != null) {
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

					if (command.equals("getmenuitems")) {
						outComm(socket, getMenuItems(lstArg));
					} else {
						new Thread(new ProcessInput(command, lstArg), line).start();
						outComm(socket, "Processing...\n");
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

	private void makeMenuItem(StringBuilder sb, String label, String tooltip, String icon, String mode, String command) {
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
			if (hasSvned || status.equals(STATUS.ADDED) || status.equals(STATUS.CONFLICTS) || status.equals(STATUS.DELETED) || status.equals(STATUS.IGNORED)
			        || status.equals(STATUS.LOCKED) || status.equals(STATUS.MODIFIED) || status.equals(STATUS.NORMAL) || status.equals(STATUS.OBSTRUCTED)
			        || status.equals(STATUS.READONLY) || status.equals(STATUS.SVNED) || status.equals(STATUS.UNVERSIONED)) {
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
			makeMenuItem(sb, "Show changes", "Show local changes", "pagavcs-difficon", "", COMMAND_SHOWLOCALCHANGES);
			showShowChanges = true;
		}
		if (hasSvned) {
			makeMenuItem(sb, "Update", "Update", "pagavcs-update", "tp" + (showShowChanges ? "s" : ""), COMMAND_UPDATE);
		}
		if (hasSvned) {
			makeMenuItem(sb, "Commit", "Commit", "pagavcs-commit", "tp", COMMAND_COMMIT);
		}
		if (hasSvned) {
			makeMenuItem(sb, "Log", "Log", "pagavcs-log", "t", COMMAND_LOG);
		}

		makeMenuItem(sb, "Repo Browser", "Repo Browser", "pagavcs-drive", "", COMMAND_REPOBROWSER);

		if (hasSvned) {
			makeMenuItem(sb, "Update to revision", "Update to revision", "pagavcs-update", "", COMMAND_UPDATE_TO_REVISION);
			makeMenuItem(sb, "Blame", "Blame", "pagavcs-other", "", COMMAND_BLAME);
		}

		if (hasSvned) {
			makeMenuItem(sb, "Ignore", "Ignore", "pagavcs-ignore", "s", COMMAND_IGNORE);
		}
		if (hasSvned) {
			makeMenuItem(sb, "Unignore", "Unignore", "pagavcs-unignore", "", COMMAND_UNIGNORE);
		}
		if (hasSvned) {
			makeMenuItem(sb, "Copy-move-rename", "Copy-move-rename", "pagavcs-rename", "", COMMAND_COPYMOVERENAME);
			makeMenuItem(sb, "Delete", "Delete", "pagavcs-delete", "", COMMAND_DELETE);
		}
		if (hasSvned) {
			makeMenuItem(sb, "Revert", "Revert", "pagavcs-revert", "", COMMAND_REVERT);
		}
		if (!hasSvned) {
			makeMenuItem(sb, "Checkout", "Checkout", "pagavcs-checkout", "t", COMMAND_CHECKOUT);
		}
		if (hasSvned) {
			makeMenuItem(sb, "Cleanup", "Cleanup", "pagavcs-cleanup", "t", COMMAND_CLEANUP);
			makeMenuItem(sb, "Get lock", "Get lock", "pagavcs-lock", "", COMMAND_LOCK);
			makeMenuItem(sb, "Release lock", "Release lock", "pagavcs-unlock", "", COMMAND_UNLOCK);
		}
		if (hasConflicted && !hasNotConflicted) {
			makeMenuItem(sb, "Resolve using mine", "Resolve using mine", "pagavcs-resolve", "", COMMAND_RESOLVEUSINGMINE);
			makeMenuItem(sb, "Resolve using theirs", "Resolve using theirs", "pagavcs-resolve", "", COMMAND_RESOLVEUSINGTHEIRS);
			makeMenuItem(sb, "Resolve", "Resolve", "pagavcs-resolve", "", COMMAND_RESOLVE);
		}
		if (hasSvned) {
			makeMenuItem(sb, "Switch", "Switch", "pagavcs-switch", "t", COMMAND_SWITCH);
			makeMenuItem(sb, "Merge", "Merge", "pagavcs-merge", "t", COMMAND_MERGE);
			makeMenuItem(sb, "Export", "Export", "pagavcs-export", "", COMMAND_EXPORT);
		}
		if (hasSvned) {
			makeMenuItem(sb, "Apply patch", "Apply patch", "pagavcs-applypatch", "", COMMAND_APPLY_PATCH);
		}

		makeMenuItem(sb, "Settings", "Settings", "pagavcs-settings", "s", COMMAND_SETTINGS);

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
			} catch (IOException e) {}
		}
	}

	public static Communication getInstance() throws Exception {
		if (singleton == null) {
			singleton = new Communication();
		}
		return singleton;
	}

	private static class ProcessInput implements Runnable {

		private final String       command;
		private final List<String> lstArg;

		public ProcessInput(String command, List<String> lstArg) {
			this.command = command;
			this.lstArg = lstArg;
		}

		public void run() {
			try {
				if (COMMAND_APPLY_PATCH.equals(command)) {
					for (String path : lstArg) {
						ApplyPatchOperation applyPatch = new ApplyPatchOperation(path);
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
				} else if (COMMAND_UPDATE.equals(command)) {
					Update update = new Update(lstArg);
					update.execute();
				} else if (COMMAND_UPDATE_TO_REVISION.equals(command)) {
					for (String path : lstArg) {
						UpdateToRevisionOperation updateToRevision = new UpdateToRevisionOperation(path);
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
					ShowChangesOperation showChanges = new ShowChangesOperation(lstArg);
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
						cleanup.execute();
					}
				} else if (COMMAND_LOCK.equals(command)) {
					for (String path : lstArg) {
						LockOperation lockOperation = new LockOperation(path);
						lockOperation.execute();
					}
				} else if (COMMAND_UNLOCK.equals(command)) {
					for (String path : lstArg) {
						UnlockOperation unlockOperation = new UnlockOperation(path);
						unlockOperation.execute();
					}
				} else if (COMMAND_COPYMOVERENAME.equals(command)) {
					for (String path : lstArg) {
						CopyMoveRename copyMoveReaname = new CopyMoveRename(path);
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
						ResolveConflict resolve = new ResolveConflict(null, path, false);
						resolve.execute();
					}
				} else if (COMMAND_RESOLVEUSINGMINE.equals(command)) {
					ResolveConflictUsingMine resolveUsingMine = new ResolveConflictUsingMine(lstArg);
					resolveUsingMine.execute();
				} else if (COMMAND_RESOLVEUSINGTHEIRS.equals(command)) {
					ResolveConflictUsingTheirs resolveUsingTheirs = new ResolveConflictUsingTheirs(lstArg);
					resolveUsingTheirs.execute();
				} else if (COMMAND_REPOBROWSER.equals(command)) {
					for (String path : lstArg) {
						RepoBrowser repoBrowser = new RepoBrowser(path);
						repoBrowser.execute();
					}
				} else if (COMMAND_STOP.equals(command)) {
					System.exit(0);
				} else if (COMMAND_PING.equals(command)) {
					// do nothing
				} else {
					throw new RuntimeException("unimplemented command");
				}
			} catch (Exception ex) {
				Manager.handle(ex);
			}
		}
	}

}
