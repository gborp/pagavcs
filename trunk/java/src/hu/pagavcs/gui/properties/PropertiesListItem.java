package hu.pagavcs.gui.properties;

import hu.pagavcs.gui.platform.ListItem;

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
public class PropertiesListItem implements ListItem {

	private String  key;
	private String  displayValue;
	// hidden columns
	private String  value;
	private boolean textMode;
	private boolean recoursively;

	public String[] getColumnNames() {
		return new String[] { "Property", "Value" };
	}

	public Object getValue(int index) {
		if (index == 0) {
			return getKey();
		} else if (index == 1) {
			return getDisplayValue();
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

	public String getKey() {
		return this.key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public void setDisplayValue(String displayValue) {
		this.displayValue = displayValue;
	}

	public String getDisplayValue() {
		return displayValue;
	}

	public String getValue() {
		return this.value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public boolean isTextMode() {
		return this.textMode;
	}

	public void setTextMode(boolean textMode) {
		this.textMode = textMode;
	}

	public void setRecoursively(boolean recoursively) {
		this.recoursively = recoursively;
	}

	public boolean isRecoursively() {
		return recoursively;
	}

}
