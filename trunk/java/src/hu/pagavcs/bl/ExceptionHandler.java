package hu.pagavcs.bl;

import hu.pagavcs.gui.platform.EditField;
import hu.pagavcs.gui.platform.MessagePane;
import hu.pagavcs.gui.platform.TextArea;

import java.awt.BorderLayout;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.tmatesoft.svn.core.SVNErrorCode;
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
public class ExceptionHandler implements java.lang.Thread.UncaughtExceptionHandler {

	public void uncaughtException(Thread t, Throwable ex) {
		handle(ex);
	}

	public void handle(Throwable ex) {

		if (Manager.isShutdown()) {
			return;
		}

		if (ex instanceof SVNException) {
			SVNErrorCode errorCode = ((SVNException) ex).getErrorMessage().getErrorCode();
			if (SVNErrorCode.UNVERSIONED_RESOURCE.equals(errorCode)) {
				MessagePane.showError(null, ex.getMessage(), "Error");
				return;
			}

			if (SVNErrorCode.WC_LOCKED.equals(errorCode)) {
				MessagePane.showError(null, "Working Copy Locked", ((SVNException) ex).getErrorMessage().getMessage());
				return;
			}
			System.out.println("SVNexception. category: " + errorCode.getCategory() + " code:" + errorCode.getCode() + " description:"
			        + errorCode.getDescription() + "\n");
		}

		if (ex instanceof PagaException) {
			PagaException pex = (PagaException) ex;
			switch (pex.getType()) {
				case LOGIN_FAILED:
					MessagePane.showError(null, "Login", "Login failed");
					return;
				case CONNECTION_ERROR:
					MessagePane.showError(null, "Connection Error", "Error in Communication");
					return;
				case UNIMPLEMENTED:
					MessagePane.showError(null, "Connection Error", "Unimplemented");
					return;
			}
		}

		ex.printStackTrace();

		EditField sfMessage = new EditField();
		sfMessage.setEditable(false);
		TextArea taStacktrace = new TextArea();
		taStacktrace.setEditable(false);

		sfMessage.setText(ex.getMessage());

		StringWriter writer = new StringWriter();
		ex.printStackTrace(new PrintWriter(writer));
		try {
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		taStacktrace.setText(writer.getBuffer().toString());

		JPanel pnlMain = new JPanel(new BorderLayout());
		pnlMain.add(sfMessage, BorderLayout.NORTH);
		pnlMain.add(new JScrollPane(taStacktrace), BorderLayout.CENTER);

		Manager.createAndShowFrame(pnlMain, "Exception occured");
	}
}
