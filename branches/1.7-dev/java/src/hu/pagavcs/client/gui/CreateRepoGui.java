package hu.pagavcs.client.gui;

import hu.pagavcs.client.gui.platform.Frame;
import hu.pagavcs.client.gui.platform.GuiHelper;
import hu.pagavcs.client.gui.platform.Label;
import hu.pagavcs.client.gui.platform.action.CloseAction;
import hu.pagavcs.client.operation.CreateRepo;
import hu.pagavcs.client.operation.CreateRepo.CreateRepoStatus;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.tmatesoft.svn.core.SVNException;

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
public class CreateRepoGui {

	private CreateRepo createRepo;
	private JLabel     lblStatus;
	private Frame      frame;

	public CreateRepoGui(CreateRepo createRepo) {
		this.createRepo = createRepo;
	}

	public void display() throws SVNException {

		JPanel pnlMain = new JPanel(new FormLayout("r:p,2dlu,p:g", "p,2dlu:g,p,2dlu,p"));
		CellConstraints cc = new CellConstraints();
		frame = GuiHelper.createFrame(pnlMain, "Create Repository", null);

		Label lblWorkingCopy = new Label("Path:");
		Label sfWorkingCopy = new Label(createRepo.getPath());
		JButton btnClose = new JButton(new CloseAction(frame));
		lblStatus = new Label(" ");

		pnlMain.add(lblWorkingCopy, cc.xy(1, 1));
		pnlMain.add(sfWorkingCopy, cc.xy(3, 1));
		pnlMain.add(btnClose, cc.xywh(1, 3, 3, 1, CellConstraints.CENTER, CellConstraints.DEFAULT));
		pnlMain.add(lblStatus, cc.xywh(1, 5, 3, 1, CellConstraints.LEFT, CellConstraints.DEFAULT));

		frame.execute();
	}

	public void setStatus(CreateRepoStatus status) {
		if (lblStatus != null) {
			lblStatus.setText("Status: " + status.toString());
		}
	}

	public void close() {
		frame.setVisible(false);
		frame.dispose();
	}

}
