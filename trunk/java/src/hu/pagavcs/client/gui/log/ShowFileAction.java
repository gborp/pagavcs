/**
 * 
 */
package hu.pagavcs.client.gui.log;

import hu.pagavcs.client.bl.PagaException;
import hu.pagavcs.client.bl.ThreadAction;
import hu.pagavcs.client.bl.PagaException.PagaExceptionType;
import hu.pagavcs.client.gui.platform.MessagePane;
import hu.pagavcs.client.operation.ContentStatus;

import java.awt.event.ActionEvent;

import org.tmatesoft.svn.core.SVNNodeKind;

class ShowFileAction extends ThreadAction {

	private final LogGui logGui;

	public ShowFileAction(LogGui logGui) {
		super("Show file");
		this.logGui = logGui;
	}

	public void actionProcess(ActionEvent e) throws Exception {
		for (LogDetailListItem liDetail : this.logGui.getSelectedDetailLogItems()) {
			if (SVNNodeKind.DIR.equals(liDetail.getKind())) {
				throw new PagaException(PagaExceptionType.UNIMPLEMENTED);
			} else {
				ContentStatus cs = liDetail.getAction();
				if (ContentStatus.DELETED.equals(cs)) {
					MessagePane.showError(this.logGui.getFrame(), "Cannot save", "File is deleted in this revision.");
				}
				logGui.showFile(liDetail.getPath(), liDetail.getRevision());
			}
		}
	}
}
