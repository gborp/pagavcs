package hu.pagavcs.gui.platform;

import hu.pagavcs.bl.Manager;
import hu.pagavcs.bl.WindowPreferencesSaverOnClose;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

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
public class GuiHelper {

	public static void addPopupMenu(JLabel component) {
		JPopupMenu contextMenu = new JPopupMenu();
		contextMenu.add(new CopyLabelAction(component));
		addPopupMenu(component, contextMenu);
	}

	public static void addPopupMenu(JTextComponent component) {
		JPopupMenu contextMenu = new JPopupMenu();
		contextMenu.add(new CutAction(component));
		contextMenu.add(new CopyAction(component));
		contextMenu.add(new PasteAction(component));
		addPopupMenu(component, contextMenu);
	}

	public static void addPopupMenu(JComponent component, final JPopupMenu contextMenu) {

		component.addMouseListener(new MouseAdapter() {

			public void mousePressed(MouseEvent e) {
				maybeShowPopup(e);
			}

			public void mouseReleased(MouseEvent e) {
				maybeShowPopup(e);
			}

			private void maybeShowPopup(MouseEvent e) {
				if (e.isPopupTrigger()) {
					contextMenu.show(e.getComponent(), e.getX(), e.getY());
				}
			}
		});
	}

	private static class CopyLabelAction extends AbstractAction {

		private final JLabel target;

		public CopyLabelAction(JLabel target) {
			super("Copy");
			this.target = target;
		}

		public void actionPerformed(ActionEvent e) {
			Manager.setClipboard(target.getText());
		}
	}

	private static class CopyAction extends AbstractAction {

		private final JTextComponent target;

		public CopyAction(JTextComponent target) {
			super("Copy");
			this.target = target;
		}

		public void actionPerformed(ActionEvent e) {
			boolean deselectAfterCopy = false;
			if ((target.getSelectedText() == null) || !target.isEditable()) {
				target.selectAll();
				deselectAfterCopy = true;
			}
			target.copy();
			if (deselectAfterCopy) {
				target.select(0, 0);
			}
		}
	}

	private static class CutAction extends AbstractAction {

		private final JTextComponent target;

		public CutAction(JTextComponent target) {
			super("Cut");
			this.target = target;
		}

		public void actionPerformed(ActionEvent e) {
			boolean deselectAfterCopy = false;
			if (target.getSelectedText() == null) {
				target.selectAll();
				deselectAfterCopy = true;
			}
			if (target.isEditable()) {
				target.cut();
			} else {
				target.copy();
			}

			if (deselectAfterCopy) {
				target.select(0, 0);
			}
		}
	}

	private static class PasteAction extends AbstractAction {

		private final JTextComponent target;

		public PasteAction(JTextComponent target) {
			super("Paste");
			this.target = target;
		}

		public void actionPerformed(ActionEvent e) {
			target.paste();
		}
	}

	private static void centerScreen(Window window) {
		Dimension dim = window.getToolkit().getScreenSize();
		Rectangle bounds = window.getBounds();
		window.setLocation((dim.width - bounds.width) / 2, (dim.height - bounds.height) / 2);
	}

	public static Window createAndShowFrame(JComponent pnlMain, String applicationName) {

		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.getContentPane().add(addBorder(pnlMain));
		frame.setTitle(applicationName + " [" + Manager.getApplicationRootName() + "]");
		frame.setIconImage(Toolkit.getDefaultToolkit().getImage(Manager.class.getResource("/hu/pagavcs/resources/icon.png")));
		frame.pack();

		Rectangle bounds = Manager.getSettings().getWindowBounds(applicationName);
		if (bounds != null) {
			frame.setBounds(bounds);
		} else {
			centerScreen(frame);
		}

		frame.addWindowListener(new WindowPreferencesSaverOnClose(applicationName));
		frame.setVisible(true);

		return frame;
	}

	public static JDialog createDialog(Window parent, JComponent main, String title) {
		JDialog dialog = new JDialog(parent);
		dialog.getContentPane().add(addBorder(main));
		dialog.setTitle(title);
		dialog.setIconImage(Toolkit.getDefaultToolkit().getImage(Manager.class.getResource("/hu/pagavcs/resources/icon.png")));
		dialog.pack();

		Rectangle bounds = Manager.getSettings().getWindowBounds(title);
		if (bounds != null) {
			dialog.setBounds(bounds);
		} else {
			centerScreen(dialog);
		}

		return dialog;
	}

	private static JComponent addBorder(JComponent pnlMain) {
		JPanel pnlBorder = new JPanel(new FormLayout("10dlu,fill:p:g,10dlu", "10dlu,fill:p:g,10dlu"));

		pnlBorder.add(pnlMain, new CellConstraints(2, 2));

		return new JScrollPane(pnlBorder);
	}

	public static void addUndoRedo(JTextComponent comp) {
		final UndoManager undo = new UndoManager();
		Document doc = comp.getDocument();
		doc.addUndoableEditListener(new UndoableEditListener() {

			public void undoableEditHappened(UndoableEditEvent evt) {
				undo.addEdit(evt.getEdit());
			}
		});
		ActionMap actionMap = comp.getActionMap();
		InputMap inputMap = comp.getInputMap();
		actionMap.put("Undo", new AbstractAction("Undo") {

			public void actionPerformed(ActionEvent evt) {
				try {
					if (undo.canUndo()) {
						undo.undo();
					}
				} catch (CannotUndoException e) {}
			}
		});
		inputMap.put(KeyStroke.getKeyStroke("control Z"), "Undo");
		actionMap.put("Redo", new AbstractAction("Redo") {

			public void actionPerformed(ActionEvent evt) {
				try {
					if (undo.canRedo()) {
						undo.redo();
					}
				} catch (CannotRedoException e) {}
			}
		});
		inputMap.put(KeyStroke.getKeyStroke("control Y"), "Redo");
	}
}
