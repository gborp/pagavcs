/**
 * 
 */
package hu.pagavcs.client.gui.log;

import hu.pagavcs.client.bl.ThreadAction;

import java.awt.event.ActionEvent;

import org.tmatesoft.svn.core.SVNNodeKind;

class DetailShowChangesAction extends ThreadAction {

	private final LogGui logGui;

	public DetailShowChangesAction(LogGui logGui) {
		super("Show changes");
		this.logGui = logGui;
	}

	public void actionProcess(ActionEvent e) throws Exception {
		for (LogDetailListItem liDetail : this.logGui.getSelectedDetailLogItems()) {
			if (SVNNodeKind.DIR.equals(liDetail.getKind())) {
				logGui.showDirChanges(liDetail.getPath(), liDetail.getRevision(), liDetail.getAction());
			} else if (SVNNodeKind.FILE.equals(liDetail.getKind())) {
				logGui.showChanges(liDetail.getPath(), liDetail.getRevision(), liDetail.getAction());
			} else {
				logGui.showChanges(liDetail.getPath(), liDetail.getRevision(), liDetail.getAction());
			}
		}
	}
}
