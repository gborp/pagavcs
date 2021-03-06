package hu.pagavcs.client.gui;

import hu.pagavcs.client.bl.OnSwingWait;
import hu.pagavcs.client.bl.SettingsStore;
import hu.pagavcs.client.bl.ThreadAction;
import hu.pagavcs.client.gui.platform.EditField;
import hu.pagavcs.client.gui.platform.Frame;
import hu.pagavcs.client.gui.platform.GuiHelper;
import hu.pagavcs.client.gui.platform.Label;
import hu.pagavcs.client.gui.platform.MessagePane;
import hu.pagavcs.client.operation.Commit;
import hu.pagavcs.client.operation.MergeOperation;

import java.awt.event.ActionEvent;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class MergeGui {

	private EditField sfWorkingCopy;
	private EditField sfRepo;
	private JButton btnShowLog;

	private MergeOperation backend;
	private JComboBox cboUrlMergeFrom;
	private EditField sfRevisionRange;
	private JCheckBox cbReverseMerge;
	private JButton btnShowLogFrom;
	private JButton btnMergeRevisions;
	private Frame frame;
	private JCheckBox cbIgnoreEolStyle;
	private JCheckBox cbCommitToo;

	public MergeGui(MergeOperation backend) {
		this.backend = backend;
	}

	public void display() {
		new OnSwingWait<Object, Object>() {
			protected Object process() throws Exception {
				FormLayout layout = new FormLayout("right:p, 2dlu,p:g, p",
						"p,2dlu,p,2dlu,p,4dlu,p,2dlu,p,2dlu,p,2dlu,p,2dlu,p");
				JPanel pnlMain = new JPanel(layout);
				CellConstraints cc = new CellConstraints();

				Label lblWorkingCopy = new Label("Path:");
				sfWorkingCopy = new EditField(backend.getPath());
				sfWorkingCopy.setEditable(false);
				Label lblRepo = new Label("Repository:");
				sfRepo = new EditField();
				sfRepo.setEditable(false);
				btnShowLog = new JButton(new ShowLogAction());

				cboUrlMergeFrom = new JComboBox();
				cboUrlMergeFrom.setEditable(true);

				sfRevisionRange = new EditField();
				sfRevisionRange
						.setToolTipText("<html>Example: 4-7,9,11,15-HEAD<br>To merge all revisions, leave the box empty.</html>");
				cbReverseMerge = new JCheckBox("Reverse merge");
				btnShowLogFrom = new JButton(new ShowLogMergeFromAction());
				btnMergeRevisions = new JButton(new MergeAction());
				cbIgnoreEolStyle = new JCheckBox("Ignore EOL style");
				cbIgnoreEolStyle
						.setToolTipText("Ignore end-of-line style (eg. windows or unix style)");
				cbCommitToo = new JCheckBox("Commit too");

				if (!Boolean.FALSE.equals(SettingsStore.getInstance()
						.getAutoCopyCommitRevisionToClipboard())) {
					cbIgnoreEolStyle.setSelected(true);
				}

				pnlMain.add(lblWorkingCopy, cc.xywh(1, 1, 1, 1));
				pnlMain.add(sfWorkingCopy, cc.xywh(3, 1, 2, 1));
				pnlMain.add(lblRepo, cc.xywh(1, 3, 1, 1));
				pnlMain.add(sfRepo, cc.xywh(3, 3, 2, 1));
				pnlMain.add(btnShowLog, cc.xywh(4, 5, 1, 1));

				pnlMain.add(new JSeparator(), cc.xywh(1, 6, 4, 1));
				pnlMain.add(new JLabel("URL to merge from:"),
						cc.xywh(1, 7, 1, 1));
				pnlMain.add(cboUrlMergeFrom, cc.xywh(3, 7, 2, 1));
				pnlMain.add(new JLabel("Revision range to merge:"),
						cc.xywh(1, 9, 1, 1));
				pnlMain.add(sfRevisionRange, cc.xywh(3, 9, 2, 1));
				pnlMain.add(cbReverseMerge, cc.xywh(3, 11, 1, 1));
				pnlMain.add(btnShowLogFrom, cc.xywh(4, 11, 1, 1));
				pnlMain.add(cbIgnoreEolStyle, cc.xywh(3, 13, 1, 1));
				pnlMain.add(btnMergeRevisions, cc.xywh(4, 13, 1, 1));
				pnlMain.add(cbCommitToo, cc.xywh(3, 15, 1, 1));

				frame = GuiHelper.createAndShowFrame(pnlMain, "Merge Settings",
						"other-app-icon.png");
				frame.setTitlePrefix(backend.getPath());
				return null;
			}
		}.run();
	}

	public void setURL(final String text) {
		sfRepo.setText(text);
	}

	public void setPrefillMergeFromRevision(String prefillMergeFromRevision) {
		sfRevisionRange.setText(prefillMergeFromRevision);
	}

	public void setPrefillMergeFromUrl(String prefillMergeFromUrl) {
		cboUrlMergeFrom.setSelectedItem(prefillMergeFromUrl);
	}

	public void setPrefillCommitToo(Boolean prefillCommitToo) {
		cbIgnoreEolStyle.setSelected(prefillCommitToo);
	}

	public void setUrlHistory(final String[] urlHistory) {
		ComboBoxModel modelUrl = new DefaultComboBoxModel(urlHistory);
		cboUrlMergeFrom.setModel(modelUrl);
	}

	public void pack() {
		new OnSwingWait<Object, Object>() {
			protected Object process() throws Exception {
				frame.pack();
				return null;
			}
		}.run();
	}

	private class ShowLogAction extends ThreadAction {

		public ShowLogAction() {
			super("Show log");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			backend.doShowLog(sfWorkingCopy.getText());
		}
	}

	private class ShowLogMergeFromAction extends ThreadAction {

		public ShowLogMergeFromAction() {
			super("Show log (from)");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			String url = cboUrlMergeFrom.getSelectedItem().toString().trim();
			backend.doShowLogUrl(url);
		}
	}

	private class MergeAction extends ThreadAction {

		public MergeAction() {
			super("Merge");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			String url = cboUrlMergeFrom.getSelectedItem().toString().trim();
			SettingsStore.getInstance().setMergeIgnoreEol(
					cbIgnoreEolStyle.isSelected());

			int count = backend.doMerge(sfRepo.getText(), sfWorkingCopy
					.getText(), url, sfRevisionRange.getText().trim(),
					cbReverseMerge.isSelected(), cbIgnoreEolStyle.isSelected());

			if (count < 1) {
				MessagePane.showWarning(frame, "Nothing merged",
						"Nothing has been merged!");
			}

			if (cbCommitToo.isSelected()) {
				new Commit(sfWorkingCopy.getText()).execute();
			}
		}

	}

}
