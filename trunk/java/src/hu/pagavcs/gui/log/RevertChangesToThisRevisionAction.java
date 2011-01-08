/**
 * 
 */
package hu.pagavcs.gui.log;

import hu.pagavcs.bl.ThreadAction;

import java.awt.event.ActionEvent;

class RevertChangesToThisRevisionAction extends ThreadAction {

	private final LogGui logGui;

	public RevertChangesToThisRevisionAction(LogGui logGui) {
		super("Revert changes to this revision");
		this.logGui = logGui;
	}

	public void actionProcess(ActionEvent e) throws Exception {
		long rev = this.logGui.getSelectedLogItems().get(0).getRevision();
		logGui.revertChangesToThisRevisionExact((rev + 1) + "-working");
	}
}
