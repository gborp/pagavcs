package hu.pagavcs.gui.platform;

import hu.pagavcs.bl.Manager;
import hu.pagavcs.bl.WindowPreferencesSaverOnClose;

import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.KeyStroke;

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
public class Frame extends JFrame {

	private final String applicationName;

	public Frame(String applicationName, String iconName) {
		super();
		this.applicationName = applicationName;
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setIconImage(Toolkit.getDefaultToolkit().getImage(Manager.class.getResource(iconName)));

		getRootPane().registerKeyboardAction(new ActionListener() {

			public void actionPerformed(ActionEvent actionEvent) {
				setVisible(false);
			}
		}, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
	}

	public void setTitlePrefix(String prefix) {
		setTitle(prefix + " - " + getTitle());
	}

	public void execute() {
		pack();

		Rectangle bounds = Manager.getSettings().getWindowBounds(applicationName);
		if (bounds != null) {
			setBounds(bounds);
		} else {
			GuiHelper.centerScreen(this);
		}

		addWindowListener(new WindowPreferencesSaverOnClose(applicationName));
		setVisible(true);
	}
}
