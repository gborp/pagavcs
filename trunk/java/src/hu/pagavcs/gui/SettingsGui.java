package hu.pagavcs.gui;

import hu.pagavcs.bl.Manager;
import hu.pagavcs.operation.Settings;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.tmatesoft.svn.core.SVNException;

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

		JPanel pnlMain = new JPanel(new GridLayout(1, 1));
		JButton btnClearLogin = new JButton("Clear saved login name/password");
		btnClearLogin.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				int choice = JOptionPane.showConfirmDialog(Manager.getRootFrame(), "Are you sure you want to delete all saved login and password information?",
				        "Deleting login/password cache", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
				if (choice == JOptionPane.YES_OPTION) {
					clearLogin();
				}
			}
		});

		pnlMain.add(btnClearLogin);

		Manager.createAndShowFrame(pnlMain, "Settings");
	}

	public void clearLogin() {
		Manager.getSettings().clearLogin();
	}

}
