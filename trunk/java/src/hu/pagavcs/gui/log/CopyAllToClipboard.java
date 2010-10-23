/**
 * 
 */
package hu.pagavcs.gui.log;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

class CopyAllToClipboard extends AbstractAction {

	private final LogGui logGui;

	public CopyAllToClipboard(LogGui logGui) {
		super("Copy all to clipboard");
		this.logGui = logGui;
	}

	public void actionPerformed(ActionEvent e) {
		this.logGui.copyLogListItemsToClipboard(this.logGui.getAllTableDataFromMain());
	}
}
