package hu.pagavcs.gui.platform;

import hu.pagavcs.bl.Manager;
import hu.pagavcs.bl.ThreadAction;

import java.awt.Window;
import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class MessagePane {

	public enum OPTIONS {
		OK, CANCEL
	}

	private final Window       parent;
	private final String       title;
	private final String       message;
	private OPTIONS            optionChoosed;
	private JDialog            dialog;
	private final Icon         icon;
	private final ModalityType modalityType;

	public static OPTIONS showError(Window parent, String title, String message) {
		return execute(parent, Manager.ICON_ERROR, title, message, ModalityType.DOCUMENT_MODAL);
	}

	public static OPTIONS showWarning(Window parent, String title, String message) {
		return execute(parent, Manager.ICON_WARNING, title, message, ModalityType.DOCUMENT_MODAL);
	}

	public static OPTIONS showInfo(Window parent, String title, String message) {
		return execute(parent, Manager.ICON_INFORMATION, title, message, ModalityType.DOCUMENT_MODAL);
	}

	private static OPTIONS execute(Window parent, Icon icon, String title, String message, ModalityType modalityType) {
		return new MessagePane(parent, icon, title, message, modalityType).execute();
	}

	public MessagePane(Window parent, Icon icon, String title, String message, ModalityType modalityType) {
		this.parent = parent;
		this.icon = icon;
		this.title = title;
		this.message = message;
		this.modalityType = modalityType;
	}

	public OPTIONS execute() {
		JPanel panel = new JPanel(new FormLayout("p:g", "p:g,4dlu,p"));
		CellConstraints cc = new CellConstraints();

		Label lblMessage = new Label(message);
		lblMessage.setHorizontalAlignment(SwingConstants.CENTER);
		lblMessage.setIcon(icon);
		JButton btnOk = new JButton(new OkAction());

		panel.add(lblMessage, cc.xywh(1, 1, 1, 1));
		panel.add(btnOk, cc.xywh(1, 3, 1, 1, CellConstraints.CENTER, CellConstraints.DEFAULT));

		dialog = GuiHelper.createDialog(parent, panel, title);
		dialog.setModalityType(modalityType);
		dialog.setVisible(true);
		dialog.dispose();

		if (optionChoosed == null) {
			optionChoosed = OPTIONS.CANCEL;
		}
		return optionChoosed;
	}

	private class OkAction extends ThreadAction {

		public OkAction() {
			super("Ok");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			optionChoosed = OPTIONS.OK;
			dialog.setVisible(false);
		}
	}
}
