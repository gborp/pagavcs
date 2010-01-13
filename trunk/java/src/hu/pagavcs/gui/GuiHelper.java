package hu.pagavcs.gui;

import hu.pagavcs.bl.Manager;

import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.text.JTextComponent;

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

}
