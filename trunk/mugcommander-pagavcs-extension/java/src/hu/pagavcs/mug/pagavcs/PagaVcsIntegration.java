package hu.pagavcs.mug.pagavcs;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.net.UnknownHostException;
import java.util.HashMap;

import javax.swing.ImageIcon;

import cx.ath.matthew.unix.UnixSocket;

public class PagaVcsIntegration {

	private static final String UNIX_SOCKET = System.getProperty("user.home")
			+ "/.pagavcs/socket";
	private static final String PAGAVCS_SERVER_START_COMMAND = "/usr/bin/pagavcs ping";

	private static HashMap<String, ImageIcon> mapPagaVcsIcon;

	public static UnixSocket getSocket() throws IOException {
		try {
			UnixSocket socket = new UnixSocket(UNIX_SOCKET);
			// timeout: 10 sec
			socket.setSoTimeout(10 * 1000);
			return socket;
		} catch (Exception ex) {
			return createAndGetSocket();
		}
	}

	private static UnixSocket createAndGetSocket() throws IOException {
		Runtime.getRuntime().exec(PAGAVCS_SERVER_START_COMMAND);
		try {
			Thread.sleep(500);
		} catch (InterruptedException ex) {
		}
		return new UnixSocket(UNIX_SOCKET);
	}

	public synchronized static ImageIcon getIcon(String name)
			throws UnknownHostException, IOException, ClassNotFoundException {
		initPagaVcsIcon();
		return mapPagaVcsIcon.get(name);
	}

	private static void initOnePagaVcsIcon(String name, int width, int height)
			throws UnknownHostException, IOException, ClassNotFoundException {
		String strOut = "getemblem " + name + " " + width + " " + height + "\n";

		UnixSocket socket = PagaVcsIntegration.getSocket();

		BufferedWriter outToClient = new BufferedWriter(new OutputStreamWriter(
				socket.getOutputStream()));
		outToClient.write(strOut);
		outToClient.flush();
		ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
		ImageIcon imageIcon = (ImageIcon) input.readObject();

		mapPagaVcsIcon.put(name, imageIcon);
	}

	private static void initPagaVcsIcon() throws UnknownHostException,
			IOException, ClassNotFoundException {
		if (mapPagaVcsIcon == null) {
			mapPagaVcsIcon = new HashMap<String, ImageIcon>();

			int emblemWidth = 8;
			int emblemHeight = 8;
			initOnePagaVcsIcon("emblems/added", emblemWidth, emblemHeight);
			initOnePagaVcsIcon("emblems/conflict", emblemWidth, emblemHeight);
			initOnePagaVcsIcon("emblems/deleted", emblemWidth, emblemHeight);
			initOnePagaVcsIcon("emblems/locked", emblemWidth, emblemHeight);
			initOnePagaVcsIcon("emblems/modified", emblemWidth, emblemHeight);
			initOnePagaVcsIcon("emblems/normal", emblemWidth, emblemHeight);
			initOnePagaVcsIcon("emblems/obstructed", emblemWidth, emblemHeight);
			initOnePagaVcsIcon("emblems/readonly", emblemWidth, emblemHeight);
			initOnePagaVcsIcon("emblems/svn", emblemWidth, emblemHeight);
			initOnePagaVcsIcon("emblems/unversioned", emblemWidth, emblemHeight);

			int actionWidth = 16;
			int actionHeight = 16;
			initOnePagaVcsIcon("actions/pagavcs-about", actionWidth,
					actionHeight);
			initOnePagaVcsIcon("actions/pagavcs-add", actionWidth, actionHeight);
			initOnePagaVcsIcon("actions/pagavcs-annotate", actionWidth,
					actionHeight);
			initOnePagaVcsIcon("actions/pagavcs-applypatch", actionWidth,
					actionHeight);
			initOnePagaVcsIcon("actions/pagavcs-asynchronous", actionWidth,
					actionHeight);
			initOnePagaVcsIcon("actions/pagavcs-branch", actionWidth,
					actionHeight);
			initOnePagaVcsIcon("actions/pagavcs-bug", actionWidth, actionHeight);
			initOnePagaVcsIcon("actions/pagavcs-checkout", actionWidth,
					actionHeight);
			initOnePagaVcsIcon("actions/pagavcs-cleanup", actionWidth,
					actionHeight);
			initOnePagaVcsIcon("actions/pagavcs-clear", actionWidth,
					actionHeight);
			initOnePagaVcsIcon("actions/pagavcs-commit", actionWidth,
					actionHeight);
			initOnePagaVcsIcon("actions/pagavcs-createpatch", actionWidth,
					actionHeight);
			initOnePagaVcsIcon("actions/pagavcs-dbus", actionWidth,
					actionHeight);
			initOnePagaVcsIcon("actions/pagavcs-delete", actionWidth,
					actionHeight);
			initOnePagaVcsIcon("actions/pagavcs-difficon", actionWidth,
					actionHeight);
			initOnePagaVcsIcon("actions/pagavcs-drive", actionWidth,
					actionHeight);
			initOnePagaVcsIcon("actions/pagavcs-emblems", actionWidth,
					actionHeight);
			initOnePagaVcsIcon("actions/pagavcs-export", actionWidth,
					actionHeight);
			initOnePagaVcsIcon("actions/pagavcs-help", actionWidth,
					actionHeight);
			initOnePagaVcsIcon("actions/pagavcs-ignore", actionWidth,
					actionHeight);
			initOnePagaVcsIcon("actions/pagavcs-import", actionWidth,
					actionHeight);
			initOnePagaVcsIcon("actions/pagavcs-lock", actionWidth,
					actionHeight);
			initOnePagaVcsIcon("actions/pagavcs-logo", actionWidth,
					actionHeight);
			initOnePagaVcsIcon("actions/pagavcs-log", actionWidth, actionHeight);
			initOnePagaVcsIcon("actions/pagavcs-merge", actionWidth,
					actionHeight);
			initOnePagaVcsIcon("actions/pagavcs-other", actionWidth,
					actionHeight);
			initOnePagaVcsIcon("actions/pagavcs-properties", actionWidth,
					actionHeight);
			initOnePagaVcsIcon("actions/pagavcs-refresh", actionWidth,
					actionHeight);
			initOnePagaVcsIcon("actions/pagavcs-relocate", actionWidth,
					actionHeight);
			initOnePagaVcsIcon("actions/pagavcs-rename", actionWidth,
					actionHeight);
			initOnePagaVcsIcon("actions/pagavcs-resolve", actionWidth,
					actionHeight);
			initOnePagaVcsIcon("actions/pagavcs-revert", actionWidth,
					actionHeight);
			initOnePagaVcsIcon("actions/pagavcs-run", actionWidth, actionHeight);
			initOnePagaVcsIcon("actions/pagavcs-settings", actionWidth,
					actionHeight);
			initOnePagaVcsIcon("actions/pagavcs-show_log", actionWidth,
					actionHeight);
			initOnePagaVcsIcon("actions/pagavcs-stop", actionWidth,
					actionHeight);
			initOnePagaVcsIcon("actions/pagavcs-switch", actionWidth,
					actionHeight);
			initOnePagaVcsIcon("actions/pagavcs-unignore", actionWidth,
					actionHeight);
			initOnePagaVcsIcon("actions/pagavcs-unlock", actionWidth,
					actionHeight);
			initOnePagaVcsIcon("actions/pagavcs-update", actionWidth,
					actionHeight);
		}
	}
}
