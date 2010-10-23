/**
 * 
 */
package hu.pagavcs.gui.log;

import hu.pagavcs.bl.PagaException;
import hu.pagavcs.bl.ThreadAction;
import hu.pagavcs.bl.PagaException.PagaExceptionType;
import hu.pagavcs.gui.LogDetailListItem;
import hu.pagavcs.gui.platform.MessagePane;
import hu.pagavcs.operation.ContentStatus;

import java.awt.event.ActionEvent;

import org.tmatesoft.svn.core.SVNNodeKind;

class ShowFileAction extends ThreadAction {

	/**
     * 
     */
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
					MessagePane.showError(this.logGui.frame, "Cannot save", "File is deleted in this revision.");
				}
				this.logGui.log.showFile(liDetail.getPath(), liDetail.getRevision());
			}
		}
	}
}