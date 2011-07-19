/**
 * 
 */
package hu.pagavcs.gui.log;

import hu.pagavcs.bl.ThreadAction;
import hu.pagavcs.gui.LogDetailListItem;
import hu.pagavcs.operation.ContentStatus;

import java.awt.event.ActionEvent;

class DetailRevertChangesFromThisRevisionAction extends ThreadAction {

	private final LogGui logGui;

	public DetailRevertChangesFromThisRevisionAction(LogGui logGui) {
		super("Revert changes from this revision");
		this.logGui = logGui;
	}

	public void actionProcess(ActionEvent e) throws Exception {
		for (LogDetailListItem liDetail : this.logGui.getSelectedDetailLogItems()) {

			if (liDetail.getAction().equals(ContentStatus.ADDED)) {

			}
			logGui.revertChanges(liDetail.getPath(), liDetail.getRevision());
		}
	}
}
