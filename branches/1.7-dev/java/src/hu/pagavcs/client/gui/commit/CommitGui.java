package hu.pagavcs.client.gui.commit;

import hu.pagavcs.client.bl.Manager;
import hu.pagavcs.client.bl.OnSwing;
import hu.pagavcs.client.bl.OnSwingWait;
import hu.pagavcs.client.bl.PagaException;
import hu.pagavcs.client.bl.SettingsStore;
import hu.pagavcs.client.bl.ThreadAction;
import hu.pagavcs.client.gui.CommitListItem;
import hu.pagavcs.client.gui.Refreshable;
import hu.pagavcs.client.gui.StatusCellRendererForCommitListItem;
import hu.pagavcs.client.gui.Working;
import hu.pagavcs.client.gui.platform.DotTextCellRenderer;
import hu.pagavcs.client.gui.platform.Frame;
import hu.pagavcs.client.gui.platform.GuiHelper;
import hu.pagavcs.client.gui.platform.Label;
import hu.pagavcs.client.gui.platform.MessagePane;
import hu.pagavcs.client.gui.platform.MessagePane.OPTIONS;
import hu.pagavcs.client.gui.platform.ProgressBar;
import hu.pagavcs.client.gui.platform.Table;
import hu.pagavcs.client.gui.platform.TableModel;
import hu.pagavcs.client.gui.platform.TextArea;
import hu.pagavcs.client.operation.Commit;
import hu.pagavcs.client.operation.Commit.CommitStatus;
import hu.pagavcs.client.operation.Commit.CommittedItemStatus;
import hu.pagavcs.client.operation.ContentStatus;
import hu.pagavcs.client.operation.Log;
import hu.pagavcs.client.operation.MergeOperation;
import hu.pagavcs.client.operation.ResolveConflict;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Window;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.prefs.BackingStoreException;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableRowSorter;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
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

	private Frame frame;
	private String path;
	private Table<CommitListItem> tblCommit;
	private TableModel<CommitListItem> tmdlCommit;
	private Commit commit;
	private JButton btnStop;
	private TextArea taMessage;
	private JButton btnCommit;
	private Label lblUrl;
	private ProgressBar prgWorkinProgress;
	private Label lblInfo;
	private int noCommit;
	private boolean preRealCommitProcess;
	private JButton btnRefresh;
	private JComboBox cboMessage;
	private int logMinSize;
	private Label lblWorkingCopy;
	private JCheckBox btnSelectAllNone;
	private Label lblSelectedInfo;
	private JButton btnCreatePatch;
	private JCheckBox cbHelpMerge;
	private String url;
	private Semaphore refreshFinished;

	private DotTextCellRenderer pathCellRenderer;

	public CommitGui(Commit commit) {
		this.commit = commit;
		refreshFinished = new Semaphore(1, true);
	}

	public void display() throws SVNException {
		logMinSize = 0;
		CellConstraints cc = new CellConstraints();
		tmdlCommit = new TableModel<CommitListItem>(new CommitListItem());
		tblCommit = new Table<CommitListItem>(tmdlCommit);
		tblCommit.addMouseListener(new PopupupMouseListener());
		tblCommit.setRowSorter(new TableRowSorter<TableModel<CommitListItem>>(tmdlCommit));
		tblCommit.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		tblCommit.addKeyListener(new SelectDeselectSelectedKeyListener());
		tblCommit.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		pathCellRenderer = new DotTextCellRenderer();
		tblCommit.getColumnModel().getColumn(3).setCellRenderer(new StatusCellRendererForCommitListItem(tblCommit, pathCellRenderer));

		SelectDeselectListener selectDeselectListener = new SelectDeselectListener();
		tmdlCommit.addTableModelListener(selectDeselectListener);

		tblCommit.getSelectionModel().addListSelectionListener(selectDeselectListener);

		new StatusCellRendererForCommitListItem(tblCommit);
		final JScrollPane spCommitList = new JScrollPane(tblCommit);
		spCommitList.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

		taMessage = new TextArea();
		taMessage.setLineWrap(true);
		taMessage.setWrapStyleWord(true);
		JScrollPane spMessage = new JScrollPane(taMessage);

		JSplitPane splMain = new JSplitPane(JSplitPane.VERTICAL_SPLIT, spMessage, spCommitList);
		splMain.setPreferredSize(new Dimension(300, 300));
		lblUrl = new Label();
		lblWorkingCopy = new Label();
		cboMessage = new JComboBox();
		cboMessage.setPreferredSize(new Dimension(100, (int) cboMessage.getPreferredSize().getHeight()));
		cboMessage.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					if (e.getItem() != null) {
						taMessage.setText(((RecentMessageSlot) e.getItem()).message);
					}
				}
			}
		});

		JPanel pnlTop = new JPanel(new FormLayout("r:p,2dlu,p:g", "p,2dlu,p,2dlu,p,2dlu"));
		pnlTop.add(new Label("Commit to:"), cc.xy(1, 1));
		pnlTop.add(lblUrl, cc.xy(3, 1));
		pnlTop.add(new Label("Working copy:"), cc.xy(1, 3));
		pnlTop.add(lblWorkingCopy, cc.xy(3, 3));
		pnlTop.add(new Label("Recent messages:"), cc.xy(1, 5));
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

		btnSelectAllNone = new JCheckBox(new SelectAllNoneAction());
		lblSelectedInfo = new Label();
		cbHelpMerge = new JCheckBox("Merge too");

		JPanel pnlCheck = new JPanel(new FormLayout("p,2dlu:g,p,2dlu,p", "p"));
		pnlCheck.add(btnSelectAllNone, cc.xy(1, 1));
		pnlCheck.add(lblSelectedInfo, cc.xy(3, 1));
		pnlCheck.add(cbHelpMerge, cc.xy(5, 1));

		JPanel pnlBottom = new JPanel(new FormLayout("p,2dlu, 50dlu:g, 2dlu,p, 2dlu,p, 2dlu,p", "p,2dlu,p"));

		pnlBottom.add(pnlCheck, cc.xywh(1, 1, 9, 1, CellConstraints.FILL, CellConstraints.DEFAULT));
		pnlBottom.add(btnRefresh, cc.xy(1, 3));
		pnlBottom.add(prgWorkinProgress, cc.xy(3, 3));
		pnlBottom.add(btnCreatePatch, cc.xy(5, 3));
		pnlBottom.add(btnStop, cc.xy(7, 3));
		pnlBottom.add(btnCommit, cc.xy(9, 3));

		FormLayout lyMain = new FormLayout("p:g", "p,2dlu,f:max(80dlu;p):g,2dlu,p");
		JPanel pnlMain = new JPanel(lyMain);
		pnlMain.add(pnlTop, cc.xy(1, 1));
		pnlMain.add(splMain, cc.xy(1, 3));
		pnlMain.add(pnlBottom, cc.xy(1, 5));

		frame = GuiHelper.createAndShowFrame(pnlMain, "Commit", "commit-app-icon.png", false);
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

	public void setUrlLabel(String url) {
		this.url = url;
		lblUrl.setText(url);
	}

	public void setPath(String path) {
		this.path = path;

		pathCellRenderer.setTruncatePrefix(path);

		lblWorkingCopy.setText(path);
		frame.setTitlePrefix(path);
		// it's executed last, so it's enough to call the pack only here.
		frame.packLater();
	}

	public void setRecentMessages(String[] recentMessages) {
		List<RecentMessageSlot> lstRecentMessageSlot = new ArrayList<RecentMessageSlot>();
		for (String str : recentMessages) {
			RecentMessageSlot li = new RecentMessageSlot(str);
			lstRecentMessageSlot.add(li);
		}
		ComboBoxModel modelUrl = new DefaultComboBoxModel(lstRecentMessageSlot.toArray());
		cboMessage.setModel(modelUrl);
		cboMessage.setPreferredSize(new Dimension(10, (int) cboMessage.getPreferredSize().getHeight()));
	}

	public void refreshSelectedInfo() {
		int selectedCount = 0;
		List<CommitListItem> lstAllData = tblCommit.getModel().getAllData();
		for (CommitListItem li : lstAllData) {
			if (li.isSelected()) {
				selectedCount++;
			}
		}
		lblSelectedInfo.setText(selectedCount + "/" + lstAllData.size() + " selected");
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

					refreshSelectedInfo();

					workEnded();

					btnSelectAllNone.setEnabled(true);

					prgWorkinProgress.setIndeterminate(false);
					btnStop.setEnabled(false);
					btnCommit.setEnabled(true);
					btnCreatePatch.setEnabled(true);
					btnCreatePatch.setEnabled(true);
					btnRefresh.setEnabled(true);
					if (tmdlCommit.getAllData().isEmpty()) {
						tblCommit.showMessage("There is nothing to commit", Manager.getIconWarning());
					}
				}
				if (CommitStatus.INIT.equals(status)) {
					workStarted();
					tblCommit.showMessage("Working...", Manager.getIconInformation());

					btnSelectAllNone.setEnabled(false);

					mapDeletedHiddenFiles = new HashMap<File, List<CommitListItem>>();

				} else if (CommitStatus.COMMIT_COMPLETED.equals(status)) {
					commitCompleted(message);
				} else if (CommitStatus.FAILED.equals(status)) {
					// MessagePaune.showError(frame, "Failed",
					// "Commit failed!");

					tblCommit.setEnabled(true);
					btnCommit.setEnabled(true);
					btnCreatePatch.setEnabled(true);
					prgWorkinProgress.setValue(0);
					prgWorkinProgress.setIndeterminate(false);
					prgWorkinProgress.setString("");
					preRealCommitProcess = false;
				} else if (CommitStatus.CANCEL.equals(status)) {
					MessagePane.showInfo(frame, "Cancelled", "Commit cancelled!");

					tblCommit.setEnabled(true);
					btnCommit.setEnabled(true);
					btnCreatePatch.setEnabled(true);
					prgWorkinProgress.setValue(0);
					prgWorkinProgress.setIndeterminate(false);
					prgWorkinProgress.setString("");
					preRealCommitProcess = false;
				}
			}

		}.run();
	}

	private void commitCompleted(String message) throws BackingStoreException, SVNException, PagaException {
		CommitCompletedMessagePane.showInfo(frame, "Completed", getCommitNotifyMessage(message));

		if (cbHelpMerge.isSelected()) {
			String mergeToDir = SettingsStore.getInstance().getLastHelpMergeToDir();
			JFileChooser jc = new JFileChooser();
			if (mergeToDir != null && new File(mergeToDir).isDirectory()) {
				jc.setCurrentDirectory(new File(mergeToDir));
			} else {
				File defaultDir = new File(path);
				if (!defaultDir.isDirectory()) {
					defaultDir = defaultDir.getParentFile();
				}
				jc.setCurrentDirectory(defaultDir);
			}
			jc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			jc.setAcceptAllFileFilterUsed(false);
			int option = jc.showOpenDialog(frame);
			GuiHelper.closeWindow(frame);

			if (option == JFileChooser.APPROVE_OPTION) {
				String selectedDir = jc.getSelectedFile().getAbsolutePath();
				SettingsStore.getInstance().setLastHelpMergeToDir(selectedDir);
				MergeOperation mergeOperation = new MergeOperation(selectedDir);
				mergeOperation.setPrefillCommitNumber(message);
				mergeOperation.setPrefillMergeFromUrl(url);
				mergeOperation.setPrefillCommitToo(true);
				mergeOperation.execute();
			}
		} else {
			GuiHelper.closeWindow(frame);
		}
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
		refreshFinished.acquire();
		new OnSwing() {

			protected void process() throws Exception {
				workStarted();
				btnStop.setEnabled(true);
				btnCommit.setEnabled(false);
				btnCreatePatch.setEnabled(false);
				btnRefresh.setEnabled(false);

				final HashMap<File, Boolean> mapOldSelectionState = new HashMap<File, Boolean>();
				for (CommitListItem li : tmdlCommit.getAllData()) {
					mapOldSelectionState.put(li.getPath(), li.isSelected());
				}

				tmdlCommit.clear();

				new Thread(new Runnable() {

					public void run() {
						try {
							commit.refresh();
							workEnded();

							new OnSwingWait<Object, Object>() {

								protected Object process() throws Exception {
									for (CommitListItem li : tmdlCommit.getAllData()) {
										Boolean oldSelectionState = mapOldSelectionState.get(li.getPath());
										if (oldSelectionState != null) {
											li.setSelected(oldSelectionState);
										}
									}
									tblCommit.repaint();
									return null;
								}
							}.run();
						} catch (Exception e) {
							Manager.handle(e);
						} finally {
							refreshFinished.release();
						}
					}
				}).start();
			}

		}.run();

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

	public void addItem(final File file, final ContentStatus contentStatus, final ContentStatus propertyStatus, final String contentStatusRemark,
			final SVNNodeKind nodeKind) throws Exception {
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
				li.setStatusRemark(contentStatusRemark);
				li.setNodeKind(nodeKind);
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
			prgWorkinProgress.setString(fileName);
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
		int[] selectedRows = tblCommit.getSelectedRows();
		ArrayList<CommitListItem> lstResult = new ArrayList<CommitListItem>(selectedRows.length);
		for (int row : selectedRows) {
			lstResult.add(tmdlCommit.getRow(tblCommit.convertRowIndexToModel(row)));
		}
		return lstResult;
	}

	private List<Integer> getSelectedItemIndeces() {
		int[] selectedRows = tblCommit.getSelectedRows();
		ArrayList<Integer> lstResult = new ArrayList<Integer>(selectedRows.length);
		for (int row : selectedRows) {
			lstResult.add(tblCommit.convertRowIndexToModel(row));
		}
		return lstResult;
	}

	private boolean addRecoursively(List<CommitListItem> lstItems) throws SVNException {
		boolean hasDirectory = false;
		for (CommitListItem li : lstItems) {
			if (li != null && li.getPath() != null && li.getPath().isDirectory()) {
				if (li.getPath().listFiles().length != 0) {
					hasDirectory = true;
					break;
				}
			}
		}

		boolean addRecursively = false;
		if (hasDirectory) {
			OPTIONS answer = MessagePane.executeConfirmDialog(frame, "Do you want to add recursively?");
			if (answer == OPTIONS.CANCEL) {
				return false;
			} else {
				if (answer == OPTIONS.OK) {
					addRecursively = true;
				}
			}
		}

		for (CommitListItem li : lstItems) {
			li.setSelected(true);
			commit.add(li.getPath(), addRecursively);
		}
		return true;
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

					if (li.getStatus().equals(ContentStatus.UNVERSIONED)) {
						// auto-add unversioned items
						commit.add(li.getPath(), false);
					} else if (li.getStatus().equals(ContentStatus.CONFLICTED)) {
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
			MessagePane.showInfo(frame, "Patch created", "Patch created");
		}
	}

	private class CommitAction extends ThreadAction {

		private boolean needRefreshAndRecount;
		private boolean firstPass;

		public CommitAction() {
			super("Commit");
		}

		private List<File> commitHelper() throws SVNException {
			ArrayList<File> lstCommit = new ArrayList<File>();
			for (CommitListItem li : tmdlCommit.getAllData()) {
				if (li.isSelected()) {

					if (firstPass && li.getStatus().equals(ContentStatus.UNVERSIONED)) {
						// auto-add unversioned items
						boolean success = addRecoursively(Arrays.asList(li));
						if (!success) {
							MessagePane.showError(frame, "Cancelled", "Cancelled.");
							return null;
						} else {
							needRefreshAndRecount = true;
						}

					} else if (li.getStatus().equals(ContentStatus.CONFLICTED)) {
						int modelRowIndex = tmdlCommit.getAllData().indexOf(li);
						tblCommit.scrollRectToVisible(tblCommit.getCellRect(tblCommit.convertRowIndexToView(modelRowIndex), 0, true));
						MessagePane.showError(frame, "Cannot commit", "Cannot commit conflicted file! Please resolve the conflict first.");
						return null;
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
			return lstCommit;
		}

		public void actionProcess(ActionEvent e) throws Exception {
			workStarted();
			List<File> lstCommit;
			try {
				if (taMessage.getText().trim().length() < logMinSize) {
					MessagePane.showError(frame, "Cannot commit", "Message length must be at least " + logMinSize + "!");
					return;
				}

				Manager.getSettings().addCommitMessageForHistory(taMessage.getText().trim());

				noCommit = 0;
				needRefreshAndRecount = false;
				firstPass = true;
				lstCommit = commitHelper();
				if (lstCommit != null && needRefreshAndRecount) {
					firstPass = false;
					refresh();
					refreshFinished.acquire();
					refreshFinished.release();
					lstCommit = commitHelper();
				}

				if (lstCommit == null) {
					return;
				}

				if (noCommit == 0) {
					MessagePane.showError(frame, "Cannot commit", "Nothing is selected to commit");
					return;
				}

				tblCommit.setEnabled(false);
				btnCommit.setEnabled(false);
				btnCreatePatch.setEnabled(false);
				prgWorkinProgress.setValue(0);
				prgWorkinProgress.setIndeterminate(true);
				preRealCommitProcess = true;
			} finally {
				workEnded();
			}
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
			prgWorkinProgress.setString("");
			commit.setCancel(true);
		}
	}

	private class AddAction extends ThreadAction {

		public AddAction() {
			super("Add");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			workStarted();
			try {
				boolean success = addRecoursively(getSelectedItems());
				if (success) {
					refresh();
				}
			} finally {
				workEnded();
			}
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
				li.setSelected(true);
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
				li.setSelected(false);
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

	private class ResolveTreeConflictUsingMergedAction extends ThreadAction {

		public ResolveTreeConflictUsingMergedAction() {
			super("Resolve tree conflict");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			for (CommitListItem li : getSelectedItems()) {
				Manager.resolveTreeConflictUsingMerged(li.getPath().getPath());
			}
			refresh();
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

	private class CreateMissingDirAction extends ThreadAction {

		public CreateMissingDirAction() {
			super("Create missing directory");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			workStarted();
			List<CommitListItem> lstFiles = getSelectedItems();

			for (CommitListItem li : lstFiles) {
				if (ContentStatus.MISSING.equals(li.getStatus())) {
					li.getPath().mkdirs();
				}
			}
			refresh();
			workEnded();
		}
	}

	private class PopupupMouseListener extends MouseAdapter {

		private JPopupMenu ppVisible;

		public PopupupMouseListener() {
		}

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
			// boolean hasFiles = false;
			// boolean hasDirs = false;
			for (int rowLi : selectedRows) {
				CommitListItem li = tmdlCommit.getRow(tblCommit.convertRowIndexToModel(rowLi));
				setUsedStatus.add(li.getStatus());
				setUsedPropertyStatus.add(li.getPropertyStatus());
				// if (SVNNodeKind.DIR.equals(li.getNodeKind())) {
				// hasDirs = true;
				// } else if (SVNNodeKind.FILE.equals(li.getNodeKind())) {
				// hasFiles = true;
				// }
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

			if (setUsedStatus.contains(ContentStatus.MISSING)) {
				ppMixed.add(new CreateMissingDirAction());
			}

			if ((setUsedStatus.contains(ContentStatus.MODIFIED) || setUsedStatus.contains(ContentStatus.ADDED) || setUsedStatus.contains(ContentStatus.MISSING) || setUsedStatus
					.contains(ContentStatus.DELETED)) && !setUsedStatus.contains(ContentStatus.UNVERSIONED)) {
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
			if (onlyOneKind && setUsedStatus.contains(ContentStatus.REPLACED)) {
				ppMixed.add(new ResolveTreeConflictUsingMergedAction());
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
				} else if (selected.getPropertyStatus().equals(ContentStatus.MODIFIED)) {
					new ShowPropertyChangesAction().actionPerformed(null);
				}
			}
		}
	}

	private class SelectAllNoneAction extends ThreadAction {

		public SelectAllNoneAction() {
			super("Select / deselect all");
		}

		public void actionProcess(ActionEvent e) throws Exception {

			boolean selected = btnSelectAllNone.isSelected();

			for (CommitListItem li : tmdlCommit.getAllData()) {
				if (selected) {
					li.setSelected(true);
				} else {
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

	private class SelectDeselectSelectedKeyListener extends KeyAdapter {

		public void keyTyped(KeyEvent e) {
			if (e.getKeyChar() == ' ') {
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

				tblCommit.repaint();
			}
		}

	}

	private static class RecentMessageSlot {

		private static final int SHORT_MESSAGE_LENGTH = 64;
		private final String message;
		private String shortMessage;

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

	private class SelectDeselectListener implements TableModelListener, ListSelectionListener {

		private List<Integer> lstLastSelectedItems;
		private boolean suppressListSelectionListener;

		public void valueChanged(ListSelectionEvent e) {
			if (!e.getValueIsAdjusting() && !suppressListSelectionListener) {
				lstLastSelectedItems = getSelectedItemIndeces();
			}
		}

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
				suppressListSelectionListener = true;

				if (lstLastSelectedItems != null) {

					int selectedRowIndex = tblCommit.convertRowIndexToModel(tblCommit.getSelectedRow());
					if (lstLastSelectedItems.contains(selectedRowIndex)) {
						boolean newSelectionState = tmdlCommit.getRow(selectedRowIndex).isSelected();

						for (Integer index : lstLastSelectedItems) {
							CommitListItem li = tmdlCommit.getRow(index);
							li.setSelected(newSelectionState);
							int indexView = tblCommit.convertRowIndexToView(index);
							tblCommit.getSelectionModel().addSelectionInterval(indexView, indexView);
						}
						tblCommit.repaint();
					}
				}

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

			refreshSelectedInfo();

			suppressListSelectionListener = false;
		}
	}

	public Window getFrame() {
		return frame;
	}

}
