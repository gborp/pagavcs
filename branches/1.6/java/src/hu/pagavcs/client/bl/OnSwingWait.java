package hu.pagavcs.client.bl;

import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

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
public abstract class OnSwingWait<P extends Object, R extends Object> {

	protected R returnValue;
	protected P argument;

	public OnSwingWait() {
	}

	public OnSwingWait(P argument) {
		this.argument = argument;
	}

	public R run() {
		if (SwingUtilities.isEventDispatchThread()) {
			new Runner().run();
		} else {
			try {
				SwingUtilities.invokeAndWait(new Runner());
			} catch (InvocationTargetException ex) {
				Manager.handle(ex);
			} catch (InterruptedException ex) {
				Manager.handle(ex);
			}
		}

		return returnValue;

	}

	protected abstract R process() throws Exception;

	private class Runner implements Runnable {

		public void run() {
			try {
				returnValue = OnSwingWait.this.process();
			} catch (Exception ex) {
				Manager.handle(ex);
			}
		}
	}

}
