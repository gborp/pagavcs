/**
 * 
 */
package hu.pagavcs.gui.log;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

class CopyLineToClipboard extends AbstractAction {

	private final LogGui logGui;

	public CopyLineToClipboard(LogGui logGui) {
		super("Copy to clipboard");
		this.logGui = logGui;
	}

	public void actionPerformed(ActionEvent e) {
		this.logGui.copyLogListItemsToClipboard(this.logGui.getSelectedLogItems());
	}
}
