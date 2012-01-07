package hu.pagavcs.client.bl;

import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

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
public class WindowPreferencesSaverOnClose extends WindowAdapter {

	private final String windowName;
	private final Window parent;

	public WindowPreferencesSaverOnClose(Window parent, String windowName) {
		this.parent = parent;
		this.windowName = windowName;
	}

	public void windowClosing(WindowEvent e) {
		Window window = e.getWindow();
		if (window.isVisible()) {
			Manager.getSettings().setWindowBounds(parent, windowName, window.getBounds());
		}
	}

	public void windowClosed(WindowEvent e) {
		try {
			Manager.getSettings().save();
		} catch (Exception ex) {
			Manager.handle(ex);
		}
	}
}
