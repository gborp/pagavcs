/**
 * 
 */
package hu.pagavcs.client.gui.log;

import hu.pagavcs.client.bl.OnSwing;
import hu.pagavcs.client.bl.ThreadAction;
import hu.pagavcs.client.gui.LogListItem;
import hu.pagavcs.client.operation.Log;

import java.awt.event.ActionEvent;
import java.util.List;

import org.tmatesoft.svn.core.wc.SVNRevision;

public class ShowMoreUntilFoundAction extends ThreadAction {

	private final LogGui logGui;

	public ShowMoreUntilFoundAction(LogGui logGui) {
		super("More until found");
		this.logGui = logGui;
	}

	public void actionProcess(ActionEvent e) throws Exception {
		this.logGui.workStarted();
		try {
			List<LogListItem> allRetrivedLogs = this.logGui
					.getAllTableDataFromMain();
			if (!allRetrivedLogs.isEmpty()) {
				LogListItem lastLi = allRetrivedLogs
						.get(allRetrivedLogs.size() - 1);
				if (lastLi.getRevision() > 1) {
					logGui.doShowLog(
							SVNRevision.create(lastLi.getRevision() + 1),
							Log.LIMIT);
				}
			} else {
				logGui.doShowLog(SVNRevision.HEAD, Log.LIMIT);
			}
			this.logGui.workEnded();
			new OnSwing(true) {

				@Override
				protected void process() throws Exception {
					// TODO Auto-generated method stub

				}
			};
		} catch (Exception ex) {
			this.logGui.workEnded();
			throw ex;
		}
	}

}
