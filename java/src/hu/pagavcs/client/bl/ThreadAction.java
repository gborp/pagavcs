package hu.pagavcs.client.bl;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;

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
public abstract class ThreadAction extends AbstractAction {

	public ThreadAction(String string, Icon icon) {
		super(string);
		if (icon != null) {
			setIcon(icon);
		}
	}

	public ThreadAction(String string) {
		this(string, null);
	}

	public void actionPerformed(final ActionEvent e) {
		new Thread(new Runnable() {

			public void run() {
				try {
					actionProcess(e);
				} catch (Exception e) {
					Manager.handle(e);
				}
			}

		}).start();
	}

	public void setLabel(String label) {
		putValue(Action.NAME, label);
	}

	public void setTooltip(String label) {
		putValue(Action.SHORT_DESCRIPTION, label);
	}

	public void setIcon(Icon icon) {
		putValue(Action.SMALL_ICON, icon);
	}

	public abstract void actionProcess(ActionEvent e) throws Exception;

}
