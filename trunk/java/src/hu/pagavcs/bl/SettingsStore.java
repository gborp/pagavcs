package hu.pagavcs.bl;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

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
public class SettingsStore {

	private Preferences          prefs                        = Preferences.userNodeForPackage(this.getClass());
	private static SettingsStore singleton;

	private static final String  KEY_USERNAME                 = "username";
	private static final String  KEY_PASSWORD                 = "password";
	private static final String  KEY_COMMIT_MESSAGES          = "commit-messages";
	private static final String  KEY_REPO_URL                 = "repo-url";
	private static final String  KEY_WINDOW_BOUNDS            = "window-bounds";
	private static final String  KEY_GUI_LOG_SEPARATOR_DETAIL = "gui-log-separator-detail";
	private static final String  KEY_GUI_LOG_SEPARATOR_MAIN   = "gui-log-separator-main";

	private Map<String, String>  mapUsername                  = new HashMap<String, String>();
	private Map<String, String>  mapPassword                  = new HashMap<String, String>();
	private List<String>         lstCommitMessages            = new ArrayList<String>();
	private List<String>         lstRepoUrl                   = new ArrayList<String>();
	private Map<String, String>  mapWindowBounds              = new HashMap<String, String>();
	private Integer              guiLogSeparatorDetail;
	private Integer              guiLogSeparatorMain;

	public static SettingsStore getInstance() {
		if (singleton == null) {
			singleton = new SettingsStore();
		}
		return singleton;
	}

	public void save() throws SVNException, BackingStoreException {
		storeMap(KEY_USERNAME, mapUsername);
		storeMap(KEY_PASSWORD, mapPassword);
		storeList(KEY_COMMIT_MESSAGES, lstCommitMessages);
		storeList(KEY_REPO_URL, lstRepoUrl);
		storeMap(KEY_WINDOW_BOUNDS, mapWindowBounds);
		storeInteger(KEY_GUI_LOG_SEPARATOR_DETAIL, guiLogSeparatorDetail);
		storeInteger(KEY_GUI_LOG_SEPARATOR_MAIN, guiLogSeparatorMain);
		prefs.flush();
	}

	public void load() throws BackingStoreException {
		mapUsername = loadMap(KEY_USERNAME);
		mapPassword = loadMap(KEY_PASSWORD);
		lstCommitMessages = loadList(KEY_COMMIT_MESSAGES);
		lstRepoUrl = loadList(KEY_REPO_URL);
		mapWindowBounds = loadMap(KEY_WINDOW_BOUNDS);
		guiLogSeparatorDetail = loadInteger(KEY_GUI_LOG_SEPARATOR_DETAIL);
		guiLogSeparatorMain = loadInteger(KEY_GUI_LOG_SEPARATOR_MAIN);
	}

	private List<String> loadList(String listName) throws BackingStoreException {
		Map<String, String> result = new HashMap<String, String>();
		List<String> resultList = new ArrayList<String>();
		Preferences node = prefs.node(listName);
		for (String key : node.keys()) {
			result.put(key, node.get(key, null));
		}
		Object[] keys = result.keySet().toArray();
		Arrays.sort(keys);
		for (Object key : keys) {
			resultList.add(result.get(key));
		}
		return resultList;
	}

	private void storeList(String mapName, List<String> data) throws BackingStoreException {
		Preferences node = prefs.node(mapName);
		node.clear();
		int index = 0;
		for (String li : data) {
			node.put(String.format("%05d", index), li);
		}
	}

	private Map<String, String> loadMap(String mapName) throws BackingStoreException {
		Map<String, String> result = new HashMap<String, String>();
		Preferences node = prefs.node(mapName);
		for (String key : node.keys()) {
			result.put(key, node.get(key, null));
		}
		return result;
	}

	private void storeMap(String mapName, Map<String, String> data) throws BackingStoreException {
		Preferences node = prefs.node(mapName);
		node.clear();
		for (Entry<String, String> entry : data.entrySet()) {
			node.put(entry.getKey(), entry.getValue());
		}
	}

	private Integer loadInteger(String name) {
		String value = prefs.get(name, null);
		if (value == null) {
			return null;
		}
		return Integer.valueOf(value);
	}

	private void storeInteger(String name, Integer value) {
		if (value == null) {
			prefs.remove(name);
		} else {
			prefs.put(name, Integer.toString(value));
		}
	}

	private Double loadDouble(String name) {
		String value = prefs.get(name, null);
		if (value == null) {
			return null;
		}
		return Double.valueOf(value);
	}

	private void storeDouble(String name, Double value) {
		if (value == null) {
			prefs.remove(name);
		} else {
			prefs.put(name, Double.toString(value));
		}
	}

	public void clearLogin() {
		mapUsername.clear();
		mapPassword.clear();
	}

	public String getUsername(String repoid) {
		return mapUsername.get(repoid);
	}

	public void setUsername(String repoid, String username) {
		mapUsername.put(repoid, username);
	}

	public String getPassword(String repoid) {
		return mapPassword.get(repoid);
	}

	public void setPassword(String repoid, String password) {
		mapPassword.put(repoid, password);
	}

	public void addCommitMessageForHistory(String message) {
		for (String li : lstCommitMessages) {
			if (message.equals(li)) {
				return;
			}
		}
		lstCommitMessages.add(message);
	}

	public void setLstCommitMessages(List<String> lstCommitMessages) {
		this.lstCommitMessages = lstCommitMessages;
	}

	public List<String> getLstCommitMessages() {
		return lstCommitMessages;
	}

	public void setLstRepoUrl(List<String> lstRepoUrl) {
		this.lstRepoUrl = lstRepoUrl;
	}

	public List<String> getLstRepoUrl() {
		return lstRepoUrl;
	}

	private String rectangleToString(Rectangle r) {
		return "" + r.x + " " + r.y + " " + r.width + " " + r.height;
	}

	private Rectangle stringToRectangle(String s) {
		String[] v = s.split(" ");
		return new Rectangle(Integer.valueOf(v[0]), Integer.valueOf(v[1]), Integer.valueOf(v[2]), Integer.valueOf(v[3]));
	}

	public Rectangle getWindowBounds(String windowName) {
		if (mapWindowBounds.containsKey(windowName)) {
			return stringToRectangle(mapWindowBounds.get(windowName));
		} else {
			return null;
		}
	}

	public void setWindowBounds(String windowName, Rectangle bounds) {
		mapWindowBounds.put(windowName, rectangleToString(bounds));
	}

	public Integer getGuiLogSeparatorDetail() {
		return this.guiLogSeparatorDetail;
	}

	public void setGuiLogSeparatorDetail(Integer guiLogSeparatorDetail) {
		this.guiLogSeparatorDetail = guiLogSeparatorDetail;
	}

	public Integer getGuiLogSeparatorMain() {
		return this.guiLogSeparatorMain;
	}

	public void setGuiLogSeparatorMain(Integer guiLogSeparatorMain) {
		this.guiLogSeparatorMain = guiLogSeparatorMain;
	}

	// store and load immediately settings.
}
