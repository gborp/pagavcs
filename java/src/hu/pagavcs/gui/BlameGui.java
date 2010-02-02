package hu.pagavcs.gui;

import hu.pagavcs.bl.Cancelable;
import hu.pagavcs.bl.Manager;
import hu.pagavcs.bl.ThreadAction;
import hu.pagavcs.gui.platform.GuiHelper;
import hu.pagavcs.gui.platform.Table;
import hu.pagavcs.gui.platform.TableModel;
import hu.pagavcs.operation.Log;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

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
public class BlameGui {

	private Table<BlameListItem>      tblBlame;
	private TableModel<BlameListItem> tableModel;
	private final Cancelable          update;
	private JButton                   btnStop;
	// TODO use Progress, btnStop make it stop too
	private JProgressBar              prgWorking;
	private List<BlameListItem>       lstBlame;
	private final String              file;

	public BlameGui(Cancelable update, String file) {
		this.update = update;
		this.file = file;
	}

	public void setBlamedFile(List<BlameListItem> lstBlame) {
		this.lstBlame = lstBlame;
	}

	public void display() {

		FormLayout layout = new FormLayout("1dlu:g, p, p", "fill:10dlu:g,p");
		JPanel pnlMain = new JPanel(layout);
		CellConstraints cc = new CellConstraints();

		tableModel = new TableModel<BlameListItem>(new BlameListItem());

		tblBlame = new Table<BlameListItem>(tableModel);
		tblBlame.addMouseListener(new PopupupMouseListener());
		JScrollPane scrollPane = new JScrollPane(tblBlame);

		btnStop = new JButton("Stop");
		btnStop.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				try {
					btnStop.setEnabled(false);
					update.setCancel(true);
				} catch (Exception e1) {
					Manager.handle(e1);
				}
			}
		});
		prgWorking = new JProgressBar();

		pnlMain.add(scrollPane, cc.xywh(1, 1, 3, 1));
		pnlMain.add(prgWorking, cc.xywh(2, 2, 1, 1));
		pnlMain.add(btnStop, cc.xywh(3, 2, 1, 1));

		GuiHelper.createAndShowFrame(pnlMain, "Blame");

		tableModel.addLines(lstBlame);
	}

	private class ShowLog extends ThreadAction {

		public ShowLog() {
			super("Show log");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			try {
				new Log(file).execute();
			} catch (Exception ex) {
				Manager.handle(ex);
			}
		}
	}

	private class PopupupMouseListener extends MouseAdapter {

		private JPopupMenu ppModified;

		public PopupupMouseListener() {
			ppModified = new JPopupMenu();
			ppModified.add(new ShowLog());
		}

		private void showPopup(MouseEvent e) {
			if (e.isPopupTrigger()) {
				JPopupMenu ppVisible = ppModified;
				if (ppVisible != null) {
					ppVisible.setInvoker(tblBlame);
					ppVisible.setLocation(e.getXOnScreen(), e.getYOnScreen());
					ppVisible.setVisible(true);
					e.consume();
				}
			}
		}

		public void mousePressed(MouseEvent e) {
			showPopup(e);
		}

		public void mouseReleased(MouseEvent e) {
			showPopup(e);
		}
	}
}
