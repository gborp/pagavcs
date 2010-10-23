/**
 * 
 */
package hu.pagavcs.gui.log;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

class CopyDetailLineToClipboard extends AbstractAction {

	/**
     * 
     */
    private final LogGui logGui;

	public CopyDetailLineToClipboard(LogGui logGui) {
		super("Copy selected lines to clipboard");
		this.logGui = logGui;
	}

	public void actionPerformed(ActionEvent e) {
		this.logGui.copyLogDetailListItemsToClipboard(this.logGui.getSelectedDetailLogItems());
	}
}