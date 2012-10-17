/**
 * PagaVCS is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.<br>
 * <br>
 * PagaVCS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.<br>
 * <br>
 * You should have received a copy of the GNU General Public License along with
 * PagaVCS; If not, see http://www.gnu.org/licenses/.
 */
package hu.pagavcs.client.gui.log;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

class CopyDetailLineToClipboard extends AbstractAction {

	/**
     * 
     */
	private final LogGui logGui;

	public CopyDetailLineToClipboard(LogGui logGui) {
		super("Copy to clipboard");
		this.logGui = logGui;
	}

	public void actionPerformed(ActionEvent e) {
		this.logGui.copyLogDetailListItemsToClipboard(this.logGui.getSelectedDetailLogItems(), LogDetailCopyToClipboardType.DETAIL);
	}
}
