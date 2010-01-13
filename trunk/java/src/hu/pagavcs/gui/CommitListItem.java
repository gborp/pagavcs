package hu.pagavcs.gui;

import hu.pagavcs.operation.ContentStatus;

import java.io.File;

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
public class CommitListItem implements ListItem {

	private boolean       selected;
	private File          path;
	private ContentStatus status;
	private ContentStatus propertyStatus;

	public String[] getColumnNames() {
		return new String[] { "Selected", "Path", "Status", "PropertyStatus" };
	}

	public Object getValue(int index) {
		if (index == 0) {
			return isSelected();
		} else if (index == 1) {
			return getPath();
		} else if (index == 2) {
			return getStatus();
		} else if (index == 3) {
			return getPropertyStatus();
		}
		throw new RuntimeException("not implemented");
	}

	public boolean isColumnEditable(int columnIndex) {
		return columnIndex == 0;
	}

	public void setValue(int index, Object value) {
		selected = (Boolean) value;
	}

	public boolean isSelected() {
		return this.selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	public File getPath() {
		return this.path;
	}

	public void setPath(File path) {
		this.path = path;
	}

	public ContentStatus getStatus() {
		return this.status;
	}

	public void setStatus(ContentStatus status) {
		this.status = status;
	}

	public void setPropertyStatus(ContentStatus propertyStatus) {
		this.propertyStatus = propertyStatus;
	}

	public ContentStatus getPropertyStatus() {
		return propertyStatus;
	}

}
