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

public class ShowMoreAction extends ThreadAction {

	/**
     * 
     */
    private final LogGui logGui;

	public ShowMoreAction(LogGui logGui) {
		super("Show more");
		this.logGui = logGui;
	}

	public void actionProcess(ActionEvent e) throws Exception {
		this.logGui.workStarted();
		try {
			List<LogListItem> allRetrivedLogs = this.logGui.tmdlLog.getAllData();
			if (!allRetrivedLogs.isEmpty()) {
				LogListItem lastLi = allRetrivedLogs.get(allRetrivedLogs.size() - 1);
				if (lastLi.getRevision() > 1) {
					this.logGui.log.doShowLog(SVNRevision.create(lastLi.getRevision() + 1), Log.LIMIT);
				}
			}
			this.logGui.workEnded();
		} catch (Exception ex) {
			this.logGui.workEnded();
			throw ex;
		}
	}

}