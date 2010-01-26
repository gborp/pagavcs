package hu.pagavcs.gui;

import hu.pagavcs.bl.Manager;
import hu.pagavcs.bl.OnSwing;
import hu.pagavcs.bl.ThreadAction;

import java.awt.Color;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.ToolTipManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNConflictChoice;
import org.tmatesoft.svn.core.wc.SVNWCClient;

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
public class ResolveConflictGui {

	private static final String ATTRIBUTE_TYPE_KEY = "PAGAVCS-CONFLICT";
	private static final String ATTRIBUTE_NORMAL   = "normal";
	private static final String ATTRIBUTE_WORKING  = "working";
	private static final String ATTRIBUTE_THEIRS   = "theirs";
	private static final String ATTRIBUTE_MIXED    = "mixed";

	private TextPane            tpConflict;
	private final File          mixedFile;
	private final File          oldFile;
	private final File          newFile;
	private final File          wrkFile;
	private SimpleAttributeSet  setNormalText;
	private SimpleAttributeSet  setConflictWorking;
	private SimpleAttributeSet  setConflictTheirs;
	private SimpleAttributeSet  setMixedText;
	private JButton             btnReload;
	private JButton             btnSaveResolved;
	private Window              frame;
	private final Refreshable   parentRefreshable;

	public ResolveConflictGui(Refreshable parentRefreshable, File mixedFile, File oldFile, File newFile, File wrkFile) {
		this.parentRefreshable = parentRefreshable;
		this.mixedFile = mixedFile;
		this.oldFile = oldFile;
		this.newFile = newFile;
		this.wrkFile = wrkFile;
	}

	public void display() throws IOException, BadLocationException {

		if (wrkFile == null) {
			MessagePane.showError(null, "No conflict", "Unable to resolve conflict on a non-conflicted file.");
			return;
		}

		FormLayout layout = new FormLayout("p,1dlu:g, p", "p,4dlu,fill:10dlu:g,4dlu,p");
		JPanel pnlMain = new JPanel(layout);
		CellConstraints cc = new CellConstraints();

		tpConflict = new TextPane();
		tpConflict.setBackground(Color.WHITE);
		JScrollPane spConflict = new JScrollPane(tpConflict);
		btnReload = new JButton(new ReloadAction());
		btnSaveResolved = new JButton(new SaveResolvedAction());

		pnlMain.add(new Label(mixedFile.getPath()), cc.xywh(1, 1, 3, 1, CellConstraints.LEFT, CellConstraints.DEFAULT));
		pnlMain.add(spConflict, cc.xywh(1, 3, 3, 1));
		pnlMain.add(btnReload, cc.xy(1, 5));
		pnlMain.add(btnSaveResolved, cc.xy(3, 5));

		setNormalText = new SimpleAttributeSet();
		setNormalText.addAttribute(ATTRIBUTE_TYPE_KEY, ATTRIBUTE_NORMAL);

		setMixedText = new SimpleAttributeSet();
		StyleConstants.setBackground(setMixedText, Color.LIGHT_GRAY);
		setMixedText.addAttribute(ATTRIBUTE_TYPE_KEY, ATTRIBUTE_MIXED);

		setConflictWorking = new SimpleAttributeSet();
		StyleConstants.setBackground(setConflictWorking, Color.YELLOW);
		setConflictWorking.addAttribute(ATTRIBUTE_TYPE_KEY, ATTRIBUTE_WORKING);

		setConflictTheirs = new SimpleAttributeSet();
		StyleConstants.setBackground(setConflictTheirs, Color.CYAN);
		setConflictTheirs.addAttribute(ATTRIBUTE_TYPE_KEY, ATTRIBUTE_THEIRS);

		reload();

		frame = Manager.createAndShowFrame(new JScrollPane(pnlMain), "Resolve Conflict");

		Document doc = tpConflict.getStyledDocument();
		doc.addDocumentListener(new DocumentListener() {

			public void removeUpdate(DocumentEvent e) {
			// TODO Auto-generated method stub

			}

			public void insertUpdate(DocumentEvent e) {
			// TODO Auto-generated method stub

			}

			public void changedUpdate(DocumentEvent e) {
			// TODO Auto-generated method stub

			}
		});
	}

	private void reload() throws IOException, BadLocationException {
		String strMixed = Manager.getFileAsString(mixedFile);
		String[] lines = strMixed.split("\n");

		Document doc = tpConflict.getStyledDocument();
		doc.remove(0, doc.getLength());

		boolean conflictStarted = false;
		boolean conflictOtherBlock = false;

		for (String line : lines) {

			if (line.startsWith("<<<<<<< ")) {
				conflictStarted = true;
				continue;
			} else if (line.equals("=======")) {
				conflictStarted = false;
				conflictOtherBlock = true;
				continue;
			} else if (line.startsWith(">>>>>>> ")) {
				conflictStarted = false;
				conflictOtherBlock = false;
				continue;
			}

			SimpleAttributeSet setActual;
			if (conflictStarted) {
				setActual = setConflictWorking;
			} else if (conflictOtherBlock) {
				setActual = setConflictTheirs;
			} else {
				setActual = setNormalText;
			}

			doc.insertString(doc.getLength(), line + "\n", setActual);
		}
	}

	public void saveResolved() throws Exception {
		StyledDocument doc = (StyledDocument) tpConflict.getDocument();
		boolean unresolved = false;

		for (int offset = 0; offset < doc.getLength(); offset++) {
			Object attrType = tpConflict.getAttributeType(offset);
			if (attrType.equals(ATTRIBUTE_THEIRS) || attrType.equals(ATTRIBUTE_WORKING)) {
				unresolved = true;
				break;
			}
		}
		if (unresolved) {
			MessagePane.showError(frame, "Unresolved conflict(s)", "There are still unresolved conflicts, it cannot be marked as resolved.");
			return;
		}

		// save
		BufferedWriter out = new BufferedWriter(new FileWriter(mixedFile));
		out.write(doc.getText(0, doc.getLength()));
		out.close();

		SVNClientManager svnMgr = Manager.getSVNClientManagerForWorkingCopyOnly();
		SVNWCClient client = svnMgr.getWCClient();
		client.doResolve(mixedFile, SVNDepth.INFINITY, true, true, true, SVNConflictChoice.MERGED);
		Manager.invalidate(mixedFile);

		if (parentRefreshable != null) {
			parentRefreshable.refresh();
		}

		frame.setVisible(false);
		frame.dispose();
	}

	private class ReloadAction extends ThreadAction {

		public ReloadAction() {
			super("Reload");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			new OnSwing() {

				protected void process() throws Exception {
					reload();
				};
			}.run();

		}

	}

	private class SaveResolvedAction extends ThreadAction {

		public SaveResolvedAction() {
			super("Save resolved");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			new OnSwing() {

				protected void process() throws Exception {
					saveResolved();
				};
			}.run();

		}

	}

	private abstract class AbstractBlockAction extends ThreadAction {

		protected final TextPane tp;
		protected final Element  element;
		protected int            startMineOffset;
		protected int            endMineOffset;
		protected int            startTheirsOffset;
		protected int            endTheirsOffset;

		public AbstractBlockAction(TextPane tp, Element element, String label) {
			super(label);
			this.tp = tp;
			this.element = element;
		}

		public void actionProcess(ActionEvent e) throws Exception {
			StyledDocument doc = (StyledDocument) tp.getDocument();

			startMineOffset = element.getStartOffset();
			while (startMineOffset > 0 && (tp.getAttributeType(startMineOffset).equals(ATTRIBUTE_THEIRS))) {
				startMineOffset--;
			}
			while (startMineOffset > 0 && (tp.getAttributeType(startMineOffset).equals(ATTRIBUTE_WORKING))) {
				startMineOffset--;
			}
			startMineOffset++;
			endMineOffset = startMineOffset;
			while (endMineOffset < doc.getLength() && tp.getAttributeType(endMineOffset).equals(ATTRIBUTE_WORKING)) {
				endMineOffset++;
			}

			startTheirsOffset = endMineOffset;
			endTheirsOffset = startTheirsOffset;
			while (endTheirsOffset < doc.getLength() && tp.getAttributeType(endTheirsOffset).equals(ATTRIBUTE_THEIRS)) {
				endTheirsOffset++;
			}

			doBlockEdit(doc);
		}

		public abstract void doBlockEdit(StyledDocument doc) throws Exception;

	}

	private class UseMineTextBlockAction extends AbstractBlockAction {

		public UseMineTextBlockAction(TextPane tp, Element element) {
			super(tp, element, "Use mine text block");
		}

		public void doBlockEdit(StyledDocument doc) throws Exception {
			doc.setCharacterAttributes(startMineOffset, endMineOffset - startMineOffset, setMixedText, true);
			doc.remove(startTheirsOffset, endTheirsOffset - startTheirsOffset);
		}
	}

	private class UseMineBeforTheirsTextBlockAction extends AbstractBlockAction {

		public UseMineBeforTheirsTextBlockAction(TextPane tp, Element element) {
			super(tp, element, "Use mine text block before theirs");
		}

		public void doBlockEdit(StyledDocument doc) throws Exception {
			doc.setCharacterAttributes(startMineOffset, endMineOffset - startMineOffset, setMixedText, true);
			doc.setCharacterAttributes(startTheirsOffset, endTheirsOffset - startTheirsOffset, setMixedText, true);
		}
	}

	private class UseTheirsTextBlockAction extends AbstractBlockAction {

		public UseTheirsTextBlockAction(TextPane tp, Element element) {
			super(tp, element, "Use theirs text block");
		}

		public void doBlockEdit(StyledDocument doc) throws Exception {
			doc.setCharacterAttributes(startTheirsOffset, endTheirsOffset - startTheirsOffset, setMixedText, true);
			doc.remove(startMineOffset, endMineOffset - startMineOffset);
		}
	}

	private class UseTheirsBeforeMineTextBlockAction extends AbstractBlockAction {

		public UseTheirsBeforeMineTextBlockAction(TextPane tp, Element element) {
			super(tp, element, "Use theirs text block before mine");
		}

		public void doBlockEdit(StyledDocument doc) throws Exception {
			doc.setCharacterAttributes(startTheirsOffset, endTheirsOffset - startTheirsOffset, setMixedText, true);
			doc.insertString(endTheirsOffset, doc.getText(startMineOffset, endMineOffset - startMineOffset), setMixedText);
			doc.remove(startMineOffset, endMineOffset - startMineOffset);
		}

	}

	private class TextPane extends JTextPane {

		public TextPane() {
			ToolTipManager.sharedInstance().registerComponent(this);
			addMouseListener(new MouseAdapter() {

				private void showPopup(MouseEvent e) {
					JPopupMenu ppVisible = new JPopupMenu();

					Element element = getElementAt(e.getX(), e.getY());
					Object type = getAttributeType(element);

					if (type.equals(ATTRIBUTE_WORKING)) {
						ppVisible.add(new UseMineTextBlockAction(TextPane.this, element));
						ppVisible.add(new UseMineBeforTheirsTextBlockAction(TextPane.this, element));
					} else if (type.equals(ATTRIBUTE_THEIRS)) {
						ppVisible.add(new UseTheirsTextBlockAction(TextPane.this, element));
						ppVisible.add(new UseTheirsBeforeMineTextBlockAction(TextPane.this, element));
					}

					if (ppVisible.getComponentCount() > 0) {
						ppVisible.setInvoker(TextPane.this);
						ppVisible.setLocation(e.getXOnScreen(), e.getYOnScreen());
						ppVisible.setVisible(true);
						e.consume();
					}
				}

				public void mousePressed(MouseEvent e) {
					if (e.isPopupTrigger()) {
						showPopup(e);
					}
				}

				public void mouseReleased(MouseEvent e) {
					if (e.isPopupTrigger()) {
						showPopup(e);
					}
				}

			});
		}

		private Element getElementAt(int x, int y) {
			int charPos = viewToModel(new Point(x, y));
			StyledDocument doc = (StyledDocument) getDocument();
			return doc.getCharacterElement(charPos);
		}

		public Object getAttributeType(Element element) {
			AttributeSet charAttributes = element.getAttributes();
			return charAttributes.getAttribute(ATTRIBUTE_TYPE_KEY);
		}

		public Object getAttributeType(int pos) {
			StyledDocument doc = (StyledDocument) getDocument();
			return getAttributeType(doc.getCharacterElement(pos));
		}

		public String getToolTipText(MouseEvent event) {

			Element charElement = getElementAt(event.getX(), event.getY());
			Object type = getAttributeType(charElement);

			if (ATTRIBUTE_WORKING.equals(type)) {
				return "Working copy";
			} else if (ATTRIBUTE_THEIRS.equals(type)) {
				return "Theirs";
			} else if (ATTRIBUTE_MIXED.equals(type)) {
				return "Mixed";
			}
			return null;
		}
	}

}
