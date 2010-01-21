package hu.pagavcs.gui;

import hu.pagavcs.bl.Manager;
import hu.pagavcs.bl.OnSwing;
import hu.pagavcs.bl.SettingsStore;
import hu.pagavcs.bl.ThreadAction;
import hu.pagavcs.operation.ContentStatus;
import hu.pagavcs.operation.Log;
import hu.pagavcs.operation.Log.ShowLogStatus;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableRowSorter;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntryPath;
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

	private Table                              tblLog;
	private TableModel<LogListItem>            logTableModel;
	private final Log                          log;
	private JButton                            btnStop;
	private TableModel<LogDetailListItem>      logDetailTableModel;
	private Table                              tblDetailLog;
	private TextArea                           taMessage;
	private ProgressBar                        prgWorkInProgress;
	private Label                              lblUrl;
	private JSplitPane                         splDetail;
	private JSplitPane                         splMain;
	private Timer                              tmrTableRevalidate;
	private boolean                            revalidateIsTimed;
	private JButton                            btnShowMore;
	private JButton                            btnShowAll;
	private ConcurrentLinkedQueue<LogListItem> quNewItems = new ConcurrentLinkedQueue<LogListItem>();
	private JDateChooser                       calFrom;
	private JDateChooser                       calTo;
	private EditField                          sfFilter;
	private boolean                            shuttingDown;

	public LogGui(Log log) {
		this.log = log;
	}

	public void display() throws SVNException {

		tmrTableRevalidate = new Timer("Revalidate table");
		SettingsStore settingsStore = Manager.getSettings();
		logTableModel = new TableModel<LogListItem>(new LogListItem());
		tblLog = new Table(logTableModel);
		tblLog.setRowSorter(new TableRowSorter<TableModel<LogListItem>>(logTableModel));
		tblLog.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

			public void valueChanged(ListSelectionEvent e) {
				refreshDetailView();
			}
		});
		JScrollPane spLog = new JScrollPane(tblLog);

		logDetailTableModel = new TableModel<LogDetailListItem>(new LogDetailListItem());
		tblDetailLog = new Table(logDetailTableModel);
		tblDetailLog.addMouseListener(new PopupupMouseListener());
		tblDetailLog.setRowSorter(new TableRowSorter<TableModel<LogDetailListItem>>(logDetailTableModel));
		new StatusCellRendererForLogDetailListItem(tblDetailLog);
		JScrollPane spDetailLog = new JScrollPane(tblDetailLog);

		taMessage = new TextArea();
		JScrollPane spMessage = new JScrollPane(taMessage);

		splDetail = new JSplitPane(JSplitPane.VERTICAL_SPLIT, spMessage, spDetailLog);
		if (settingsStore.getGuiLogSeparatorDetail() != null) {
			splDetail.setDividerLocation(settingsStore.getGuiLogSeparatorDetail());
		}
		splMain = new JSplitPane(JSplitPane.VERTICAL_SPLIT, spLog, splDetail);
		if (settingsStore.getGuiLogSeparatorMain() != null) {
			splMain.setDividerLocation(settingsStore.getGuiLogSeparatorMain());
		}

		// table.addMouseListener(new PopupupMouseListener());

		calFrom = new JDateChooser();
		calFrom.setEnabled(false);
		calTo = new JDateChooser();
		calTo.setEnabled(false);
		sfFilter = new EditField(20);
		sfFilter.setEnabled(false);
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

		Window window = Manager.createAndShowFrame(pnlMain, "Show Log");
		window.addWindowListener(new WindowAdapter() {

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
	}

	public synchronized void workEnded() throws Exception {
		prgWorkInProgress.stopProgress();
	}

	public void setStatus(ShowLogStatus status) {
		if (ShowLogStatus.COMPLETED.equals(status)) {
			btnStop.setEnabled(false);
		}
	}

	public void setStatusStartWorking() {
		setStatus(ShowLogStatus.STARTED);
	}

	public void setStatusStopWorking() {
		setStatus(ShowLogStatus.COMPLETED);
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
						logTableModel.addLines(lstLi);
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

	public void refreshDetailView() {
		int selectedRow = tblLog.getSelectedRow();
		if (selectedRow == -1) {
			return;
		}

		tblDetailLog.getSelectionModel().clearSelection();
		LogListItem liLog = logTableModel.getRow(tblDetailLog.convertRowIndexToModel(selectedRow));
		taMessage.setText(liLog.getMessage());
		logDetailTableModel.clear();
		for (SVNLogEntryPath liEntryPath : liLog.getChanges().values()) {
			LogDetailListItem liDetail = new LogDetailListItem();
			liDetail.setPath(liEntryPath.getPath());
			liDetail.setAction(getContentStatusByTypeChar(liEntryPath.getType()));
			liDetail.setCopyFromPath(liEntryPath.getCopyPath());
			liDetail.setRevision(liEntryPath.getCopyRevision() != -1 ? liEntryPath.getCopyRevision() : null);
			liDetail.setKind(liEntryPath.getKind());
			logDetailTableModel.addLine(liDetail);
		}
	}

	private class DetailShowChangesAction extends ThreadAction {

		private final PopupupMouseListener popupupMouseListener;

		public DetailShowChangesAction(PopupupMouseListener popupupMouseListener) {
			super("Show changes");
			this.popupupMouseListener = popupupMouseListener;
		}

		public void actionProcess(ActionEvent e) throws Exception {
			LogDetailListItem liDetail = popupupMouseListener.getSelected();
			LogListItem liLog = logTableModel.getRow(tblLog.getSelectedRow());
			log.showChanges(liDetail.getPath(), liLog.getRevision());
		}
	}

	private class DetailRevertChangesAction extends ThreadAction {

		private final PopupupMouseListener popupupMouseListener;

		public DetailRevertChangesAction(PopupupMouseListener popupupMouseListener) {
			super("Revert changes");
			this.popupupMouseListener = popupupMouseListener;
		}

		public void actionProcess(ActionEvent e) throws Exception {
			LogDetailListItem liDetail = popupupMouseListener.getSelected();
			LogListItem liLog = logTableModel.getRow(tblLog.getSelectedRow());
			log.revertChanges(liDetail.getPath(), liLog.getRevision());
		}
	}

	public class ShowMoreAction extends ThreadAction {

		public ShowMoreAction() {
			super("Show more");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			workStarted();
			try {
				List<LogListItem> allRetrivedLogs = logTableModel.getAllData();
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
				List<LogListItem> allRetrivedLogs = logTableModel.getAllData();
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

	private class PopupupMouseListener extends MouseAdapter {

		private JPopupMenu        ppVisible;
		private JPopupMenu        ppModified;
		private LogDetailListItem selected;

		public PopupupMouseListener() {
			ppModified = new JPopupMenu();
			ppModified.add(new DetailShowChangesAction(this));
			ppModified.add(new DetailRevertChangesAction(this));
		}

		public LogDetailListItem getSelected() {
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
				int row = tblDetailLog.rowAtPoint(p);
				selected = logDetailTableModel.getRow(row);

				if (selected.getAction().equals(ContentStatus.MODIFIED)) {
					ppVisible = ppModified;
					ppVisible.setInvoker(tblDetailLog);
					ppVisible.setLocation(e.getXOnScreen(), e.getYOnScreen());
					ppVisible.setVisible(true);
					e.consume();
				}
			}
			super.mousePressed(e);
		}
	}

}
