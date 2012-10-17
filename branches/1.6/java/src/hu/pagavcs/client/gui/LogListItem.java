package hu.pagavcs.client.gui;

import hu.pagavcs.client.gui.platform.ListItem;

import java.text.DateFormat;
import java.util.Date;
import java.util.Map;

import org.tmatesoft.svn.core.SVNLogEntryPath;

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
public class LogListItem implements ListItem {

	private long                         revision;
	private String                       actions;
	private String                       author;
	private Date                         date;
	private String                       message;
	private Map<String, SVNLogEntryPath> mapChanges;

	public String[] getColumnNames() {
		return new String[] { "Revision", "Actions", "Author", "Date", "Message" };
	}

	public Object getValue(int index) {
		if (index == 0) {
			return getRevision();
		} else if (index == 1) {
			return getActions();
		} else if (index == 2) {
			return getAuthor();
		} else if (index == 3) {
			return getDateAsString();
		} else if (index == 4) {
			return getMessage();
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

	public long getRevision() {
		return this.revision;
	}

	public void setRevision(long revision) {
		this.revision = revision;
	}

	public String getActions() {
		return this.actions;
	}

	public void setActions(String actions) {
		this.actions = actions;
	}

	public String getAuthor() {
		return this.author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public Date getDate() {
		return this.date;
	}

	public String getDateAsString() {
		if (date == null) {
			return "";
		}
		DateFormat formatter = DateFormat.getDateTimeInstance();
		return formatter.format(date);
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public String getMessage() {
		return this.message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public void setChanges(Map<String, SVNLogEntryPath> mapChanges) {
		this.mapChanges = mapChanges;
	}

	public Map<String, SVNLogEntryPath> getChanges() {
		return mapChanges;
	}

}
