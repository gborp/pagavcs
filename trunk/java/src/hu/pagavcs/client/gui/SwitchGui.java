package hu.pagavcs.client.gui;

import hu.pagavcs.client.bl.Cancelable;
import hu.pagavcs.client.bl.ThreadAction;
import hu.pagavcs.client.gui.platform.EditField;
import hu.pagavcs.client.gui.platform.Frame;
import hu.pagavcs.client.gui.platform.GuiHelper;
import hu.pagavcs.client.gui.platform.Label;
import hu.pagavcs.client.gui.platform.ProgressBar;
import hu.pagavcs.client.operation.SwitchOperation;
import hu.pagavcs.client.operation.SwitchOperation.SwitchStatus;

import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

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
public class SwitchGui implements Working, Cancelable {

	private SwitchOperation switchOp;
	private JLabel          lblStatus;
	private Frame           frame;
	private EditField       sfRepo;
	private EditField       sfWorkingCopy;
	private JButton         btnShowLog;
	private JButton         btnSwitch;
	private EditField       sfSwitchToUrl;
	private EditField       sfSwitchToRevision;
	private ProgressBar     prgBusy;

	public SwitchGui(SwitchOperation other) {
		this.switchOp = other;
	}

	public void display() throws SVNException {
		FormLayout layout = new FormLayout("right:p, 2dlu,p:g, p", "p,2dlu,p,2dlu,p,4dlu,p,2dlu,p,2dlu,p,4dlu,p");
		JPanel pnlMain = new JPanel(layout);
		CellConstraints cc = new CellConstraints();

		Label lblWorkingCopy = new Label("Path:");
		sfWorkingCopy = new EditField(switchOp.getPath());
		sfWorkingCopy.setEditable(false);
		Label lblRepo = new Label("Repository:");
		sfRepo = new EditField();
		sfRepo.setEditable(false);
		btnShowLog = new JButton(new ShowLogAction());

		sfSwitchToUrl = new EditField();
		sfSwitchToRevision = new EditField();
		btnSwitch = new JButton(new SwitchAction());

		lblStatus = new Label(" ");
		prgBusy = new ProgressBar(this);

		pnlMain.add(lblWorkingCopy, cc.xywh(1, 1, 1, 1));
		pnlMain.add(sfWorkingCopy, cc.xywh(3, 1, 2, 1));
		pnlMain.add(lblRepo, cc.xywh(1, 3, 1, 1));
		pnlMain.add(sfRepo, cc.xywh(3, 3, 2, 1));
		pnlMain.add(btnShowLog, cc.xywh(4, 5, 1, 1));

		pnlMain.add(new JSeparator(), cc.xywh(1, 6, 4, 1));
		pnlMain.add(new JLabel("To url:"), cc.xywh(1, 7, 1, 1));
		pnlMain.add(sfSwitchToUrl, cc.xywh(3, 7, 2, 1));
		pnlMain.add(new JLabel("Revison:"), cc.xywh(1, 9, 1, 1));
		pnlMain.add(sfSwitchToRevision, cc.xywh(3, 9, 2, 1));
		pnlMain.add(btnSwitch, cc.xywh(4, 11, 1, 1));

		pnlMain.add(new JSeparator(), cc.xywh(1, 12, 4, 1));
		pnlMain.add(prgBusy, cc.xywh(1, 13, 3, 1));
		pnlMain.add(lblStatus, cc.xywh(4, 13, 1, 1));

		frame = GuiHelper.createAndShowFrame(pnlMain, "Switch", "/hu/pagavcs/resources/switch-app-icon.png");
		frame.setTitlePrefix(switchOp.getPath());
	}

	public void setStatus(SwitchStatus status) {
		lblStatus.setText("Status: " + status.toString());
	}

	public void close() {
		frame.setVisible(false);
		frame.dispose();
	}

	public void setURL(String text) {
		sfRepo.setText(text);
	}

	private void doSwitch() throws Exception {
		try {
			prgBusy.startProgress();
			switchOp.doSwitch(sfWorkingCopy.getText(), sfSwitchToUrl.getText().trim(), sfSwitchToRevision.getText().trim());
		} finally {
			prgBusy.stopProgress();
		}
	}

	public void workStarted() {
		setStatus(SwitchStatus.WORKING);
	}

	public void workEnded() {
		setStatus(SwitchStatus.COMPLETED);
	}

	private class SwitchAction extends ThreadAction {

		public SwitchAction() {
			super("Switch");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			doSwitch();
		}

	}

	private class ShowLogAction extends ThreadAction {

		public ShowLogAction() {
			super("Show log");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			switchOp.doShowLog(sfWorkingCopy.getText());
		}

	}

	public boolean isCancel() {
		return switchOp.isCancel();
	}

	public void setCancel(boolean cancel) throws Exception {
		switchOp.setCancel(true);
	}

}
