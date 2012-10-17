package hu.pagavcs.client.gui;

import hu.pagavcs.client.bl.Manager;
import hu.pagavcs.client.bl.SettingsStore;
import hu.pagavcs.client.gui.platform.EditField;
import hu.pagavcs.client.gui.platform.GuiHelper;
import hu.pagavcs.client.gui.platform.Label;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

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
public class LoginGui {

	private EditField      sfUsername;
	private JPasswordField sfPassword;
	private JButton        btnLogin;
	private Object         logMeIn = new Object();
	private Window         window;
	private final String   predefinedUsername;
	private final String   predefinedPassword;
	private boolean        loginButtonPressed;
	private JCheckBox      cbRemember;

	public LoginGui(String username, String password) {
		this.predefinedUsername = username;
		this.predefinedPassword = password;

	}

	public void display() {
		JPanel pnlMain = new JPanel(new FormLayout("r:p,2dlu, p:g,p", "p,2dlu,p,2dlu,p,2dlu,p"));
		CellConstraints cc = new CellConstraints();

		cbRemember = new JCheckBox();
		cbRemember.setMnemonic('r');
		cbRemember.setToolTipText("Remember username and password (ALT+R)");
		cbRemember.setFocusable(false);
		Label lblUsername = new Label("Username:");
		lblUsername.setToolTipText("Remember username and password (ALT+R)");
		sfUsername = new EditField(predefinedUsername, 20);
		sfUsername.setToolTipText("Leave it empty if username is not required");

		Label lblPassword = new Label("Password:");
		sfPassword = new JPasswordField(predefinedPassword, 20);
		btnLogin = new JButton("Login");
		btnLogin.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				loginButtonPressed = true;

				SettingsStore settings = Manager.getSettings();
				settings.setRememberUsername(cbRemember.isSelected());

				synchronized (logMeIn) {
					logMeIn.notifyAll();
				}
			}
		});

		pnlMain.add(lblUsername, cc.xywh(1, 1, 1, 1));
		pnlMain.add(sfUsername, cc.xywh(3, 1, 2, 1));
		pnlMain.add(lblPassword, cc.xywh(1, 3, 1, 1));
		pnlMain.add(sfPassword, cc.xywh(3, 3, 2, 1));
		pnlMain.add(cbRemember, cc.xywh(1, 5, 1, 1));
		pnlMain.add(new Label("Remember"), cc.xywh(3, 5, 2, 1));
		pnlMain.add(btnLogin, cc.xywh(4, 7, 1, 1));

		SettingsStore settings = Manager.getSettings();
		cbRemember.setSelected(Boolean.TRUE.equals(settings.getRememberPassword()));

		sfUsername.addKeyListener(new KeyAdapter() {

			public void keyTyped(KeyEvent e) {
				if (e.getKeyChar() == '\n') {
					sfPassword.requestFocus();
					e.consume();
				}
			}
		});

		sfPassword.addKeyListener(new KeyAdapter() {

			public void keyTyped(KeyEvent e) {
				if (e.getKeyChar() == '\n') {
					btnLogin.doClick();
					e.consume();
				}
			}
		});

		window = GuiHelper.createAndShowFrame(pnlMain, "Login");
		window.addWindowListener(new WindowAdapter() {

			public void windowClosing(WindowEvent e) {

				synchronized (logMeIn) {
					logMeIn.notifyAll();
				}
			}
		});

	}

	/**
	 * @return true: login button is pressed; false: cancel.
	 */
	public boolean waitForLoginButton() {
		try {
			synchronized (logMeIn) {
				logMeIn.wait();
			}
		} catch (InterruptedException e) {
			Manager.handle(e);
		}

		window.setVisible(false);
		window.dispose();
		return loginButtonPressed;
	}

	public String getPredefinedUsername() {
		return sfUsername.getText().trim();
	}

	public String getPredefinedPassword() {
		return new String(sfPassword.getPassword()).trim();
	}

	public boolean isRememberChecked() {
		return cbRemember.isSelected();
	}

}
