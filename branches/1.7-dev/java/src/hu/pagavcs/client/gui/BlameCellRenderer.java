package hu.pagavcs.client.gui;

import hu.pagavcs.client.gui.platform.Table;
import hu.pagavcs.client.gui.platform.TableModel;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

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

public class BlameCellRenderer implements TableCellRenderer {

	private final TableCellRenderer delegate;
	private Table<BlameListItem> decorTable;

	// private static final Color alternateColor = new Color(240, 240, 240);

	public BlameCellRenderer(Table<BlameListItem> table) {
		delegate = table.getDefaultRenderer(Object.class);
		table.setDefaultRenderer(Object.class, this);
		decorTable = table;
	}

	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int rowIndex, int vColIndex) {

		Component delegeteComponent = delegate.getTableCellRendererComponent(
				decorTable, value, isSelected, hasFocus, rowIndex, vColIndex);
		if (!isSelected && decorTable.getModel().getRowCount() > 0) {
			TableModel<BlameListItem> model = decorTable.getModel();
			delegeteComponent.setForeground(getForegroundColor(decorTable
					.convertColumnIndexToModel(vColIndex)));
			// delegeteComponent.setBackground(rowIndex % 2 == 0 ?
			// AbstractCellRendererColorizator.alternateColor : null);
		} else {
			delegeteComponent.setForeground(null);
		}

		return delegeteComponent;
	}

	public Color getForegroundColor(int column) {
		return BlameListItem.getForegroundColor(column);
	}

}
