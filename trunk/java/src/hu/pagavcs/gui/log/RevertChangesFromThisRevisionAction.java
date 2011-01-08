/**
 * 
 */
package hu.pagavcs.gui.log;

import hu.pagavcs.bl.ThreadAction;
import hu.pagavcs.gui.LogListItem;

import java.awt.event.ActionEvent;

class RevertChangesFromThisRevisionAction extends ThreadAction {

	private final LogGui logGui;

	public RevertChangesFromThisRevisionAction(LogGui logGui) {
		super("Revert changes from this revision");
		this.logGui = logGui;
	}

	public void actionProcess(ActionEvent e) throws Exception {

		for (LogListItem liLog : this.logGui.getSelectedLogItems()) {
			logGui.revertChangesExact(liLog.getRevision());
		}
	}
}
