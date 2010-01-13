package hu.pagavcs.gui;

import hu.pagavcs.operation.ContentStatus;

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
public class LogDetailListItem implements ListItem {

	private String        path;
	private ContentStatus action;
	private String        copyFromPath;
	private Long          revision;

	public String[] getColumnNames() {
		return new String[] { "Path", "Action", "Copy from path", "Revision" };
	}

	public Object getValue(int index) {
		if (index == 0) {
			return getPath();
		} else if (index == 1) {
			return getAction();
		} else if (index == 2) {
			return getCopyFromPath();
		} else if (index == 3) {
			return getRevision();
		}
		throw new RuntimeException("not implemented");
	}

	public boolean isColumnEditable(int columnIndex) {
		return false;
	}

	public void setValue(int index, Object value) {}

	public String getPath() {
		return this.path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public ContentStatus getAction() {
		return this.action;
	}

	public void setAction(ContentStatus action) {
		this.action = action;
	}

	public Long getRevision() {
		return this.revision;
	}

	public void setRevision(Long revision) {
		this.revision = revision;
	}

	public String getCopyFromPath() {
		return this.copyFromPath;
	}

	public void setCopyFromPath(String copyFromPath) {
		this.copyFromPath = copyFromPath;
	}

}
