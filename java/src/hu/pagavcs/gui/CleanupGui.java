package hu.pagavcs.gui;

import hu.pagavcs.bl.Manager;
import hu.pagavcs.gui.platform.Label;
import hu.pagavcs.operation.Cleanup;
import hu.pagavcs.operation.Cleanup.CleanupStatus;

import java.awt.BorderLayout;
import java.awt.Window;

import javax.swing.JLabel;
import javax.swing.JPanel;

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
public class CleanupGui {

	private Cleanup cleanup;
	private JLabel  lblStatus;
	private Window  window;

	public CleanupGui(Cleanup cleanup) {
		this.cleanup = cleanup;
	}

	public void display() throws SVNException {

		JPanel pnlMain = new JPanel(new BorderLayout());
		Label lblWorkingCopy = new Label("Path: " + cleanup.getPath());
		lblStatus = new Label(" ");
		pnlMain.add(lblWorkingCopy, BorderLayout.NORTH);
		pnlMain.add(lblStatus, BorderLayout.SOUTH);

		window = Manager.createAndShowFrame(pnlMain, "Cleanup");
	}

	public void setStatus(CleanupStatus status) {
		if (lblStatus != null) {
			lblStatus.setText("Status: " + status.toString());
		}
	}

	public void close() {
		window.setVisible(false);
		window.dispose();
	}

}
