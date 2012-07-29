package hu.pagavcs.client.gui.action;

import hu.pagavcs.client.bl.Manager;
import hu.pagavcs.client.bl.ThreadAction;
import hu.pagavcs.client.gui.LocationCallback;
import hu.pagavcs.client.operation.Log;
import hu.pagavcs.common.ResourceBundleAccessor;

import java.awt.event.ActionEvent;

public class ShowLogAction extends ThreadAction {

	private final LocationCallback callback;

	public ShowLogAction(LocationCallback callback) {
		super("Show log", ResourceBundleAccessor
				.getSmallImage("actions/pagavcs-log.png"));
		this.callback = callback;
	}

	public void actionProcess(ActionEvent e) throws Exception {
		try {
			new Log(callback.getLocation(), callback.isFilePath()).execute();
		} catch (Exception ex) {
			Manager.handle(ex);
		}
	}
}
