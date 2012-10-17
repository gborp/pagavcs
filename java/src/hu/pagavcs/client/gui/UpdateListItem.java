package hu.pagavcs.client.gui;

import hu.pagavcs.client.gui.platform.ListItem;
import hu.pagavcs.client.operation.ContentStatus;
import hu.pagavcs.client.operation.Update.UpdateContentStatus;

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
public class UpdateListItem implements ListItem {

	private ContentStatus       status;
	private String              path;
	private UpdateContentStatus contentStatus;
	// hidden columns
	private long                previousRevision;
	private ContentStatus       svnContentStatus;

	public String[] getColumnNames() {
		return new String[] { "Status", "Path" };
	}

	public Object getValue(int index) {
		if (index == 0) {
			return getStatus();
		} else if (index == 1) {
			return getPath();
		} else if (index == 2) {
			return getContentStatus();
		}
		throw new RuntimeException("not implemented");
	}

	public String getTooltip(int column) {
		return null;
	}

	public boolean isColumnEditable(int columnIndex) {
		return false;
	}

	public void setValue(int index, Object value) {}

	public void setStatus(ContentStatus status) {
		this.status = status;
	}

	public ContentStatus getStatus() {
		return status;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getPath() {
		return path;
	}

	public void setContentStatus(UpdateContentStatus contentStatus) {
		this.contentStatus = contentStatus;
	}

	public UpdateContentStatus getContentStatus() {
		return contentStatus;
	}

	public void setPreviousRevision(long previousRevision) {
		this.previousRevision = previousRevision;
	}

	public long getPreviousRevision() {
		return previousRevision;
	}

	public void setSvnContentStatus(ContentStatus svnContentStatus) {
		this.svnContentStatus = svnContentStatus;
	}

	public ContentStatus getSvnContentStatus() {
		return svnContentStatus;
	}
}
