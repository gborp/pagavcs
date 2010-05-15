package hu.pagavcs.operation;

import hu.pagavcs.bl.Manager;

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
public class ResolveConflictUsingMine {

	private final List<String> lstPath;

	public ResolveConflictUsingMine(List<String> lstPath) {
		this.lstPath = lstPath;
	}

	public void execute() throws Exception {
		for (String path : lstPath) {
			try {
				Manager.resolveConflictUsingMine(path);
			} catch (Exception ex) {
				Manager.handle(ex);
			}
		}
	}

}
