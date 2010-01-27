package hu.pagavcs.gui;

import hu.pagavcs.bl.Manager;
import hu.pagavcs.bl.ThreadAction;
import hu.pagavcs.operation.Settings;

import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.tmatesoft.svn.core.SVNException;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

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
public class SettingsGui {

	private Settings settings;

	public SettingsGui(Settings settings) {
		this.settings = settings;
	}

	public void display() throws SVNException {
		FormLayout layout = new FormLayout("p", "p,4dlu,p,4dlu,p");
		JPanel pnlMain = new JPanel(layout);
		CellConstraints cc = new CellConstraints();

		JButton btnClearLogin = new JButton(new ClearLoginCacheAction());
		JButton btnShowIcons = new JButton(new ShowIconsInContextMenuAction());
		JButton btnShowLoginDialogNextTime = new JButton(new ShowLoginDialogNextTimeAction());

		pnlMain.add(btnClearLogin, cc.xy(1, 1));
		pnlMain.add(btnShowIcons, cc.xy(1, 3));
		pnlMain.add(btnShowLoginDialogNextTime, cc.xy(1, 5));

		Manager.createAndShowFrame(pnlMain, "Settings");
	}

	private class ClearLoginCacheAction extends ThreadAction {

		public ClearLoginCacheAction() {
			super("Clear saved login name/password");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			int choice = JOptionPane.showConfirmDialog(Manager.getRootFrame(), "Are you sure you want to delete all saved login and password information?",
			        "Deleting login/password cache", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
			if (choice == JOptionPane.YES_OPTION) {
				Manager.getSettings().clearLogin();
			}
		}

	}

	private class ShowIconsInContextMenuAction extends ThreadAction {

		public ShowIconsInContextMenuAction() {
			super("Show icons in context menu");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			Runtime.getRuntime().exec("gconftool-2 –type bool –set /desktop/gnome/interface/menus_have_icons true");
			// get the status:
			// gconftool-2 -g /desktop/gnome/interface/menus_have_icons
		}

	}

	private class ShowLoginDialogNextTimeAction extends ThreadAction {

		public ShowLoginDialogNextTimeAction() {
			super("Force showing login dialog next time");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			Manager.setForceShowingLoginDialogNextTime(true);
		}

	}

}
