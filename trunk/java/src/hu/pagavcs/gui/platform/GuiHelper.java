package hu.pagavcs.gui.platform;

import hu.pagavcs.bl.Manager;
import hu.pagavcs.bl.WindowPreferencesSaverOnClose;
import hu.pagavcs.gui.CommitListItem;
import hu.pagavcs.operation.ContentStatus;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.util.HashMap;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JDialog;
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

	private static HashMap<ContentStatus, ImageIcon> mapContentStatusIcon;
	private static HashMap<ContentStatus, ImageIcon> mapPropertyContentStatusIcon;

	static {
		initIcons();
	}

	private static ImageIcon loadEmblem(String name) {

		Integer width = 12;
		Integer height = 12;

		ImageIcon ii = new ImageIcon(Toolkit.getDefaultToolkit().getImage(CommitListItem.class.getResource("/hu/pagavcs/resources/emblems/" + name + ".png")));
		ImageIcon imageIcon = new ImageIcon(ii.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH));

		return imageIcon;
	}

	private static void initIcons() {
		mapContentStatusIcon = new HashMap<ContentStatus, ImageIcon>();
		mapContentStatusIcon.put(ContentStatus.ADDED, loadEmblem("added"));
		mapContentStatusIcon.put(ContentStatus.CONFLICTED, loadEmblem("conflict"));
		mapContentStatusIcon.put(ContentStatus.DELETED, loadEmblem("deleted"));
		mapContentStatusIcon.put(ContentStatus.IGNORED, loadEmblem("ignored"));
		mapContentStatusIcon.put(ContentStatus.MODIFIED, loadEmblem("modified"));
		mapContentStatusIcon.put(ContentStatus.OBSTRUCTED, loadEmblem("obstructed"));
		mapContentStatusIcon.put(ContentStatus.UNVERSIONED, loadEmblem("unversioned"));
		mapContentStatusIcon.put(ContentStatus.MISSING, loadEmblem("missing"));

		mapPropertyContentStatusIcon = new HashMap<ContentStatus, ImageIcon>();
		mapPropertyContentStatusIcon.put(ContentStatus.ADDED, loadEmblem("added"));
		mapPropertyContentStatusIcon.put(ContentStatus.CONFLICTED, loadEmblem("conflict"));
		mapPropertyContentStatusIcon.put(ContentStatus.DELETED, loadEmblem("deleted"));
		mapPropertyContentStatusIcon.put(ContentStatus.IGNORED, loadEmblem("ignored"));
		mapPropertyContentStatusIcon.put(ContentStatus.MODIFIED, loadEmblem("modified"));
		mapPropertyContentStatusIcon.put(ContentStatus.OBSTRUCTED, loadEmblem("obstructed"));
		mapPropertyContentStatusIcon.put(ContentStatus.UNVERSIONED, loadEmblem("unversioned"));
		mapPropertyContentStatusIcon.put(ContentStatus.MISSING, loadEmblem("missing"));
	}

	public static ImageIcon getContentStatusIcon(ContentStatus contentStatus) {
		return mapContentStatusIcon.get(contentStatus);
	}

	public static ImageIcon getPropertyContentStatusIcon(ContentStatus contentStatus) {
		return mapPropertyContentStatusIcon.get(contentStatus);
	}

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

	public static void centerScreen(Window window) {
		Dimension dim = window.getToolkit().getScreenSize();
		Rectangle bounds = window.getBounds();
		bounds.x = (dim.width - bounds.width) / 2;
		bounds.y = (dim.height - bounds.height) / 2;
		setBounds(window, bounds);
	}

	private static void centerOnParent(Window parent, Window child) {
		Rectangle dim = new Rectangle(parent.getLocationOnScreen(), parent.getSize());
		Rectangle bounds = child.getBounds();
		bounds.x = dim.x + (dim.width - bounds.width) / 2;
		bounds.y = dim.y + (dim.height - bounds.height) / 2;
		setBounds(child, bounds);
	}

	public static Frame createFrame(JComponent pnlMain, String applicationName, String iconName) {
		return createFrame(pnlMain, applicationName, iconName, true);
	}

	public static Frame createFrame(JComponent pnlMain, String applicationName, String iconName, boolean addScrollPane) {
		if (iconName == null) {
			iconName = "/hu/pagavcs/resources/icon.png";
		}
		Frame frame = new Frame(applicationName, iconName);
		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().add(addBorder(pnlMain, addScrollPane), BorderLayout.CENTER);
		frame.setTitle(applicationName + " [" + Manager.getApplicationRootName() + "]");
		return frame;
	}

	public static Frame createAndShowFrame(JComponent pnlMain, String applicationName) {
		return createAndShowFrame(pnlMain, applicationName, null);
	}

	public static Frame createAndShowFrame(JComponent pnlMain, String applicationName, String iconName) {
		return createAndShowFrame(pnlMain, applicationName, iconName, true);
	}

	public static Frame createAndShowFrame(JComponent pnlMain, String applicationName, String iconName, boolean addScrollPane) {
		Frame frame = createFrame(pnlMain, applicationName, iconName, addScrollPane);
		frame.invalidate();
		frame.pack();

		Rectangle bounds = Manager.getSettings().getWindowBounds(applicationName);
		if (bounds != null) {
			setBounds(frame, bounds);
		} else {
			centerScreen(frame);
		}

		frame.addWindowListener(new WindowPreferencesSaverOnClose(null, applicationName));
		frame.setVisible(true);

		return frame;
	}

	public static JDialog createDialog(Window parent, JComponent main, String title) {
		final JDialog dialog = new JDialog(parent);
		dialog.getContentPane().add(addBorder(main, true));
		dialog.setTitle(title);
		dialog.setIconImage(Toolkit.getDefaultToolkit().getImage(Manager.class.getResource("/hu/pagavcs/resources/icon.png")));
		dialog.pack();

		dialog.addWindowListener(new WindowPreferencesSaverOnClose(parent, title));

		dialog.getRootPane().registerKeyboardAction(new ActionListener() {

			public void actionPerformed(ActionEvent actionEvent) {
				dialog.setVisible(false);
			}
		}, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

		Rectangle bounds = Manager.getSettings().getWindowBounds(parent, title);
		if (bounds != null) {
			setBounds(dialog, bounds);
		} else {
			centerOnParent(parent, dialog);
		}

		return dialog;
	}

	public static void setBounds(Window window, Rectangle bounds) {
		Dimension dim = window.getToolkit().getScreenSize();
		if (bounds.x < 0) {
			bounds.x = 0;
		}
		if (bounds.y < 0) {
			bounds.y = 0;
		}
		if (bounds.x + bounds.width >= dim.width) {
			bounds.x = dim.width - bounds.width;
		}
		if (bounds.y + bounds.height >= dim.height) {
			bounds.y = dim.height - bounds.height;
		}
		if (bounds.x < 0) {
			bounds.x = 0;
		}
		if (bounds.y < 0) {
			bounds.y = 0;
		}
		if (bounds.x + bounds.width >= dim.width) {
			bounds.width = dim.width - bounds.x;
		}
		if (bounds.y + bounds.height >= dim.height) {
			bounds.height = dim.height - bounds.y;
		}

		window.setBounds(bounds);
	}

	public static void closeWindow(Window window) {
		AWTEvent windowEvenet = new WindowEvent(window, WindowEvent.WINDOW_CLOSING);
		window.dispatchEvent(windowEvenet);
		window.setVisible(false);
		window.dispose();
	}

	private static JComponent addBorder(JComponent pnlMain, boolean addScrollPane) {
		JPanel pnlBorder = new JPanel(new FormLayout("2dlu,fill:p:g,2dlu", "2dlu,fill:p:g,2dlu"));

		pnlBorder.add(pnlMain, new CellConstraints(2, 2));

		if (addScrollPane) {
			return new JScrollPane(pnlBorder);
		} else {
			return pnlBorder;
		}
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

	/**
	 * @param color1
	 * @param color2
	 * @param mix
	 *            0.0: fully color1, 1.1: fully color2
	 * @return
	 */
	public static Color getColorMix(Color color1, Color color2, float mix) {
		int red = (int) ((1 - mix) * color1.getRed() + mix * color2.getRed());
		int green = (int) ((1 - mix) * color1.getGreen() + mix * color2.getGreen());
		int blue = (int) ((1 - mix) * color1.getBlue() + mix * color2.getBlue());
		return new Color(red, green, blue, color1.getAlpha());
	}
}
