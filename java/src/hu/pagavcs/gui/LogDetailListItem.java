package hu.pagavcs.gui;

import hu.pagavcs.gui.platform.GuiHelper;
import hu.pagavcs.gui.platform.ListItem;
import hu.pagavcs.operation.ContentStatus;

import org.tmatesoft.svn.core.SVNNodeKind;

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
	private Long          copyRevision;
	private SVNNodeKind   kind;
	private boolean       inScope;
	private long          revision;

	public String[] getColumnNames() {
		return new String[] { "Path", "Action", "Copy from path", "Revision" };
	}

	public Object getValue(int index) {
		if (index == 0) {
			return getPath();
		} else if (index == 1) {
			return GuiHelper.getContentStatusIcon(getAction());
		} else if (index == 2) {
			return getCopyFromPath();
		} else if (index == 3) {
			return getCopyRevision();
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

	public Long getCopyRevision() {
		return this.copyRevision;
	}

	public void setCopyRevision(Long revision) {
		this.copyRevision = revision;
	}

	public String getCopyFromPath() {
		return this.copyFromPath;
	}

	public void setCopyFromPath(String copyFromPath) {
		this.copyFromPath = copyFromPath;
	}

	public void setKind(SVNNodeKind kind) {
		this.kind = kind;
	}

	public SVNNodeKind getKind() {
		return kind;
	}

	public boolean isInScope() {
		return this.inScope;
	}

	public void setInScope(boolean inScope) {
		this.inScope = inScope;
	}

	public long getRevision() {
		return this.revision;
	}

	public void setRevision(long revision) {
		this.revision = revision;
	}

}
