package hu.pagavcs.gui.platform;

import hu.pagavcs.bl.Manager;
import hu.pagavcs.bl.OnSwing;
import hu.pagavcs.bl.ThreadAction;

import java.awt.Window;
import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class MessagePane {

	public enum OPTIONS {
		OK, CANCEL
	}

	protected final Window       parent;
	protected final String       title;
	protected final String       message;
	protected OPTIONS            optionChoosed;
	protected JDialog            dialog;
	protected final Icon         icon;
	protected final ModalityType modalityType;

	public static OPTIONS showError(Window parent, String title, String message) {
		return execute(parent, Manager.ICON_ERROR, title, message, ModalityType.DOCUMENT_MODAL);
	}

	public static OPTIONS showWarning(Window parent, String title, String message) {
		return execute(parent, Manager.ICON_WARNING, title, message, ModalityType.DOCUMENT_MODAL);
	}

	public static OPTIONS showInfo(Window parent, String title, String message) {
		return execute(parent, Manager.ICON_INFORMATION, title, message, ModalityType.DOCUMENT_MODAL);
	}

	protected static OPTIONS execute(Window parent, Icon icon, String title, String message, ModalityType modalityType) {
		return new MessagePane(parent, icon, title, message, modalityType).execute();
	}

	public MessagePane(Window parent, Icon icon, String title, String message, ModalityType modalityType) {
		this.parent = parent;
		this.icon = icon;
		this.title = title;
		this.message = message;
		this.modalityType = modalityType;
	}

	public JComponent getDisplayComponent() {
		JPanel panel = new JPanel(new FormLayout("p:g", "p:g,2dlu,p"));
		CellConstraints cc = new CellConstraints();

		Label lblMessage = new Label(StringHelper.convertMultilineTextToHtml(message));
		lblMessage.setHorizontalAlignment(SwingConstants.CENTER);
		lblMessage.setIcon(icon);
		JButton btnOk = new JButton(new OkAction());

		panel.add(lblMessage, cc.xywh(1, 1, 1, 1));
		panel.add(btnOk, cc.xywh(1, 3, 1, 1, CellConstraints.CENTER, CellConstraints.DEFAULT));

		return panel;
	}

	public OPTIONS execute() {
		dialog = GuiHelper.createDialog(parent, getDisplayComponent(), title);
		dialog.setModalityType(modalityType);
		dialog.setVisible(true);
		dialog.dispose();

		if (optionChoosed == null) {
			optionChoosed = OPTIONS.CANCEL;
		}
		return optionChoosed;
	}

	protected class OkAction extends ThreadAction {

		public OkAction() {
			super("Ok");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			optionChoosed = OPTIONS.OK;
			new OnSwing() {

				protected void process() throws Exception {
					dialog.setVisible(false);
				}
			}.run();
		}
	}
}
