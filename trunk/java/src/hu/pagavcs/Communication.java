package hu.pagavcs;

import hu.pagavcs.bl.Manager;
import hu.pagavcs.operation.Checkout;
import hu.pagavcs.operation.Cleanup;
import hu.pagavcs.operation.Commit;
import hu.pagavcs.operation.Delete;
import hu.pagavcs.operation.Ignore;
import hu.pagavcs.operation.Log;
import hu.pagavcs.operation.Other;
import hu.pagavcs.operation.Revert;
import hu.pagavcs.operation.Settings;
import hu.pagavcs.operation.Unignore;
import hu.pagavcs.operation.Update;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
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

	private static final int    PORT                          = 12905;
	private static final String SERVER_RUNNING_INDICATOR_FILE = "server-running-indicator";
	private boolean             shutdown;

	public Communication() throws Exception {

		Manager.init();

		String tempDir = Manager.getTempDir();
		File running = new File(tempDir + SERVER_RUNNING_INDICATOR_FILE);
		running.createNewFile();
		running.deleteOnExit();

		ServerSocket serverSocket = ServerSocketFactory.getDefault().createServerSocket(PORT);

		while (!shutdown) {
			try {
				Socket socket = serverSocket.accept();

				BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

				String line = br.readLine();
				new Thread(new ProcessInput(line), line).start();

			} catch (Exception ex) {
				Manager.handle(ex);
			}
		}
	}

	private static class ProcessInput implements Runnable {

		private String line;

		public ProcessInput(String line) {
			this.line = line;
		}

		public void run() {
			try {
				int commandEndIndex = line.indexOf(' ');
				String command = line.substring(0, commandEndIndex > -1 ? commandEndIndex : line.length());

				String arg = null;
				if (commandEndIndex > -1) {
					arg = line.substring(commandEndIndex + 1);
				}

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
				} else if ("stop".equals(command)) {
					System.exit(0);
				} else {
					throw new RuntimeException("unimplemented command");
				}
			} catch (Exception ex) {
				Manager.handle(ex);
			}
		}
	}

}
