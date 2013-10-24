package hu.pagavcs.client.gui.platform;

import javax.swing.JTextArea;

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
public class TextArea extends JTextArea {

	public TextArea() {
		super();
		init();
	}

	public TextArea(String text) {
		super(text);
		init();
	}

	private void init() {
		GuiHelper.addPopupMenu(this);
		GuiHelper.addUndoRedo(this);
	}

}
