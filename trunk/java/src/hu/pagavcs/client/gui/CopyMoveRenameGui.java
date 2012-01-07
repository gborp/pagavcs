package hu.pagavcs.client.gui;

import hu.pagavcs.client.bl.Cancelable;
import hu.pagavcs.client.bl.Manager;
import hu.pagavcs.client.gui.platform.EditField;
import hu.pagavcs.client.gui.platform.Frame;
import hu.pagavcs.client.gui.platform.GuiHelper;
import hu.pagavcs.client.gui.platform.Label;
import hu.pagavcs.client.gui.platform.MessagePane;
import hu.pagavcs.client.gui.platform.ProgressBar;
import hu.pagavcs.client.gui.platform.MessagePane.OPTIONS;
import hu.pagavcs.client.operation.CopyMoveRename;
import hu.pagavcs.client.operation.CopyMoveRename.CopyMoveRenameStatus;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

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
public class CopyMoveRenameGui implements Working, Cancelable {

	private CopyMoveRename other;
	private JLabel         lblStatus;
	private Frame          frame;
	private EditField      sfRepo;
	private EditField      sfNewPath;
	private JCheckBox      cbCopy;
	private EditField      sfWorkingCopy;
	private ProgressBar    prgBusy;
	private JButton        btnCopyMoveRename;

	public CopyMoveRenameGui(CopyMoveRename other) {
		this.other = other;
	}

	public void display() throws SVNException {
		FormLayout layout = new FormLayout("right:p, 2dlu,p:g, p", "p,2dlu,p,2dlu,4dlu,p,2dlu,p,4dlu,p");
		JPanel pnlMain = new JPanel(layout);
		CellConstraints cc = new CellConstraints();

		Label lblWorkingCopy = new Label("Path:");
		sfWorkingCopy = new EditField(other.getPath());
		sfWorkingCopy.setEditable(false);
		Label lblRepo = new Label("Repository:");
		sfRepo = new EditField();
		sfRepo.setEditable(false);

		sfNewPath = new EditField();
		cbCopy = new JCheckBox("Copy");
		btnCopyMoveRename = new JButton("Copy/move/rename");
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

		sfNewPath.getDocument().addDocumentListener(new DocumentListener() {

			public void changedUpdate(DocumentEvent e) {
				changeButtonLabel();
			}

			public void removeUpdate(DocumentEvent e) {
				changeButtonLabel();
			}

			public void insertUpdate(DocumentEvent e) {
				changeButtonLabel();
			}

			private void changeButtonLabel() {
				String origPath = other.getPath();
				String newPath = sfNewPath.getText();

				File origFile = new File(origPath);
				File newFile = new File(newPath);

				if (other.getPath().equals(sfNewPath.getText())) {
					btnCopyMoveRename.setEnabled(false);
					btnCopyMoveRename.setText("Nothing to do");
				} else {
					btnCopyMoveRename.setEnabled(true);
					if (newFile.exists()) {
						btnCopyMoveRename.setEnabled(false);
						btnCopyMoveRename.setText("Already exists");
					} else {
						if (!cbCopy.isSelected()) {
							if (origFile.getParent().equals(newFile.getParent())) {
								btnCopyMoveRename.setText("Rename");
							} else {
								btnCopyMoveRename.setText("Move");
							}
						} else {
							btnCopyMoveRename.setText("Copy");
						}
					}

				}
			}
		});

		sfNewPath.setText(other.getPath());

		lblStatus = new Label(" ");
		prgBusy = new ProgressBar(this);

		pnlMain.add(lblWorkingCopy, cc.xywh(1, 1, 1, 1));
		pnlMain.add(sfWorkingCopy, cc.xywh(3, 1, 2, 1));
		pnlMain.add(lblRepo, cc.xywh(1, 3, 1, 1));
		pnlMain.add(sfRepo, cc.xywh(3, 3, 2, 1));

		pnlMain.add(new JSeparator(), cc.xywh(1, 5, 4, 1));
		pnlMain.add(new JLabel("Copy/move/rename"), cc.xywh(1, 6, 1, 1));
		pnlMain.add(sfNewPath, cc.xywh(3, 6, 2, 1));
		pnlMain.add(cbCopy, cc.xywh(3, 8, 1, 1));
		pnlMain.add(btnCopyMoveRename, cc.xywh(4, 8, 1, 1));

		pnlMain.add(new JSeparator(), cc.xywh(1, 9, 4, 1));
		pnlMain.add(prgBusy, cc.xywh(1, 10, 3, 1));
		pnlMain.add(lblStatus, cc.xywh(4, 10, 1, 1));

		frame = GuiHelper.createAndShowFrame(pnlMain, "Copy-Move-Rename", "/hu/pagavcs/resources/other-app-icon.png");
		frame.setTitlePrefix(other.getPath());
	}

	public void setStatus(CopyMoveRenameStatus status) {
		lblStatus.setText("Status: " + status.toString());
	}

	public void close() {
		frame.setVisible(false);
		frame.dispose();
	}

	public void setURL(String text) {
		sfRepo.setText(text);
	}

	private void copyMoveRename() throws Exception {
		try {
			prgBusy.startProgress();
			other.copyMoveRename(sfWorkingCopy.getText(), sfNewPath.getText().trim(), cbCopy.isSelected());
		} finally {
			prgBusy.stopProgress();
		}
	}

	public void workStarted() {
		setStatus(CopyMoveRenameStatus.WORKING);
	}

	public void workEnded() {
		setStatus(CopyMoveRenameStatus.COMPLETED);
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
