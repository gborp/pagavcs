package hu.pagavcs.gui;

import hu.pagavcs.bl.OnSwing;

import javax.swing.JProgressBar;

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
public class Progress extends JProgressBar {

	int     busyCount;
	Working working;

	public Progress(Working working) {
		this.working = working;
	}

	public void startProgress() throws Exception {
		new OnSwing() {

			protected void process() throws Exception {
				if (busyCount == 0) {
					setIndeterminate(true);
					working.setStatusStartWorking();
				}
				busyCount++;
			}
		}.run();
	}

	public void stopProgress() throws Exception {
		new OnSwing() {

			protected void process() throws Exception {
				busyCount--;
				if (busyCount == 0) {
					setIndeterminate(false);
					working.setStatusStopWorking();
				}
			}
		}.run();
	}
}
