package hu.pagavcs.client.gui;

import hu.pagavcs.client.bl.Manager;
import hu.pagavcs.client.bl.OnSwing;
import hu.pagavcs.client.bl.ThreadAction;
import hu.pagavcs.client.gui.platform.EditField;
import hu.pagavcs.client.gui.platform.Frame;
import hu.pagavcs.client.gui.platform.GuiHelper;
import hu.pagavcs.client.gui.platform.Label;
import hu.pagavcs.client.gui.platform.ProgressBar;
import hu.pagavcs.client.gui.platform.Table;
import hu.pagavcs.client.gui.platform.TableModel;
import hu.pagavcs.client.operation.BlameOperation;
import hu.pagavcs.client.operation.GeneralStatus;
import hu.pagavcs.client.operation.Log;
import hu.pagavcs.common.ResourceBundleAccessor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;

import org.tmatesoft.svn.core.SVNCancelException;

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
public class BlameGui implements Working {

	private Table<BlameListItem> tblBlame;
	private TableModel<BlameListItem> tableModel;
	private final BlameOperation blameOperation;
	private JButton btnBlame;
	private JButton btnStop;
	private JLabel lblStatus;
	private EditField sfRepo;
	private EditField sfRevision;
	// TODO use Progress, btnStop make it stop too
	private ProgressBar prgBusy;
	private String file;
	private Frame frame;

	public BlameGui(BlameOperation blameOperation) {
		this.blameOperation = blameOperation;
	}

	public void display() {

		FormLayout layout = new FormLayout("p, 2dlu,p:g,2dlu, p",
				"p,2dlu,p,2dlu,fill:4dlu:g,2dlu,p");
		JPanel pnlMain = new JPanel(layout);
		CellConstraints cc = new CellConstraints();

		tableModel = new TableModel<BlameListItem>(new BlameListItem());

		tblBlame = new Table<BlameListItem>(tableModel);
		tblBlame.addMouseListener(new PopupupMouseListener());
		new BlameCellRenderer(tblBlame);
		JScrollPane scrollPane = new JScrollPane(tblBlame);

		btnStop = new JButton("Stop");
		btnStop.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				try {
					if (prgBusy.isWorking()) {
						btnStop.setEnabled(false);
						blameOperation.setCancel(true);
					} else {
						frame.setVisible(false);
						frame.dispose();
					}
				} catch (Exception e1) {
					Manager.handle(e1);
				}
			}
		});

		btnBlame = new JButton("Blame");
		btnBlame.setEnabled(false);
		btnBlame.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				try {
					prgBusy.startProgress();
					tblBlame.hideMessage();
					new Thread(new Runnable() {

						public void run() {
							try {
								try {
									final List<BlameListItem> lstBlame = blameOperation
											.doBlame(blameOperation.getPath(),
													sfRevision.getText());
									new OnSwing() {

										protected void process()
												throws Exception {
											setStatus(GeneralStatus.COMPLETED);
											tableModel.clear();
											tableModel.addLines(lstBlame);
											prgBusy.stopProgress();
										}

									}.run();
								} catch (SVNCancelException ex) {
									new OnSwing() {

										protected void process()
												throws Exception {
											btnStop.setEnabled(true);
											prgBusy.stopProgress();
										}
									}.run();
								}

							} catch (Exception e1) {
								Manager.handle(e1);
							}
						}
					}).start();

				} catch (Exception e1) {
					Manager.handle(e1);
				}
			}
		});

		prgBusy = new ProgressBar(this);

		Label lblRepo = new Label("Repository:");
		sfRepo = new EditField();
		sfRepo.setEditable(false);
		lblStatus = new Label(" ");
		Label lblRevision = new Label("Revision:");
		sfRevision = new EditField();
		sfRevision.setColumns(10);

		pnlMain.add(lblRepo, cc.xywh(1, 1, 1, 1));
		pnlMain.add(sfRepo, cc.xywh(3, 1, 3, 1));
		pnlMain.add(lblRevision, cc.xywh(1, 3, 1, 1));
		pnlMain.add(sfRevision, cc.xywh(3, 3, 1, 1));
		pnlMain.add(btnBlame, cc.xywh(5, 3, 1, 1));

		pnlMain.add(scrollPane, cc.xywh(1, 5, 5, 1));
		pnlMain.add(prgBusy, cc.xywh(1, 7, 3, 1));
		pnlMain.add(btnStop, cc.xywh(5, 7, 1, 1));

		tblBlame.showMessage("Click the \"Blame\" button",
				Manager.getIconInformation());

		frame = GuiHelper.createAndShowFrame(pnlMain, "Blame",
				"other-app-icon.png");

	}

	public void setStatus(GeneralStatus status) {
		lblStatus.setText("Status: " + status.toString());
	}

	public void setFile(String file) {
		this.file = file;
		frame.setTitlePrefix(file);
	}

	public void setURL(String text) {
		sfRepo.setText(text);
		btnBlame.setEnabled(true);
	}

	private class ShowLog extends ThreadAction {

		public ShowLog() {
			super("Show log", ResourceBundleAccessor
					.getSmallImage("actions/pagavcs-log.png"));
		}

		public void actionProcess(ActionEvent e) throws Exception {
			try {
				new Log(file).execute();
			} catch (Exception ex) {
				Manager.handle(ex);
			}
		}
	}

	private class BlameBefore extends ThreadAction {

		public BlameBefore() {
			super("Blame before", ResourceBundleAccessor
					.getSmallImage("actions/pagavcs-blame.png"));
		}

		public void actionProcess(ActionEvent e) throws Exception {
			OnSwing.execute(new Runnable() {

				@Override
				public void run() {
					try {
						sfRevision.setText(Long.toString(tblBlame
								.getSelectedItem().getRevision() - 1));
						btnBlame.doClick(500);
					} catch (Exception ex) {
						Manager.handle(ex);
					}
				}

			});
		}
	}

	private class PopupupMouseListener extends MouseAdapter {

		private JPopupMenu ppModified;

		public PopupupMouseListener() {
			ppModified = new JPopupMenu();
			ppModified.add(new ShowLog());
			ppModified.add(new BlameBefore());
		}

		private void showPopup(MouseEvent e) {
			if (e.isPopupTrigger()) {
				int r = tblBlame.rowAtPoint(e.getPoint());
				if (r >= 0 && r < tblBlame.getRowCount()) {
					tblBlame.setRowSelectionInterval(r, r);
				} else {
					tblBlame.clearSelection();
				}

				JPopupMenu ppVisible = ppModified;
				if (ppVisible != null) {
					ppVisible.setInvoker(tblBlame);
					ppVisible.setLocation(e.getXOnScreen(), e.getYOnScreen());
					ppVisible.setVisible(true);
					e.consume();
				}
			}
		}

		public void mousePressed(MouseEvent e) {
			showPopup(e);
		}

		public void mouseReleased(MouseEvent e) {
			showPopup(e);
		}
	}

	public void workStarted() {
		setStatus(GeneralStatus.WORKING);
	}

	public void workEnded() {
		setStatus(GeneralStatus.COMPLETED);
	}
}
