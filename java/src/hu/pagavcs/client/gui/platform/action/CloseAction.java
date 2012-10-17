package hu.pagavcs.client.gui.platform.action;

import hu.pagavcs.client.bl.OnSwing;
import hu.pagavcs.client.bl.ThreadAction;

import java.awt.Window;
import java.awt.event.ActionEvent;

public class CloseAction extends ThreadAction {

	private final Window window;

	public CloseAction(Window window) {
		super("Close");
		this.window = window;
	}

	public void actionProcess(ActionEvent e) throws Exception {
		System.nanoTime();
		new OnSwing() {

			protected void process() throws Exception {
				exit();
			}
		}.run();
	}

	private void exit() {
		window.setVisible(false);
		window.dispose();
	}
}
