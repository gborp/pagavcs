package hu.pagavcs;

import hu.pagavcs.bl.FileStatusCache;
import hu.pagavcs.bl.Manager;
import hu.pagavcs.bl.FileStatusCache.STATUS;
import hu.pagavcs.gui.platform.MessagePane;
import hu.pagavcs.operation.Checkout;
import hu.pagavcs.operation.Cleanup;
import hu.pagavcs.operation.Commit;
import hu.pagavcs.operation.Delete;
import hu.pagavcs.operation.Ignore;
import hu.pagavcs.operation.Log;
import hu.pagavcs.operation.Other;
import hu.pagavcs.operation.ResolveConflict;
import hu.pagavcs.operation.Revert;
import hu.pagavcs.operation.Settings;
import hu.pagavcs.operation.Unignore;
import hu.pagavcs.operation.Update;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import javax.net.ServerSocketFactory;

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

	private static final int     PORT                          = 12905;
	private static final String  SERVER_RUNNING_INDICATOR_FILE = "server-running-indicator";
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
			default:
				return "";
		}
	}

	private void outComm(Socket socket, String strOut) throws IOException {
		BufferedWriter outToClient = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
		outToClient.write(strOut);
		// outToClient.flush();
		outToClient.close();
	}

	public void execute() throws Exception {
		String tempDir = Manager.getTempDir();
		running = new File(tempDir + SERVER_RUNNING_INDICATOR_FILE);
		running.createNewFile();
		try {
			serverSocket = ServerSocketFactory.getDefault().createServerSocket(PORT);
		} catch (IOException ex) {
			System.exit(-5);
		}

		Manager.init();
		fileStatusCache = FileStatusCache.getInstance();

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

			} catch (Exception ex) {
				Manager.handle(ex);
			}
		}
		running.delete();
	}

	private String getMenuItems(List<String> lstArg) throws SVNException {

		boolean hasSvned = false;
		boolean hasConflicted = false;
		boolean hasNotConflicted = false;
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
		}

		StringBuilder sb = new StringBuilder();
		if (hasSvned) {
			sb.append("NautilusPython::update_file_item\n");
			sb.append("Update\n");
			sb.append("Update\n");
			sb.append("pagavcs-update\n");
			sb.append("t\n");
			sb.append("update\n");
		}
		if (hasSvned) {
			sb.append("NautilusPython::commit_file_item\n");
			sb.append("Commit\n");
			sb.append("Commit\n");
			sb.append("pagavcs-commit\n");
			sb.append("t\n");
			sb.append("commit\n");
		}
		if (hasSvned) {
			sb.append("NautilusPython::log_file_item\n");
			sb.append("Log\n");
			sb.append("Log\n");
			sb.append("pagavcs-log\n");
			sb.append("t\n");
			sb.append("log\n");
		}
		if (hasSvned) {
			sb.append("NautilusPython::ignore_file_item\n");
			sb.append("Ignore\n");
			sb.append("Ignore\n");
			sb.append("pagavcs-ignore\n");
			sb.append("\n");
			sb.append("ignore\n");
		}
		if (hasSvned) {
			sb.append("NautilusPython::unignore_file_item\n");
			sb.append("Unignore\n");
			sb.append("Unignore\n");
			sb.append("pagavcs-unignore\n");
			sb.append("\n");
			sb.append("unignore\n");
		}
		if (hasSvned) {
			sb.append("NautilusPython::delete_file_item\n");
			sb.append("Delete\n");
			sb.append("Delete\n");
			sb.append("pagavcs-delete\n");
			sb.append("\n");
			sb.append("delete\n");
		}
		if (hasSvned) {
			sb.append("NautilusPython::revert_file_item\n");
			sb.append("Revert\n");
			sb.append("Revert\n");
			sb.append("pagavcs-revert\n");
			sb.append("\n");
			sb.append("revert\n");
		}
		if (!hasSvned) {
			sb.append("NautilusPython::checkout_file_item\n");
			sb.append("Checkout\n");
			sb.append("Checkout\n");
			sb.append("pagavcs-checkout\n");
			sb.append("\n");
			sb.append("checkout\n");
		}
		if (hasSvned) {
			sb.append("NautilusPython::cleanup_file_item\n");
			sb.append("Cleanup\n");
			sb.append("Cleanup\n");
			sb.append("pagavcs-cleanup\n");
			sb.append("t\n");
			sb.append("cleanup\n");
		}
		if (hasConflicted && !hasNotConflicted) {
			// TODO also include use theirs and use mine option
			sb.append("NautilusPython::resolve_file_item\n");
			sb.append("Resolve\n");
			sb.append("Resolve\n");
			sb.append("pagavcs-resolve\n");
			sb.append("\n");
			sb.append("resolve\n");
		}
		if (hasSvned) {
			sb.append("NautilusPython::other_file_item\n");
			sb.append("Other\n");
			sb.append("Other\n");
			sb.append("pagavcs-other\n");
			sb.append("t\n");
			sb.append("other\n");
		}
		sb.append("NautilusPython::settings_file_item\n");
		sb.append("Settings\n");
		sb.append("Settings\n");
		sb.append("pagavcs-settings\n");
		sb.append("\n");
		sb.append("settings\n");

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

		private void warningIfMultiSelection() {
			if (lstArg.size() > 1) {
				MessagePane.showWarning(null, "Only on first selected", "THe operation is executed only on the first selected element.");
			}
		}

		public void run() {
			try {

				if ("update".equals(command)) {
					Update update = new Update(lstArg);
					update.execute();
				} else if ("log".equals(command)) {
					warningIfMultiSelection();
					Log showlog = new Log(lstArg.get(0));
					showlog.execute();
				} else if ("commit".equals(command)) {
					warningIfMultiSelection();
					Commit commit = new Commit(lstArg.get(0));
					commit.execute();
				} else if ("ignore".equals(command)) {
					warningIfMultiSelection();
					Ignore ignore = new Ignore(lstArg.get(0));
					ignore.execute();
				} else if ("unignore".equals(command)) {
					warningIfMultiSelection();
					Unignore unignore = new Unignore(lstArg.get(0));
					unignore.execute();
				} else if ("revert".equals(command)) {
					warningIfMultiSelection();
					Revert revert = new Revert(lstArg.get(0));
					revert.execute();
				} else if ("cleanup".equals(command)) {
					warningIfMultiSelection();
					Cleanup cleanup = new Cleanup(lstArg.get(0));
					cleanup.execute();
				} else if ("delete".equals(command)) {
					warningIfMultiSelection();
					Delete delete = new Delete(lstArg.get(0));
					delete.execute();
				} else if ("other".equals(command)) {
					warningIfMultiSelection();
					Other other = new Other(lstArg.get(0));
					other.execute();
				} else if ("checkout".equals(command)) {
					warningIfMultiSelection();
					Checkout checkout = new Checkout(lstArg.get(0));
					checkout.execute();
				} else if ("settings".equals(command)) {
					Settings settings = new Settings();
					settings.execute();
				} else if ("resolve".equals(command)) {
					warningIfMultiSelection();
					ResolveConflict resolve = new ResolveConflict(null, lstArg.get(0), false);
					resolve.execute();
				} else if ("stop".equals(command)) {
					System.exit(0);
				} else if ("ping".equals(command)) {
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
