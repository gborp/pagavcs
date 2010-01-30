package hu.pagavcs;

import hu.pagavcs.bl.FileStatusCache;
import hu.pagavcs.bl.Manager;
import hu.pagavcs.bl.FileStatusCache.STATUS;
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

import javax.net.ServerSocketFactory;

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
			default:
				return "";
		}
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
		FileStatusCache fileStatusCache = FileStatusCache.getInstance();

		while (!shutdown) {
			try {
				Socket socket = serverSocket.accept();

				BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

				String line = br.readLine();
				if (line == null) {
					continue;
				}

				int commandEndIndex = line.indexOf(' ');
				String command = line.substring(0, commandEndIndex > -1 ? commandEndIndex : line.length());

				String arg = null;
				if (commandEndIndex > -1) {
					arg = line.substring(commandEndIndex + 1);
				}

				if (command.equals("getfileinfo")) {

					BufferedWriter outToClient = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
					outToClient.write(getFileEmblem(fileStatusCache.getStatus(new File(arg))));
					outToClient.flush();
					outToClient.close();
				} else {
					new Thread(new ProcessInput(command, arg), line).start();
				}

			} catch (Exception ex) {
				Manager.handle(ex);
			}
		}
		running.delete();
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

		private final String command;
		private final String arg;

		public ProcessInput(String command, String arg) {
			this.command = command;
			this.arg = arg;
		}

		public void run() {
			try {

				if ("update".equals(command)) {
					Update update = new Update(arg);
					update.execute();
				} else if ("log".equals(command)) {
					Log showlog = new Log(arg);
					showlog.execute();
				} else if ("commit".equals(command)) {
					Commit commit = new Commit(arg);
					commit.execute();
				} else if ("ignore".equals(command)) {
					Ignore ignore = new Ignore(arg);
					ignore.execute();
				} else if ("unignore".equals(command)) {
					Unignore unignore = new Unignore(arg);
					unignore.execute();
				} else if ("revert".equals(command)) {
					Revert revert = new Revert(arg);
					revert.execute();
				} else if ("cleanup".equals(command)) {
					Cleanup cleanup = new Cleanup(arg);
					cleanup.execute();
				} else if ("delete".equals(command)) {
					Delete delete = new Delete(arg);
					delete.execute();
				} else if ("other".equals(command)) {
					Other other = new Other(arg);
					other.execute();
				} else if ("checkout".equals(command)) {
					Checkout checkout = new Checkout(arg);
					checkout.execute();
				} else if ("settings".equals(command)) {
					Settings settings = new Settings();
					settings.execute();
				} else if ("resolve".equals(command)) {
					ResolveConflict resolve = new ResolveConflict(null, arg);
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
