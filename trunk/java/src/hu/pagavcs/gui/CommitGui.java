package hu.pagavcs.gui;

import hu.pagavcs.bl.Manager;
import hu.pagavcs.bl.OnSwing;
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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableRowSorter;

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
	private JComboBox                  cboMessage;
	private int                        logMinSize;
	private Label                      lblWorkingCopy;
	private JButton                    btnSelectAll;
	private JButton                    btnSelectNone;
	private JButton                    btnSelectNonVersioned;
	private JButton                    btnSelectAdded;
	private JButton                    btnSelectDeleted;
	private JButton                    btnSelectModified;
	private JButton                    btnSelectFiles;
	private JButton                    btnSelectDirectories;

	public CommitGui(Commit commit) {
		this.commit = commit;
	}

	public void display() throws SVNException {
		logMinSize = 0;
		CellConstraints cc = new CellConstraints();
		commitTableModel = new TableModel<CommitListItem>(new CommitListItem());
		tblCommit = new Table(commitTableModel);
		tblCommit.addMouseListener(new PopupupMouseListener());
		tblCommit.setRowSorter(new TableRowSorter<TableModel<CommitListItem>>(commitTableModel));
		tblCommit.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		new StatusCellRendererForCommitListItem(tblCommit);
		JScrollPane spCommitList = new JScrollPane(tblCommit);

		taMessage = new TextArea();
		JScrollPane spMessage = new JScrollPane(taMessage);

		JSplitPane splMain = new JSplitPane(JSplitPane.VERTICAL_SPLIT, spMessage, spCommitList);

		lblUrl = new Label();
		lblWorkingCopy = new Label();
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

		JPanel pnlTop = new JPanel(new FormLayout("r:p,4dlu,p:g", "p,4dlu,p,4dlu,p,4dlu"));
		pnlTop.add(new JLabel("Commit to:"), cc.xy(1, 1));
		pnlTop.add(lblUrl, cc.xy(3, 1));
		pnlTop.add(new JLabel("Working copy:"), cc.xy(1, 3));
		pnlTop.add(lblWorkingCopy, cc.xy(3, 3));
		pnlTop.add(new JLabel("Recent messages:"), cc.xy(1, 5));
		pnlTop.add(cboMessage, cc.xy(3, 5));

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
				try {
					refresh();
				} catch (Exception e1) {
					Manager.handle(e1);
				}
			}
		});

		btnSelectAll = new JButton(new SelectAllAction());
		btnSelectNone = new JButton(new SelectNoneAction());
		btnSelectNonVersioned = new JButton(new SelectNonVersionedAction());
		btnSelectAdded = new JButton(new SelectAddedAction());
		btnSelectDeleted = new JButton(new SelectDeletedAction());
		btnSelectModified = new JButton(new SelectModifiedAction());
		btnSelectFiles = new JButton(new SelectFilesAction());
		btnSelectDirectories = new JButton(new SelectDirectoriesAction());

		JPanel pnlCheck = new JPanel(new FormLayout("p,4dlu,p,4dlu,p,4dlu,p,4dlu,p,4dlu,p,4dlu,p,4dlu,p,4dlu,p,4dlu,p", "p"));
		pnlCheck.add(new JLabel("Check:"), cc.xy(1, 1));
		pnlCheck.add(btnSelectAll, cc.xy(3, 1));
		pnlCheck.add(btnSelectNone, cc.xy(5, 1));
		pnlCheck.add(btnSelectNonVersioned, cc.xy(7, 1));
		pnlCheck.add(btnSelectAdded, cc.xy(9, 1));
		pnlCheck.add(btnSelectDeleted, cc.xy(11, 1));
		pnlCheck.add(btnSelectModified, cc.xy(13, 1));
		pnlCheck.add(btnSelectFiles, cc.xy(15, 1));
		pnlCheck.add(btnSelectDirectories, cc.xy(17, 1));

		JPanel pnlBottom = new JPanel(new FormLayout("p,4dlu, p:g, 4dlu,p, 4dlu,p", "p,4dlu,p"));

		pnlBottom.add(pnlCheck, cc.xywh(1, 1, 7, 1, CellConstraints.LEFT, CellConstraints.DEFAULT));
		pnlBottom.add(btnRefresh, cc.xy(1, 3));
		pnlBottom.add(prgWorkinProgress, cc.xy(3, 3));
		pnlBottom.add(btnStop, cc.xy(5, 3));
		pnlBottom.add(btnCommit, cc.xy(7, 3));

		JPanel pnlMain = new JPanel(new BorderLayout());
		pnlMain.add(pnlTop, BorderLayout.NORTH);
		pnlMain.add(splMain, BorderLayout.CENTER);
		pnlMain.add(pnlBottom, BorderLayout.SOUTH);

		frame = Manager.createAndShowFrame(pnlMain, "Commit");
		frame.addWindowListener(new WindowAdapter() {

			public void windowClosing(WindowEvent e) {
				commit.setCancel(true);
			}
		});
	}

	public void setLogTemplate(String strLogTemplate) {
		taMessage.setText(strLogTemplate);
	}

	public void setLogMinSize(int logMinSize) {
		this.logMinSize = logMinSize;
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

	public void setPath(String path) {
		lblWorkingCopy.setText(path);
	}

	public void setRecentMessages(String[] recentMessages) {
		ComboBoxModel modelUrl = new DefaultComboBoxModel(recentMessages);
		cboMessage.setModel(modelUrl);
	}

	public void setStatus(final CommitStatus status, final String message) throws Exception {
		new OnSwing() {

			protected void process() throws Exception {

				if (CommitStatus.FILE_LIST_GATHERING_COMPLETED.equals(status)) {
					workEnded();
					refreshSelectButtons();

					prgWorkinProgress.setIndeterminate(false);
					btnStop.setEnabled(false);
					btnCommit.setEnabled(true);
					btnRefresh.setEnabled(true);
					if (commitTableModel.getAllData().isEmpty()) {
						tblCommit.showMessage("There is nothing to commit", Manager.ICON_WARNING);

						// MessagePane.showWarning(frame, "Nothing to commit",
						// "There's nothing to commit!");
						// frame.setVisible(false);
						// frame.dispose();
					}
				}
				if (CommitStatus.INIT.equals(status)) {
					workStarted();
					tblCommit.showMessage("Working...", Manager.ICON_INFORMATION);

					btnSelectAll.setEnabled(false);
					btnSelectNone.setEnabled(false);
					btnSelectNonVersioned.setEnabled(false);
					btnSelectAdded.setEnabled(false);
					btnSelectDeleted.setEnabled(false);
					btnSelectModified.setEnabled(false);
					btnSelectFiles.setEnabled(false);
					btnSelectDirectories.setEnabled(false);

				} else if (CommitStatus.COMMIT_COMPLETED.equals(status)) {

					MessagePane.showInfo(frame, "Completed", message);
					frame.setVisible(false);
					frame.dispose();
				} else if (CommitStatus.COMMIT_FAILED.equals(status)) {
					MessagePane.showError(frame, "Failed!", "Commit failed!");
					frame.setVisible(false);
					frame.dispose();
				}
			}

		}.run();
	}

	public void refresh() throws Exception {
		new OnSwing<Object>() {

			protected void process() throws Exception {
				workStarted();
				commitTableModel.clear();
				btnStop.setEnabled(true);
				btnCommit.setEnabled(false);
				btnRefresh.setEnabled(false);
				new Thread(new Runnable() {

					public void run() {
						try {
							commit.refresh();
							workEnded();
						} catch (Exception e) {
							Manager.handle(e);
						}
					}
				}).start();
			}

		}.run();

	}

	private void refreshSelectButtons() {

		List<CommitListItem> list = commitTableModel.getAllData();

		boolean hasNonVersioned = false;
		boolean hasAdded = false;
		boolean hasDeleted = false;
		boolean hasModified = false;
		boolean hasFiles = false;
		boolean hasDirectories = false;

		for (CommitListItem li : list) {
			ContentStatus contentStatus = li.getStatus();
			File file = li.getPath();

			if (contentStatus.equals(ContentStatus.UNVERSIONED)) {
				hasNonVersioned = true;
			} else if (contentStatus.equals(ContentStatus.ADDED)) {
				hasAdded = true;
			} else if (contentStatus.equals(ContentStatus.DELETED)) {
				hasDeleted = true;
			} else if (contentStatus.equals(ContentStatus.MODIFIED)) {
				hasModified = true;
			}

			if (file.isFile()) {
				hasFiles = true;
			} else if (file.isDirectory()) {
				hasDirectories = true;
			}

		}

		btnSelectAll.setEnabled(!list.isEmpty());
		btnSelectNone.setEnabled(!list.isEmpty());
		btnSelectNonVersioned.setEnabled(hasNonVersioned);
		btnSelectAdded.setEnabled(hasAdded);
		btnSelectDeleted.setEnabled(hasDeleted);
		btnSelectModified.setEnabled(hasModified);
		btnSelectFiles.setEnabled(hasFiles);
		btnSelectDirectories.setEnabled(hasDirectories);

	}

	public void addItem(final File file, final ContentStatus contentStatus, final ContentStatus propertyStatus) throws Exception {
		if ((contentStatus.equals(ContentStatus.NORMAL) || contentStatus.equals(ContentStatus.NONE))
		        && (propertyStatus.equals(ContentStatus.NORMAL) || propertyStatus.equals(ContentStatus.NONE))) {
			return;
		}
		new OnSwing() {

			protected void process() throws Exception {
				tblCommit.hideMessage();
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

		}.run();
	}

	public void commitSelected() throws Exception {
		if (taMessage.getText().trim().length() < logMinSize) {
			MessagePane.showError(frame, "Cannot commit", "Message length must be at least " + logMinSize + "!");
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
					// auto-add unversioned items
					commit.add(li.getPath());
					// MessagePane.showWarning(frame, "Cannot commit",
					// "Cannot commit unversioned file! Please Add, Delete or Ignore it (or deselect it).");
					// return;
				}
			}
		}
		if (noCommit == 0) {
			MessagePane.showError(frame, "Nothing to commit", "Nothing is selected to commit");
			return;
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

	private void removeListItemIfNormal(CommitListItem li) {
		if (li.getStatus().equals(ContentStatus.NORMAL) && li.getPropertyStatus().equals(ContentStatus.NONE)
		        && li.getPropertyStatus().equals(ContentStatus.NORMAL)) {
			commitTableModel.removeLine(li);
			tblCommit.getSelectionModel().clearSelection();
		}
	}

	public void resolveConflict(CommitListItem li) throws Exception {

		File file = li.getPath();
		if (file.isDirectory()) {
			return;
		}
		SVNInfo info = Manager.getInfo(file.getPath());

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
			Manager.resolveConflictUsingMine(file.getPath());
			refresh();
		}
	}

	private List<CommitListItem> getSelectedItems() {
		ArrayList<CommitListItem> lstResult = new ArrayList<CommitListItem>();
		for (int row : tblCommit.getSelectedRows()) {
			lstResult.add(commitTableModel.getRow(tblCommit.convertRowIndexToModel(row)));
		}
		return lstResult;
	}

	private class AddAction extends ThreadAction {

		public AddAction(PopupupMouseListener popupupMouseListener) {
			super("Add");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			for (CommitListItem li : getSelectedItems()) {
				commit.add(li.getPath());
			}
			refresh();
		}
	}

	private class IgnoreAction extends ThreadAction {

		public IgnoreAction(PopupupMouseListener popupupMouseListener) {
			super("Ignore");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			for (CommitListItem li : getSelectedItems()) {
				commit.ignore(li.getPath());
				// the callback from Commit doesn't work, because it knows only
				// the
				// parent dir
				// changeToIgnore(li.getPath());
			}
			refresh();
		}
	}

	private class DeleteAction extends ThreadAction {

		public DeleteAction(PopupupMouseListener popupupMouseListener) {
			super("Delete");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			for (CommitListItem li : getSelectedItems()) {
				commit.delete(li.getPath());
				// changeToDeleted(li.getPath());
			}
			refresh();
		}
	}

	private class ShowChangesAction extends ThreadAction {

		public ShowChangesAction(PopupupMouseListener popupupMouseListener) {
			super("Show changes");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			for (CommitListItem li : getSelectedItems()) {
				commit.showChanges(li.getPath());
			}
		}
	}

	private class RevertChangesAction extends ThreadAction {

		public RevertChangesAction(PopupupMouseListener popupupMouseListener) {
			super("Revert changes");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			for (CommitListItem li : getSelectedItems()) {
				commit.revertChanges(li.getPath());
				// li.setStatus(ContentStatus.NORMAL);
			}
			// removeListItemIfNormal(li);
			refresh();
		}
	}

	private class RevertPropertyChangesAction extends ThreadAction {

		public RevertPropertyChangesAction(PopupupMouseListener popupupMouseListener) {
			super("Revert property changes");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			for (CommitListItem li : getSelectedItems()) {
				commit.revertPropertyChanges(li.getPath());
				// li.setPropertyStatus(ContentStatus.NORMAL);
			}
			// removeListItemIfNormal(li);
			refresh();
		}
	}

	private class ResolvedAction extends ThreadAction {

		public ResolvedAction(PopupupMouseListener popupupMouseListener) {
			super("Resolved");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			for (CommitListItem li : getSelectedItems()) {
				commit.resolved(li.getPath());
			}
			refresh();
		}
	}

	private class ShowLog extends ThreadAction {

		public ShowLog(PopupupMouseListener popupupMouseListener) {
			super("Show log");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			try {
				for (CommitListItem li : getSelectedItems()) {
					new Log(li.getPath().getPath()).execute();
				}
			} catch (Exception ex) {
				Manager.handle(ex);
			}
		}
	}

	private class ResolveConflictUsingTheirsAction extends ThreadAction {

		public ResolveConflictUsingTheirsAction(PopupupMouseListener popupupMouseListener) {
			super("Resolve conflict using theirs");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			for (CommitListItem li : getSelectedItems()) {
				Manager.resolveConflictUsingTheirs(li.getPath().getPath());
			}
			refresh();
		}
	}

	private class ResolveConflictUsingMineAction extends ThreadAction {

		public ResolveConflictUsingMineAction(PopupupMouseListener popupupMouseListener) {
			super("Resolve conflict using mine");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			for (CommitListItem li : getSelectedItems()) {
				Manager.resolveConflictUsingMine(li.getPath().getPath());
			}
			refresh();
		}
	}

	private class ResolveConflictAction extends ThreadAction {

		public ResolveConflictAction(PopupupMouseListener popupupMouseListener) {
			super("Resolve conflict");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			for (CommitListItem li : getSelectedItems()) {
				resolveConflict(li);
			}
		}
	}

	private class PopupupMouseListener extends MouseAdapter {

		private JPopupMenu ppVisible;

		public PopupupMouseListener() {}

		private void showPopup(MouseEvent e) {
			Point p = new Point(e.getX(), e.getY());
			int rowAtPoint = tblCommit.rowAtPoint(p);
			if (rowAtPoint == -1) {
				return;
			}
			int row = tblCommit.convertRowIndexToModel(rowAtPoint);

			boolean isSelected = false;
			for (int rowLi : tblCommit.getSelectedRows()) {
				if (rowLi == row) {
					isSelected = true;
					break;
				}
			}
			if (!isSelected) {
				tblCommit.getSelectionModel().setSelectionInterval(row, row);
			}

			HashSet<ContentStatus> setUsedStatus = new HashSet<ContentStatus>();
			HashSet<ContentStatus> setUsedPropertyStatus = new HashSet<ContentStatus>();
			int[] selectedRows = tblCommit.getSelectedRows();
			for (int rowLi : selectedRows) {
				CommitListItem li = commitTableModel.getRow(tblCommit.convertRowIndexToModel(rowLi));
				setUsedStatus.add(li.getStatus());
				setUsedPropertyStatus.add(li.getPropertyStatus());
			}

			JPopupMenu ppMixed = new JPopupMenu();
			boolean onlyOneKind = setUsedStatus.size() == 1;

			if (setUsedStatus.contains(ContentStatus.MODIFIED)) {
				ppMixed.add(new ShowChangesAction(this));
			}

			if (setUsedStatus.contains(ContentStatus.MODIFIED) && !setUsedStatus.contains(ContentStatus.UNVERSIONED)
			        && !setUsedStatus.contains(ContentStatus.ADDED)) {
				ppMixed.add(new ShowLog(this));
			}

			if ((setUsedStatus.contains(ContentStatus.MODIFIED) || setUsedStatus.contains(ContentStatus.ADDED))
			        && !setUsedStatus.contains(ContentStatus.UNVERSIONED)) {
				ppMixed.add(new RevertChangesAction(this));
			}

			ppMixed.add(new IgnoreAction(this));
			if (onlyOneKind && setUsedStatus.contains(ContentStatus.UNVERSIONED)) {
				ppMixed.add(new AddAction(this));
			}
			ppMixed.add(new DeleteAction(this));

			if (setUsedPropertyStatus.contains(ContentStatus.MODIFIED)) {
				ppMixed.add(new RevertPropertyChangesAction(this));
			}

			if (onlyOneKind && setUsedStatus.contains(ContentStatus.CONFLICTED)) {
				ppMixed.add(new ResolvedAction(this));
				ppMixed.add(new ResolveConflictUsingTheirsAction(this));
				ppMixed.add(new ResolveConflictUsingMineAction(this));
				ppMixed.add(new ResolveConflictAction(this));
			}

			ppVisible = ppMixed;
			ppVisible.setInvoker(tblCommit);
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

		public void mouseClicked(MouseEvent e) {
			if (e.getClickCount() == 2) {
				Point p = new Point(e.getX(), e.getY());
				int rowAtPoint = tblCommit.rowAtPoint(p);
				if (rowAtPoint == -1) {
					return;
				}
				int row = tblCommit.convertRowIndexToModel(rowAtPoint);
				CommitListItem selected = commitTableModel.getRow(row);
				ContentStatus status = selected.getStatus();
				ContentStatus propertyStatus = selected.getPropertyStatus();
				if (status.equals(ContentStatus.MODIFIED)) {
					new ShowChangesAction(this).actionPerformed(null);
				}
			}
		}
	}

	private abstract class AbstractSelectAction extends ThreadAction {

		public AbstractSelectAction(String string) {
			super(string);
		}

		public abstract boolean doSelect(CommitListItem li);

		public abstract boolean doUnSelect(CommitListItem li);

		public void actionProcess(ActionEvent e) throws Exception {
			for (CommitListItem li : commitTableModel.getAllData()) {
				if (doSelect(li)) {
					li.setSelected(true);
				} else if (doUnSelect(li)) {
					li.setSelected(false);
				}
			}
			new OnSwing() {

				protected void process() throws Exception {
					tblCommit.repaint();
				}

			}.run();

		}
	}

	private class SelectAllAction extends AbstractSelectAction {

		public SelectAllAction() {
			super("All");
		}

		public boolean doSelect(CommitListItem li) {
			return true;
		}

		public boolean doUnSelect(CommitListItem li) {
			return false;
		}

	}

	private class SelectNoneAction extends AbstractSelectAction {

		public SelectNoneAction() {
			super("None");
		}

		public boolean doSelect(CommitListItem li) {
			return false;
		}

		public boolean doUnSelect(CommitListItem li) {
			return true;
		}
	}

	private class SelectNonVersionedAction extends AbstractSelectAction {

		public SelectNonVersionedAction() {
			super("Non-versioned");
		}

		public boolean doSelect(CommitListItem li) {
			return li.getStatus().equals(ContentStatus.UNVERSIONED);
		}

		public boolean doUnSelect(CommitListItem li) {
			return false;
		}
	}

	private class SelectAddedAction extends AbstractSelectAction {

		public SelectAddedAction() {
			super("Added");
		}

		public boolean doSelect(CommitListItem li) {
			return li.getStatus().equals(ContentStatus.ADDED);
		}

		public boolean doUnSelect(CommitListItem li) {
			return false;
		}
	}

	private class SelectDeletedAction extends AbstractSelectAction {

		public SelectDeletedAction() {
			super("Deleted");
		}

		public boolean doSelect(CommitListItem li) {
			return li.getStatus().equals(ContentStatus.DELETED);
		}

		public boolean doUnSelect(CommitListItem li) {
			return false;
		}
	}

	private class SelectModifiedAction extends AbstractSelectAction {

		public SelectModifiedAction() {
			super("Modified");
		}

		public boolean doSelect(CommitListItem li) {
			return li.getStatus().equals(ContentStatus.MODIFIED);
		}

		public boolean doUnSelect(CommitListItem li) {
			return false;
		}
	}

	private class SelectFilesAction extends AbstractSelectAction {

		public SelectFilesAction() {
			super("Files");
		}

		public boolean doSelect(CommitListItem li) {
			return li.getPath().isFile();
		}

		public boolean doUnSelect(CommitListItem li) {
			return false;
		}
	}

	private class SelectDirectoriesAction extends AbstractSelectAction {

		public SelectDirectoriesAction() {
			super("Directories");
		}

		public boolean doSelect(CommitListItem li) {
			return li.getPath().isDirectory();
		}

		public boolean doUnSelect(CommitListItem li) {
			return false;
		}
	}
}
