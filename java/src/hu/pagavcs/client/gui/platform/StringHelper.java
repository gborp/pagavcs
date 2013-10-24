package hu.pagavcs.client.gui.platform;

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
public class StringHelper {

	private static final String TAG_HTML_START = "<html>";
	private static final String TAG_HTML_END = "</html>";
	private static final String TAG_CENTER_START = "<center>";
	private static final String TAG_CENTER_END = "</center>";

	public static String toNullAware(String str) {
		return str == null ? "" : str;
	}

	public static String toNullAware(Long str) {
		return str == null ? "" : str.toString();
	}

	public static String convertMultilineTextToHtml(String str) {
		if ((str != null) && !str.isEmpty() && !str.startsWith(TAG_HTML_START)) {
			str = TAG_HTML_START + str.replaceAll("\n", "<br>") + TAG_HTML_END;
		}

		return str;
	}

	public static String convertMultilineTextToHtmlCenter(String str) {
		if ((str != null) && !str.isEmpty() && !str.startsWith(TAG_HTML_START)) {
			str = TAG_HTML_START + TAG_CENTER_START
					+ str.replaceAll("\n", "<br>") + TAG_CENTER_END
					+ TAG_HTML_END;
		}

		return str;
	}

	public static String depathString(String str) {
		str = str.replace("/", "_");
		str = str.replace("\\", "_");
		str = str.replace(":", "_");
		return str;
	}
}
