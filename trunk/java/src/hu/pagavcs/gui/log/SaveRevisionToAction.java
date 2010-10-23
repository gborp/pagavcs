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
import java.io.File;

import javax.swing.JFileChooser;

import org.tmatesoft.svn.core.SVNNodeKind;

class SaveRevisionToAction extends ThreadAction {

	private final LogGui logGui;

	public SaveRevisionToAction(LogGui logGui) {
		super("Save revision to");
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

				String path = liDetail.getPath();
				if (path.lastIndexOf('/') != -1) {
					path = path.substring(path.lastIndexOf('/'));
				}
				JFileChooser fc = new JFileChooser();
				fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
				fc.setSelectedFile(new File(path));

				int choosed = fc.showSaveDialog(this.logGui.getFrame());

				if (choosed == JFileChooser.APPROVE_OPTION) {
					File file = fc.getSelectedFile();
					logGui.saveRevisionTo(liDetail.getPath(), liDetail.getRevision(), file);
				}
			}
		}
	}
}
