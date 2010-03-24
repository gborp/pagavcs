package hu.pagavcs.mug.pagavcs;

import java.io.IOException;
import java.net.Socket;

public class PagaVcsIntegration {

	private static final String PAGAVCS_SERVER_START_COMMAND = "/usr/bin/pagavcs ping";
	private static final String HOST                         = "localhost";
	private static final int    PORT                         = 12905;

	public static Socket getSocket() throws IOException {
		try {
			Socket socket = new Socket(HOST, PORT);
			// timeout: 10 sec
			socket.setSoTimeout(10 * 1000);
			return socket;
		} catch (Exception ex) {
			return createAndGetSocket();
		}
	}

	private static Socket createAndGetSocket() throws IOException {
		Runtime.getRuntime().exec(PAGAVCS_SERVER_START_COMMAND);
		try {
			Thread.sleep(500);
		} catch (InterruptedException ex) {}
		return new Socket(HOST, PORT);
	}

}
