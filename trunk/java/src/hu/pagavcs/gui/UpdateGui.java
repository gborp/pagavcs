package hu.pagavcs.gui;

import hu.pagavcs.bl.Cancelable;
import hu.pagavcs.bl.Manager;
import hu.pagavcs.bl.OnSwing;
import hu.pagavcs.bl.ThreadAction;
import hu.pagavcs.operation.ContentStatus;
import hu.pagavcs.operation.Log;
import hu.pagavcs.operation.Update.UpdateContentStatus;

import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.SVNInfo;

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
public class UpdateGui {

	private Table                                 tblUpdate;
	private TableModel<UpdateListItem>            tableModel;
	private final Cancelable                      update;
	private JButton                               btnStop;
	private final String                          title;
	// TODO use Progress, btnStop make it stop too
	private JProgressBar                          prgWorking;
	private boolean                               started;
	private boolean                               conflictedItemsPresent;
	private ConcurrentLinkedQueue<UpdateListItem> quNewItems = new ConcurrentLinkedQueue<UpdateListItem>();
	private Timer                                 tmrTableRevalidate;
	private boolean                               revalidateIsTimed;
	private boolean                               shuttingDown;
	private Label                                 lblWorkingCopy;
	private Label                                 lblRepo;

	public UpdateGui(Cancelable update) {
		this(update, "Update");
	}

	public UpdateGui(Cancelable update, String title) {
		this.update = update;
		this.title = title;
	}

	public void display() {

		FormLayout layout = new FormLayout("p,1dlu:g, p,4dlu, p", "p,4dlu,p,4dlu,fill:10dlu:g,4dlu,p");
		JPanel pnlMain = new JPanel(layout);
		CellConstraints cc = new CellConstraints();

		lblWorkingCopy = new Label();
		lblRepo = new Label();

		tableModel = new TableModel<UpdateListItem>(new UpdateListItem());

		tblUpdate = new Table(tableModel);
		tblUpdate.addMouseListener(new PopupupMouseListener());
		new StatusCellRendererForUpdateListItem(tblUpdate);
		JScrollPane scrollPane = new JScrollPane(tblUpdate);

		btnStop = new JButton("Stop");
		btnStop.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				try {
					btnStop.setEnabled(false);
					update.setCancel(true);
				} catch (Exception e1) {
					Manager.handle(e1);
				}
			}
		});
		prgWorking = new JProgressBar();

		pnlMain.add(lblWorkingCopy, cc.xywh(1, 1, 1, 1));
		pnlMain.add(lblRepo, cc.xywh(1, 3, 1, 1));
		pnlMain.add(scrollPane, cc.xywh(1, 5, 5, 1));
		pnlMain.add(prgWorking, cc.xywh(2, 7, 2, 1));
		pnlMain.add(btnStop, cc.xywh(5, 7, 1, 1));

		Window window = Manager.createAndShowFrame(new JScrollPane(pnlMain), title);
		window.addWindowListener(new WindowAdapter() {

			public void windowClosing(WindowEvent e) {
				try {
					update.setCancel(true);
				} catch (Exception ex) {
					Manager.handle(ex);
				}
				shuttingDown = true;
				tmrTableRevalidate.purge();
				tmrTableRevalidate.cancel();
			}

			public void windowClosed(WindowEvent e) {}
		});

		tmrTableRevalidate = new Timer("Revalidate table");
		started = false;
	}

	public void setWorkingCopy(String workingCopy) {
		lblWorkingCopy.setText(workingCopy);
	}

	public void setRepo(String repo) {
		lblRepo.setText(repo);
	}

	public void setStatus(ContentStatus status) throws Exception {
		addItem("", null, status);
	}

	public void addItem(final String path, final UpdateContentStatus updateContentStatus, final ContentStatus status) throws Exception {

		new OnSwing() {

			protected void process() throws Exception {
				if (!started) {
					prgWorking.setIndeterminate(true);
					conflictedItemsPresent = false;
					started = true;
				}

				if (ContentStatus.NONE.equals(status)) {
					return;
				}

				if (ContentStatus.CANCEL.equals(status)) {
					prgWorking.setIndeterminate(false);
					btnStop.setEnabled(false);
					btnStop.setText("Cancelled");
				}

				if (ContentStatus.FAILED.equals(status)) {
					prgWorking.setIndeterminate(false);
					btnStop.setEnabled(false);
					btnStop.setText("Failed");
				}

				if (ContentStatus.COMPLETED.equals(status)) {
					prgWorking.setIndeterminate(false);
					btnStop.setEnabled(false);
					btnStop.setText("Finished");
					if (conflictedItemsPresent) {
						JOptionPane.showMessageDialog(Manager.getRootFrame(), "There were conflicted items!", "Conflict", JOptionPane.WARNING_MESSAGE);
					}
				}

				if (update.isCancel() && !ContentStatus.CANCEL.equals(status)) {
					return;
				}

				if (UpdateContentStatus.CONFLICTED.equals(updateContentStatus)) {
					conflictedItemsPresent = true;
				}

				ContentStatus effectiveStatus = status;

				if (UpdateContentStatus.CONFLICTED.equals(updateContentStatus)) {
					effectiveStatus = ContentStatus.CONFLICTED;
				} else if (UpdateContentStatus.MERGED.equals(updateContentStatus)) {
					effectiveStatus = ContentStatus.MERGED;
				}

				UpdateListItem li = new UpdateListItem();
				li.setStatus(effectiveStatus);
				li.setPath(path);
				li.setContentStatus(updateContentStatus);
				quNewItems.add(li);
				doRevalidateTable();
			}

		}.run();

	}

	public void resolveConflict(UpdateListItem li) throws SVNException, IOException, InterruptedException {

		String path = li.getPath();
		File file = new File(li.getPath());
		if (file.isDirectory()) {
			return;
		}
		SVNInfo info = Manager.getInfo(path);

		// info.getConflictOldFile();
		File newFile = info.getConflictNewFile();
		File oldFile = info.getConflictOldFile();
		File wrkFile = info.getConflictWrkFile();

		Process process = Runtime.getRuntime().exec(
		        "meld -L old " + oldFile.getPath() + " -L working-copy " + wrkFile.getPath() + " -L new " + newFile.getPath());
		process.waitFor();

		int choosed = JOptionPane.showConfirmDialog(Manager.getRootFrame(), "Is conflict resolved?", "Resolved?", JOptionPane.YES_NO_OPTION,
		        JOptionPane.QUESTION_MESSAGE);
		if (choosed == JOptionPane.YES_OPTION) {
			Manager.resolveConflictUsingMine(path);
			li.setContentStatus(null);
			tblUpdate.repaint();
		}
	}

	private void doRevalidateTable() {
		synchronized (quNewItems) {
			if (!revalidateIsTimed && !shuttingDown) {
				revalidateIsTimed = true;
				tmrTableRevalidate.schedule(new DoRevalidateTask(), Manager.REVALIDATE_DELAY);
			}
		}
	}

	private class DoRevalidateTask extends TimerTask {

		public void run() {
			try {
				new OnSwing() {

					protected void process() throws Exception {
						ArrayList<UpdateListItem> lstLi = new ArrayList<UpdateListItem>();
						synchronized (quNewItems) {
							revalidateIsTimed = false;
							UpdateListItem li;
							while ((li = quNewItems.poll()) != null) {
								lstLi.add(li);
							}
						}
						tableModel.addLines(lstLi);
						tblUpdate.scrollRectToVisible(tblUpdate.getCellRect(tblUpdate.getRowCount() - 1, 0, true));
					}
				}.run();
			} catch (Exception e) {
				Manager.handle(e);
			}
		}
	}

	private class CopyAllToClipboard extends AbstractAction {

		private final PopupupMouseListener popupupMouseListener;

		public CopyAllToClipboard(PopupupMouseListener popupupMouseListener) {
			super("Copy all to clipboard");
			this.popupupMouseListener = popupupMouseListener;
		}

		public void actionPerformed(ActionEvent e) {
			popupupMouseListener.hidePopup();
			StringBuilder result = new StringBuilder();
			for (UpdateListItem li : tableModel.getAllData()) {
				result.append(li.getStatus() + " " + li.getPath() + "\n");
			}
			Manager.setClipboard(result.toString());
		}
	}

	private class CopyLineToClipboard extends AbstractAction {

		private final PopupupMouseListener popupupMouseListener;

		public CopyLineToClipboard(PopupupMouseListener popupupMouseListener) {
			super("Copy line to clipboard");
			this.popupupMouseListener = popupupMouseListener;
		}

		public void actionPerformed(ActionEvent e) {
			popupupMouseListener.hidePopup();
			UpdateListItem li = popupupMouseListener.getSelected();
			Manager.setClipboard(li.getStatus() + " " + li.getPath());
		}
	}

	private class ShowLog extends ThreadAction {

		private final PopupupMouseListener popupupMouseListener;

		public ShowLog(PopupupMouseListener popupupMouseListener) {
			super("Show log");
			this.popupupMouseListener = popupupMouseListener;
		}

		public void actionProcess(ActionEvent e) throws Exception {
			popupupMouseListener.hidePopup();
			UpdateListItem li = popupupMouseListener.getSelected();
			new Log(li.getPath()).execute();
		}
	}

	private class ResolveConflictUsingTheirs extends AbstractAction {

		private final PopupupMouseListener popupupMouseListener;

		public ResolveConflictUsingTheirs(PopupupMouseListener popupupMouseListener) {
			super("Resolve conflict using theirs");
			this.popupupMouseListener = popupupMouseListener;
		}

		public void actionPerformed(ActionEvent e) {
			try {
				popupupMouseListener.hidePopup();
				UpdateListItem li = popupupMouseListener.getSelected();
				Manager.resolveConflictUsingTheirs(li.getPath());
				li.setContentStatus(null);
				tblUpdate.repaint();
			} catch (SVNException e1) {
				Manager.handle(e1);
			}

		}
	}

	private class ResolveConflictUsingMine extends AbstractAction {

		private final PopupupMouseListener popupupMouseListener;

		public ResolveConflictUsingMine(PopupupMouseListener popupupMouseListener) {
			super("Resolve conflict using mine");
			this.popupupMouseListener = popupupMouseListener;
		}

		public void actionPerformed(ActionEvent e) {
			try {
				popupupMouseListener.hidePopup();
				UpdateListItem li = popupupMouseListener.getSelected();
				Manager.resolveConflictUsingMine(li.getPath());
				li.setContentStatus(null);
				tblUpdate.repaint();
			} catch (SVNException e1) {
				Manager.handle(e1);
			}

		}
	}

	private class ResolveConflict extends AbstractAction {

		private final PopupupMouseListener popupupMouseListener;

		public ResolveConflict(PopupupMouseListener popupupMouseListener) {
			super("Resolve conflict");
			this.popupupMouseListener = popupupMouseListener;
		}

		public void actionPerformed(ActionEvent e) {
			try {
				popupupMouseListener.hidePopup();
				UpdateListItem li = popupupMouseListener.getSelected();
				resolveConflict(li);

			} catch (Exception e1) {
				Manager.handle(e1);
			}

		}
	}

	private class PopupupMouseListener extends MouseAdapter {

		private JPopupMenu     ppVisible;
		private JPopupMenu     ppCompleted;
		private JPopupMenu     ppConflicted;
		private UpdateListItem selected;
		private JPopupMenu     ppUpdated;

		public PopupupMouseListener() {
			ppCompleted = new JPopupMenu();
			ppCompleted.add(new CopyLineToClipboard(this));
			ppCompleted.add(new CopyAllToClipboard(this));

			ppUpdated = new JPopupMenu();
			ppUpdated.add(new CopyLineToClipboard(this));
			ppUpdated.add(new CopyAllToClipboard(this));
			ppUpdated.add(new ShowLog(this));

			ppConflicted = new JPopupMenu();
			ppConflicted.add(new CopyLineToClipboard(this));
			ppConflicted.add(new CopyAllToClipboard(this));
			ppConflicted.add(new ShowLog(this));
			ppConflicted.add(new ResolveConflictUsingTheirs(this));
			ppConflicted.add(new ResolveConflictUsingMine(this));
			ppConflicted.add(new ResolveConflict(this));
		}

		public UpdateListItem getSelected() {
			return selected;
		}

		private void hidePopup() {
			if (ppVisible != null) {
				ppVisible.setVisible(false);
				ppVisible = null;
			}
		}

		public void mousePressed(MouseEvent e) {
			hidePopup();
			if (e.getButton() == MouseEvent.BUTTON3) {

				Point p = new Point(e.getX(), e.getY());
				int row = tblUpdate.rowAtPoint(p);
				selected = tableModel.getRow(row);
				ContentStatus status = selected.getStatus();

				if (UpdateContentStatus.CONFLICTED.equals(selected.getContentStatus())) {
					ppVisible = ppConflicted;
				} else if (ContentStatus.ADDED.equals(status) || ContentStatus.DELETED.equals(status) || ContentStatus.EXISTS.equals(status)
				        || ContentStatus.EXTERNAL.equals(status) || ContentStatus.NONE.equals(status) || ContentStatus.REPLACED.equals(status)
				        || ContentStatus.UPDATE.equals(status)) {
					ppVisible = ppUpdated;
				} else {
					ppVisible = ppCompleted;
				}
				ppVisible.setInvoker(tblUpdate);
				ppVisible.setLocation(e.getXOnScreen(), e.getYOnScreen());
				ppVisible.setVisible(true);
				e.consume();
			}
			super.mousePressed(e);
		}
	}
}
