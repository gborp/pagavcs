package hu.pagavcs.gui;

import hu.pagavcs.bl.Cancelable;
import hu.pagavcs.bl.Manager;
import hu.pagavcs.bl.ThreadAction;
import hu.pagavcs.gui.platform.EditField;
import hu.pagavcs.gui.platform.Frame;
import hu.pagavcs.gui.platform.GuiHelper;
import hu.pagavcs.gui.platform.Label;
import hu.pagavcs.gui.platform.ProgressBar;
import hu.pagavcs.operation.ApplyPatchOperation;
import hu.pagavcs.operation.GeneralStatus;
import hu.pagavcs.operation.ResolveConflict;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFileChooser;
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
public class ApplyPatchGui implements Working, Cancelable {

	private ApplyPatchOperation other;
	private JLabel              lblStatus;
	private Frame               frame;
	private EditField           sfRepo;
	private EditField           sfWorkingCopy;
	private JButton             btnShowLog;
	private ProgressBar         prgBusy;
	private JButton             btnApplyPatch;

	public ApplyPatchGui(ApplyPatchOperation other) {
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

		btnApplyPatch = new JButton(new ApplyPatchAction());

		lblStatus = new Label(" ");
		prgBusy = new ProgressBar(this);

		pnlMain.add(lblWorkingCopy, cc.xywh(1, 1, 1, 1));
		pnlMain.add(sfWorkingCopy, cc.xywh(3, 1, 2, 1));
		pnlMain.add(lblRepo, cc.xywh(1, 3, 1, 1));
		pnlMain.add(sfRepo, cc.xywh(3, 3, 2, 1));
		pnlMain.add(btnShowLog, cc.xywh(4, 5, 1, 1));

		pnlMain.add(new JSeparator(), cc.xywh(1, 6, 4, 1));
		pnlMain.add(btnApplyPatch, cc.xywh(4, 7, 1, 1));

		pnlMain.add(new JSeparator(), cc.xywh(1, 8, 4, 1));
		pnlMain.add(prgBusy, cc.xywh(1, 9, 3, 1));
		pnlMain.add(lblStatus, cc.xywh(4, 9, 1, 1));

		frame = GuiHelper.createAndShowFrame(pnlMain, "Other", "/hu/pagavcs/resources/other-app-icon.png");
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
		setStatus(GeneralStatus.WORKING);
	}

	public void workEnded() {
		setStatus(GeneralStatus.COMPLETED);
	}

	private class ApplyPatchAction extends ThreadAction {

		public ApplyPatchAction() {
			super("Apply patch");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			doApplyPatch(other.getPath());
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

}
