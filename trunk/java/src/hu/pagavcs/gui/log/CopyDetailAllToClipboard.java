/**
 * 
 */
package hu.pagavcs.gui.log;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

class CopyDetailAllToClipboard extends AbstractAction {

	/**
     * 
     */
	private final LogGui logGui;

	public CopyDetailAllToClipboard(LogGui logGui) {
		super("Copy all to clipboard");
		this.logGui = logGui;
	}

	public void actionPerformed(ActionEvent e) {
		this.logGui.copyLogDetailListItemsToClipboard(this.logGui.getAllTableDataFromDetail());
	}
}
