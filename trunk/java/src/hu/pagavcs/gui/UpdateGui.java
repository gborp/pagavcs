package hu.pagavcs.gui;

import hu.pagavcs.bl.Cancelable;
import hu.pagavcs.bl.Manager;
import hu.pagavcs.bl.OnSwing;
import hu.pagavcs.bl.SvnHelper;
import hu.pagavcs.bl.ThreadAction;
import hu.pagavcs.gui.platform.Frame;
import hu.pagavcs.gui.platform.GuiHelper;
import hu.pagavcs.gui.platform.Label;
import hu.pagavcs.gui.platform.MessagePane;
import hu.pagavcs.gui.platform.Table;
import hu.pagavcs.gui.platform.TableModel;
import hu.pagavcs.operation.ContentStatus;
import hu.pagavcs.operation.Log;
import hu.pagavcs.operation.ResolveConflict;
import hu.pagavcs.operation.Update.UpdateContentStatus;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
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
public class UpdateGui implements Working {

	private Table<UpdateListItem>                 tblUpdate;
	private TableModel<UpdateListItem>            tmdlUpdate;
	private final Cancelable                      update;
	private JButton                               btnStopFinish;
	private final String                          title;
	// TODO use Progress, btnStop make it stop too
	private JProgressBar                          prgWorking;
	private boolean                               started;
	private int                                   numberOfPathUpdated;
	private ConcurrentLinkedQueue<UpdateListItem> quNewItems = new ConcurrentLinkedQueue<UpdateListItem>();
	private Timer                                 tmrTableRevalidate;
	private boolean                               revalidateIsTimed;
	private boolean                               shuttingDown;
	private Label                                 lblWorkingCopy;
	private Label                                 lblRepo;
	private Frame                                 frame;
	private StopExitAction                        actStopFinish;
	private Label                                 lblInfo;
	private List<File>                            lstPath;

	public UpdateGui(Cancelable update) {
		this(update, "Update");
	}

	public UpdateGui(Cancelable update, String title) {
		this.update = update;
		this.title = title;
	}

	public void display() {

		FormLayout layout = new FormLayout("r:p,4dlu,p,4dlu,1dlu:g, p,4dlu, p", "p,4dlu,p,4dlu,fill:10dlu:g,4dlu,p");
		JPanel pnlMain = new JPanel(layout);
		CellConstraints cc = new CellConstraints();

		lblWorkingCopy = new Label();
		lblRepo = new Label();

		tmdlUpdate = new TableModel<UpdateListItem>(new UpdateListItem());

		tblUpdate = new Table<UpdateListItem>(tmdlUpdate);
		tblUpdate.addMouseListener(new PopupupMouseListener());
		new StatusCellRendererForUpdateListItem(tblUpdate);
		JScrollPane scrollPane = new JScrollPane(tblUpdate);

		lblInfo = new Label();

		actStopFinish = new StopExitAction();
		btnStopFinish = new JButton(actStopFinish);

		prgWorking = new JProgressBar();

		pnlMain.add(new Label("Working copy:"), cc.xywh(1, 1, 1, 1));
		pnlMain.add(lblWorkingCopy, cc.xywh(3, 1, 6, 1));
		pnlMain.add(new Label("Url:"), cc.xywh(1, 3, 1, 1));
		pnlMain.add(lblRepo, cc.xywh(3, 3, 6, 1));
		pnlMain.add(scrollPane, cc.xywh(1, 5, 8, 1));
		pnlMain.add(lblInfo, cc.xy(1, 7));
		pnlMain.add(prgWorking, cc.xywh(3, 7, 4, 1, CellConstraints.FILL, CellConstraints.DEFAULT));
		pnlMain.add(btnStopFinish, cc.xy(8, 7));

		frame = GuiHelper.createAndShowFrame(pnlMain, title);
		frame.addWindowListener(new WindowAdapter() {

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
		numberOfPathUpdated = 0;
	}

	public void setWorkingCopy(String workingCopy) {
		lblWorkingCopy.setText(workingCopy);
		frame.setTitlePrefix(workingCopy);
	}

	public void setRepo(String repo) {
		lblRepo.setText(repo);
	}

	public void setPaths(List<File> lstPath) {
		this.lstPath = lstPath;
	}

	public void setStatus(ContentStatus status) throws Exception {
		addItem("", null, status);
	}

	public void addItem(final String path, final UpdateContentStatus updateContentStatus, final ContentStatus status) throws Exception {

		new OnSwing() {

			protected void process() throws Exception {
				if (!started) {
					prgWorking.setIndeterminate(true);
					started = true;
				}

				if (ContentStatus.NONE.equals(status)) {
					return;
				}

				if (ContentStatus.CANCEL.equals(status)) {
					prgWorking.setIndeterminate(false);
					actStopFinish.setType(StopExitActionType.Cancelled);
				}

				if (ContentStatus.FAILED.equals(status)) {
					prgWorking.setIndeterminate(false);
					actStopFinish.setType(StopExitActionType.Failed);
				}

				if (update.isCancel() && !ContentStatus.CANCEL.equals(status)) {
					return;
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

				if (ContentStatus.COMPLETED.equals(status)) {
					numberOfPathUpdated++;
					if (numberOfPathUpdated == lstPath.size()) {

						prgWorking.setIndeterminate(false);
						actStopFinish.setType(StopExitActionType.Finished);

						int total = 0;
						int conflicted = 0;
						for (UpdateListItem uli : tmdlUpdate.getAllData()) {
							UpdateContentStatus updateContentStatus = uli.getContentStatus();
							ContentStatus contentStatus = uli.getStatus();
							if (UpdateContentStatus.CONFLICTED.equals(updateContentStatus) || ContentStatus.CONFLICTED.equals(contentStatus)) {
								conflicted++;
							}
							if (contentStatus != null) {
								if (ContentStatus.ADDED.equals(contentStatus) || ContentStatus.ADDED.equals(contentStatus)
								        || ContentStatus.CONFLICTED.equals(contentStatus) || ContentStatus.DELETED.equals(contentStatus)
								        || ContentStatus.EXTERNAL.equals(contentStatus) || ContentStatus.IGNORED.equals(contentStatus)
								        || ContentStatus.INCOMPLETE.equals(contentStatus) || ContentStatus.MERGED.equals(contentStatus)
								        || ContentStatus.MISSING.equals(contentStatus) || ContentStatus.MODIFIED.equals(contentStatus)
								        || ContentStatus.NONE.equals(contentStatus) || ContentStatus.NORMAL.equals(contentStatus)
								        || ContentStatus.OBSTRUCTED.equals(contentStatus) || ContentStatus.REPLACED.equals(contentStatus)
								        || ContentStatus.UNVERSIONED.equals(contentStatus) || ContentStatus.EXISTS.equals(contentStatus)
								        || ContentStatus.UPDATE.equals(contentStatus)) {
									total++;
								}
							}
						}
						String strInfo = "Changed: " + total;
						if (conflicted > 0) {
							strInfo += " Conflicted: " + conflicted;
						}
						lblInfo.setText(strInfo);

						if (conflicted > 0) {
							JOptionPane.showMessageDialog(Manager.getRootFrame(), "There were " + conflicted + " conflicted items!", "Conflict",
							        JOptionPane.WARNING_MESSAGE);
						}
					}
				}
			}

		}.run();

	}

	private class RefreshUpdateGuiIfResolved implements Refreshable {

		private final UpdateListItem li;

		public RefreshUpdateGuiIfResolved(UpdateListItem li) {
			this.li = li;
		}

		public void refresh() throws Exception {
			li.setContentStatus(UpdateContentStatus.RESOLVED);
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
						tmdlUpdate.addLines(lstLi);
						tblUpdate.scrollRectToVisible(tblUpdate.getCellRect(tblUpdate.getRowCount() - 1, 0, true));
					}
				}.run();
			} catch (Exception e) {
				Manager.handle(e);
			}
		}
	}

	private UpdateListItem getSelectedUpdateListItem() {
		return tmdlUpdate.getRow(tblUpdate.convertRowIndexToModel(tblUpdate.getSelectedRow()));
	}

	private class CopyAllToClipboard extends AbstractAction {

		public CopyAllToClipboard() {
			super("Copy all to clipboard");
		}

		public void actionPerformed(ActionEvent e) {
			StringBuilder result = new StringBuilder();
			for (UpdateListItem li : tmdlUpdate.getAllData()) {
				result.append(li.getStatus() + " " + li.getPath() + "\n");
			}
			Manager.setClipboard(result.toString());
		}
	}

	private class CopyLineToClipboard extends AbstractAction {

		public CopyLineToClipboard() {
			super("Copy line to clipboard");
		}

		public void actionPerformed(ActionEvent e) {
			UpdateListItem li = getSelectedUpdateListItem();
			Manager.setClipboard(li.getStatus() + " " + li.getPath());
		}
	}

	private class ShowLog extends ThreadAction {

		public ShowLog() {
			super("Show log");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			UpdateListItem li = getSelectedUpdateListItem();
			new Log(li.getPath()).execute();
		}
	}

	private class ShowChanges extends ThreadAction {

		public ShowChanges() {
			super("Show changes");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			UpdateListItem li = getSelectedUpdateListItem();
			SvnHelper.showChangesFromBase(UpdateGui.this, new File(li.getPath()));
		}
	}

	private class ResolveConflictUsingTheirsAction extends AbstractAction {

		public ResolveConflictUsingTheirsAction() {
			super("Resolve conflict using theirs");
		}

		public void actionPerformed(ActionEvent e) {
			try {
				UpdateListItem li = getSelectedUpdateListItem();
				Manager.resolveConflictUsingTheirs(li.getPath());
				li.setContentStatus(null);
				tblUpdate.repaint();
			} catch (SVNException e1) {
				Manager.handle(e1);
			}

		}
	}

	private class ResolveConflictUsingMineAction extends AbstractAction {

		public ResolveConflictUsingMineAction() {
			super("Resolve conflict using mine");
		}

		public void actionPerformed(ActionEvent e) {
			try {
				UpdateListItem li = getSelectedUpdateListItem();
				Manager.resolveConflictUsingMine(li.getPath());
				li.setContentStatus(null);
				tblUpdate.repaint();
			} catch (SVNException e1) {
				Manager.handle(e1);
			}

		}
	}

	private class ResolveConflictAction extends AbstractAction {

		public ResolveConflictAction() {
			super("Resolve conflict");
		}

		public void actionPerformed(ActionEvent e) {
			try {
				UpdateListItem li = getSelectedUpdateListItem();
				File file = new File(li.getPath());
				if (file.isDirectory()) {
					MessagePane.showError(frame, "Cannot resolve conflict", "Cannot resolve conflict on directory");
					return;
				}
				new ResolveConflict(new RefreshUpdateGuiIfResolved(li), file.getPath(), false).execute();

			} catch (Exception ex) {
				Manager.handle(ex);
			}
		}
	}

	private class PopupupMouseListener extends MouseAdapter {

		private JPopupMenu ppVisible;
		private JPopupMenu ppCompleted;
		private JPopupMenu ppConflicted;
		private JPopupMenu ppUpdated;

		public PopupupMouseListener() {
			ppCompleted = new JPopupMenu();
			ppCompleted.add(new CopyLineToClipboard());
			ppCompleted.add(new CopyAllToClipboard());

			ppUpdated = new JPopupMenu();
			ppUpdated.add(new CopyLineToClipboard());
			ppUpdated.add(new CopyAllToClipboard());
			ppUpdated.add(new ShowLog());
			// TODO ShowChanges
			// ppUpdated.add(new ShowChanges());

			ppConflicted = new JPopupMenu();
			ppConflicted.add(new CopyLineToClipboard());
			ppConflicted.add(new CopyAllToClipboard());
			ppConflicted.add(new ShowLog());
			ppConflicted.add(new ResolveConflictUsingTheirsAction());
			ppConflicted.add(new ResolveConflictUsingMineAction());
			ppConflicted.add(new ResolveConflictAction());
		}

		public void showPopup(MouseEvent e) {
			Point p = new Point(e.getX(), e.getY());
			int row = tblUpdate.rowAtPoint(p);
			if (row == -1) {
				return;
			}
			int rowIndex = tblUpdate.convertRowIndexToModel(row);
			tblUpdate.getSelectionModel().setSelectionInterval(rowIndex, rowIndex);
			UpdateListItem selected = getSelectedUpdateListItem();
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

		public void mousePressed(MouseEvent e) {
			if (e.isPopupTrigger()) {
				showPopup(e);
			}
		}

		public void mouseReleased(MouseEvent e) {
			if (e.isPopupTrigger()) {
				showPopup(e);
			}
		}
	}

	private enum StopExitActionType {
		Stop, Cancelled, Failed, Finished
	}

	private class StopExitAction extends ThreadAction {

		private StopExitActionType type;

		public StopExitAction() {
			super("");
			setType(StopExitActionType.Stop);
		}

		public void setType(StopExitActionType type) {
			this.type = type;
			setLabel(type.toString());
		}

		public void actionProcess(ActionEvent e) throws Exception {
			switch (type) {
				case Stop:
					update.setCancel(true);
					setType(StopExitActionType.Cancelled);
					break;
				case Cancelled:
					exit();
					break;
				case Failed:
					exit();
					break;
				case Finished:
					exit();
					break;
			}
		}

		private void exit() {
			frame.setVisible(false);
			frame.dispose();
		}
	}

	public void workEnded() throws Exception {
	// TODO workEnded
	}

	public void workStarted() throws Exception {
	// TODO workStarted
	}

}
