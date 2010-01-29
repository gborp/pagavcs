package hu.pagavcs.gui;

import hu.pagavcs.bl.Manager;
import hu.pagavcs.gui.platform.EditField;
import hu.pagavcs.gui.platform.Label;
import hu.pagavcs.operation.Checkout;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
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
public class CheckoutGui {

	private Checkout  checkout;
	private JComboBox cboUrl;
	private EditField sfDir;
	private EditField sfRevision;
	private JButton   btnCheckout;

	public CheckoutGui(Checkout checkout) {
		this.checkout = checkout;
	}

	public void display() throws SVNException {

		JPanel pnlMain = new JPanel(new BorderLayout());

		Label lblUrl = new Label("Url:");
		cboUrl = new JComboBox();
		cboUrl.setEditable(true);

		Label lblDir = new Label("Dir:");
		sfDir = new EditField(20);
		sfDir.setText(checkout.getPath());
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
		JPanel pnlInner = new JPanel(new GridLayout(4, 2));
		pnlInner.add(lblUrl);
		pnlInner.add(cboUrl);
		pnlInner.add(lblDir);
		pnlInner.add(sfDir);
		pnlInner.add(lblRevison);
		pnlInner.add(sfRevision);
		pnlInner.add(btnCheckout);

		pnlMain.add(pnlInner, BorderLayout.CENTER);
		Manager.createAndShowFrame(pnlMain, "Checkout Settings");
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
			throw new RuntimeException("Dir can't be empty");
		}
		if (url.isEmpty()) {
			throw new RuntimeException("Url can't be empty");
		}
		long numberRevision = Checkout.HEAD_REVISION;
		if (!revision.isEmpty()) {
			numberRevision = Long.valueOf(revision);
		}
		checkout.doCheckout(url, dir, numberRevision);
	}

}
