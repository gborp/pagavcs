package hu.pagavcs.gui;

import hu.pagavcs.bl.Manager;
import hu.pagavcs.gui.platform.AbstractCellRendererColorizator;
import hu.pagavcs.gui.platform.ListItem;
import hu.pagavcs.gui.platform.Table;

import java.awt.Color;

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
public class StatusCellRendererForUpdateListItem extends AbstractCellRendererColorizator {

	public StatusCellRendererForUpdateListItem(Table table) {
		super(table);
	}

	public Color getForegroundColor(ListItem li) {
		return Manager.getColorByContentStatus(((UpdateListItem) li).getStatus());
	}
}
