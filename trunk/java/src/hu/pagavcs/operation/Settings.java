package hu.pagavcs.operation;

import hu.pagavcs.gui.SettingsGui;

import java.io.IOException;
import java.util.prefs.BackingStoreException;

import org.tmatesoft.svn.core.SVNException;

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
public class Settings {

	private String      path;
	private SettingsGui gui;

	public Settings() throws BackingStoreException {}

	public void execute() throws SVNException, BackingStoreException, IOException {
		gui = new SettingsGui(this);
		gui.display();
	}
}
