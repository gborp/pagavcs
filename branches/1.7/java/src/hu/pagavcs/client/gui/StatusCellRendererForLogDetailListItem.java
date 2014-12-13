package hu.pagavcs.client.gui;

import hu.pagavcs.client.bl.Manager;
import hu.pagavcs.client.gui.log.LogDetailListItem;
import hu.pagavcs.client.gui.platform.AbstractCellRendererColorizator;
import hu.pagavcs.client.gui.platform.Table;

import java.awt.Color;

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
public class StatusCellRendererForLogDetailListItem extends AbstractCellRendererColorizator<LogDetailListItem> {

	public StatusCellRendererForLogDetailListItem(Table<LogDetailListItem> table) {
		super(table);
	}

	public StatusCellRendererForLogDetailListItem(Table<LogDetailListItem> table, TableCellRenderer delegate) {
		super(table, delegate);
	}

	public Color getForegroundColor(LogDetailListItem li) {
		if (li.isInScope()) {
			return Manager.getColorByContentStatus(li.getAction());
		}
		return Color.GRAY;
	}
}
