/**
 * 
 */
package hu.pagavcs.gui.log;

import hu.pagavcs.bl.ThreadAction;
import hu.pagavcs.gui.LogDetailListItem;

import java.awt.event.ActionEvent;

import org.tmatesoft.svn.core.SVNNodeKind;

class DetailShowChangesAction extends ThreadAction {

	/**
     * 
     */
    private final LogGui logGui;

	public DetailShowChangesAction(LogGui logGui) {
		super("Show changes");
		this.logGui = logGui;
	}

	public void actionProcess(ActionEvent e) throws Exception {
		for (LogDetailListItem liDetail : this.logGui.getSelectedDetailLogItems()) {
			if (SVNNodeKind.DIR.equals(liDetail.getKind())) {
				this.logGui.log.showDirChanges(liDetail.getPath(), liDetail.getRevision(), liDetail.getAction());
			} else if (SVNNodeKind.FILE.equals(liDetail.getKind())) {
				this.logGui.log.showChanges(liDetail.getPath(), liDetail.getRevision(), liDetail.getAction());
			} else {
				this.logGui.log.showChanges(liDetail.getPath(), liDetail.getRevision(), liDetail.getAction());
			}
		}
	}
}