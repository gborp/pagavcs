package hu.pagavcs.gui;

import hu.pagavcs.bl.Manager;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;

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

	public LoginGui(String username, String password) {
		this.predefinedUsername = username;
		this.predefinedPassword = password;

	}

	public void display() {
		FormLayout layout = new FormLayout("4dlu,p,4dlu, p:g,p,4dlu", "4dlu,p,4dlu,p,4dlu,p,4dlu");
		JPanel pnlMain = new JPanel(layout);
		CellConstraints cc = new CellConstraints();

		Label lblUsername = new Label("UserName:");
		sfUsername = new EditField(predefinedUsername, 20);
		Label lblPassword = new Label("Password:");
		sfPassword = new JPasswordField(predefinedPassword, 20);
		btnLogin = new JButton("Login");
		btnLogin.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				loginButtonPressed = true;
				synchronized (logMeIn) {
					logMeIn.notifyAll();
				}
			}
		});
		pnlMain.add(lblUsername, cc.xywh(2, 2, 1, 1));
		pnlMain.add(sfUsername, cc.xywh(4, 2, 2, 1));
		pnlMain.add(lblPassword, cc.xywh(2, 4, 1, 1));
		pnlMain.add(sfPassword, cc.xywh(4, 4, 2, 1));
		pnlMain.add(btnLogin, cc.xywh(5, 6, 1, 1));

		window = Manager.createAndShowFrame(new JScrollPane(pnlMain), "Login");
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
		return sfPassword.getText().trim();
	}

}
