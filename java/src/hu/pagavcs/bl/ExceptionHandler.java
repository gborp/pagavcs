package hu.pagavcs.bl;

import hu.pagavcs.gui.platform.EditField;
import hu.pagavcs.gui.platform.GuiHelper;
import hu.pagavcs.gui.platform.MessagePane;
import hu.pagavcs.gui.platform.TextArea;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

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
			SVNErrorMessage errorMessage = ((SVNException) ex).getErrorMessage();

			SVNErrorCode errorCode = errorMessage.getErrorCode();

			if (errorCode.isAuthentication()) {
				Manager.setForceShowingLoginDialogNextTime(true);
			}

			String errorString = errorMessage.getFullMessage();
			if (SVNErrorCode.UNVERSIONED_RESOURCE.equals(errorCode)) {
				MessagePane.showError(null, ex.getMessage(), "Error");
				return;
			} else if (SVNErrorCode.WC_LOCKED.equals(errorCode)) {
				MessagePane.showError(null, "Working Copy Locked", errorString);
				return;
			} else if (SVNErrorCode.ENTRY_NOT_FOUND.equals(errorCode)) {
				MessagePane.showError(null, "Not under version control", errorString);
				return;
			} else if (SVNErrorCode.WC_CORRUPT_TEXT_BASE.equals(errorCode)) {
				try {
					Object[] relObjects = errorMessage.getRelatedObjects();
					File file = (File) relObjects[0];

					File dir = file.getParentFile().getParentFile().getParentFile();
					String fileRealName = file.getName().substring(0, file.getName().length() - ".svn-base".length());

					SVNClientManager mgr = Manager.getSVNClientManager(dir);

					file.getParentFile().mkdirs();
					FileOutputStream out = new FileOutputStream(file);

					File realFile = new File(dir, fileRealName);

					SVNRevision revision = mgr.getWCClient().doInfo(realFile, SVNRevision.WORKING).getRevision();

					mgr.getWCClient().doGetFileContents(realFile, SVNRevision.HEAD, revision, false, out);
					out.close();

					MessagePane.showError(null, "Local-checksum check failed, please rerun your operation. \n(" + realFile.getPath() + ")",
					        "Local-checksum check failed, please rerun your operation. \n(" + realFile.getPath() + ")");

					return;

				} catch (Exception ex2) {
					Manager.handle(ex2);
				}
			}

			SVNErrorCode rootErrorCode = errorMessage.getRootErrorMessage().getErrorCode();

			if (rootErrorCode == SVNErrorCode.RA_DAV_REQUEST_FAILED) {
				MessagePane.showError(null, "Locking is not supperted by the host", errorString);
				return;
			} else if (rootErrorCode == SVNErrorCode.RA_NOT_AUTHORIZED) {
				MessagePane.showError(null, "Authorization failed", errorString);
				return;
			}

			System.out.println("SVNexception. category: " + errorCode.getCategory() + " code:" + errorCode.getCode() + " description:"
			        + errorCode.getDescription() + "\n");
		}

		if (ex instanceof PagaException) {
			PagaException pex = (PagaException) ex;
			switch (pex.getType()) {
				case LOGIN_FAILED:
					MessagePane.showError(null, "Login failed", "Login failed");
					return;
				case CONNECTION_ERROR:
					MessagePane.showError(null, "Connection Error", "Error in Communication");
					return;
				case UNIMPLEMENTED:
					MessagePane.showError(null, "Unimplemented", "Unimplemented");
					return;
				case INVALID_PARAMETERS:
					MessagePane.showError(null, "Invalid parameters", "Invalid parameters");
					return;
				case NOT_DIRECTORY:
					MessagePane.showError(null, "Not directory", "Not directory");
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

		JButton btnRestart = new JButton(new RestartPagaVcsAction());

		CellConstraints cc = new CellConstraints();
		FormLayout lyTop = new FormLayout("f:1dlu:g,2dlu,p", "p");
		JPanel pnlTop = new JPanel(lyTop);
		pnlTop.add(sfMessage, cc.xy(1, 1));
		pnlTop.add(btnRestart, cc.xy(3, 1));

		FormLayout lyMain = new FormLayout("f:20dlu:g", "p,2dlu,p");
		JPanel pnlMain = new JPanel(lyMain);
		pnlMain.add(pnlTop, cc.xy(1, 1));
		pnlMain.add(new JScrollPane(taStacktrace), cc.xy(1, 3));

		GuiHelper.createAndShowFrame(pnlMain, "Exception occured");
	}

	private class RestartPagaVcsAction extends ThreadAction {

		public RestartPagaVcsAction() {
			super("Restart PagaVCS");
			setTooltip("Press refresh in nautilus after pressing this button");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			System.exit(0);
		}
	}
}
