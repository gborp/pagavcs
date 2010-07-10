package hu.pagavcs.gui;

import hu.pagavcs.bl.Cancelable;
import hu.pagavcs.bl.Manager;
import hu.pagavcs.bl.ThreadAction;
import hu.pagavcs.gui.platform.EditField;
import hu.pagavcs.gui.platform.Frame;
import hu.pagavcs.gui.platform.GuiHelper;
import hu.pagavcs.gui.platform.Label;
import hu.pagavcs.gui.platform.MessagePane;
import hu.pagavcs.gui.platform.ProgressBar;
import hu.pagavcs.gui.platform.MessagePane.OPTIONS;
import hu.pagavcs.operation.Other;
import hu.pagavcs.operation.ResolveConflict;
import hu.pagavcs.operation.Other.OtherStatus;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
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
	private Frame       frame;
	private EditField   sfRepo;
	private EditField   sfWorkingCopy;
	private JButton     btnShowLog;
	private ProgressBar prgBusy;
	private EditField   sfBlameRevision;
	private JButton     btnBlame;
	private EditField   sfExportTo;
	private JButton     btnExportTo;
	private JButton     btnUpdateToRevision;
	private EditField   sfUpdateTo;
	private JButton     btnApplyPatch;

	public OtherGui(Other other) {
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

		sfBlameRevision = new EditField();
		sfBlameRevision.setToolTipText("Leave empty for HEAD revision");
		btnBlame = new JButton(new BlameAction());

		sfExportTo = new EditField();
		btnExportTo = new JButton(new ExportAction());

		sfUpdateTo = new EditField();
		btnUpdateToRevision = new JButton(new UpdateToRevisionAction());

		btnApplyPatch = new JButton(new ApplyPatchAction());

		lblStatus = new Label(" ");
		prgBusy = new ProgressBar(this);

		pnlMain.add(lblWorkingCopy, cc.xywh(1, 1, 1, 1));
		pnlMain.add(sfWorkingCopy, cc.xywh(3, 1, 2, 1));
		pnlMain.add(lblRepo, cc.xywh(1, 3, 1, 1));
		pnlMain.add(sfRepo, cc.xywh(3, 3, 2, 1));
		pnlMain.add(btnShowLog, cc.xywh(4, 5, 1, 1));

		pnlMain.add(new JSeparator(), cc.xywh(1, 6, 4, 1));
		pnlMain.add(new JLabel("Revison:"), cc.xywh(1, 7, 1, 1));
		pnlMain.add(sfBlameRevision, cc.xywh(3, 7, 2, 1));
		pnlMain.add(btnBlame, cc.xywh(4, 9, 1, 1));

		pnlMain.add(new JSeparator(), cc.xywh(1, 10, 4, 1));
		pnlMain.add(new JLabel("Export to:"), cc.xywh(1, 11, 1, 1));
		pnlMain.add(sfExportTo, cc.xywh(3, 11, 2, 1));
		pnlMain.add(btnExportTo, cc.xywh(4, 13, 1, 1));

		pnlMain.add(new JSeparator(), cc.xywh(1, 14, 4, 1));
		pnlMain.add(new JLabel("Update to revision:"), cc.xywh(1, 15, 1, 1));
		pnlMain.add(sfUpdateTo, cc.xywh(3, 15, 2, 1));
		pnlMain.add(btnUpdateToRevision, cc.xywh(4, 17, 1, 1));

		pnlMain.add(new JSeparator(), cc.xywh(1, 18, 4, 1));
		pnlMain.add(btnApplyPatch, cc.xywh(4, 19, 1, 1));

		pnlMain.add(new JSeparator(), cc.xywh(1, 20, 4, 1));
		pnlMain.add(prgBusy, cc.xywh(1, 21, 3, 1));
		pnlMain.add(lblStatus, cc.xywh(4, 21, 1, 1));

		frame = GuiHelper.createAndShowFrame(pnlMain, "Other", "/hu/pagavcs/resources/other-app-icon.png");
		frame.setTitlePrefix(other.getPath());
	}

	public void setStatus(OtherStatus status) {
		lblStatus.setText("Status: " + status.toString());
	}

	public void close() {
		frame.setVisible(false);
		frame.dispose();
	}

	public void setURL(String text) {
		sfRepo.setText(text);
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

	private void doApplyPatch(String path) throws Exception {
		JFileChooser fc = new JFileChooser(new File(other.getPath()));
		fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		int choosed = fc.showSaveDialog(frame);
		if (choosed == JFileChooser.APPROVE_OPTION) {

			File baseDirCandiate = new File(path);
			if (!baseDirCandiate.isDirectory()) {
				baseDirCandiate = baseDirCandiate.getParentFile();
			}

			final File baseDir = baseDirCandiate;
			File file = fc.getSelectedFile();

			String result = Manager.getOsCommandResult(baseDir, "lsdiff", file.getPath());
			// TODO display file names, select from files

			List<String> lstFilesToPatch = new ArrayList<String>();
			for (String filename : result.split("\n")) {
				lstFilesToPatch.add(filename);
			}
			Manager.getOsCommandResult(baseDir, "patch", "-p0", "--no-backup-if-mismatch", "-U", "-i", file.getPath());
			/*
			 * example output: patching file a Hunk #1 FAILED at 9. 1 out of 1
			 * hunk FAILED -- saving rejects to file a.rej
			 */

			// find rejected files
			List<String> lstRejected = new ArrayList<String>();
			for (String fileName : lstFilesToPatch) {
				File f = new File(baseDir, fileName + ".rej");
				if (f.exists() && f.isFile()) {
					lstRejected.add(fileName);
				}
			}

			List<String> lstUnresolved = new ArrayList<String>();
			for (String fileName : lstRejected) {
				/*
				 * example output: 1 unresolved conflict found
				 */
				String output = Manager.getOsCommandResult(baseDir, "wiggle", "--replace", fileName, fileName + ".rej");
				if (output.contains("unresolved conflict")) {
					lstUnresolved.add(fileName);
				}
			}

			for (final String fileName : lstUnresolved) {

				Refreshable li = new Refreshable() {

					public void refresh() throws Exception {
						// delete .rej and .porig files if everything was
						// successful
						new File(baseDir, fileName + ".rej").delete();
						new File(baseDir, fileName + ".porig").delete();
					}
				};
				ResolveConflict resolveConflict = new ResolveConflict(li, baseDir.getPath() + "/" + fileName, true);
				resolveConflict.execute();
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
			doApplyPatch(other.getPath());
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
