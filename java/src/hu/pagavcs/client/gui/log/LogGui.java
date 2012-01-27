package hu.pagavcs.client.gui.log;

import hu.pagavcs.client.bl.Manager;
import hu.pagavcs.client.bl.OnSwing;
import hu.pagavcs.client.bl.PagaException;
import hu.pagavcs.client.bl.SettingsStore;
import hu.pagavcs.client.gui.LogListItem;
import hu.pagavcs.client.gui.StatusCellRendererForLogDetailListItem;
import hu.pagavcs.client.gui.Working;
import hu.pagavcs.client.gui.platform.EditField;
import hu.pagavcs.client.gui.platform.Frame;
import hu.pagavcs.client.gui.platform.GuiHelper;
import hu.pagavcs.client.gui.platform.Label;
import hu.pagavcs.client.gui.platform.NullCellRenderer;
import hu.pagavcs.client.gui.platform.ProgressBar;
import hu.pagavcs.client.gui.platform.StringHelper;
import hu.pagavcs.client.gui.platform.Table;
import hu.pagavcs.client.gui.platform.TableModel;
import hu.pagavcs.client.gui.platform.TextArea;
import hu.pagavcs.client.operation.ContentStatus;
import hu.pagavcs.client.operation.Log;
import hu.pagavcs.client.operation.Log.ShowLogStatus;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
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
import org.tmatesoft.svn.core.wc.SVNInfo;
import org.tmatesoft.svn.core.wc.SVNRevision;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
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

	private Table<LogListItem> tblLog;
	private TableModel<LogListItem> tmdlLog;
	private final Log log;
	private JButton btnStop;
	private TableModel<LogDetailListItem> tmdlLogDetail;
	private Table<LogDetailListItem> tblDetailLog;
	private TextArea taMessage;
	private ProgressBar prgWorkInProgress;
	private Label lblUrl;
	private JSplitPane splDetail;
	private JSplitPane splMain;
	private Timer tmrTableRevalidate;
	private volatile boolean revalidateIsTimed;
	private JButton btnShowMore;
	private JButton btnShowAll;
	private ConcurrentLinkedQueue<LogListItem> quNewItems = new ConcurrentLinkedQueue<LogListItem>();
	private JDateChooser calFrom;
	private JDateChooser calTo;
	private EditField sfFilter;
	private boolean shuttingDown;
	private TableRowSorter<TableModel<LogListItem>> filterLog;
	private List<SVNURL> lstLogRoot;
	private SVNURL svnRepoRootUrl;
	private Frame frame;

	public LogGui(Log log) {
		this.log = log;
	}

	public void display() throws SVNException {

		CellConstraints cc = new CellConstraints();

		tmrTableRevalidate = new Timer("Revalidate table", true);
		SettingsStore settingsStore = Manager.getSettings();
		tmdlLog = new TableModel<LogListItem>(new LogListItem());
		tblLog = new Table<LogListItem>(tmdlLog);
		tblLog.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		tblLog.addMouseListener(new PopupupMouseListener());
		filterLog = new TableRowSorter<TableModel<LogListItem>>(tmdlLog);
		filterLog.setRowFilter(new LogRowFilter());
		tblLog.setRowSorter(filterLog);
		new NullCellRenderer<LogListItem>(tblLog);
		tblLog.getSelectionModel().addListSelectionListener(
				new ListSelectionListener() {

					public void valueChanged(ListSelectionEvent e) {
						refreshDetailView();
					}
				});
		JScrollPane spLog = new JScrollPane(tblLog);

		tmdlLogDetail = new TableModel<LogDetailListItem>(
				new LogDetailListItem());
		tblDetailLog = new Table<LogDetailListItem>(tmdlLogDetail);
		tblDetailLog.addMouseListener(new DetailPopupupMouseListener());
		tblDetailLog
				.setRowSorter(new TableRowSorter<TableModel<LogDetailListItem>>(
						tmdlLogDetail));
		new StatusCellRendererForLogDetailListItem(tblDetailLog);
		JScrollPane spDetailLog = new JScrollPane(tblDetailLog);

		taMessage = new TextArea();
		taMessage.setEditable(false);
		taMessage.setWrapStyleWord(true);
		taMessage.setLineWrap(true);
		JScrollPane spMessage = new JScrollPane(taMessage);

		splDetail = new JSplitPane(JSplitPane.VERTICAL_SPLIT, spMessage,
				spDetailLog);
		splDetail.setResizeWeight(0);

		// if (settingsStore.getGuiLogSeparatorDetail() != null) {
		// splDetail.setDividerLocation(settingsStore.getGuiLogSeparatorDetail());
		// }
		splMain = new JSplitPane(JSplitPane.VERTICAL_SPLIT, spLog, splDetail);
		splMain.setResizeWeight(0.5);

		// if (settingsStore.getGuiLogSeparatorMain() != null) {
		// splMain.setDividerLocation(settingsStore.getGuiLogSeparatorMain());
		// }

		calFrom = new JDateChooser();
		calFrom.getCalendarButton().setToolTipText("Right click to clear");
		calFrom.getCalendarButton().addMouseListener(
				new CalendarFromButtonMouseListener());
		calFrom.addPropertyChangeListener("date", new PropertyChangeListener() {

			public void propertyChange(PropertyChangeEvent evt) {
				filterChanged();
			}
		});
		calTo = new JDateChooser();
		calTo.addFocusListener(new FocusListener() {

			public void focusLost(FocusEvent e) {
				filterChanged();
			}

			public void focusGained(FocusEvent e) {
			}
		});
		calTo.getCalendarButton().setToolTipText("Right click to clear");
		calTo.getCalendarButton().addMouseListener(
				new CalendarToButtonMouseListener());
		calTo.addPropertyChangeListener("date", new PropertyChangeListener() {

			public void propertyChange(PropertyChangeEvent evt) {
				filterChanged();
			}
		});
		sfFilter = new EditField(5);
		sfFilter.setToolTipText("Type your filter text here");
		sfFilter.getDocument()
				.addDocumentListener(new FilterDocumentListener());
		lblUrl = new Label();

		FormLayout lyTop = new FormLayout("p,2dlu,p,2dlu,p:g(0.5),2dlu,p", "p");
		JPanel pnlTop = new JPanel(lyTop);
		pnlTop.add(calFrom, cc.xy(1, 1));
		pnlTop.add(calTo, cc.xy(3, 1));
		pnlTop.add(sfFilter, cc.xy(5, 1));
		pnlTop.add(lblUrl, cc.xy(7, 1));

		btnStop = new JButton("Stop");
		btnStop.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				btnStop.setEnabled(false);
				btnStop.setText("Cancelle");
				btnShowMore.setEnabled(true);
				btnShowAll.setEnabled(true);
				log.setCancel(true);
			}
		});
		btnShowMore = new JButton(new ShowMoreAction(this));
		btnShowAll = new JButton(new ShowAllAction(this));

		prgWorkInProgress = new ProgressBar(this);

		FormLayout lyBottom = new FormLayout("f:1dlu:g,2dlu,p,2dlu,p,2dlu,p",
				"p");
		JPanel pnlBottom = new JPanel(lyBottom);
		pnlBottom.add(prgWorkInProgress, cc.xy(1, 1));
		pnlBottom.add(btnShowMore, cc.xy(3, 1));
		pnlBottom.add(btnShowAll, cc.xy(5, 1));
		pnlBottom.add(btnStop, cc.xy(7, 1));

		FormLayout lyMain = new FormLayout("f:200dlu:g",
				"p,2dlu,f:100dlu:g,2dlu,p");
		JPanel pnlMain = new JPanel(lyMain);
		pnlMain.add(pnlTop, cc.xy(1, 1));
		pnlMain.add(splMain, cc.xy(1, 3));
		pnlMain.add(pnlBottom, cc.xy(1, 5));

		frame = GuiHelper.createAndShowFrame(pnlMain, "Show Log",
				"showlog-app-icon.png", false);
		frame.addWindowListener(new FrameWindowListener());
	}

	private void filterChanged() {
		filterLog.sort();
		tblLog.resizeColumns();
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
			btnShowMore.setEnabled(true);
			btnShowAll.setEnabled(true);
		} else if (ShowLogStatus.STARTED.equals(status)) {
			btnStop.setText("Stop");
			btnStop.setEnabled(true);
			btnShowMore.setEnabled(false);
			btnShowAll.setEnabled(false);
		}
	}

	public void addItem(long revision, String author, Date date,
			String message, Map<String, SVNLogEntryPath> mapChanges) {
		LogListItem li = new LogListItem();
		li.setRevision(revision);
		li.setAuthor(author);
		li.setDate(date);
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
				tmrTableRevalidate.schedule(new DoRevalidateTask(),
						Manager.REVALIDATE_DELAY);
			}
		}
	}

	private final class FrameWindowListener extends WindowAdapter {

		public void windowClosing(WindowEvent e) {
			SettingsStore settingsStore = Manager.getSettings();
			settingsStore.setGuiLogSeparatorDetail(splDetail
					.getDividerLocation());
			settingsStore.setGuiLogSeparatorMain(splMain.getDividerLocation());
			shuttingDown = true;
		}

		public void windowClosed(WindowEvent e) {
			tmrTableRevalidate.cancel();
			tmrTableRevalidate.purge();
		}
	}

	private final class FilterDocumentListener implements DocumentListener {

		public void changedUpdate(DocumentEvent e) {
			filterChanged();
		}

		public void removeUpdate(DocumentEvent e) {
			filterChanged();
		}

		public void insertUpdate(DocumentEvent e) {
			filterChanged();
		}
	}

	private final class CalendarToButtonMouseListener extends MouseAdapter {

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

		public void showPopup(MouseEvent e) {
			calTo.setDate(null);
			filterChanged();
		}
	}

	private final class CalendarFromButtonMouseListener extends MouseAdapter {

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

		public void showPopup(MouseEvent e) {
			calFrom.setDate(null);
			filterChanged();
		}
	}

	private final class LogRowFilter extends
			RowFilter<TableModel<LogListItem>, Integer> {

		private boolean nullSafeContains(String where, String what) {
			if (where == null || what == null) {
				return false;
			}
			return where.toLowerCase().contains(what.toLowerCase());
		}

		public boolean include(
				javax.swing.RowFilter.Entry<? extends TableModel<LogListItem>, ? extends Integer> entry) {
			String textFilter = sfFilter.getText();
			Date dateFromFilter = calFrom.getDate();
			Date dateToFilter = calTo.getDate();

			if (textFilter.isEmpty() && dateFromFilter == null
					&& dateToFilter == null) {
				return true;
			}

			LogListItem row = entry.getModel().getRow(entry.getIdentifier());

			if (!textFilter.isEmpty()
					&& !nullSafeContains(row.getMessage(), textFilter)
					&& !nullSafeContains(row.getAuthor(), textFilter)
					&& !nullSafeContains(Long.toString(row.getRevision()),
							textFilter)) {
				return false;
			}

			if (dateFromFilter != null
					&& row.getDate().compareTo(dateFromFilter) <= 0) {
				return false;
			}

			if (dateToFilter != null
					&& row.getDate().compareTo(dateToFilter) >= 0) {
				return false;
			}

			return true;
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
						if (lstLi.size() == 1) {
							List<LogListItem> lines = tmdlLog.getAllData();
							if (!lines.isEmpty()
									&& lines.get(lines.size() - 1)
											.getRevision() == lstLi.get(0)
											.getRevision()) {
								lstLi.clear();
							}
						}

						boolean firstData = tmdlLog.getAllData().isEmpty()
								&& !lstLi.isEmpty();

						tmdlLog.addLines(lstLi);
						if (firstData) {
							tblLog.getSelectionModel().setSelectionInterval(0,
									0);
						}
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

	public String getAbsoluteUrl(String detailPath) {
		return svnRepoRootUrl.toString() + detailPath;
	}

	public void refreshDetailView() {
		List<LogListItem> lstSelected = getSelectedLogItems();
		if (lstSelected.isEmpty()) {
			return;
		}
		tblDetailLog.getSelectionModel().clearSelection();
		tmdlLogDetail.clear();
		if (lstSelected.size() == 1) {
			taMessage.setText(lstSelected.get(0).getMessage());
			taMessage.setEnabled(true);
		} else {
			taMessage.setText(null);
			taMessage.setEnabled(false);
		}

		boolean hasOutOfScope = false;
		ArrayList<LogDetailListItem> lstResult = new ArrayList<LogDetailListItem>();
		for (LogListItem liLog : getSelectedLogItems()) {
			for (SVNLogEntryPath liEntryPath : liLog.getChanges().values()) {
				LogDetailListItem liDetail = new LogDetailListItem();
				liDetail.setPath(liEntryPath.getPath());
				liDetail.setAction(getContentStatusByTypeChar(liEntryPath
						.getType()));
				liDetail.setCopyFromPath(liEntryPath.getCopyPath());
				liDetail.setCopyRevision(liEntryPath.getCopyRevision() != -1 ? liEntryPath
						.getCopyRevision() : null);
				liDetail.setKind(liEntryPath.getKind());
				liDetail.setInScope(isInScope(liEntryPath.getPath()));
				liDetail.setRevision(liLog.getRevision());
				lstResult.add(liDetail);

				if (!liDetail.isInScope()) {
					hasOutOfScope = true;
				}
			}
		}

		if (hasOutOfScope) {
			ArrayList<LogDetailListItem> lstPrioResult = new ArrayList<LogDetailListItem>(
					lstResult.size());
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
		if (!lstResult.isEmpty()) {
			tblDetailLog.getSelectionModel().setSelectionInterval(0, 0);
		}
	}

	List<LogListItem> getSelectedLogItems() {
		ArrayList<LogListItem> lstResult = new ArrayList<LogListItem>();
		for (int row : tblLog.getSelectedRows()) {

			lstResult.add(tmdlLog.getRow(tblLog.convertRowIndexToModel(row)));
		}
		return lstResult;
	}

	List<LogDetailListItem> getSelectedDetailLogItems() {
		ArrayList<LogDetailListItem> lstResult = new ArrayList<LogDetailListItem>();
		for (int row : tblDetailLog.getSelectedRows()) {

			lstResult.add(tmdlLogDetail.getRow(tblDetailLog
					.convertRowIndexToModel(row)));
		}
		return lstResult;
	}

	void copyLogListItemsToClipboard(List<LogListItem> lstLogListItem) {
		StringBuilder result = new StringBuilder();
		long maxRevisionNumber = -1;
		int maxDateLength = -1;
		for (LogListItem li : lstLogListItem) {
			long revisionNumber = li.getRevision();
			if (revisionNumber > maxRevisionNumber) {
				maxRevisionNumber = revisionNumber;
			}

			if (li.getDateAsString().length() > maxDateLength) {
				maxDateLength = li.getDateAsString().length();
			}
		}
		int revisionPadding = 10 - Long.toString(maxRevisionNumber).length();
		int datePadding = 10 - maxDateLength;

		for (LogListItem li : lstLogListItem) {
			String message = li.getMessage();
			message = message.replace('\n', ' ');
			String revision = Long.toString(li.getRevision());
			revision = "          ".substring(revisionPadding
					+ revision.length())
					+ revision;
			String date = li.getDateAsString()
					+ "          ".substring(datePadding
							+ li.getDateAsString().length());
			result.append(revision + " " + li.getActions() + " "
					+ li.getAuthor() + " " + date + " " + message + "\n");
		}
		Manager.setClipboard(result.toString());
	}

	void copyLogDetailListItemsToClipboard(
			List<LogDetailListItem> lstDetailLogListItem,
			LogDetailCopyToClipboardType copyType) {
		StringBuilder result = new StringBuilder();
		if (copyType.equals(LogDetailCopyToClipboardType.DETAIL)) {
			for (LogDetailListItem li : lstDetailLogListItem) {
				result.append(li.getPath() + " " + li.getAction() + " "
						+ StringHelper.toNullAware(li.getCopyFromPath()) + " "
						+ StringHelper.toNullAware(li.getCopyRevision()) + "\n");
			}
		} else if (copyType
				.equals(LogDetailCopyToClipboardType.GROUP_NAME_ONLY)) {
			HashSet<String> setPath = new HashSet<String>();
			for (LogDetailListItem li : lstDetailLogListItem) {
				setPath.add(li.getPath());
			}
			ArrayList<String> lstPath = new ArrayList<String>(setPath);
			Collections.sort(lstPath);
			for (String liPath : lstPath) {
				result.append(liPath);
				result.append('\n');
			}
		}
		Manager.setClipboard(result.toString());
	}

	public void revertChanges(String revertPath, long revision)
			throws Exception {
		log.revertChanges(revertPath, revision);
	}

	public void revertChangesExact(long revision) throws Exception {
		log.revertChangesExact(revision);
	}

	public void revertChangesToThisRevisionExact(String revisionRange)
			throws Exception {
		log.revertChangesToThisRevisionExact(revisionRange);
	}

	public void showDirChanges(String showChangesPath, long revision,
			ContentStatus contentStatus) throws Exception {
		log.showDirChanges(showChangesPath, revision, contentStatus);
	}

	public void showChanges(String showChangesPath, long revision,
			ContentStatus contentStatus) throws Exception {
		log.showChanges(showChangesPath, revision, contentStatus);
	}

	public void compareWithWorkingCopy(String showChangesPath, long revision,
			ContentStatus contentStatus) throws Exception {

		log.compareWithWorkingCopy(
				Manager.getAbsoluteFile(log.getPath(), showChangesPath),
				Manager.getAbsoluteUrl(log.getUrl(), showChangesPath),
				revision, contentStatus);
	}

	public void saveRevisionTo(String showChangesPath, long revision,
			File destination) throws Exception {
		log.saveRevisionTo(showChangesPath, revision, destination);
	}

	public void doShowLog(SVNRevision startRevision, long limit)
			throws Exception {
		log.doShowLog(startRevision, limit);
	}

	public void showFile(String showChangesPath, long revision)
			throws Exception {
		log.showFile(showChangesPath, revision);
	}

	public List<LogListItem> getAllTableDataFromMain() {
		return tmdlLog.getAllData();
	}

	public List<LogDetailListItem> getAllTableDataFromDetail() {
		return tmdlLogDetail.getAllData();
	}

	public Frame getFrame() {
		return frame;
	}

	private class DetailPopupupMouseListener extends MouseAdapter {

		private JPopupMenu ppModified;
		private JPopupMenu ppAdded;
		private JPopupMenu ppDeleted;

		public DetailPopupupMouseListener() {
			ppModified = new JPopupMenu();
			ppModified.add(new DetailShowChangesAction(LogGui.this));
			ppModified.add(new ShowFileAction(LogGui.this));
			ppModified.add(new SaveRevisionToAction(LogGui.this));
			ppModified.add(new ShowLogAction(LogGui.this));
			ppModified.add(new DetailCompareWithWorkingCopyAction(LogGui.this));
			ppModified.add(new DetailRevertChangesFromThisRevisionAction(
					LogGui.this));
			ppModified.add(new CopyDetailLineToClipboard(LogGui.this));
			ppModified.add(new CopyDetailAllToClipboard(LogGui.this));
			ppModified.add(new CopyDetailAllGrouppedToClipboard(LogGui.this));

			ppAdded = new JPopupMenu();
			ppAdded.add(new ShowFileAction(LogGui.this));
			ppAdded.add(new SaveRevisionToAction(LogGui.this));
			ppAdded.add(new DetailCompareWithWorkingCopyAction(LogGui.this));
			ppAdded.add(new DetailRevertChangesFromThisRevisionAction(
					LogGui.this));
			ppAdded.add(new ShowLogAction(LogGui.this));
			ppAdded.add(new CopyDetailLineToClipboard(LogGui.this));
			ppAdded.add(new CopyDetailAllToClipboard(LogGui.this));
			ppAdded.add(new CopyDetailAllGrouppedToClipboard(LogGui.this));

			ppDeleted = new JPopupMenu();
			ppDeleted.add(new ShowFileAction(LogGui.this));
			ppDeleted.add(new SaveRevisionToAction(LogGui.this));
			ppDeleted.add(new DetailCompareWithWorkingCopyAction(LogGui.this));
			ppDeleted.add(new DetailRevertChangesFromThisRevisionAction(
					LogGui.this));
			ppDeleted.add(new ShowLogAction(LogGui.this));
			ppDeleted.add(new CopyDetailLineToClipboard(LogGui.this));
			ppDeleted.add(new CopyDetailAllToClipboard(LogGui.this));
			ppDeleted.add(new CopyDetailAllGrouppedToClipboard(LogGui.this));
		}

		private void showPopup(MouseEvent e) {
			Point p = new Point(e.getX(), e.getY());
			int row = tblDetailLog.rowAtPoint(p);
			if (row == -1) {
				return;
			}

			tblDetailLog.getSelectionModel().setSelectionInterval(row, row);
			LogDetailListItem selected = tmdlLogDetail.getRow(row);

			if (selected.getAction().equals(ContentStatus.MODIFIED)) {
				JPopupMenu ppVisible = ppModified;
				ppVisible.setInvoker(tblDetailLog);
				ppVisible.setLocation(e.getXOnScreen(), e.getYOnScreen());
				ppVisible.setVisible(true);
				e.consume();
			} else if (selected.getAction().equals(ContentStatus.ADDED)) {
				JPopupMenu ppVisible = ppAdded;
				ppVisible.setInvoker(tblDetailLog);
				ppVisible.setLocation(e.getXOnScreen(), e.getYOnScreen());
				ppVisible.setVisible(true);
				e.consume();
			} else if (selected.getAction().equals(ContentStatus.DELETED)) {
				JPopupMenu ppVisible = ppDeleted;
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
							new DetailShowChangesAction(LogGui.this)
									.actionProcess(null);
						} catch (Exception e1) {
							Manager.handle(e1);
						}
					}
				}).start();
			}

		}
	}

	private class PopupupMouseListener extends MouseAdapter {

		private JPopupMenu pp;

		public PopupupMouseListener() {

			pp = new JPopupMenu();
			pp.add(new CopyLineToClipboard(LogGui.this));
			pp.add(new CopyAllToClipboard(LogGui.this));
			pp.add(new JSeparator());
			pp.add(new RevertChangesFromThisRevisionAction(LogGui.this));
			pp.add(new RevertChangesToThisRevisionAction(LogGui.this));
		}

		private void showPopup(MouseEvent e) {
			Point p = new Point(e.getX(), e.getY());
			int row = tblLog.rowAtPoint(p);
			if (row == -1) {
				return;
			}

			LogListItem selected = tmdlLog.getRow(row);
			if (!getSelectedLogItems().contains(selected)) {
				tblLog.getSelectionModel().setSelectionInterval(row, row);
			}

			JPopupMenu ppVisible = pp;
			ppVisible.setInvoker(tblLog);
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
				new Thread(new Runnable() {

					public void run() {
						try {
							new DetailShowChangesAction(LogGui.this)
									.actionProcess(null);
						} catch (Exception e1) {
							Manager.handle(e1);
						}
					}
				}).start();
			}

		}

	}

	public SVNNodeKind getNodeKind(String path, long revision)
			throws SVNException, PagaException {
		if (svnRepoRootUrl != null) {
			SVNURL url = Manager.getAbsoluteUrl(svnRepoRootUrl, path);
			SVNInfo info = Manager.getInfo(url, SVNRevision.create(revision));
			return info.getKind();
		}
		return null;
	}
}
