package hu.pagavcs.client.gui.properties;

import hu.pagavcs.client.bl.Manager;
import hu.pagavcs.client.gui.platform.Frame;
import hu.pagavcs.client.gui.platform.GuiHelper;
import hu.pagavcs.client.gui.platform.Label;
import hu.pagavcs.client.gui.platform.Table;
import hu.pagavcs.client.gui.platform.TableModel;
import hu.pagavcs.client.gui.platform.TextArea;
import hu.pagavcs.client.operation.PropertiesOperation;

import java.awt.Dialog.ModalityType;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;

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
public class PropertiesGui {

	private Table<PropertiesListItem> tblProperties;
	private TableModel<PropertiesListItem> tmdlProperties;
	private JButton btnSave;
	private Label lblWorkingCopy;
	private Label lblRepo;
	private Frame frame;
	private SaveAction actSave;
	private Label lblInfo;
	private PropertiesOperation propertiesOperation;

	public PropertiesGui(PropertiesOperation propertiesOperation) {
		this.propertiesOperation = propertiesOperation;
	}

	public void display() {

		FormLayout lyTop = new FormLayout("r:p,2dlu,1dlu:g", "p,2dlu,p");
		JPanel pnlTop = new JPanel(lyTop);

		FormLayout lyBottom = new FormLayout("p,2dlu,1dlu:g,2dlu,p", "p");
		JPanel pnlBottom = new JPanel(lyBottom);

		FormLayout lyMain = new FormLayout("200dlu:g",
				"p,2dlu,fill:60dlu:g,2dlu,p");
		JPanel pnlMain = new JPanel(lyMain);

		CellConstraints cc = new CellConstraints();

		lblWorkingCopy = new Label();
		lblRepo = new Label();

		tmdlProperties = new TableModel<PropertiesListItem>(
				new PropertiesListItem());

		tblProperties = new Table<PropertiesListItem>(tmdlProperties);
		tblProperties.addMouseListener(new PopupupMouseListener());
		JScrollPane scrollPane = new JScrollPane(tblProperties);

		lblInfo = new Label();

		actSave = new SaveAction();
		btnSave = new JButton(actSave);

		pnlTop.add(new Label("Working copy:"), cc.xy(1, 1));
		pnlTop.add(lblWorkingCopy, cc.xy(3, 1));
		pnlTop.add(new Label("URL:"), cc.xy(1, 3));
		pnlTop.add(lblRepo, cc.xy(3, 3));

		pnlBottom.add(lblInfo, cc.xy(1, 1));
		pnlBottom.add(btnSave, cc.xy(5, 1));

		pnlMain.add(pnlTop, cc.xy(1, 1));
		pnlMain.add(scrollPane, cc.xy(1, 3));
		pnlMain.add(pnlBottom, cc.xy(1, 5));

		frame = GuiHelper.createAndShowFrame(pnlMain, "Properties",
				"other-app-icon.png", false);

		frame.addWindowListener(new WindowAdapter() {

			public void windowClosing(WindowEvent e) {
				// TODO do you want to save?
			}

			public void windowClosed(WindowEvent e) {
			}
		});

	}

	public Frame getFrame() {
		return frame;
	}

	public void setWorkingCopy(String workingCopy) {
		lblWorkingCopy.setText(workingCopy);
		lblWorkingCopy.setToolTipText(workingCopy);
		frame.setTitlePrefix(workingCopy);
	}

	public void setRepo(String repo) {
		lblRepo.setText(repo);
		lblRepo.setToolTipText(repo);
	}

	public void setProperties(List<PropertiesListItem> lstProperties) {
		tmdlProperties.clear();
		tmdlProperties.addLines(lstProperties);
	}

	private PropertiesListItem getSelectedPropertyListItem() {
		return tmdlProperties.getRow(tblProperties
				.convertRowIndexToModel(tblProperties.getSelectedRow()));
	}

	private class CopyAllToClipboard extends AbstractAction {

		public CopyAllToClipboard() {
			super("Copy all to clipboard");
		}

		public void actionPerformed(ActionEvent e) {
			StringBuilder result = new StringBuilder();
			for (PropertiesListItem li : tmdlProperties.getAllData()) {
				result.append(li.getKey() + " = " + li.getValue() + "\n");
			}
			Manager.setClipboard(result.toString());
		}
	}

	private class CopyLineToClipboard extends AbstractAction {

		public CopyLineToClipboard() {
			super("Copy line to clipboard");
		}

		public void actionPerformed(ActionEvent e) {
			PropertiesListItem li = getSelectedPropertyListItem();
			Manager.setClipboard(li.getKey() + " = " + li.getValue());
		}
	}

	private class DeleteAction extends AbstractAction {

		public DeleteAction() {
			super("Delete");
		}

		public void actionPerformed(ActionEvent e) {
			PropertiesListItem li = getSelectedPropertyListItem();

			int choice = JOptionPane.showConfirmDialog(frame,
					"Delete <" + li.getKey() + "> property?",
					"Delete property", JOptionPane.YES_NO_CANCEL_OPTION,
					JOptionPane.QUESTION_MESSAGE);
			if (choice == JOptionPane.YES_OPTION) {
				li.setRecoursively(true);
				propertiesOperation.removeProperty(li);
				tmdlProperties.removeLine(li);
			} else if (choice == JOptionPane.NO_OPTION) {
				li.setRecoursively(false);
				propertiesOperation.removeProperty(li);
				tmdlProperties.removeLine(li);
			}
		}
	}

	private class EditAction extends AbstractAction {

		JDialog dlgEditProp;
		private PropertiesListItem propertiesListItem;
		private TextArea taValue;
		private JCheckBox cbRecursively;

		public EditAction() {
			super("Edit");
		}

		public void actionPerformed(ActionEvent e) {
			propertiesListItem = getSelectedPropertyListItem();

			CellConstraints cc = new CellConstraints();
			FormLayout lyMain = new FormLayout("f:200:g,p,2dlu,p",
					"f:200:g,2dlu,p");
			JPanel pnlMain = new JPanel(lyMain);
			taValue = new TextArea();
			taValue.setColumns(40);
			taValue.setRows(10);
			taValue.setText(propertiesListItem.getValue());
			JButton btnOk = new JButton("Ok");
			btnOk.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					String value = taValue.getText();
					if (propertiesOperation
							.needsValueValidation(propertiesListItem.getKey())) {
						value = propertiesOperation.validateProperty(value);
					}
					propertiesListItem.setValue(value);
					propertiesListItem.setDisplayValue(propertiesOperation
							.valueToDisplayValue(value));
					propertiesListItem.setRecoursively(cbRecursively
							.isSelected());
					propertiesOperation.modifyProperty(propertiesListItem);
					tmdlProperties.fireTableDataChanged();
					dlgEditProp.setVisible(false);
				}
			});

			cbRecursively = new JCheckBox("Apply recursively");

			pnlMain.add(new JScrollPane(taValue), cc.xywh(1, 1, 4, 1,
					CellConstraints.FILL, CellConstraints.FILL));
			pnlMain.add(btnOk, cc.xy(4, 3));
			pnlMain.add(cbRecursively, cc.xy(2, 3));

			dlgEditProp = GuiHelper.createDialog(frame, pnlMain,
					"Edit svn property");
			dlgEditProp.setModalityType(ModalityType.DOCUMENT_MODAL);
			dlgEditProp.setTitle("Edit " + propertiesListItem.getKey());
			dlgEditProp.setVisible(true);
		}
	}

	private class PopupupMouseListener extends MouseAdapter {

		private JPopupMenu popup;

		public PopupupMouseListener() {
			popup = new JPopupMenu();
			popup.add(new EditAction());
			popup.add(new DeleteAction());
			popup.add(new CopyLineToClipboard());
			popup.add(new CopyAllToClipboard());
		}

		public void mouseClicked(MouseEvent e) {
			if (e.getClickCount() == 2) {
				Point p = new Point(e.getX(), e.getY());
				int row = tblProperties.rowAtPoint(p);
				if (row == -1) {
					return;
				}
				tblProperties.getSelectionModel()
						.setSelectionInterval(row, row);
				new EditAction().actionPerformed(null);
			}
		}

		public void showPopup(MouseEvent e) {
			Point p = new Point(e.getX(), e.getY());
			int row = tblProperties.rowAtPoint(p);
			if (row == -1) {
				return;
			}
			tblProperties.getSelectionModel().setSelectionInterval(row, row);
			// PropertiesListItem selected = getSelectedPropertyListItem();

			popup.setInvoker(tblProperties);
			popup.setLocation(e.getXOnScreen(), e.getYOnScreen());
			popup.setVisible(true);
			e.consume();
		}

		public void mousePressed(MouseEvent e) {
			if (e.isPopupTrigger()) {
				showPopup(e);
			}
		}

		public void mouseReleased(MouseEvent e) {
			if (e.isPopupTrigger()) {
				showPopup(e);
			}
		}
	}

	private class SaveAction extends AbstractAction {

		public SaveAction() {
			super("Save");
		}

		public void actionPerformed(ActionEvent e) {
			try {
				propertiesOperation.commitChanges();
			} catch (SVNException ex) {
				Manager.handle(ex);
			}
			frame.setVisible(false);
			frame.dispose();
		}
	}

}
