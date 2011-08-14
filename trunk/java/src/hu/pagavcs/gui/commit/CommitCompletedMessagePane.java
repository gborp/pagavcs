package hu.pagavcs.gui.commit;

import hu.pagavcs.bl.Manager;
import hu.pagavcs.bl.OnSwing;
import hu.pagavcs.bl.SettingsStore;
import hu.pagavcs.gui.platform.Label;
import hu.pagavcs.gui.platform.MessagePane;

import java.awt.Dialog.ModalityType;
import java.awt.Window;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class CommitCompletedMessagePane extends MessagePane {

	private JCheckBox cbAutoCopyToClipboard;

	public CommitCompletedMessagePane(Window parent, Icon icon, String title, String message, ModalityType modalityType) {
		super(parent, icon, title, message, modalityType);
	}

	public static OPTIONS showInfo(Window parent, String title, String message) {
		return execute(parent, Manager.getIconInformation(), title, message, ModalityType.DOCUMENT_MODAL);
	}

	protected static OPTIONS execute(Window parent, Icon icon, String title, String message, ModalityType modalityType) {
		return new CommitCompletedMessagePane(parent, icon, title, message, modalityType).execute();
	}

	public JComponent getDisplayComponent() {
		JPanel panel = new JPanel(new FormLayout("p:g", "p:g,2dlu,p,2dlu,p"));
		CellConstraints cc = new CellConstraints();

		Label lblMessage = new Label(message);
		lblMessage.setHorizontalAlignment(SwingConstants.CENTER);
		lblMessage.setIcon(icon);
		final JButton btnOk = new JButton(new OkAction());

		cbAutoCopyToClipboard = new JCheckBox("Copy to clipboard");
		if (Boolean.TRUE.equals(SettingsStore.getInstance().getAutoCopyCommitRevisionToClipboard())) {
			cbAutoCopyToClipboard.setSelected(true);
		}

		panel.add(lblMessage, cc.xywh(1, 1, 1, 1, CellConstraints.LEFT, CellConstraints.DEFAULT));
		panel.add(cbAutoCopyToClipboard, cc.xywh(1, 3, 1, 1, CellConstraints.LEFT, CellConstraints.DEFAULT));
		panel.add(btnOk, cc.xywh(1, 5, 1, 1, CellConstraints.CENTER, CellConstraints.DEFAULT));
		try {
			new OnSwing(true) {

				protected void process() throws Exception {
					btnOk.requestFocus();
				}

			}.run();
		} catch (Exception ex) {
			Manager.handle(ex);
		}

		return panel;
	}

	public OPTIONS execute() {
		OPTIONS result = super.execute();

		SettingsStore.getInstance().setAutoCopyCommitRevisionToClipboard(cbAutoCopyToClipboard.isSelected());
		if (cbAutoCopyToClipboard.isSelected()) {
			Manager.setClipboard(message);
		}

		return result;
	}

}
