package hu.pagavcs.gui;

import hu.pagavcs.bl.Manager;
import hu.pagavcs.gui.platform.EditField;
import hu.pagavcs.gui.platform.Frame;
import hu.pagavcs.gui.platform.GuiHelper;
import hu.pagavcs.gui.platform.Label;
import hu.pagavcs.gui.platform.MessagePane;
import hu.pagavcs.operation.Checkout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
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
public class CheckoutGui {

	private Checkout  checkout;
	private JComboBox cboUrl;
	private EditField sfDir;
	private EditField sfRevision;
	private JButton   btnCheckout;
	private Frame     frame;

	public CheckoutGui(Checkout checkout) {
		this.checkout = checkout;
	}

	public void display() throws SVNException {

		CellConstraints cc = new CellConstraints();
		JPanel pnlMain = new JPanel(new FormLayout("r:p,4dlu,p:g,p", "p,4dlu,p,4dlu,p,4dlu,p"));

		Label lblUrl = new Label("Url:");
		cboUrl = new JComboBox();
		cboUrl.setEditable(true);
		if (checkout.getUrl() != null) {
			cboUrl.setSelectedItem(checkout.getUrl());
		}

		Label lblDir = new Label("Dir:");
		sfDir = new EditField(20);
		if (checkout.getPath() != null) {
			sfDir.setText(checkout.getPath());
		}
		Label lblRevison = new Label("Revision (empty for head):");
		sfRevision = new EditField(20);
		btnCheckout = new JButton("Checkout");
		btnCheckout.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable() {

					public void run() {
						try {
							checkout();
						} catch (Exception e) {
							Manager.handle(e);
						}
					}
				}).start();
			}
		});
		pnlMain.add(lblUrl, cc.xy(1, 1));
		pnlMain.add(cboUrl, cc.xywh(3, 1, 2, 1));
		pnlMain.add(lblDir, cc.xy(1, 3));
		pnlMain.add(sfDir, cc.xywh(3, 3, 2, 1));
		pnlMain.add(lblRevison, cc.xy(1, 5));
		pnlMain.add(sfRevision, cc.xywh(3, 5, 2, 1));
		pnlMain.add(btnCheckout, cc.xy(4, 7));

		frame = GuiHelper.createAndShowFrame(pnlMain, "Checkout Settings");
	}

	public void setUrlHistory(String[] urlHistory) {
		ComboBoxModel modelUrl = new DefaultComboBoxModel(urlHistory);
		cboUrl.setModel(modelUrl);
	}

	public void checkout() throws Exception {

		String dir = sfDir.getText().trim();
		String url = cboUrl.getSelectedItem().toString().trim();
		String revision = sfRevision.getText().trim();

		checkout.storeUrlForHistory(url);

		if (dir.isEmpty()) {
			MessagePane.showError(frame, "Error", "Directory must be set");
			return;
		}
		if (url.isEmpty()) {
			MessagePane.showError(frame, "Error", "Url must be set");
			return;
		}
		long numberRevision = Checkout.HEAD_REVISION;
		if (!revision.isEmpty()) {
			numberRevision = Long.valueOf(revision);
		}
		checkout.doCheckout(url, dir, numberRevision);
	}

}
