package hu.pagavcs.gui;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

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
public class Table extends JTable {

	private static final long serialVersionUID = -1;
	private Label             lblMessage;

	public Table(TableModel<?> tableModel) {
		super(tableModel);
		setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		setFillsViewportHeight(true);
		setShowGrid(false);
		tableModel.addTableModelListener(new TableModelListener() {

			public void tableChanged(TableModelEvent e) {
				if (e.getType() == TableModelEvent.INSERT) {
					resizeColumns();
				}
			}
		});
	}

	public void showMessage(String message, Icon icon) {
		hideMessage();
		lblMessage = new Label(message);
		lblMessage.setIcon(icon);
		lblMessage.setHorizontalAlignment(SwingConstants.CENTER);
		lblMessage.setVerticalAlignment(SwingConstants.CENTER);

		setLayout(new BorderLayout());
		add(lblMessage, BorderLayout.CENTER);
		revalidate();
		repaint();
	}

	public void hideMessage() {
		if (lblMessage != null) {
			remove(lblMessage);
			setLayout(null);
			lblMessage = null;
			revalidate();
			repaint();
		}
	}

	public void resizeColumns() {
		for (int i = 0; i < getColumnCount(); i++) {
			DefaultTableColumnModel colModel = (DefaultTableColumnModel) getColumnModel();
			TableColumn col = colModel.getColumn(i);
			int width = 0;

			TableCellRenderer renderer = col.getHeaderRenderer();
			for (int r = 0; r < getRowCount(); r++) {
				renderer = getCellRenderer(r, i);
				Component comp = renderer.getTableCellRendererComponent(this, getValueAt(r, i), false, false, r, i);
				width = Math.max(width, comp.getPreferredSize().width);
			}
			col.setPreferredWidth(width + 8);
		}
	}

	public void followScrollToNewItems() {
		revalidate();
		scrollRectToVisible(getCellRect(getRowCount() - 1, 0, true));
		repaint();
	}

}
