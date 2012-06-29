package hu.pagavcs.client.gui.properties;

import hu.pagavcs.client.gui.platform.EditField;
import hu.pagavcs.client.gui.platform.Frame;
import hu.pagavcs.client.gui.platform.GuiHelper;
import hu.pagavcs.client.gui.platform.Label;
import hu.pagavcs.client.gui.platform.TableModel;
import hu.pagavcs.client.gui.platform.TextArea;
import hu.pagavcs.client.operation.PropertiesOperation;

import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class EditAddProperty {

	JDialog dlgEditProp;
	private EditField sfName;
	private TextArea taValue;
	private JCheckBox cbRecursively;
	private boolean editMode;

	public void execute(final PropertiesOperation propertiesOperation,
			final Frame frame,
			final TableModel<PropertiesListItem> tmdlProperties,
			final PropertiesListItem propertiesListItem) {

		CellConstraints cc = new CellConstraints();
		FormLayout lyMain = new FormLayout("r:p,2dlu,f:200:g,p,2dlu,p",
				"p,2dlu,f:200:g,2dlu,p");
		JPanel pnlMain = new JPanel(lyMain);

		sfName = new EditField(16);
		sfName.setToolTipText("<html>Example:<br>svn:ignore");
		if (propertiesListItem.getKey() != null) {
			sfName.setText(propertiesListItem.getKey());
			sfName.setEnabled(false);
			editMode = true;
		}

		taValue = new TextArea();
		taValue.setColumns(40);
		taValue.setRows(10);
		taValue.setText(propertiesListItem.getValue());
		JButton btnOk = new JButton("Ok");
		btnOk.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				String value = taValue.getText();
				// if
				// (propertiesOperation.needsValueValidation(propertiesListItem
				// .getKey())) {
				value = propertiesOperation.validateProperty(value);
				// }
				if (!editMode) {
					propertiesListItem.setKey(sfName.getText());
				}
				propertiesListItem.setValue(value);
				propertiesListItem.setDisplayValue(propertiesOperation
						.valueToDisplayValue(value));
				propertiesListItem.setRecoursively(cbRecursively.isSelected());
				propertiesOperation.modifyProperty(propertiesListItem);
				if (editMode) {
					tmdlProperties.fireTableDataChanged();
				} else {
					tmdlProperties.addLine(propertiesListItem);
				}
				dlgEditProp.setVisible(false);
			}
		});

		cbRecursively = new JCheckBox("Apply recursively");

		pnlMain.add(new Label("Key"), cc.xy(1, 1));
		pnlMain.add(sfName,
				cc.xywh(3, 1, 4, 1, CellConstraints.FILL, CellConstraints.FILL));
		pnlMain.add(new JScrollPane(taValue),
				cc.xywh(1, 3, 6, 1, CellConstraints.FILL, CellConstraints.FILL));
		pnlMain.add(btnOk, cc.xy(6, 5));
		pnlMain.add(cbRecursively, cc.xy(4, 5));

		dlgEditProp = GuiHelper.createDialog(frame, pnlMain,
				"Edit svn property");
		dlgEditProp.setModalityType(ModalityType.DOCUMENT_MODAL);
		if (propertiesListItem.getKey() != null) {
			dlgEditProp.setTitle("Edit " + propertiesListItem.getKey()
					+ " property");
		} else {
			dlgEditProp.setTitle("Add new property");
		}
		dlgEditProp.setVisible(true);
	}
}
