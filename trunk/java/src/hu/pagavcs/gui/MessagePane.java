package hu.pagavcs.gui;

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

	private final Window parent;
	private final String title;
	private final String message;
	private OPTIONS      optionChoosed;
	private JDialog      dialog;
	private final Icon   icon;

	public static OPTIONS showError(Window parent, String title, String message) {
		return execute(parent, Manager.ICON_ERROR, title, message);
	}

	public static OPTIONS showWarning(Window parent, String title, String message) {
		return execute(parent, Manager.ICON_WARNING, title, message);
	}

	public static OPTIONS showInfo(Window parent, String title, String message) {
		return execute(parent, Manager.ICON_INFORMATION, title, message);
	}

	private static OPTIONS execute(Window parent, Icon icon, String title, String message) {
		return new MessagePane(parent, icon, title, message).execute();
	}

	public MessagePane(Window parent, Icon icon, String title, String message) {
		this.parent = parent;
		this.icon = icon;
		this.title = title;
		this.message = message;
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
