package hu.pagavcs.gui.platform;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.table.AbstractTableModel;

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
public class TableModel<L extends ListItem> extends AbstractTableModel {

	private final List<L>  lstData;
	private final String[] columnNames;

	public TableModel(L infoInstance) {
		this.columnNames = infoInstance.getColumnNames();
		lstData = new ArrayList<L>();
	}

	public int getColumnCount() {
		return columnNames.length;
	}

	public int getRowCount() {
		return lstData.size();
	}

	public L getRow(final int row) {
		return lstData.get(row);
	}

	public List<L> getAllData() {
		return lstData;
	}

	public String getColumnName(final int column) {
		return columnNames[column];
	}

	public String getColumnDescription(int column) {
		return getColumnName(column);
	}

	public Object getValueAt(final int rowIndex, final int columnIndex) {
		final L li = lstData.get(rowIndex);
		return li.getValue(columnIndex);
	}

	public void addLine(final L li) {
		addLines(Collections.singleton(li));
	}

	public void addLines(final Collection<L> lstLi) {
		if (!lstLi.isEmpty()) {
			lstData.addAll(lstLi);
			fireTableRowsInserted(getRowCount() - lstLi.size(), getRowCount() - 1);
		}
	}

	public void removeLine(L li) {
		int index = lstData.indexOf(li);
		if (index != -1) {
			lstData.remove(index);
			fireTableRowsDeleted(index, index);
		}

	}

	public void clear() {
		int count = lstData.size();
		if (count > 0) {
			lstData.clear();
			fireTableRowsDeleted(0, count - 1);
		}
	}

	public void setValueAt(final Object value, final int rowIndex, final int columnIndex) {
		if (!lstData.get(rowIndex).isColumnEditable(columnIndex)) {
			throw new RuntimeException("not implemented");
		}
		lstData.get(rowIndex).setValue(columnIndex, value);
		fireTableCellUpdated(rowIndex, columnIndex);
	}

	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return lstData.get(rowIndex).isColumnEditable(columnIndex);
	}

	public Class<?> getColumnClass(int c) {
		if (getRowCount() < 1) {
			return Object.class;
		}

		int rowCount = getRowCount();
		int row = 0;
		while (row < rowCount && getValueAt(row, c) == null) {
			row++;
		}
		if (row < rowCount) {
			return getValueAt(row, c).getClass();
		}
		return Object.class;
	}

	public String getCellToolTip(int column, int row) {
		return getRow(row).getTooltip(column);
	}

}
