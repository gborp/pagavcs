/**
 * 
 */
package hu.pagavcs.gui.log;

import hu.pagavcs.bl.ThreadAction;
import hu.pagavcs.gui.LogDetailListItem;

import java.awt.event.ActionEvent;

import org.tmatesoft.svn.core.SVNNodeKind;

class DetailCompareWithWorkingCopyAction extends ThreadAction {

	private final LogGui logGui;

	public DetailCompareWithWorkingCopyAction(LogGui logGui) {
		super("Compare with working copy");
		this.logGui = logGui;
	}

	public void actionProcess(ActionEvent e) throws Exception {
		for (LogDetailListItem liDetail : this.logGui.getSelectedDetailLogItems()) {
			if (SVNNodeKind.DIR.equals(liDetail.getKind())) {
				// do nothing
			} else if (SVNNodeKind.FILE.equals(liDetail.getKind())) {
				logGui.compareWithWorkingCopy(liDetail.getPath(), liDetail.getRevision(), liDetail.getAction());
			} else {
				// it's a file too
				logGui.compareWithWorkingCopy(liDetail.getPath(), liDetail.getRevision(), liDetail.getAction());
			}
		}
	}
}
