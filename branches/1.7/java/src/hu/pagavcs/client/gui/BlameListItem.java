package hu.pagavcs.client.gui;

import hu.pagavcs.client.gui.platform.ListItem;

import java.awt.Color;
import java.util.Date;

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
public class BlameListItem implements ListItem {

	private static final Color[] colors = new Color[] { Color.RED,
			Color.DARK_GRAY, Color.DARK_GRAY, Color.BLUE, Color.BLACK };

	private long revision;
	private String author;
	private Date date;
	private int lineNumber;;
	private String line;

	public String[] getColumnNames() {
		return new String[] { "Revision", "Author", "Date", "Nr", "Line" };
	}

	public Object getValue(int index) {
		if (index == 0) {
			return getRevision();
		} else if (index == 1) {
			return getAuthor();
		} else if (index == 2) {
			return getDate();
		} else if (index == 3) {
			return getLineNumber();
		} else if (index == 4) {
			return getLine();
		}
		throw new RuntimeException("not implemented");
	}

	public String getTooltip(int column) {
		return null;
	}

	public boolean isColumnEditable(int columnIndex) {
		return false;
	}

	public void setValue(int index, Object value) {
	}

	public long getRevision() {
		return this.revision;
	}

	public void setRevision(long revision) {
		this.revision = revision;
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

	public void setDate(Date date) {
		this.date = date;
	}

	public String getLine() {
		return this.line;
	}

	public void setLine(String message) {
		this.line = message;
	}

	public int getLineNumber() {
		return this.lineNumber;
	}

	public void setLineNumber(int lineNumber) {
		this.lineNumber = lineNumber;
	}

	public static Color getForegroundColor(int column) {
		return colors[column];
	}

}
