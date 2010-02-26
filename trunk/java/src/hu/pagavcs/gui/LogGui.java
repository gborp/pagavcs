package hu.pagavcs.gui;

import hu.pagavcs.bl.Manager;
import hu.pagavcs.bl.OnSwing;
import hu.pagavcs.bl.PagaException;
import hu.pagavcs.bl.SettingsStore;
import hu.pagavcs.bl.ThreadAction;
import hu.pagavcs.bl.PagaException.PagaExceptionType;
import hu.pagavcs.gui.platform.EditField;
import hu.pagavcs.gui.platform.Frame;
import hu.pagavcs.gui.platform.GuiHelper;
import hu.pagavcs.gui.platform.Label;
import hu.pagavcs.gui.platform.MessagePane;
import hu.pagavcs.gui.platform.NullCellRenderer;
import hu.pagavcs.gui.platform.ProgressBar;
import hu.pagavcs.gui.platform.Table;
import hu.pagavcs.gui.platform.TableModel;
import hu.pagavcs.gui.platform.TextArea;
import hu.pagavcs.operation.ContentStatus;
import hu.pagavcs.operation.Log;
import hu.pagavcs.operation.Log.ShowLogStatus;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableRowSorter;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntryPath;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNRevision;

import com.toedter.calendar.JDateChooser;

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
public class LogGui implements Working {

	private Table<LogListItem>                      tblLog;
	private TableModel<LogListItem>                 tmdlLog;
	private final Log                               log;
	private JButton                                 btnStop;
	private TableModel<LogDetailListItem>           tmdlLogDetail;
	private Table<LogDetailListItem>                tblDetailLog;
	private TextArea                                taMessage;
	private ProgressBar                             prgWorkInProgress;
	private Label                                   lblUrl;
	private JSplitPane                              splDetail;
	private JSplitPane                              splMain;
	private Timer                                   tmrTableRevalidate;
	private boolean                                 revalidateIsTimed;
	private JButton                                 btnShowMore;
	private JButton                                 btnShowAll;
	private ConcurrentLinkedQueue<LogListItem>      quNewItems = new ConcurrentLinkedQueue<LogListItem>();
	private JDateChooser                            calFrom;
	private JDateChooser                            calTo;
	private EditField                               sfFilter;
	private boolean                                 shuttingDown;
	private TableRowSorter<TableModel<LogListItem>> sorterLog;
	private List<SVNURL>                            lstLogRoot;
	private SVNURL                                  svnRepoRootUrl;
	private Frame                                   frame;

	public LogGui(Log log) {
		this.log = log;
	}

	public void display() throws SVNException {

		tmrTableRevalidate = new Timer("Revalidate table", true);
		SettingsStore settingsStore = Manager.getSettings();
		tmdlLog = new TableModel<LogListItem>(new LogListItem());
		tblLog = new Table<LogListItem>(tmdlLog);
		sorterLog = new TableRowSorter<TableModel<LogListItem>>(tmdlLog);
		sorterLog.setRowFilter(new RowFilter<TableModel<LogListItem>, Integer>() {

			private boolean nullSafeContains(String where, String what) {
				if (where == null) {
					return false;
				}
				return where.contains(what);
			}

			public boolean include(javax.swing.RowFilter.Entry<? extends TableModel<LogListItem>, ? extends Integer> entry) {
				String filter = sfFilter.getText();
				if (filter.isEmpty()) {
					return true;
				}
				LogListItem row = entry.getModel().getRow(entry.getIdentifier());

				return nullSafeContains(row.getMessage(), filter) || nullSafeContains(row.getAuthor(), filter)
				        || nullSafeContains(Long.toString(row.getRevision()), filter);
			}

		});
		tblLog.setRowSorter(sorterLog);
		new NullCellRenderer<LogListItem>(tblLog);
		tblLog.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

			public void valueChanged(ListSelectionEvent e) {
				refreshDetailView();
			}
		});
		JScrollPane spLog = new JScrollPane(tblLog);

		tmdlLogDetail = new TableModel<LogDetailListItem>(new LogDetailListItem());
		tblDetailLog = new Table<LogDetailListItem>(tmdlLogDetail);
		tblDetailLog.addMouseListener(new DetailPopupupMouseListener());
		tblDetailLog.setRowSorter(new TableRowSorter<TableModel<LogDetailListItem>>(tmdlLogDetail));
		new StatusCellRendererForLogDetailListItem(tblDetailLog);
		JScrollPane spDetailLog = new JScrollPane(tblDetailLog);

		taMessage = new TextArea();
		taMessage.setEditable(false);
		taMessage.setWrapStyleWord(true);
		taMessage.setLineWrap(true);
		JScrollPane spMessage = new JScrollPane(taMessage);

		splDetail = new JSplitPane(JSplitPane.VERTICAL_SPLIT, spMessage, spDetailLog);
		splDetail.setPreferredSize(new Dimension(40, 40));

		if (settingsStore.getGuiLogSeparatorDetail() != null) {
			splDetail.setDividerLocation(settingsStore.getGuiLogSeparatorDetail());
		}
		splMain = new JSplitPane(JSplitPane.VERTICAL_SPLIT, spLog, splDetail);
		splMain.setPreferredSize(new Dimension(40, 40));
		if (settingsStore.getGuiLogSeparatorMain() != null) {
			splMain.setDividerLocation(settingsStore.getGuiLogSeparatorMain());
		}

		calFrom = new JDateChooser();
		calFrom.setEnabled(false);
		calTo = new JDateChooser();
		calTo.setEnabled(false);
		sfFilter = new EditField(20);
		sfFilter.setToolTipText("Type your filter text here");
		sfFilter.getDocument().addDocumentListener(new DocumentListener() {

			public void changedUpdate(DocumentEvent e) {
				sorterLog.modelStructureChanged();
			}

			public void removeUpdate(DocumentEvent e) {
				sorterLog.modelStructureChanged();
			}

			public void insertUpdate(DocumentEvent e) {
				sorterLog.modelStructureChanged();
			}
		});
		lblUrl = new Label();

		JPanel pnlTop = new JPanel(new FlowLayout(FlowLayout.LEFT));
		pnlTop.add(calFrom);
		pnlTop.add(calTo);
		pnlTop.add(sfFilter);
		pnlTop.add(lblUrl);

		btnStop = new JButton("Stop");
		btnStop.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				btnStop.setEnabled(false);
				btnStop.setText("Cancelled");
				log.setCancel(true);
			}
		});
		btnShowMore = new JButton(new ShowMoreAction());
		btnShowAll = new JButton(new ShowAllAction());

		prgWorkInProgress = new ProgressBar(this);

		JPanel pnlBottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
		pnlBottom.add(prgWorkInProgress);
		pnlBottom.add(btnShowMore);
		pnlBottom.add(btnShowAll);
		pnlBottom.add(btnStop);

		JPanel pnlMain = new JPanel(new BorderLayout());
		pnlMain.add(pnlTop, BorderLayout.NORTH);
		pnlMain.add(splMain, BorderLayout.CENTER);
		pnlMain.add(pnlBottom, BorderLayout.SOUTH);

		frame = GuiHelper.createAndShowFrame(pnlMain, "Show Log", "/hu/pagavcs/resources/showlog-app-icon.png");
		frame.addWindowListener(new WindowAdapter() {

			public void windowClosing(WindowEvent e) {
				SettingsStore settingsStore = Manager.getSettings();
				settingsStore.setGuiLogSeparatorDetail(splDetail.getDividerLocation());
				settingsStore.setGuiLogSeparatorMain(splMain.getDividerLocation());
				shuttingDown = true;
			}

			public void windowClosed(WindowEvent e) {
				tmrTableRevalidate.cancel();
				tmrTableRevalidate.purge();
			}
		});
	}

	public void setUrlLabel(String urlLabel) {
		lblUrl.setText(urlLabel);
	}

	public synchronized void workStarted() throws Exception {
		prgWorkInProgress.startProgress();
		setStatus(ShowLogStatus.STARTED);
	}

	public synchronized void workEnded() throws Exception {
		prgWorkInProgress.stopProgress();
		setStatus(ShowLogStatus.COMPLETED);
	}

	public void setStatus(ShowLogStatus status) {
		if (ShowLogStatus.COMPLETED.equals(status)) {
			btnStop.setEnabled(false);
		}
	}

	public void addItem(long revision, String author, Date date, String message, Map<String, SVNLogEntryPath> mapChanges) {
		LogListItem li = new LogListItem();
		li.setRevision(revision);
		li.setAuthor(author);
		li.setDate(date.toLocaleString());
		li.setMessage(message);
		li.setChanges(mapChanges);

		boolean hasAdded = false;
		boolean hasModified = false;
		boolean hasReplaced = false;
		boolean hasDeleted = false;
		for (SVNLogEntryPath li2 : mapChanges.values()) {
			if (li2.getType() == SVNLogEntryPath.TYPE_ADDED) {
				hasAdded = true;
			} else if (li2.getType() == SVNLogEntryPath.TYPE_DELETED) {
				hasDeleted = true;
			} else if (li2.getType() == SVNLogEntryPath.TYPE_MODIFIED) {
				hasModified = true;
			} else if (li2.getType() == SVNLogEntryPath.TYPE_REPLACED) {
				hasReplaced = true;
			}
		}
		String actions = "";
		if (hasAdded) {
			actions += "A";
		} else {
			actions += "_";
		}
		if (hasDeleted) {
			actions += "D";
		} else {
			actions += "_";
		}
		if (hasModified) {
			actions += "M";
		} else {
			actions += "_";
		}
		if (hasReplaced) {
			actions += "R";
		} else {
			actions += "_";
		}
		li.setActions(actions);

		quNewItems.add(li);
		doRevalidateTable();
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

						ArrayList<LogListItem> lstLi = new ArrayList<LogListItem>();
						synchronized (quNewItems) {
							revalidateIsTimed = false;
							LogListItem li;
							while ((li = quNewItems.poll()) != null) {
								lstLi.add(li);
							}
						}
						tmdlLog.addLines(lstLi);
					}
				}.run();
			} catch (Exception e) {
				Manager.handle(e);
			}
		}
	}

	private ContentStatus getContentStatusByTypeChar(char action) {
		if (action == SVNLogEntryPath.TYPE_ADDED) {
			return ContentStatus.ADDED;
		} else if (action == SVNLogEntryPath.TYPE_DELETED) {
			return ContentStatus.DELETED;
		} else if (action == SVNLogEntryPath.TYPE_MODIFIED) {
			return ContentStatus.MODIFIED;
		} else if (action == SVNLogEntryPath.TYPE_REPLACED) {
			return ContentStatus.REPLACED;
		}
		throw new RuntimeException("Unimplemented");
	}

	public void setSvnRepoRootUrl(SVNURL svnRepoRootUrl) {
		this.svnRepoRootUrl = svnRepoRootUrl;
	}

	public void setLogRootsFiles(List<File> lstLogRoot) throws SVNException {
		if (lstLogRoot.size() == 1) {
			frame.setTitlePrefix(lstLogRoot.get(0).getPath());
		}
		ArrayList<SVNURL> svnLogRoots = new ArrayList<SVNURL>(lstLogRoot.size());
		for (File logRoot : lstLogRoot) {
			svnLogRoots.add(Manager.getSvnUrlByFile(logRoot));
		}
		setLogRoots(svnLogRoots);
	}

	public void setLogRoots(List<SVNURL> lstLogRoot) throws SVNException {
		this.lstLogRoot = lstLogRoot;
	}

	private boolean isInScope(String entryPath) {
		String pathPrefix = svnRepoRootUrl.getPath();

		for (SVNURL svnurl : lstLogRoot) {
			String absPath = pathPrefix + entryPath;
			String svnPath = svnurl.getPath();
			if (absPath.startsWith(svnPath)) {
				String restOfPath = absPath.substring(svnPath.length());
				if (restOfPath.isEmpty() || restOfPath.charAt(0) == '/') {
					return true;
				}
			}
		}

		return false;
	}

	public void refreshDetailView() {
		int selectedRow = tblLog.getSelectedRow();
		if (selectedRow == -1) {
			return;
		}

		tblDetailLog.getSelectionModel().clearSelection();
		LogListItem liLog = getSelectedLogItem();
		taMessage.setText(liLog.getMessage());
		tmdlLogDetail.clear();
		boolean hasOutOfScope = false;
		ArrayList<LogDetailListItem> lstResult = new ArrayList<LogDetailListItem>();
		for (SVNLogEntryPath liEntryPath : liLog.getChanges().values()) {
			LogDetailListItem liDetail = new LogDetailListItem();
			liDetail.setPath(liEntryPath.getPath());
			liDetail.setAction(getContentStatusByTypeChar(liEntryPath.getType()));
			liDetail.setCopyFromPath(liEntryPath.getCopyPath());
			liDetail.setRevision(liEntryPath.getCopyRevision() != -1 ? liEntryPath.getCopyRevision() : null);
			liDetail.setKind(liEntryPath.getKind());
			liDetail.setInScope(isInScope(liEntryPath.getPath()));
			lstResult.add(liDetail);

			if (!liDetail.isInScope()) {
				hasOutOfScope = true;
			}
		}

		if (hasOutOfScope) {
			ArrayList<LogDetailListItem> lstPrioResult = new ArrayList<LogDetailListItem>(lstResult.size());
			for (LogDetailListItem li : lstResult) {
				if (li.isInScope()) {
					lstPrioResult.add(li);
				}
			}
			for (LogDetailListItem li : lstResult) {
				if (!li.isInScope()) {
					lstPrioResult.add(li);
				}
			}
			lstResult = lstPrioResult;
		}

		tmdlLogDetail.addLines(lstResult);
	}

	private LogListItem getSelectedLogItem() {
		return tmdlLog.getRow(tblLog.convertRowIndexToModel(tblLog.getSelectedRow()));
	}

	private List<LogDetailListItem> getSelectedDetailLogItems() {
		ArrayList<LogDetailListItem> lstResult = new ArrayList<LogDetailListItem>();
		for (int row : tblDetailLog.getSelectedRows()) {

			lstResult.add(tmdlLogDetail.getRow(tblDetailLog.convertRowIndexToModel(row)));
		}
		return lstResult;
	}

	private class DetailShowChangesAction extends ThreadAction {

		public DetailShowChangesAction() {
			super("Show changes");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			LogListItem liLog = getSelectedLogItem();
			for (LogDetailListItem liDetail : getSelectedDetailLogItems()) {
				if (SVNNodeKind.DIR.equals(liDetail.getKind())) {
					log.showDirChanges(liDetail.getPath(), liLog.getRevision(), liDetail.getAction());
				} else if (SVNNodeKind.FILE.equals(liDetail.getKind())) {
					log.showChanges(liDetail.getPath(), liLog.getRevision(), liDetail.getAction());
				} else {
					log.showChanges(liDetail.getPath(), liLog.getRevision(), liDetail.getAction());
				}
			}
		}
	}

	private class ShowFileAction extends ThreadAction {

		public ShowFileAction() {
			super("Show file");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			LogListItem liLog = getSelectedLogItem();
			for (LogDetailListItem liDetail : getSelectedDetailLogItems()) {
				if (SVNNodeKind.DIR.equals(liDetail.getKind())) {
					throw new PagaException(PagaExceptionType.UNIMPLEMENTED);
				} else {
					ContentStatus cs = liDetail.getAction();
					if (ContentStatus.DELETED.equals(cs)) {
						MessagePane.showError(frame, "Cannot save", "File is deleted in this revision.");
					}
					log.showFile(liDetail.getPath(), liLog.getRevision());
				}
			}
		}
	}

	private class SaveRevisionToAction extends ThreadAction {

		public SaveRevisionToAction() {
			super("Save revision to");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			LogListItem liLog = getSelectedLogItem();
			for (LogDetailListItem liDetail : getSelectedDetailLogItems()) {
				if (SVNNodeKind.DIR.equals(liDetail.getKind())) {
					throw new PagaException(PagaExceptionType.UNIMPLEMENTED);
				} else {
					ContentStatus cs = liDetail.getAction();
					if (ContentStatus.DELETED.equals(cs)) {
						MessagePane.showError(frame, "Cannot save", "File is deleted in this revision.");
					}

					String path = liDetail.getPath();
					if (path.lastIndexOf('/') != -1) {
						path = path.substring(path.lastIndexOf('/'));
					}
					JFileChooser fc = new JFileChooser();
					fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
					fc.setSelectedFile(new File(path));

					int choosed = fc.showSaveDialog(frame);

					if (choosed == JFileChooser.APPROVE_OPTION) {
						File file = fc.getSelectedFile();
						log.saveRevisionTo(liDetail.getPath(), liLog.getRevision(), file);
					}
				}
			}
		}
	}

	private class DetailRevertChangesFromThisRevisionAction extends ThreadAction {

		public DetailRevertChangesFromThisRevisionAction() {
			super("Revert changes from this revision");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			LogListItem liLog = getSelectedLogItem();
			for (LogDetailListItem liDetail : getSelectedDetailLogItems()) {
				log.revertChanges(liDetail.getPath(), liLog.getRevision());
			}
		}
	}

	public class ShowMoreAction extends ThreadAction {

		public ShowMoreAction() {
			super("Show more");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			workStarted();
			try {
				List<LogListItem> allRetrivedLogs = tmdlLog.getAllData();
				if (!allRetrivedLogs.isEmpty()) {
					LogListItem lastLi = allRetrivedLogs.get(allRetrivedLogs.size() - 1);
					if (lastLi.getRevision() > 1) {
						log.doShowLog(SVNRevision.create(lastLi.getRevision() + 1), Log.LIMIT);
					}
				}
				workEnded();
			} catch (Exception ex) {
				workEnded();
				throw ex;
			}
		}

	}

	public class ShowAllAction extends ThreadAction {

		public ShowAllAction() {
			super("Show all");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			workStarted();
			try {
				List<LogListItem> allRetrivedLogs = tmdlLog.getAllData();
				if (!allRetrivedLogs.isEmpty()) {
					LogListItem lastLi = allRetrivedLogs.get(allRetrivedLogs.size() - 1);
					if (lastLi.getRevision() > 1) {
						log.doShowLog(SVNRevision.create(lastLi.getRevision() + 1), Log.NO_LIMIT);
					}
				} else {
					log.doShowLog(SVNRevision.HEAD, Log.NO_LIMIT);
				}
				workEnded();
			} catch (Exception ex) {
				workEnded();
				throw ex;
			}
		}

	}

	private class DetailPopupupMouseListener extends MouseAdapter {

		private JPopupMenu ppModified;

		public DetailPopupupMouseListener() {
			ppModified = new JPopupMenu();
			ppModified.add(new DetailShowChangesAction());
			ppModified.add(new ShowFileAction());
			ppModified.add(new SaveRevisionToAction());
			ppModified.add(new DetailRevertChangesFromThisRevisionAction());
		}

		private void showPopup(MouseEvent e) {
			Point p = new Point(e.getX(), e.getY());
			int row = tblDetailLog.rowAtPoint(p);
			if (row == -1) {
				return;
			}

			tblDetailLog.getSelectionModel().setSelectionInterval(row, row);
			LogDetailListItem selected = tmdlLogDetail.getRow(tblDetailLog.convertRowIndexToModel(row));

			if (selected.getAction().equals(ContentStatus.MODIFIED)) {
				JPopupMenu ppVisible = ppModified;
				ppVisible.setInvoker(tblDetailLog);
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
				new Thread(new Runnable() {

					public void run() {
						try {
							new DetailShowChangesAction().actionProcess(null);
						} catch (Exception e1) {
							Manager.handle(e1);
						}
					}
				}).start();
			}

		}
	}

}
