package hu.pagavcs.gui;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
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

	private static final long serialVersionUID = -4582720512439012384L;
	private Color             alternateColor;

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

	public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {

		Component prep = super.prepareRenderer(renderer, row, column);

		int selectedRow = -1;
		if (getSelectedRow() != -1) {
			selectedRow = convertRowIndexToModel(getSelectedRow());
		}
		if (selectedRow != row) {
			if (alternateColor == null) {
				alternateColor = new Color(240, 240, 240);
			}
			Color result = row % 2 == 0 ? alternateColor : super.getBackground();
			prep.setBackground(result);
		}
		return prep;
	}

	public void followScrollToNewItems() {
		revalidate();
		scrollRectToVisible(getCellRect(getRowCount() - 1, 0, true));
		repaint();
	}

}
