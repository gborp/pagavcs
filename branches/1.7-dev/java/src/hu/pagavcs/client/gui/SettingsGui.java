package hu.pagavcs.client.gui;

import hu.pagavcs.client.bl.Manager;
import hu.pagavcs.client.bl.SettingsStore;
import hu.pagavcs.client.bl.ThreadAction;
import hu.pagavcs.client.gui.platform.Frame;
import hu.pagavcs.client.gui.platform.GuiHelper;
import hu.pagavcs.client.gui.platform.Label;
import hu.pagavcs.client.gui.platform.StringHelper;
import hu.pagavcs.client.gui.platform.TextArea;
import hu.pagavcs.client.operation.Settings;

import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import org.tmatesoft.svn.core.SVNException;

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
public class SettingsGui {

	private static final String ABOUT_TEXT = "PagaVCS\nPaga Version Control System\n\npagavcs.googlecode.com\n\n©2010-2014 Gábor Pápai";

	private static final String KEY_CONTEXT_MENU_ICON = "/org/gnome/desktop/interface/menus-have-icons";

	private Settings settings;
	private TextArea taCommitCompleteTemplate;
	private Frame frame;

	private JCheckBox cbGlobalIgnoreEol;

	private JCheckBox cbShowIconsInContextMenu;

	public SettingsGui(Settings settings) {
		this.settings = settings;
	}

	public void display() throws SVNException, IOException {

		CellConstraints cc = new CellConstraints();

		JButton btnAbout = new JButton(new AboutAction());
		JButton btnClearLogin = new JButton(new ClearLoginCacheAction());
		JButton btnShowLoginDialogNextTime = new JButton(new ShowLoginDialogNextTimeAction());
		JButton btnExitPagavcs = new JButton(new ExitPagavcsAction());
		JButton btnSetCommitCompletedMessageTemplates = new JButton(new SetCommitCompletedMessageTemplatesAction());
		cbGlobalIgnoreEol = new JCheckBox("Global Ignore EOL");
		if (Boolean.TRUE.equals(SettingsStore.getInstance().getGlobalIgnoreEol())) {
			cbGlobalIgnoreEol.setSelected(true);
		}

		cbShowIconsInContextMenu = new JCheckBox(new ShowIconsInContextMenuAction());
		cbShowIconsInContextMenu.setSelected(isShowIconsInContextMenus());

		taCommitCompleteTemplate = new TextArea();
		taCommitCompleteTemplate.setToolTipText("Example: /pagavcs/trunk>>>#{0} trunk.PagaVCS");
		taCommitCompleteTemplate.setRows(3);

		JPanel pnlTemplate = new JPanel(new FormLayout("max(200dlu;p),1dlu:g", "p,2dlu,max(50dlu;p):g"));

		FormLayout lyMain = new FormLayout("1dlu:g,2dlu,p", "p:g,2dlu,p,2dlu,p,2dlu,p,2dlu,p,2dlu,p,2dlu,p,2dlu,p");
		JPanel pnlMain = new JPanel(lyMain);

		pnlTemplate.add(new Label("Commit-completed message templates:"), cc.xy(1, 1));
		pnlTemplate.add(new JScrollPane(taCommitCompleteTemplate), cc.xywh(1, 3, 2, 1, CellConstraints.FILL, CellConstraints.FILL));

		pnlMain.add(pnlTemplate, cc.xywh(1, 1, 3, 1, CellConstraints.FILL, CellConstraints.FILL));
		pnlMain.add(btnSetCommitCompletedMessageTemplates, cc.xy(3, 3));
		pnlMain.add(new JSeparator(), cc.xywh(1, 4, 3, 1, CellConstraints.FILL, CellConstraints.FILL));

		pnlMain.add(cbGlobalIgnoreEol, cc.xy(3, 5));
		pnlMain.add(cbShowIconsInContextMenu, cc.xy(3, 7));
		pnlMain.add(new JSeparator(), cc.xywh(1, 8, 3, 1, CellConstraints.FILL, CellConstraints.FILL));

		pnlMain.add(btnClearLogin, cc.xy(3, 9));
		pnlMain.add(btnShowLoginDialogNextTime, cc.xy(3, 11));
		pnlMain.add(btnExitPagavcs, cc.xy(3, 13));
		pnlMain.add(new JSeparator(), cc.xywh(1, 14, 3, 1, CellConstraints.FILL, CellConstraints.FILL));
		pnlMain.add(btnAbout, cc.xy(3, 15));

		taCommitCompleteTemplate.setText(Manager.getSettings().getCommitCompletedMessageTemplates());

		frame = GuiHelper.createAndShowFrame(pnlMain, "Settings", "other-app-icon.png", false);

		frame.addWindowListener(new WindowAdapter() {

			public void windowClosed(WindowEvent e) {
				SettingsStore.getInstance().setGlobalIgnoreEol(cbGlobalIgnoreEol.isSelected());
			}
		});
	}

	private boolean isShowIconsInContextMenus() throws IOException {
		String result = Manager.getOsCommandResult(null, "dconf", "read", KEY_CONTEXT_MENU_ICON);
		return Boolean.valueOf(result.trim());
	}

	private class AboutAction extends ThreadAction {

		public AboutAction() {
			super("About");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			FormLayout layout = new FormLayout("p:g", "p:g");
			JPanel pnlMain = new JPanel(layout);
			CellConstraints cc = new CellConstraints();
			JLabel lblText = new JLabel(StringHelper.convertMultilineTextToHtmlCenter(ABOUT_TEXT));
			lblText.setHorizontalAlignment(SwingConstants.CENTER);
			pnlMain.add(lblText, cc.xy(1, 1));

			JDialog dialog = GuiHelper.createDialog(frame, pnlMain, "About");
			dialog.setModalityType(JDialog.DEFAULT_MODALITY_TYPE);
			dialog.pack();
			dialog.setVisible(true);
			dialog.dispose();
		}

	}

	private class ClearLoginCacheAction extends ThreadAction {

		public ClearLoginCacheAction() {
			super("Clear saved login name/password");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			int choice = JOptionPane.showConfirmDialog(Manager.getRootFrame(), "Are you sure you want to delete all saved login and password information?",
					"Deleting login/password cache", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
			if (choice == JOptionPane.YES_OPTION) {
				Manager.getSettings().clearLogin();
			}
		}

	}

	private class ShowIconsInContextMenuAction extends ThreadAction {

		public ShowIconsInContextMenuAction() {
			super("Show icons in context menu");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			Runtime.getRuntime().exec("dconf write " + KEY_CONTEXT_MENU_ICON + " " + Boolean.toString(cbShowIconsInContextMenu.isSelected()));
		}

	}

	private class ShowLoginDialogNextTimeAction extends ThreadAction {

		public ShowLoginDialogNextTimeAction() {
			super("Force login dialog once");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			Manager.setForceShowingLoginDialogNextTime(true);
		}

	}

	private class ExitPagavcsAction extends ThreadAction {

		public ExitPagavcsAction() {
			super("Exit/Restart PagaVCS server");
			setTooltip("Press refresh in nautilus after pressing this button");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			System.exit(0);
		}

	}

	private class SetCommitCompletedMessageTemplatesAction extends ThreadAction {

		public SetCommitCompletedMessageTemplatesAction() {
			super("Set templates");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			Manager.getSettings().setCommitCompletedMessageTemplates(taCommitCompleteTemplate.getText());
		}

	}

}
