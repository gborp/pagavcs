package hu.pagavcs.gui;

import hu.pagavcs.bl.Cancelable;
import hu.pagavcs.bl.Manager;
import hu.pagavcs.bl.PagaException;
import hu.pagavcs.bl.ThreadAction;
import hu.pagavcs.bl.PagaException.PagaExceptionType;
import hu.pagavcs.gui.platform.EditField;
import hu.pagavcs.gui.platform.GuiHelper;
import hu.pagavcs.gui.platform.Label;
import hu.pagavcs.gui.platform.ProgressBar;
import hu.pagavcs.operation.Other;
import hu.pagavcs.operation.Other.OtherStatus;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
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
public class OtherGui implements Working, Cancelable {

	private Other       other;
	private JLabel      lblStatus;
	private Window      window;
	private EditField   sfRepo;
	private EditField   sfNewPath;
	private JCheckBox   cbMove;
	private EditField   sfWorkingCopy;
	private EditField   sfUrlToMergeFrom;
	private JCheckBox   cbReverseMerge;
	private JButton     btnShowLogFrom;
	private EditField   sfRevisionRange;
	private JButton     btnMergeRevisions;
	private JButton     btnShowLog;
	private JButton     btnSwitch;
	private EditField   sfSwitchToUrl;
	private EditField   sfSwitchToRevision;
	private ProgressBar prgBusy;
	private EditField   sfBlameRevision;
	private JButton     btnBlame;
	private EditField   sfExportTo;
	private JButton     btnExportTo;
	private JButton     btnRepoBrowser;
	private JButton     btnUpdateToRevision;
	private EditField   sfUpdateTo;
	private JButton     btnApplyPatch;

	public OtherGui(Other other) {
		this.other = other;
	}

	public void display() throws SVNException {
		FormLayout layout = new FormLayout("right:p, 4dlu,p:g, p",
		        "p,4dlu,p,4dlu,p,10dlu,p,4dlu,p,10dlu,p,4dlu,p,4dlu,p,4dlu,p,10dlu,p,4dlu,p,4dlu,p,10dlu,p,4dlu,p,10dlu,p,4dlu,p,10dlu,p,4dlu,p,10dlu,p,4dlu,p,10dlu,p");
		JPanel pnlMain = new JPanel(layout);
		CellConstraints cc = new CellConstraints();

		Label lblWorkingCopy = new Label("Path:");
		sfWorkingCopy = new EditField(other.getPath());
		sfWorkingCopy.setEditable(false);
		Label lblRepo = new Label("Repository:");
		sfRepo = new EditField();
		sfRepo.setEditable(false);
		btnShowLog = new JButton(new ShowLogAction());

		sfNewPath = new EditField(other.getPath());
		cbMove = new JCheckBox("Copy");
		JButton btnCopyMoveRename = new JButton("Copy/move/rename");
		btnCopyMoveRename.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable() {

					public void run() {
						try {
							copyMoveRename();
						} catch (Exception e) {
							Manager.handle(e);
						}
					}
				}).start();
			}
		});
		sfUrlToMergeFrom = new EditField();
		sfRevisionRange = new EditField();
		sfRevisionRange.setToolTipText("<html>Example: 4-7,9,11,15-HEAD<br>To merge all revisions, leave the box empty.</html>");
		cbReverseMerge = new JCheckBox("Reverse merge");
		btnShowLogFrom = new JButton(new ShowLogMergeFromAction());
		btnMergeRevisions = new JButton(new MergeAction());

		sfSwitchToUrl = new EditField();
		sfSwitchToRevision = new EditField();
		btnSwitch = new JButton(new SwitchAction());

		sfBlameRevision = new EditField();
		sfBlameRevision.setToolTipText("Leave empty for HEAD revision");
		btnBlame = new JButton(new BlameAction());

		sfExportTo = new EditField();
		btnExportTo = new JButton(new ExportAction());

		sfUpdateTo = new EditField();
		btnUpdateToRevision = new JButton(new UpdateToRevisionAction());

		btnRepoBrowser = new JButton(new RepoBrowserAction());

		btnApplyPatch = new JButton(new ApplyPatchAction());

		lblStatus = new Label(" ");
		prgBusy = new ProgressBar(this);

		pnlMain.add(lblWorkingCopy, cc.xywh(1, 1, 1, 1));
		pnlMain.add(sfWorkingCopy, cc.xywh(3, 1, 2, 1));
		pnlMain.add(lblRepo, cc.xywh(1, 3, 1, 1));
		pnlMain.add(sfRepo, cc.xywh(3, 3, 2, 1));
		pnlMain.add(btnShowLog, cc.xywh(4, 5, 1, 1));

		pnlMain.add(new JSeparator(), cc.xywh(1, 6, 4, 1));
		pnlMain.add(new JLabel("Copy/move/rename"), cc.xywh(1, 7, 1, 1));
		pnlMain.add(sfNewPath, cc.xywh(3, 7, 2, 1));
		pnlMain.add(cbMove, cc.xywh(3, 9, 1, 1));
		pnlMain.add(btnCopyMoveRename, cc.xywh(4, 9, 1, 1));

		pnlMain.add(new JSeparator(), cc.xywh(1, 10, 4, 1));
		pnlMain.add(new JLabel("Url to merge from:"), cc.xywh(1, 11, 1, 1));
		pnlMain.add(sfUrlToMergeFrom, cc.xywh(3, 11, 2, 1));
		pnlMain.add(new JLabel("Revision range to merge:"), cc.xywh(1, 13, 1, 1));
		pnlMain.add(sfRevisionRange, cc.xywh(3, 13, 2, 1));
		pnlMain.add(cbReverseMerge, cc.xywh(3, 15, 1, 1));
		pnlMain.add(btnShowLogFrom, cc.xywh(4, 15, 1, 1));
		pnlMain.add(btnMergeRevisions, cc.xywh(4, 17, 1, 1));

		pnlMain.add(new JSeparator(), cc.xywh(1, 18, 4, 1));
		pnlMain.add(new JLabel("To url:"), cc.xywh(1, 19, 1, 1));
		pnlMain.add(sfSwitchToUrl, cc.xywh(3, 19, 2, 1));
		pnlMain.add(new JLabel("Revison:"), cc.xywh(1, 21, 1, 1));
		pnlMain.add(sfSwitchToRevision, cc.xywh(3, 21, 2, 1));
		pnlMain.add(btnSwitch, cc.xywh(4, 23, 1, 1));

		pnlMain.add(new JSeparator(), cc.xywh(1, 24, 4, 1));
		pnlMain.add(new JLabel("Revison:"), cc.xywh(1, 25, 1, 1));
		pnlMain.add(sfBlameRevision, cc.xywh(3, 25, 2, 1));
		pnlMain.add(btnBlame, cc.xywh(4, 27, 1, 1));

		pnlMain.add(new JSeparator(), cc.xywh(1, 28, 4, 1));
		pnlMain.add(new JLabel("Export to:"), cc.xywh(1, 29, 1, 1));
		pnlMain.add(sfExportTo, cc.xywh(3, 29, 2, 1));
		pnlMain.add(btnExportTo, cc.xywh(4, 31, 1, 1));

		pnlMain.add(new JSeparator(), cc.xywh(1, 32, 4, 1));
		pnlMain.add(new JLabel("Update to revision:"), cc.xywh(1, 33, 1, 1));
		pnlMain.add(sfUpdateTo, cc.xywh(3, 33, 2, 1));
		pnlMain.add(btnUpdateToRevision, cc.xywh(4, 35, 1, 1));

		pnlMain.add(new JSeparator(), cc.xywh(1, 36, 4, 1));
		pnlMain.add(btnRepoBrowser, cc.xywh(4, 37, 1, 1));
		pnlMain.add(btnApplyPatch, cc.xywh(4, 39, 1, 1));

		pnlMain.add(new JSeparator(), cc.xywh(1, 40, 4, 1));
		pnlMain.add(prgBusy, cc.xywh(1, 41, 3, 1));
		pnlMain.add(lblStatus, cc.xywh(4, 41, 1, 1));

		window = GuiHelper.createAndShowFrame(pnlMain, "Other");
	}

	public void setStatus(OtherStatus status) {
		lblStatus.setText("Status: " + status.toString());
	}

	public void close() {
		window.setVisible(false);
		window.dispose();
	}

	public void setURL(String text) {
		sfRepo.setText(text);
	}

	private void copyMoveRename() throws Exception {
		try {
			prgBusy.startProgress();
			other.copyMoveRename(sfWorkingCopy.getText(), sfNewPath.getText().trim(), cbMove.isSelected());
		} finally {
			prgBusy.stopProgress();
		}
	}

	private void doMerge() throws Exception {
		try {
			prgBusy.startProgress();
			other.doMerge(sfRepo.getText(), sfWorkingCopy.getText(), sfUrlToMergeFrom.getText().trim(), sfRevisionRange.getText().trim(), cbReverseMerge
			        .isSelected());
		} finally {
			prgBusy.stopProgress();
		}
	}

	private void doSwitch() throws Exception {
		try {
			prgBusy.startProgress();
			other.doSwitch(sfWorkingCopy.getText(), sfSwitchToUrl.getText().trim(), sfSwitchToRevision.getText().trim());
		} finally {
			prgBusy.stopProgress();
		}
	}

	private void doBlame() throws Exception {
		try {
			prgBusy.startProgress();
			List<BlameListItem> lstBlame = other.doBlame(sfWorkingCopy.getText(), sfBlameRevision.getText().trim());
			BlameGui blameGUi = new BlameGui(this, sfWorkingCopy.getText());
			blameGUi.setBlamedFile(lstBlame);
			blameGUi.display();
		} finally {
			prgBusy.stopProgress();
		}
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

	// TODO doApplyPatch
	private void doApplyPatch() throws Exception {
		if (true) {
			throw new PagaException(PagaExceptionType.UNIMPLEMENTED);
		}
		JFileChooser fc = new JFileChooser(new File(other.getPath()));
		fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		int choosed = fc.showSaveDialog(window);
		if (choosed == JFileChooser.APPROVE_OPTION) {
			File file = fc.getSelectedFile();

			String result = Manager.getOsCommandResult("lsdiff", file.getPath());
			// display file names, select from files

			List<String> lstFilesToPatch = new ArrayList<String>();
			for (String filename : result.split("\n")) {
				lstFilesToPatch.add(filename);
			}

		}
	}

	public void workStarted() {
		setStatus(OtherStatus.WORKING);
	}

	public void workEnded() {
		setStatus(OtherStatus.COMPLETED);
	}

	private class ApplyPatchAction extends ThreadAction {

		public ApplyPatchAction() {
			super("Apply patch");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			doApplyPatch();
		}

	}

	private class MergeAction extends ThreadAction {

		public MergeAction() {
			super("Merge revisions");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			doMerge();
		}

	}

	private class SwitchAction extends ThreadAction {

		public SwitchAction() {
			super("Switch");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			doSwitch();
		}

	}

	private class BlameAction extends ThreadAction {

		public BlameAction() {
			super("Blame");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			doBlame();
		}

	}

	private class ExportAction extends ThreadAction {

		public ExportAction() {
			super("Export");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			doExport();
		}

	}

	private class RepoBrowserAction extends ThreadAction {

		public RepoBrowserAction() {
			super("Repo Browser");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			other.doRepoBrowser(sfWorkingCopy.getText());
		}
	}

	private class UpdateToRevisionAction extends ThreadAction {

		public UpdateToRevisionAction() {
			super("Update to");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			other.doUpdateToRevision(sfWorkingCopy.getText(), sfUpdateTo.getText().trim());
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

	private class ShowLogMergeFromAction extends ThreadAction {

		public ShowLogMergeFromAction() {
			super("Show log (from)");
			setEnabled(false);
		}

		public void actionProcess(ActionEvent e) throws Exception {
			String url = sfUrlToMergeFrom.getText().trim();
			// TODO ShowLogMergeFromAction
		}

	}

	public boolean isCancel() {
		return other.isCancel();
	}

	public void setCancel(boolean cancel) throws Exception {
		other.setCancel(true);
	}

}
