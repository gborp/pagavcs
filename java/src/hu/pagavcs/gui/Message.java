package hu.pagavcs.gui;

import hu.pagavcs.bl.Manager;
import hu.pagavcs.bl.ThreadAction;

import java.awt.Window;
import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class Message {

	public enum OPTIONS {
		OK, CANCEL
	}

	private final Window parent;
	private final String title;
	private final String message;
	private OPTIONS      optionChoosed;
	private JDialog      dialog;

	public static OPTIONS showError(Window parent, String title, String message) {
		return execute(parent, title, message);
	}

	public static OPTIONS showWarning(Window parent, String title, String message) {
		return execute(parent, title, message);
	}

	public static OPTIONS showInfo(Window parent, String title, String message) {
		return execute(parent, title, message);
	}

	private static OPTIONS execute(Window parent, String title, String message) {
		return new Message(parent, title, message).execute();
	}

	public Message(Window parent, String title, String message) {
		this.parent = parent;
		this.title = title;
		this.message = message;
	}

	public OPTIONS execute() {
		JPanel panel = new JPanel(new FormLayout("p:g,p", "p,4dlu,p"));
		CellConstraints cc = new CellConstraints();

		Label lblMessage = new Label(message);
		lblMessage.setHorizontalAlignment(SwingConstants.CENTER);
		JButton btnOk = new JButton(new OkAction());

		panel.add(lblMessage, cc.xywh(1, 1, 2, 1));
		panel.add(btnOk, cc.xywh(2, 3, 1, 1));

		dialog = Manager.createDialog(parent, panel, title);
		dialog.setModalityType(ModalityType.DOCUMENT_MODAL);
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
