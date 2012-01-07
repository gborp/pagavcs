package hu.pagavcs.client.gui;

import hu.pagavcs.client.bl.Cancelable;
import hu.pagavcs.client.bl.Manager;
import hu.pagavcs.client.bl.ThreadAction;
import hu.pagavcs.client.gui.platform.EditField;
import hu.pagavcs.client.gui.platform.Frame;
import hu.pagavcs.client.gui.platform.GuiHelper;
import hu.pagavcs.client.gui.platform.Label;
import hu.pagavcs.client.gui.platform.MessagePane;
import hu.pagavcs.client.gui.platform.ProgressBar;
import hu.pagavcs.client.gui.platform.MessagePane.OPTIONS;
import hu.pagavcs.client.operation.ExportOperation;
import hu.pagavcs.client.operation.GeneralStatus;

import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
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
public class ExportGui implements Working, Cancelable {

	private ExportOperation other;
	private JLabel          lblStatus;
	private Frame           frame;
	private EditField       sfRepo;
	private EditField       sfWorkingCopy;
	private JButton         btnShowLog;
	private ProgressBar     prgBusy;
	private EditField       sfExportTo;
	private JButton         btnExportTo;

	public ExportGui(ExportOperation other) {
		this.other = other;
	}

	public void display() throws SVNException {
		FormLayout layout = new FormLayout("right:p, 2dlu,p:g, p", "p,2dlu,p,2dlu,p,4dlu,p,2dlu,p,4dlu,p,2dlu,p,4dlu,p,2dlu,p,4dlu,p,4dlu,p");
		JPanel pnlMain = new JPanel(layout);
		CellConstraints cc = new CellConstraints();

		Label lblWorkingCopy = new Label("Path:");
		sfWorkingCopy = new EditField(other.getPath());
		sfWorkingCopy.setEditable(false);
		Label lblRepo = new Label("Repository:");
		sfRepo = new EditField();
		sfRepo.setEditable(false);
		btnShowLog = new JButton(new ShowLogAction());

		sfExportTo = new EditField();
		btnExportTo = new JButton(new ExportAction());

		lblStatus = new Label(" ");
		prgBusy = new ProgressBar(this);

		pnlMain.add(lblWorkingCopy, cc.xywh(1, 1, 1, 1));
		pnlMain.add(sfWorkingCopy, cc.xywh(3, 1, 2, 1));
		pnlMain.add(lblRepo, cc.xywh(1, 3, 1, 1));
		pnlMain.add(sfRepo, cc.xywh(3, 3, 2, 1));
		pnlMain.add(btnShowLog, cc.xywh(4, 5, 1, 1));

		pnlMain.add(new JSeparator(), cc.xywh(1, 6, 4, 1));
		pnlMain.add(new JLabel("Export to:"), cc.xywh(1, 7, 1, 1));
		pnlMain.add(sfExportTo, cc.xywh(3, 7, 2, 1));
		pnlMain.add(btnExportTo, cc.xywh(4, 9, 1, 1));

		pnlMain.add(new JSeparator(), cc.xywh(1, 10, 4, 1));
		pnlMain.add(prgBusy, cc.xywh(1, 11, 3, 1));
		pnlMain.add(lblStatus, cc.xywh(4, 11, 1, 1));

		frame = GuiHelper.createAndShowFrame(pnlMain, "Export", "/hu/pagavcs/resources/actions/pagavcs-export.png");
		frame.setTitlePrefix(other.getPath());
	}

	public void setStatus(GeneralStatus status) {
		lblStatus.setText("Status: " + status.toString());
	}

	public void close() {
		frame.setVisible(false);
		frame.dispose();
	}

	public void setURL(String text) {
		sfRepo.setText(text);
	}

	private void doExport() throws Exception {
		try {
			if (sfExportTo.getText().trim().isEmpty()) {
				JOptionPane.showMessageDialog(Manager.getRootFrame(), "Directory cannot be empty", "Error", JOptionPane.ERROR_MESSAGE);
				return;
			}

			prgBusy.startProgress();
			other.doExport(sfWorkingCopy.getText(), sfExportTo.getText().trim());
		} finally {
			prgBusy.stopProgress();
		}
	}

	public void workStarted() {
		setStatus(GeneralStatus.WORKING);
	}

	public void workEnded() {
		setStatus(GeneralStatus.COMPLETED);
	}

	private class ExportAction extends ThreadAction {

		public ExportAction() {
			super("Export");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			doExport();
		}

	}

	private class ShowLogAction extends ThreadAction {

		public ShowLogAction() {
			super("Show log");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			other.doShowLog(sfWorkingCopy.getText());
		}

	}

	public boolean isCancel() {
		return other.isCancel();
	}

	public void setCancel(boolean cancel) throws Exception {
		other.setCancel(true);
	}

	public boolean exportPathExistsOverride(String filePathExport) {
		OPTIONS choice = MessagePane.showWarning(frame, "Directory exists", "Directory " + filePathExport
		        + " is already exist,\ndo you really want to export there?");
		return OPTIONS.OK.equals(choice);
	}

}
