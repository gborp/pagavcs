package hu.pagavcs.gui;

import hu.pagavcs.bl.Manager;
import hu.pagavcs.bl.ThreadAction;
import hu.pagavcs.operation.Commit;
import hu.pagavcs.operation.ContentStatus;
import hu.pagavcs.operation.Log;
import hu.pagavcs.operation.Commit.CommitStatus;
import hu.pagavcs.operation.Commit.CommittedItemStatus;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.prefs.BackingStoreException;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.table.TableRowSorter;

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
public class CommitGui implements Working {

	private Window                     frame;
	private Table                      tblCommit;
	private TableModel<CommitListItem> commitTableModel;
	private Commit                     commit;
	private JButton                    btnStop;
	private TextArea                   taMessage;
	private JButton                    btnCommit;
	private Label                      lblUrl;
	private ProgressBar                prgWorkinProgress;
	private Label                      lblInfo;
	private int                        noCommit;
	private boolean                    preRealCommitProcess;
	private JButton                    btnRefresh;
	private JCheckBox                  cbSelectDeselectAll;
	private JComboBox                  cboMessage;

	public CommitGui(Commit commit) {
		this.commit = commit;
	}

	public void display() throws SVNException {
		CellConstraints cc = new CellConstraints();
		commitTableModel = new TableModel<CommitListItem>(new CommitListItem());
		tblCommit = new Table(commitTableModel);
		tblCommit.addMouseListener(new PopupupMouseListener());
		tblCommit.setRowSorter(new TableRowSorter<TableModel<CommitListItem>>(commitTableModel));
		new StatusCellRendererForCommitListItem(tblCommit);
		JScrollPane spCommitList = new JScrollPane(tblCommit);

		taMessage = new TextArea();
		JScrollPane spMessage = new JScrollPane(taMessage);

		JSplitPane splMain = new JSplitPane(JSplitPane.VERTICAL_SPLIT, spMessage, spCommitList);

		lblUrl = new Label();
		cboMessage = new JComboBox();
		cboMessage.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					if (e.getItem() != null) {
						taMessage.setText(e.getItem().toString());
					}
				}
			}
		});

		JPanel pnlTop = new JPanel(new FormLayout("r:p,4dlu,p:g", "p,4dlu,p,4dlu"));
		pnlTop.add(new JLabel("Commit to:"), cc.xy(1, 1));
		pnlTop.add(lblUrl, cc.xy(3, 1));
		pnlTop.add(new JLabel("Recent messages:"), cc.xy(1, 3));
		pnlTop.add(cboMessage, cc.xy(3, 3));

		cbSelectDeselectAll = new JCheckBox(new SelectDeselectAllAction());

		btnStop = new JButton("Stop");
		btnStop.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				btnStop.setEnabled(false);
				commit.setCancel(true);
			}
		});
		prgWorkinProgress = new ProgressBar(this);
		lblInfo = new Label();
		btnCommit = new JButton("Commit");
		btnCommit.setEnabled(false);
		btnCommit.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable() {

					public void run() {
						try {
							commitSelected();
						} catch (Exception e) {
							Manager.handle(e);
						}
					}
				}).start();
			}
		});
		btnRefresh = new JButton("Refresh");
		btnRefresh.setEnabled(false);
		btnRefresh.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable() {

					public void run() {
						refresh();
					}
				}).start();
			}
		});

		JPanel pnlBottom = new JPanel(new FormLayout("p, 4dlu,p,4dlu, p:g, 4dlu,p, 4dlu,p", "p"));

		pnlBottom.add(cbSelectDeselectAll, cc.xy(1, 1));
		pnlBottom.add(btnRefresh, cc.xy(3, 1));
		pnlBottom.add(prgWorkinProgress, cc.xy(5, 1));
		pnlBottom.add(btnStop, cc.xy(7, 1));
		pnlBottom.add(btnCommit, cc.xy(9, 1));

		JPanel pnlMain = new JPanel(new BorderLayout());
		pnlMain.add(pnlTop, BorderLayout.NORTH);
		pnlMain.add(splMain, BorderLayout.CENTER);
		pnlMain.add(pnlBottom, BorderLayout.SOUTH);

		frame = Manager.createAndShowFrame(pnlMain, "Commit");
	}

	public void setStatusStartWorking() {}

	public void setStatusStopWorking() {}

	public synchronized void workStarted() throws Exception {
		prgWorkinProgress.startProgress();
	}

	public synchronized void workEnded() throws Exception {
		prgWorkinProgress.stopProgress();
	}

	public void setUrlLabel(String urlLabel) {
		lblUrl.setText(urlLabel);
	}

	public void setRecentMessages(String[] recentMessages) {
		ComboBoxModel modelUrl = new DefaultComboBoxModel(recentMessages);
		cboMessage.setModel(modelUrl);
	}

	public void setStatus(CommitStatus status, String message) throws Exception {
		if (CommitStatus.FILE_LIST_GATHERING_COMPLETED.equals(status)) {
			workEnded();
			prgWorkinProgress.setIndeterminate(false);
			btnStop.setEnabled(false);
			btnCommit.setEnabled(true);
			btnRefresh.setEnabled(true);
			if (commitTableModel.getAllData().isEmpty()) {
				JOptionPane.showMessageDialog(Manager.getRootFrame(), "There's nothing to commit!", "Nothing to commit", JOptionPane.INFORMATION_MESSAGE);
				frame.setVisible(false);
				frame.dispose();
			}
		}
		if (CommitStatus.INIT.equals(status)) {
			workStarted();
		} else if (CommitStatus.COMMIT_COMPLETED.equals(status)) {
			JOptionPane.showMessageDialog(Manager.getRootFrame(), message, "Completed", JOptionPane.INFORMATION_MESSAGE);
			frame.setVisible(false);
			frame.dispose();

		} else if (CommitStatus.COMMIT_FAILED.equals(status)) {
			JOptionPane.showMessageDialog(Manager.getRootFrame(), "Commit failed!", "Failed!", JOptionPane.ERROR_MESSAGE);
			frame.setVisible(false);
			frame.dispose();
		}
	}

	public void refresh() {
		try {
			commitTableModel.clear();
			btnStop.setEnabled(true);
			btnCommit.setEnabled(false);
			btnRefresh.setEnabled(false);
			commit.refresh();
		} catch (Exception ex) {
			Manager.handle(ex);
		}
	}

	public void addItem(File file, ContentStatus contentStatus, ContentStatus propertyStatus) {
		if ((contentStatus.equals(ContentStatus.NORMAL) || contentStatus.equals(ContentStatus.NONE))
		        && (propertyStatus.equals(ContentStatus.NORMAL) || propertyStatus.equals(ContentStatus.NONE))) {
			return;
		}
		CommitListItem li = new CommitListItem();
		if (contentStatus.equals(ContentStatus.MODIFIED) || contentStatus.equals(ContentStatus.ADDED) || contentStatus.equals(ContentStatus.DELETED)
		        || propertyStatus.equals(ContentStatus.MODIFIED) || propertyStatus.equals(ContentStatus.ADDED)) {
			li.setSelected(true);
		} else {
			li.setSelected(false);
		}
		li.setPath(file);
		li.setStatus(contentStatus);
		li.setPropertyStatus(propertyStatus);

		commitTableModel.addLine(li);
		tblCommit.followScrollToNewItems();
	}

	public void commitSelected() throws Exception {
		if (taMessage.getText().trim().isEmpty()) {
			JOptionPane.showMessageDialog(Manager.getRootFrame(), "Message should not be empty!", "Cannot commit", JOptionPane.WARNING_MESSAGE);
			return;
		}

		Manager.getSettings().addCommitMessageForHistory(taMessage.getText().trim());

		noCommit = 0;
		ArrayList<File> lstCommit = new ArrayList<File>();
		for (CommitListItem li : commitTableModel.getAllData()) {
			if (li.isSelected()) {
				lstCommit.add(li.getPath());
				noCommit++;

				if (li.getStatus().equals(ContentStatus.UNVERSIONED)) {
					JOptionPane.showMessageDialog(Manager.getRootFrame(), "Cannot commit unversioned file! Please Add, Delete or Ignore it (or deselect it).",
					        "Cannot commit", JOptionPane.ERROR_MESSAGE);
					return;
				}
			}
		}

		tblCommit.setEnabled(false);
		btnCommit.setEnabled(false);
		prgWorkinProgress.setValue(0);
		prgWorkinProgress.setIndeterminate(true);
		preRealCommitProcess = true;
		commit.doCommit(lstCommit, taMessage.getText());

	}

	public void addCommittedItem(String fileName, CommittedItemStatus itemStatus) {
		if (preRealCommitProcess) {
			prgWorkinProgress.setIndeterminate(false);
			prgWorkinProgress.getModel().setMaximum(noCommit);
			preRealCommitProcess = false;
		}
		if (itemStatus.equals(CommittedItemStatus.DELTA_SENT)) {
			prgWorkinProgress.setValue(prgWorkinProgress.getValue() + 1);
		}

		if (itemStatus.equals(CommittedItemStatus.COMPLETED)) {
			lblInfo.setText(fileName);
		}
	}

	private CommitListItem getCommitListItem(File file) {
		for (CommitListItem li : commitTableModel.getAllData()) {
			if (li.getPath().equals(file)) {
				return li;
			}
		}
		return null;
	}

	public void changeFromUnversionedToAdded(File file) {
		CommitListItem li = getCommitListItem(file);
		if (li != null) {
			li.setStatus(ContentStatus.ADDED);
			li.setSelected(true);
			tblCommit.repaint();
		}
	}

	public void changeToIgnore(File file) {
		CommitListItem li = getCommitListItem(file);
		li.setStatus(ContentStatus.IGNORED);
		li.setSelected(false);
		tblCommit.repaint();
	}

	public void changeToDeleted(File file) {
		CommitListItem li = getCommitListItem(file);
		li.setStatus(ContentStatus.DELETED);
		li.setSelected(true);
		tblCommit.repaint();
	}

	private class AddAction extends ThreadAction {

		private final PopupupMouseListener popupupMouseListener;

		public AddAction(PopupupMouseListener popupupMouseListener) {
			super("Add");
			this.popupupMouseListener = popupupMouseListener;
		}

		public void actionProcess(ActionEvent e) throws SVNException {
			CommitListItem li = popupupMouseListener.getSelected();
			commit.add(li.getPath());
		}
	}

	private class IgnoreAction extends ThreadAction {

		private final PopupupMouseListener popupupMouseListener;

		public IgnoreAction(PopupupMouseListener popupupMouseListener) {
			super("Ignore");
			this.popupupMouseListener = popupupMouseListener;
		}

		public void actionProcess(ActionEvent e) throws SVNException {
			CommitListItem li = popupupMouseListener.getSelected();
			commit.ignore(li.getPath());
			// the callback from Commit doesn't work, because it knows only
			// the
			// parent dir
			changeToIgnore(li.getPath());
		}
	}

	private class DeleteAction extends ThreadAction {

		private final PopupupMouseListener popupupMouseListener;

		public DeleteAction(PopupupMouseListener popupupMouseListener) {
			super("Delete");
			this.popupupMouseListener = popupupMouseListener;
		}

		public void actionProcess(ActionEvent e) throws SVNException, BackingStoreException {
			CommitListItem li = popupupMouseListener.getSelected();
			commit.delete(li.getPath());
			changeToDeleted(li.getPath());
		}
	}

	private class ShowChangesAction extends ThreadAction {

		private final PopupupMouseListener popupupMouseListener;

		public ShowChangesAction(PopupupMouseListener popupupMouseListener) {
			super("Show changes");
			this.popupupMouseListener = popupupMouseListener;
		}

		public void actionProcess(ActionEvent e) throws Exception {
			CommitListItem li = popupupMouseListener.getSelected();
			commit.showChanges(li.getPath());
		}
	}

	private void removeListItemIfNormal(CommitListItem li) {
		if (li.getStatus().equals(ContentStatus.NORMAL) && li.getPropertyStatus().equals(ContentStatus.NONE)
		        && li.getPropertyStatus().equals(ContentStatus.NORMAL)) {
			commitTableModel.removeLine(li);
			tblCommit.getSelectionModel().clearSelection();
		}
	}

	private class SelectDeselectAllAction extends ThreadAction {

		public SelectDeselectAllAction() {
			super("Select/Deselect All");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			try {
				boolean selected = cbSelectDeselectAll.isSelected();
				for (CommitListItem li : commitTableModel.getAllData()) {
					li.setSelected(selected);
				}
				tblCommit.repaint();
			} catch (Exception ex) {
				Manager.handle(ex);
			}
		}
	}

	private class RevertChangesAction extends ThreadAction {

		private final PopupupMouseListener popupupMouseListener;

		public RevertChangesAction(PopupupMouseListener popupupMouseListener) {
			super("Revert changes");
			this.popupupMouseListener = popupupMouseListener;
		}

		public void actionProcess(ActionEvent e) throws SVNException {
			CommitListItem li = popupupMouseListener.getSelected();
			commit.revertChanges(li.getPath());
			li.setStatus(ContentStatus.NORMAL);
			removeListItemIfNormal(li);
		}
	}

	private class RevertPropertyChangesAction extends ThreadAction {

		private final PopupupMouseListener popupupMouseListener;

		public RevertPropertyChangesAction(PopupupMouseListener popupupMouseListener) {
			super("Revert property changes");
			this.popupupMouseListener = popupupMouseListener;
		}

		public void actionProcess(ActionEvent e) throws SVNException {
			CommitListItem li = popupupMouseListener.getSelected();
			commit.revertPropertyChanges(li.getPath());
			li.setPropertyStatus(ContentStatus.NORMAL);
			removeListItemIfNormal(li);
		}
	}

	private class ResolvedAction extends ThreadAction {

		private final PopupupMouseListener popupupMouseListener;

		public ResolvedAction(PopupupMouseListener popupupMouseListener) {
			super("Resolved");
			this.popupupMouseListener = popupupMouseListener;
		}

		public void actionProcess(ActionEvent e) throws SVNException {
			CommitListItem li = popupupMouseListener.getSelected();
			commit.resolved(li.getPath());
		}
	}

	private class ShowLog extends ThreadAction {

		private final PopupupMouseListener popupupMouseListener;

		public ShowLog(PopupupMouseListener popupupMouseListener) {
			super("Show log");
			this.popupupMouseListener = popupupMouseListener;
		}

		public void actionProcess(ActionEvent e) throws Exception {
			try {
				popupupMouseListener.hidePopup();
				CommitListItem li = popupupMouseListener.getSelected();
				new Log(li.getPath().getPath()).execute();
			} catch (Exception ex) {
				Manager.handle(ex);
			}
		}
	}

	private class PopupupMouseListener extends MouseAdapter {

		private JPopupMenu     ppVisible;
		private JPopupMenu     ppModified;
		private JPopupMenu     ppUnversioned;
		private JPopupMenu     ppMissing;
		private JPopupMenu     ppObstructed;
		private JPopupMenu     ppDeleted;
		private JPopupMenu     ppIncomplete;
		private JPopupMenu     ppPropertyModified;
		private CommitListItem selected;

		public PopupupMouseListener() {
			ppModified = new JPopupMenu();
			ppModified.add(new ShowChangesAction(this));
			ppModified.add(new ShowLog(this));
			ppModified.add(new RevertChangesAction(this));
			ppModified.add(new IgnoreAction(this));
			ppModified.add(new DeleteAction(this));

			ppUnversioned = new JPopupMenu();
			ppUnversioned.add(new AddAction(this));
			ppUnversioned.add(new IgnoreAction(this));
			ppUnversioned.add(new DeleteAction(this));

			ppMissing = new JPopupMenu();
			ppMissing.add(new ShowLog(this));
			ppMissing.add(new RevertChangesAction(this));
			ppMissing.add(new IgnoreAction(this));
			ppMissing.add(new DeleteAction(this));

			ppObstructed = new JPopupMenu();
			ppObstructed.add(new RevertChangesAction(this));
			ppObstructed.add(new IgnoreAction(this));
			ppObstructed.add(new DeleteAction(this));

			ppDeleted = new JPopupMenu();
			ppDeleted.add(new RevertChangesAction(this));

			ppIncomplete = new JPopupMenu();
			ppIncomplete.add(new ResolvedAction(this));
			ppIncomplete.add(new IgnoreAction(this));
			ppIncomplete.add(new DeleteAction(this));

			ppPropertyModified = new JPopupMenu();
			ppPropertyModified.add(new ShowLog(this));
			ppPropertyModified.add(new RevertPropertyChangesAction(this));
		}

		public CommitListItem getSelected() {
			return selected;
		}

		private void hidePopup() {
			if (ppVisible != null) {
				ppVisible.setVisible(false);
				ppVisible = null;
			}
		}

		private void showPopup(MouseEvent e) {
			hidePopup();
			Point p = new Point(e.getX(), e.getY());
			int row = tblCommit.convertRowIndexToModel(tblCommit.rowAtPoint(p));
			selected = commitTableModel.getRow(row);
			ContentStatus status = selected.getStatus();
			ContentStatus propertyStatus = selected.getPropertyStatus();
			if (status.equals(ContentStatus.MODIFIED)) {
				ppVisible = ppModified;
			} else if (status.equals(ContentStatus.UNVERSIONED)) {
				ppVisible = ppUnversioned;
			} else if (status.equals(ContentStatus.OBSTRUCTED)) {
				ppVisible = ppObstructed;
			} else if (status.equals(ContentStatus.INCOMPLETE)) {
				ppVisible = ppIncomplete;
			} else if (status.equals(ContentStatus.MISSING)) {
				ppVisible = ppMissing;
			} else if (status.equals(ContentStatus.DELETED)) {
				ppVisible = ppDeleted;
			}
			if (propertyStatus.equals(ContentStatus.MODIFIED)) {
				ppVisible = ppPropertyModified;
			}

			if (ppVisible != null) {
				ppVisible.setInvoker(tblCommit);
				ppVisible.setLocation(e.getXOnScreen(), e.getYOnScreen());
				ppVisible.setVisible(true);
				e.consume();
			}
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

		public void mouseClicked(MouseEvent e) {
			if (e.getClickCount() == 2) {
				hidePopup();
				Point p = new Point(e.getX(), e.getY());
				int row = tblCommit.convertRowIndexToModel(tblCommit.rowAtPoint(p));
				selected = commitTableModel.getRow(row);
				ContentStatus status = selected.getStatus();
				ContentStatus propertyStatus = selected.getPropertyStatus();
				if (status.equals(ContentStatus.MODIFIED)) {
					new ShowChangesAction(this).actionPerformed(null);
				}
			}
		}
	}

}
