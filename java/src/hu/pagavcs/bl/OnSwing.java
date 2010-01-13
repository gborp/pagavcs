package hu.pagavcs.bl;

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
public abstract class OnSwing<P extends Object> {

	private static final boolean DEFAULT_FORCE_INVOKE_LATER = false;
	protected P                  argument;
	private final boolean        forceInvokeLater;

	public OnSwing() {
		this(DEFAULT_FORCE_INVOKE_LATER);
	}

	public OnSwing(P argument) {
		this(argument, DEFAULT_FORCE_INVOKE_LATER);
	}

	public OnSwing(boolean forceInvokeLater) {
		this(null, forceInvokeLater);
	}

	public OnSwing(P argument, boolean forceInvokeLater) {
		this.argument = argument;
		this.forceInvokeLater = forceInvokeLater;
	}

	public void run() throws Exception {
		if (!forceInvokeLater && SwingUtilities.isEventDispatchThread()) {
			new Runner().run();
		} else {
			SwingUtilities.invokeLater(new Runner());
		}

	}

	protected abstract void process() throws Exception;

	private class Runner implements Runnable {

		public void run() {
			try {
				OnSwing.this.process();
			} catch (Exception ex) {
				Manager.handle(ex);
			}
		}
	}

}
