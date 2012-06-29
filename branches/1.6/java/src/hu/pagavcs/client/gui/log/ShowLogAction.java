package hu.pagavcs.client.gui.log;

import hu.pagavcs.client.bl.ThreadAction;
import hu.pagavcs.client.operation.Log;

import java.awt.event.ActionEvent;

public class ShowLogAction extends ThreadAction {

	private final LogGui logGui;

	public ShowLogAction(LogGui logGui) {
		super("Show log");
		this.logGui = logGui;
	}

	public void actionProcess(ActionEvent e) throws Exception {
		for (LogDetailListItem liDetail : this.logGui
				.getSelectedDetailLogItems()) {
			new Log(logGui.getAbsoluteUrl(liDetail.getPath()), false).execute();
		}
	}
}
