package hu.pagavcs.gui;

import hu.pagavcs.gui.platform.ListItem;
import hu.pagavcs.operation.ContentStatus;

import java.awt.Image;
import java.awt.Toolkit;
import java.io.File;
import java.util.HashMap;

import javax.swing.ImageIcon;

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

	private boolean                                  selected;
	private File                                     path;
	private ContentStatus                            status;
	private ContentStatus                            propertyStatus;
	private static HashMap<ContentStatus, ImageIcon> mapContentStatusIcon;
	private static HashMap<ContentStatus, ImageIcon> mapPropertyContentStatusIcon;

	public CommitListItem() {
		if (mapContentStatusIcon == null) {
			initIcons();
		}
	}

	public String[] getColumnNames() {
		return new String[] { "Selected", "Path", "Status", "PropertyStatus" };
	}

	public Object getValue(int index) {
		if (index == 0) {
			return isSelected();
		} else if (index == 1) {
			return getPath();
		} else if (index == 2) {
			return mapContentStatusIcon.get(getStatus());
		} else if (index == 3) {
			return mapPropertyContentStatusIcon.get(getPropertyStatus());
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

	private void initIcons() {
		mapContentStatusIcon = new HashMap<ContentStatus, ImageIcon>();
		mapContentStatusIcon.put(ContentStatus.ADDED, loadEmblem("added"));
		mapContentStatusIcon.put(ContentStatus.CONFLICTED, loadEmblem("conflict"));
		mapContentStatusIcon.put(ContentStatus.DELETED, loadEmblem("deleted"));
		mapContentStatusIcon.put(ContentStatus.IGNORED, loadEmblem("ignored"));
		mapContentStatusIcon.put(ContentStatus.MODIFIED, loadEmblem("modified"));
		mapContentStatusIcon.put(ContentStatus.OBSTRUCTED, loadEmblem("obstructed"));
		mapContentStatusIcon.put(ContentStatus.UNVERSIONED, loadEmblem("unversioned"));

		mapPropertyContentStatusIcon = new HashMap<ContentStatus, ImageIcon>();
		mapPropertyContentStatusIcon.put(ContentStatus.ADDED, loadEmblem("added"));
		mapPropertyContentStatusIcon.put(ContentStatus.CONFLICTED, loadEmblem("conflict"));
		mapPropertyContentStatusIcon.put(ContentStatus.DELETED, loadEmblem("deleted"));
		mapPropertyContentStatusIcon.put(ContentStatus.IGNORED, loadEmblem("ignored"));
		mapPropertyContentStatusIcon.put(ContentStatus.MODIFIED, loadEmblem("modified"));
		mapPropertyContentStatusIcon.put(ContentStatus.OBSTRUCTED, loadEmblem("obstructed"));
		mapPropertyContentStatusIcon.put(ContentStatus.UNVERSIONED, loadEmblem("unversioned"));
	}

	private ImageIcon loadEmblem(String name) {

		Integer width = 8;
		Integer height = 8;

		ImageIcon ii = new ImageIcon(Toolkit.getDefaultToolkit().getImage(CommitListItem.class.getResource("/hu/pagavcs/resources/emblems/" + name + ".png")));
		ImageIcon imageIcon = new ImageIcon(ii.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH));

		return imageIcon;
	}

}
