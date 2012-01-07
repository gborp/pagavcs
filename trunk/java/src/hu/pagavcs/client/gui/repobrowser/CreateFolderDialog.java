package hu.pagavcs.client.gui.repobrowser;

import hu.pagavcs.client.gui.platform.EditField;
import hu.pagavcs.client.gui.platform.GuiHelper;
import hu.pagavcs.client.gui.platform.Label;

import java.awt.Window;
import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

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
class CreateFolderDialog {

	private EditField sfFolderName;
	private EditField sfLogMessage;
	private JButton   btnCreate;
	private JButton   btnCancel;
	private boolean   doCreate;
	private JDialog   window;

	public CreateFolderDialog() {}

	public void display(Window parent) {
		JPanel pnlMain = new JPanel(new FormLayout("p,2dlu,p:g,p,2dlu,p", "p,2dlu,p,2dlu,p"));
		CellConstraints cc = new CellConstraints();

		Label lblUsername = new Label("New folder name:");
		sfFolderName = new EditField(20);

		Label lblLogMessage = new Label("Log message:");
		sfLogMessage = new EditField(20);

		btnCreate = new JButton("Create");
		btnCreate.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				doCreate = true;

				window.setVisible(false);
			}
		});

		btnCancel = new JButton("Cancel");
		btnCancel.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				doCreate = false;
				window.setVisible(false);
			}
		});

		pnlMain.add(lblUsername, cc.xywh(1, 1, 1, 1));
		pnlMain.add(sfFolderName, cc.xywh(3, 1, 4, 1));
		pnlMain.add(lblLogMessage, cc.xywh(1, 3, 1, 1));
		pnlMain.add(sfLogMessage, cc.xywh(3, 3, 4, 1));
		pnlMain.add(btnCreate, cc.xywh(4, 5, 1, 1));
		pnlMain.add(btnCancel, cc.xywh(6, 5, 1, 1));

		sfFolderName.addKeyListener(new KeyAdapter() {

			public void keyTyped(KeyEvent e) {
				if (e.getKeyChar() == '\n') {
					sfLogMessage.requestFocus();
					e.consume();
				}
			}
		});

		sfLogMessage.addKeyListener(new KeyAdapter() {

			public void keyTyped(KeyEvent e) {
				if (e.getKeyChar() == '\n') {
					btnCreate.doClick();
					e.consume();
				}
			}
		});

		window = GuiHelper.createDialog(parent, pnlMain, "Create Folder");

		window.setModalityType(ModalityType.DOCUMENT_MODAL);
		window.setVisible(true);
		window.dispose();
	}

	public boolean isDoCreate() {
		return doCreate;
	}

	public String getFolderName() {
		return sfFolderName.getText();
	}

	public String getLogMessage() {
		return sfLogMessage.getText();
	}
}
