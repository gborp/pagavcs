package hu.pagavcs.client.operation;

import hu.pagavcs.client.bl.Manager;
import hu.pagavcs.client.bl.SvnHelper;
import hu.pagavcs.client.gui.Working;

import java.io.File;
import java.util.List;

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
public class ShowChangesOperation {

	private final List<String> lstPath;

	public ShowChangesOperation(List<String> lstPath) {
		this.lstPath = lstPath;
	}

	public void execute() throws Exception {
		Working working = new Working() {

			public void workEnded() throws Exception {}

			public void workStarted() throws Exception {}

		};
		for (String path : lstPath) {
			try {
				SvnHelper.showChangesFromBase(working, new File(path));
			} catch (Exception ex) {
				Manager.handle(ex);
			}
		}
	}

}
