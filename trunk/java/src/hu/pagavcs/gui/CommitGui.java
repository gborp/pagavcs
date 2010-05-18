package hu.pagavcs.gui;

import hu.pagavcs.bl.Manager;
import hu.pagavcs.bl.OnSwing;
import hu.pagavcs.bl.ThreadAction;
import hu.pagavcs.gui.platform.Frame;
import hu.pagavcs.gui.platform.GuiHelper;
import hu.pagavcs.gui.platform.Label;
import hu.pagavcs.gui.platform.MessagePane;
import hu.pagavcs.gui.platform.ProgressBar;
import hu.pagavcs.gui.platform.Table;
import hu.pagavcs.gui.platform.TableModel;
import hu.pagavcs.gui.platform.TextArea;
import hu.pagavcs.operation.Commit;
import hu.pagavcs.operation.ContentStatus;
import hu.pagavcs.operation.Log;
import hu.pagavcs.operation.ResolveConflict;
import hu.pagavcs.operation.Commit.CommitStatus;
import hu.pagavcs.operation.Commit.CommittedItemStatus;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
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
public class CommitGui implements Working, Refreshable {

	private HashMap<File, List<CommitListItem>> mapDeletedHiddenFiles;

	private Frame                               frame;
	private Table<CommitListItem>               tblCommit;
	private TableModel<CommitListItem>          tmdlCommit;
	private Commit                              commit;
	private JButton                             btnStop;
	private TextArea                            taMessage;
	private JButton                             btnCommit;
	private Label                               lblUrl;
	private ProgressBar                         prgWorkinProgress;
	private Label                               lblInfo;
	private int                                 noCommit;
	private boolean                             preRealCommitProcess;
	private JButton                             btnRefresh;
	private JComboBox                           cboMessage;
	private int                                 logMinSize;
	private Label                               lblWorkingCopy;
	private JButton                             btnSelectAllNone;
	private JButton                             btnSelectNonVersioned;
	private JButton                             btnSelectAdded;
	private JButton                             btnSelectDeleted;
	private JButton                             btnSelectModified;
	private JButton                             btnSelectFiles;
	private JButton                             btnSelectDirectories;
	private JButton                             btnCreatePatch;
	private JButton                             btnSelectDeselectSelected;

	public CommitGui(Commit commit) {
		this.commit = commit;
	}

	public void display() throws SVNException {
		logMinSize = 0;
		CellConstraints cc = new CellConstraints();
		tmdlCommit = new TableModel<CommitListItem>(new CommitListItem());
		tblCommit = new Table<CommitListItem>(tmdlCommit);
		tblCommit.addMouseListener(new PopupupMouseListener());
		tblCommit.setRowSorter(new TableRowSorter<TableModel<CommitListItem>>(tmdlCommit));
		tblCommit.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		tblCommit.addKeyListener(new KeyAdapter() {

			public void keyTyped(KeyEvent e) {
				if (e.getKeyChar() == ' ') {
					btnSelectDeselectSelected.doClick();
				}
			}

		});
		tblCommit.getModel().addTableModelListener(new SelectDeselectListener());
		new StatusCellRendererForCommitListItem(tblCommit);
		JScrollPane spCommitList = new JScrollPane(tblCommit);

		taMessage = new TextArea();
		taMessage.setLineWrap(true);
		taMessage.setWrapStyleWord(true);
		JScrollPane spMessage = new JScrollPane(taMessage);

		JSplitPane splMain = new JSplitPane(JSplitPane.VERTICAL_SPLIT, spMessage, spCommitList);
		splMain.setPreferredSize(new Dimension(40, 40));
		lblUrl = new Label();
		lblWorkingCopy = new Label();
		cboMessage = new JComboBox();
		cboMessage.setPreferredSize(new Dimension(1, (int) cboMessage.getPreferredSize().getHeight()));
		cboMessage.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					if (e.getItem() != null) {
						taMessage.setText(((RecentMessageSlot) e.getItem()).message);
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

		btnStop = new JButton(new StopAction());
		prgWorkinProgress = new ProgressBar(this);

		lblInfo = new Label();
		btnCreatePatch = new JButton(new CreatePatchAction());
		btnCreatePatch.setEnabled(false);
		btnCommit = new JButton(new CommitAction());
		btnCommit.setEnabled(false);
		btnRefresh = new JButton(new RefreshAction());
		btnRefresh.setEnabled(false);

		btnSelectAllNone = new JButton(new SelectAllNoneAction());
		btnSelectNonVersioned = new JButton(new SelectNonVersionedAction());
		btnSelectAdded = new JButton(new SelectAddedAction());
		btnSelectDeleted = new JButton(new SelectDeletedAction());
		btnSelectModified = new JButton(new SelectModifiedAction());
		btnSelectFiles = new JButton(new SelectFilesAction());
		btnSelectDirectories = new JButton(new SelectDirectoriesAction());
		btnSelectDeselectSelected = new JButton(new SelectDeselectSelectedAction());

		JPanel pnlCheck = new JPanel(new FormLayout("p,4dlu,p,4dlu,p,4dlu,p,4dlu,p,4dlu,p", "p,4dlu,p"));
		pnlCheck.add(new JLabel("Check:"), cc.xy(1, 1));
		pnlCheck.add(btnSelectAllNone, cc.xy(3, 1));
		pnlCheck.add(btnSelectNonVersioned, cc.xy(5, 1));
		pnlCheck.add(btnSelectAdded, cc.xy(7, 1));
		pnlCheck.add(btnSelectDeleted, cc.xy(9, 1));
		pnlCheck.add(btnSelectModified, cc.xy(3, 3));
		pnlCheck.add(btnSelectFiles, cc.xy(5, 3));
		pnlCheck.add(btnSelectDirectories, cc.xy(7, 3));
		pnlCheck.add(btnSelectDeselectSelected, cc.xy(9, 3));

		JPanel pnlBottom = new JPanel(new FormLayout("p,4dlu, p:g, 4dlu,p, 4dlu,p, 4dlu,p", "p,4dlu,p"));

		pnlBottom.add(pnlCheck, cc.xywh(1, 1, 7, 1, CellConstraints.LEFT, CellConstraints.DEFAULT));
		pnlBottom.add(btnRefresh, cc.xy(1, 3));
		pnlBottom.add(prgWorkinProgress, cc.xy(3, 3));
		pnlBottom.add(btnCreatePatch, cc.xy(5, 3));
		pnlBottom.add(btnStop, cc.xy(7, 3));
		pnlBottom.add(btnCommit, cc.xy(9, 3));

		JPanel pnlMain = new JPanel(new BorderLayout());
		pnlMain.add(pnlTop, BorderLayout.NORTH);
		pnlMain.add(splMain, BorderLayout.CENTER);
		pnlMain.add(pnlBottom, BorderLayout.SOUTH);

		frame = GuiHelper.createAndShowFrame(pnlMain, "Commit", "/hu/pagavcs/resources/commit-app-icon.png");
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

	public synchronized void workStarted() throws Exception {
		new OnSwing() {

			protected void process() throws Exception {
				prgWorkinProgress.startProgress();
			}
		}.run();
	}

	public synchronized void workEnded() throws Exception {
		new OnSwing() {

			protected void process() throws Exception {
				prgWorkinProgress.stopProgress();
			}
		}.run();
	}

	public void setUrlLabel(String urlLabel) {
		lblUrl.setText(urlLabel);
	}

	public void setPath(String path) {
		lblWorkingCopy.setText(path);
		frame.setTitlePrefix(path);
	}

	public void setRecentMessages(String[] recentMessages) {
		List<RecentMessageSlot> lstRecentMessageSlot = new ArrayList<RecentMessageSlot>();
		for (String str : recentMessages) {
			RecentMessageSlot li = new RecentMessageSlot(str);
			lstRecentMessageSlot.add(li);
		}
		ComboBoxModel modelUrl = new DefaultComboBoxModel(lstRecentMessageSlot.toArray());
		cboMessage.setModel(modelUrl);
	}

	public void setStatus(final CommitStatus status, final String message) throws Exception {
		new OnSwing() {

			protected void process() throws Exception {

				if (CommitStatus.FILE_LIST_GATHERING_COMPLETED.equals(status)) {

					// remove unversioned files of conflicted items
					HashSet<File> setRemoveUnversioned = new HashSet<File>();
					List<CommitListItem> lstData = tmdlCommit.getAllData();
					for (CommitListItem li : lstData) {
						if (li.getStatus().equals(ContentStatus.CONFLICTED)) {
							SVNInfo info = Manager.getInfo(li.getPath());
							setRemoveUnversioned.add(info.getConflictNewFile());
							setRemoveUnversioned.add(info.getConflictOldFile());
							setRemoveUnversioned.add(info.getConflictWrkFile());
						}
					}

					if (!setRemoveUnversioned.isEmpty()) {
						for (CommitListItem li : new ArrayList<CommitListItem>(lstData)) {
							if (li.getStatus().equals(ContentStatus.UNVERSIONED) && setRemoveUnversioned.contains(li.getPath())) {
								tmdlCommit.removeLine(li);
							}
						}
					}

					workEnded();
					refreshSelectButtons();

					prgWorkinProgress.setIndeterminate(false);
					btnStop.setEnabled(false);
					btnCommit.setEnabled(true);
					btnCreatePatch.setEnabled(true);
					btnRefresh.setEnabled(true);
					if (tmdlCommit.getAllData().isEmpty()) {
						tblCommit.showMessage("There is nothing to commit", Manager.ICON_WARNING);
					}
				}
				if (CommitStatus.INIT.equals(status)) {
					workStarted();
					tblCommit.showMessage("Working...", Manager.ICON_INFORMATION);

					btnSelectAllNone.setEnabled(false);
					btnSelectNonVersioned.setEnabled(false);
					btnSelectAdded.setEnabled(false);
					btnSelectDeleted.setEnabled(false);
					btnSelectModified.setEnabled(false);
					btnSelectFiles.setEnabled(false);
					btnSelectDirectories.setEnabled(false);

					mapDeletedHiddenFiles = new HashMap<File, List<CommitListItem>>();

				} else if (CommitStatus.COMMIT_COMPLETED.equals(status)) {
					MessagePane.showInfo(frame, "Completed", getCommitNotifyMessage(message));
					frame.setVisible(false);
					frame.dispose();
				} else if (CommitStatus.COMMIT_FAILED.equals(status)) {
					MessagePane.showError(frame, "Failed!", "Commit failed!");
					frame.setVisible(false);
					frame.dispose();
				} else if (CommitStatus.CANCEL.equals(status)) {
					MessagePane.showInfo(frame, "Cancelled!", "Commit cancelled!");

					tblCommit.setEnabled(true);
					btnCommit.setEnabled(true);
					prgWorkinProgress.setValue(0);
					prgWorkinProgress.setIndeterminate(false);
					preRealCommitProcess = false;
				}
			}

		}.run();
	}

	private String getCommitNotifyMessage(String revision) {
		String messageTemplates = Manager.getSettings().getCommitCompletedMessageTemplates();
		if (messageTemplates != null) {
			String sep = Manager.COMMIT_COMPLETED_TEMPLATE_SEPARATOR;
			String path = commit.getPath();
			for (String tmpl : messageTemplates.split("\n")) {
				int sepIndex = tmpl.indexOf(sep);
				if (sepIndex != -1) {
					String pattern = tmpl.substring(0, sepIndex);
					String template = tmpl.substring(sepIndex + sep.length());
					if (path.contains(pattern)) {
						return MessageFormat.format(template, revision);
					}
				}
			}
		}
		return "Revision: " + revision;
	}

	public void refresh() throws Exception {
		new OnSwing() {

			protected void process() throws Exception {
				workStarted();
				tmdlCommit.clear();
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

		List<CommitListItem> list = tmdlCommit.getAllData();

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

		btnSelectAllNone.setEnabled(true);
		btnSelectNonVersioned.setEnabled(hasNonVersioned);
		btnSelectAdded.setEnabled(hasAdded);
		btnSelectDeleted.setEnabled(hasDeleted);
		btnSelectModified.setEnabled(hasModified);
		btnSelectFiles.setEnabled(hasFiles);
		btnSelectDirectories.setEnabled(hasDirectories);
	}

	private boolean isParentDeleted(CommitListItem li) {
		File parent = li.getPath().getParentFile();
		while (parent != null && !mapDeletedHiddenFiles.containsKey(parent)) {
			parent = parent.getParentFile();
		}
		if (mapDeletedHiddenFiles.containsKey(parent)) {
			mapDeletedHiddenFiles.get(parent).add(li);
			return true;
		}

		return false;
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
				if (!isParentDeleted(li)) {
					if (contentStatus.equals(ContentStatus.DELETED) && file.isDirectory()) {
						mapDeletedHiddenFiles.put(file, new ArrayList<CommitListItem>());
					}
					tmdlCommit.addLine(li);
					tblCommit.followScrollToNewItems();
				}
			}

		}.run();
	}

	public void addCommittedItem(String fileName, CommittedItemStatus itemStatus) {
		if (preRealCommitProcess) {
			prgWorkinProgress.setIndeterminate(false);
			prgWorkinProgress.getModel().setMaximum(noCommit + 1);
			prgWorkinProgress.setValue(1);
			preRealCommitProcess = false;
		}

		if (itemStatus.equals(CommittedItemStatus.ADDED) || itemStatus.equals(CommittedItemStatus.DELETED) || itemStatus.equals(CommittedItemStatus.MODIFIED)
		        || itemStatus.equals(CommittedItemStatus.REPLACED)) {
			prgWorkinProgress.setStringPainted(true);
			prgWorkinProgress.setString(fileName);
		}

		if (itemStatus.equals(CommittedItemStatus.DELTA_SENT)) {
			prgWorkinProgress.setValue(prgWorkinProgress.getValue() + 1);
			prgWorkinProgress.setString("");
		}

		if (itemStatus.equals(CommittedItemStatus.COMPLETED)) {
			prgWorkinProgress.setStringPainted(false);
			lblInfo.setText(fileName);
		}
	}

	public void resolveConflict(CommitListItem li) throws Exception {

		File file = li.getPath();
		if (file.isDirectory()) {
			MessagePane.showError(frame, "Cannot resolve conflict", "Cannot resolve conflict on directory");
			return;
		}
		new ResolveConflict(this, file.getPath(), false).execute();
	}

	/**
	 * @return Creates a new list of select list items
	 */
	private List<CommitListItem> getSelectedItems() {
		ArrayList<CommitListItem> lstResult = new ArrayList<CommitListItem>();
		for (int row : tblCommit.getSelectedRows()) {
			lstResult.add(tmdlCommit.getRow(tblCommit.convertRowIndexToModel(row)));
		}
		return lstResult;
	}

	private class CreatePatchAction extends ThreadAction {

		public CreatePatchAction() {
			super("Create patch");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			noCommit = 0;
			ArrayList<File> lstCommit = new ArrayList<File>();
			for (CommitListItem li : tmdlCommit.getAllData()) {
				if (li.isSelected()) {
					lstCommit.add(li.getPath());
					noCommit++;

					if (li.getStatus().equals(ContentStatus.CONFLICTED)) {
						int modelRowIndex = tmdlCommit.getAllData().indexOf(li);
						tblCommit.scrollRectToVisible(tblCommit.getCellRect(tblCommit.convertRowIndexToView(modelRowIndex), 0, true));
						MessagePane.showError(frame, "Cannot create patch", "Cannot create patch from conflicted file! Please resolve the conflict first.");
						return;
					}
				}
			}
			if (noCommit == 0) {
				MessagePane.showError(frame, "Cannot create patch", "Nothing is selected for creating patch");
				return;
			}

			JFileChooser fc = new JFileChooser(new File(commit.getPath()));
			fc.setFileSelectionMode(JFileChooser.FILES_ONLY);

			int choosed = fc.showSaveDialog(frame);

			if (choosed == JFileChooser.APPROVE_OPTION) {
				File file = fc.getSelectedFile();
				if (file.getName().indexOf('.') == -1) {
					file = new File(file.getAbsolutePath() + ".patch");
				}

				OutputStream out = new FileOutputStream(file);
				commit.createPatch(lstCommit.toArray(new File[0]), out);
			}
		}
	}

	private class CommitAction extends ThreadAction {

		public CommitAction() {
			super("Commit");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			workStarted();
			if (taMessage.getText().trim().length() < logMinSize) {
				MessagePane.showError(frame, "Cannot commit", "Message length must be at least " + logMinSize + "!");
				return;
			}

			Manager.getSettings().addCommitMessageForHistory(taMessage.getText().trim());

			noCommit = 0;
			ArrayList<File> lstCommit = new ArrayList<File>();
			for (CommitListItem li : tmdlCommit.getAllData()) {
				if (li.isSelected()) {

					if (li.getStatus().equals(ContentStatus.UNVERSIONED)) {
						// auto-add unversioned items
						commit.add(li.getPath(), false);
					} else if (li.getStatus().equals(ContentStatus.CONFLICTED)) {
						int modelRowIndex = tmdlCommit.getAllData().indexOf(li);
						tblCommit.scrollRectToVisible(tblCommit.getCellRect(tblCommit.convertRowIndexToView(modelRowIndex), 0, true));
						MessagePane.showError(frame, "Cannot commit", "Cannot commit conflicted file! Please resolve the conflict first.");
						return;
					}

					lstCommit.add(li.getPath());
					noCommit++;
					if (ContentStatus.DELETED.equals(li.getStatus())) {
						List<CommitListItem> lstHiddenDeletedFiles = mapDeletedHiddenFiles.get(li);
						if (lstHiddenDeletedFiles != null) {
							for (CommitListItem liHidden : lstHiddenDeletedFiles) {
								lstCommit.add(liHidden.getPath());
								noCommit++;
							}
						}
					}
				}
			}
			if (noCommit == 0) {
				MessagePane.showError(frame, "Cannot commit", "Nothing is selected to commit");
				return;
			}

			tblCommit.setEnabled(false);
			btnCommit.setEnabled(false);
			prgWorkinProgress.setValue(0);
			prgWorkinProgress.setIndeterminate(true);
			preRealCommitProcess = true;
			workEnded();
			commit.doCommit(lstCommit, taMessage.getText());
		}
	}

	private class RefreshAction extends ThreadAction {

		public RefreshAction() {
			super("Refresh");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			refresh();
		}
	}

	private class StopAction extends ThreadAction {

		public StopAction() {
			super("Stop");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			new OnSwing() {

				protected void process() throws Exception {
					btnStop.setEnabled(false);
				}

			}.run();
			commit.setCancel(true);
		}
	}

	private class AddAction extends ThreadAction {

		public AddAction() {
			super("Add");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			workStarted();

			boolean hasDirectory = false;
			for (CommitListItem li : getSelectedItems()) {
				if (li.getPath().isDirectory()) {
					hasDirectory = true;
					break;
				}
			}

			boolean addRecursively = false;
			if (hasDirectory) {
				int answer = JOptionPane.showConfirmDialog(frame, "Do you want to add recursively?");
				if (answer == JOptionPane.CANCEL_OPTION) {
					workEnded();
					return;
				} else {
					if (answer == JOptionPane.YES_OPTION) {
						addRecursively = true;
					}
				}
			}

			for (CommitListItem li : getSelectedItems()) {
				commit.add(li.getPath(), addRecursively);
			}
			refresh();
			workEnded();
		}
	}

	private class IgnoreAction extends ThreadAction {

		public IgnoreAction() {
			super("Ignore");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			workStarted();
			for (CommitListItem li : getSelectedItems()) {
				commit.ignore(li.getPath());
				// the callback from Commit doesn't work, because it knows only
				// the
				// parent dir
				// changeToIgnore(li.getPath());
			}
			refresh();
			workEnded();
		}
	}

	private class DeleteAction extends ThreadAction {

		public DeleteAction() {
			super("Delete");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			workStarted();
			for (CommitListItem li : getSelectedItems()) {
				commit.delete(li.getPath());
				// changeToDeleted(li.getPath());
			}
			refresh();
			workEnded();
		}
	}

	private class ShowChangesAction extends ThreadAction {

		public ShowChangesAction(PopupupMouseListener popupupMouseListener) {
			super("Show changes");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			for (CommitListItem li : getSelectedItems()) {
				commit.showChangesFromBase(li.getPath());
			}
		}
	}

	private class RevertChangesAction extends ThreadAction {

		public RevertChangesAction() {
			super("Revert changes");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			workStarted();
			List<CommitListItem> lstFiles = getSelectedItems();

			Collections.sort(lstFiles, new Comparator<CommitListItem>() {

				public int compare(CommitListItem o1, CommitListItem o2) {
					return o2.getPath().getAbsolutePath().compareTo(o1.getPath().getAbsolutePath());
				}
			});

			for (CommitListItem li : lstFiles) {
				commit.revertChanges(li.getPath());
			}
			refresh();
			workEnded();
		}
	}

	private class ShowPropertyChangesAction extends ThreadAction {

		public ShowPropertyChangesAction() {
			super("Show property changes");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			for (CommitListItem li : getSelectedItems()) {
				commit.showPropertyChangesFromBase(li.getPath());
			}
		}
	}

	private class RevertPropertyChangesAction extends ThreadAction {

		public RevertPropertyChangesAction() {
			super("Revert property changes");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			workStarted();
			for (CommitListItem li : getSelectedItems()) {
				commit.revertPropertyChanges(li.getPath());
			}
			workEnded();
			refresh();
		}
	}

	private class ShowLog extends ThreadAction {

		public ShowLog() {
			super("Show log");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			workStarted();
			try {
				for (CommitListItem li : getSelectedItems()) {
					new Log(li.getPath().getPath()).execute();
				}
			} catch (Exception ex) {
				Manager.handle(ex);
			}
			workEnded();
		}
	}

	private class ResolveConflictUsingTheirsAction extends ThreadAction {

		public ResolveConflictUsingTheirsAction() {
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

		public ResolveConflictUsingMineAction() {
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

		public ResolveConflictAction() {
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
			int row = tblCommit.rowAtPoint(p);
			if (row == -1) {
				return;
			}

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
				CommitListItem li = tmdlCommit.getRow(tblCommit.convertRowIndexToModel(rowLi));
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
				ppMixed.add(new ShowLog());
			}

			if ((setUsedStatus.contains(ContentStatus.MODIFIED) || setUsedStatus.contains(ContentStatus.ADDED) || setUsedStatus.contains(ContentStatus.MISSING) || setUsedStatus
			        .contains(ContentStatus.DELETED))
			        && !setUsedStatus.contains(ContentStatus.UNVERSIONED)) {
				ppMixed.add(new RevertChangesAction());
			}

			ppMixed.add(new IgnoreAction());
			if (onlyOneKind && setUsedStatus.contains(ContentStatus.UNVERSIONED)) {
				ppMixed.add(new AddAction());
			}
			ppMixed.add(new DeleteAction());

			if (setUsedPropertyStatus.contains(ContentStatus.MODIFIED)) {
				ppMixed.add(new ShowPropertyChangesAction());
				ppMixed.add(new RevertPropertyChangesAction());
			}

			if (onlyOneKind && setUsedStatus.contains(ContentStatus.CONFLICTED)) {
				ppMixed.add(new ResolveConflictUsingTheirsAction());
				ppMixed.add(new ResolveConflictUsingMineAction());
				ppMixed.add(new ResolveConflictAction());
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
				int row = tblCommit.rowAtPoint(p);
				if (row == -1) {
					return;
				}
				CommitListItem selected = tmdlCommit.getRow(row);
				ContentStatus status = selected.getStatus();
				// ContentStatus propertyStatus = selected.getPropertyStatus();
				if (status.equals(ContentStatus.MODIFIED)) {
					new ShowChangesAction(this).actionPerformed(null);
				}
			}
		}
	}

	private abstract class AbstractSelectAction extends ThreadAction {

		private boolean hasSelected;

		public AbstractSelectAction(String string) {
			super(string);
		}

		public boolean hasSelected() {
			return hasSelected;
		}

		public abstract boolean doSelect(CommitListItem li);

		public abstract boolean doUnSelect(CommitListItem li);

		public void actionProcess(ActionEvent e) throws Exception {
			hasSelected = false;
			for (CommitListItem li : tmdlCommit.getAllData()) {
				if (li.isSelected()) {
					hasSelected = true;
					break;
				}
			}

			for (CommitListItem li : tmdlCommit.getAllData()) {
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

	private class SelectAllNoneAction extends AbstractSelectAction {

		public SelectAllNoneAction() {
			super("-+All");
		}

		public boolean doSelect(CommitListItem li) {
			if (!hasSelected()) {
				return true;
			} else {
				return false;
			}
		}

		public boolean doUnSelect(CommitListItem li) {
			if (hasSelected()) {
				return true;
			} else {
				return false;
			}
		}
	}

	private class SelectNonVersionedAction extends AbstractSelectAction {

		public SelectNonVersionedAction() {
			super("+Non-versioned");
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
			super("+Added");
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
			super("+Deleted");
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
			super("+Modified");
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
			super("+Files");
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
			super("+Dirs");
		}

		public boolean doSelect(CommitListItem li) {
			return li.getPath().isDirectory();
		}

		public boolean doUnSelect(CommitListItem li) {
			return false;
		}
	}

	private class SelectDeselectSelectedAction extends ThreadAction {

		public SelectDeselectSelectedAction() {
			super("-+Selected");
		}

		public void actionProcess(ActionEvent e) throws Exception {

			boolean hasSelected = false;
			for (CommitListItem li : getSelectedItems()) {
				if (li.isSelected()) {
					hasSelected = true;
					break;
				}
			}
			boolean selectThem;
			if (hasSelected) {
				selectThem = false;
			} else {
				selectThem = true;
			}
			for (CommitListItem li : getSelectedItems()) {
				li.setSelected(selectThem);
			}

			new OnSwing() {

				protected void process() throws Exception {
					tblCommit.repaint();
				}

			}.run();
		}

	}

	private class RecentMessageSlot {

		private static final int SHORT_MESSAGE_LENGTH = 64;
		private final String     message;
		private String           shortMessage;

		public RecentMessageSlot(String message) {
			this.message = message;
			shortMessage = message;
			if (shortMessage != null && shortMessage.length() > SHORT_MESSAGE_LENGTH) {
				shortMessage = shortMessage.substring(0, SHORT_MESSAGE_LENGTH) + "...";
			}
			if (shortMessage == null) {
				shortMessage = "";
			}
		}

		public String toString() {
			return shortMessage;
		}
	}

	private class SelectDeselectListener implements TableModelListener {

		private boolean checkAddedSelected(CommitListItem li) {
			File parent = li.getPath().getParentFile();
			boolean changed = false;
			for (CommitListItem li2 : tblCommit.getModel().getAllData()) {
				if (parent.equals(li2.getPath())) {
					if (ContentStatus.DELETED.equals(li2.getStatus())) {
						li.setSelected(false);
						changed = true;
						break;
					} else if (ContentStatus.IGNORED.equals(li2.getStatus())) {
						li.setSelected(false);
						changed = true;
						break;
					} else if (ContentStatus.ADDED.equals(li2.getStatus()) && !li2.isSelected()) {
						li2.setSelected(true);
						checkStatusSelected(li2);
						changed = true;
						break;
					}
				}
			}

			return changed;
		}

		private boolean checkAddedDeSelected(CommitListItem li) {
			boolean changed = false;
			for (CommitListItem li2 : tblCommit.getModel().getAllData()) {
				if (li.getPath().equals(li2.getPath().getParentFile())) {
					if (li2.isSelected()) {
						li2.setSelected(false);
						checkAddedDeSelected(li2);
						changed = true;
					}
				}
			}

			return changed;
		}

		private boolean checkStatusSelected(CommitListItem li) {
			boolean changed = false;
			if (ContentStatus.MISSING.equals(li.getStatus())) {
				li.setSelected(false);
				changed = true;
			} else if (ContentStatus.ADDED.equals(li.getStatus())) {
				changed = checkAddedSelected(li);
			}
			return changed;
		}

		private boolean checkStatusDeselected(CommitListItem li) {
			boolean changed = false;
			if (ContentStatus.ADDED.equals(li.getStatus())) {
				changed = checkAddedDeSelected(li);
			}
			return changed;
		}

		public void tableChanged(TableModelEvent e) {
			if (e.getColumn() == 0) {

				boolean changed = false;
				for (CommitListItem li : getSelectedItems()) {
					if (li.isSelected()) {
						changed |= checkStatusSelected(li);
					} else {
						changed |= checkStatusDeselected(li);
					}
				}

				if (changed) {
					tblCommit.repaint();
				}
			}

			int selectedCount = 0;
			for (CommitListItem li : tblCommit.getModel().getAllData()) {
				if (li.isSelected()) {
					selectedCount++;
				}
			}

			prgWorkinProgress.setStringPainted(true);
			prgWorkinProgress.setString("Selected items: " + selectedCount);

		}
	}
}
