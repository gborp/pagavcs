/**
 * 
 */
package hu.pagavcs.gui.log;

import hu.pagavcs.bl.ThreadAction;
import hu.pagavcs.gui.LogListItem;
import hu.pagavcs.operation.Log;

import java.awt.event.ActionEvent;
import java.util.List;

import org.tmatesoft.svn.core.wc.SVNRevision;

public class ShowAllAction extends ThreadAction {

	private final LogGui logGui;

	public ShowAllAction(LogGui logGui) {
		super("Show all");
		this.logGui = logGui;
	}

	public void actionProcess(ActionEvent e) throws Exception {
		this.logGui.workStarted();
		try {
			List<LogListItem> allRetrivedLogs = this.logGui.getAllTableDataFromMain();
			if (!allRetrivedLogs.isEmpty()) {
				LogListItem lastLi = allRetrivedLogs.get(allRetrivedLogs.size() - 1);
				if (lastLi.getRevision() > 1) {
					logGui.doShowLog(SVNRevision.create(lastLi.getRevision() + 1), Log.NO_LIMIT);
				}
			} else {
				logGui.doShowLog(SVNRevision.HEAD, Log.NO_LIMIT);
			}
			this.logGui.workEnded();
		} catch (Exception ex) {
			this.logGui.workEnded();
			throw ex;
		}
	}

}
