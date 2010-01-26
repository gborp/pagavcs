package hu.pagavcs.gui;

import hu.pagavcs.bl.Cancelable;
import hu.pagavcs.bl.Manager;
import hu.pagavcs.bl.OnSwing;
import hu.pagavcs.bl.PagaException;
import hu.pagavcs.operation.RepoBrowser;
import hu.pagavcs.operation.RepoBrowser.RepoBrowserStatus;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;

import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;

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
public class RepoBrowserGui implements Working, Cancelable, TreeWillExpandListener {

	private RepoBrowser repoBrowser;
	private Label       lblWorkingCopy;
	private Label       lblUrl;
	private Label       lblStatus;
	private JTree       tree;
	private String      rootUrl;

	public RepoBrowserGui(RepoBrowser repoBrowser) {
		this.repoBrowser = repoBrowser;
	}

	public void display() throws SVNException {
		FormLayout layout = new FormLayout("right:p, 4dlu,p:g,p", "p,4dlu,p,4dlu,p:g,4dlu,p");
		JPanel pnlMain = new JPanel(layout);
		CellConstraints cc = new CellConstraints();
		lblWorkingCopy = new Label();
		lblUrl = new Label();
		lblStatus = new Label();
		tree = new Tree();
		tree.addTreeWillExpandListener(this);
		tree.addMouseListener(new PopupupMouseListener());

		pnlMain.add(new JLabel("Url:"), cc.xywh(1, 1, 1, 1));
		pnlMain.add(lblUrl, cc.xywh(3, 1, 1, 1));

		pnlMain.add(new JLabel("Working copy:"), cc.xywh(1, 3, 1, 1));
		pnlMain.add(lblWorkingCopy, cc.xywh(3, 3, 1, 1));

		pnlMain.add(new JScrollPane(tree), cc.xywh(1, 5, 4, 1, CellConstraints.FILL, CellConstraints.FILL));

		pnlMain.add(lblStatus, cc.xywh(4, 7, 1, 1));

		Window frame = Manager.createAndShowFrame(pnlMain, "Repository Browser");
	}

	public void setStatus(RepoBrowserStatus status) {
		lblStatus.setText("Status: " + status.toString());
	}

	public void setStatusStartWorking() {
		setStatus(RepoBrowserStatus.WORKING);
	}

	public void setStatusStopWorking() {
		setStatus(RepoBrowserStatus.COMPLETED);
	}

	public boolean isCancel() {
		return repoBrowser.isCancel();
	}

	public void setCancel(boolean cancel) throws Exception {
		repoBrowser.setCancel(true);
	}

	public void setURL(final String url) throws Exception {
		new OnSwing() {

			protected void process() throws Exception {
				lblUrl.setText(url);
				rootUrl = url;

				DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Root Node");
				addRootNode(rootNode, url);
				tree.setRootVisible(false);
				tree.setModel(new DefaultTreeModel(rootNode));
			}
		}.run();
	}

	private DefaultMutableTreeNode addNode(DefaultMutableTreeNode parentNode, SVNDirEntry dirEntry) {
		TreeNode treeNode = new TreeNode(dirEntry);
		DefaultMutableTreeNode node = new DefaultMutableTreeNode(treeNode);
		parentNode.add(node);

		if (dirEntry.getKind().equals(SVNNodeKind.DIR)) {
			treeNode.setLoaded(false);
			node.add(new DefaultMutableTreeNode("..."));
		} else {
			treeNode.setLoaded(true);
		}

		return node;
	}

	private void addRootNode(DefaultMutableTreeNode parentNode, String url) throws SVNException, PagaException {

		// SVNDirEntry info = repoBrowser.getDirEntry(url);
		ArrayList<DefaultMutableTreeNode> lstPath = new ArrayList<DefaultMutableTreeNode>();
		lstPath.add(parentNode);

		for (SVNDirEntry li : repoBrowser.getDirEntryChain(url)) {
			parentNode = addNode(parentNode, li);
			lstPath.add(parentNode);
		}

		TreePath treePath = new TreePath(lstPath.toArray());
		tree.setSelectionPath(treePath);

	}

	private void loadChildren(DefaultMutableTreeNode parentNode, SVNDirEntry url) throws SVNException, PagaException {

		if (url.getKind().equals(SVNNodeKind.DIR)) {
			List<SVNDirEntry> lstChildren = repoBrowser.getChilds(url.getURL().toDecodedString());
			Collections.sort(lstChildren, SvnDirEntryComparator.getInstance());
			for (SVNDirEntry child : lstChildren) {
				addNode(parentNode, child);
			}
		}
	}

	public void setWorkingCopy(final String workingCopy) throws Exception {
		new OnSwing() {

			protected void process() throws Exception {
				lblWorkingCopy.setText(workingCopy);
			}
		}.run();
	}

	public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {}

	public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
		try {
			DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) event.getPath().getLastPathComponent();
			if (!(parentNode.getUserObject() instanceof TreeNode)) {
				return;
			}

			TreeNode parentTreeNode = (TreeNode) parentNode.getUserObject();

			if (!parentTreeNode.isLoaded()) {
				parentNode.removeAllChildren();
				loadChildren(parentNode, parentTreeNode.getSvnDirEntry());
				parentTreeNode.setLoaded(true);
			}
		} catch (Exception e) {
			Manager.handle(e);
		}
	}

	private static class TreeNode {

		private final SVNDirEntry svnDirEntry;
		private boolean           loaded;

		public TreeNode(SVNDirEntry svnDirEntry) {
			this.svnDirEntry = svnDirEntry;
		}

		public String toString() {
			if (loaded) {
				return svnDirEntry.getName();
			}
			return svnDirEntry.getName() + " ...";
		}

		public SVNDirEntry getSvnDirEntry() {
			return svnDirEntry;
		}

		public void setLoaded(boolean loaded) {
			this.loaded = loaded;
		}

		public boolean isLoaded() {
			return loaded;
		}
	}

	private static class SvnDirEntryComparator implements Comparator<SVNDirEntry> {

		private static SvnDirEntryComparator singleton;

		public static SvnDirEntryComparator getInstance() {
			if (singleton == null) {
				singleton = new SvnDirEntryComparator();
			}
			return singleton;
		}

		public int compare(SVNDirEntry o1, SVNDirEntry o2) {
			if (o1.getKind().equals(SVNNodeKind.DIR) && !o2.getKind().equals(SVNNodeKind.DIR)) {
				return -1;
			}
			if (!o1.getKind().equals(SVNNodeKind.DIR) && o2.getKind().equals(SVNNodeKind.DIR)) {
				return 1;
			}

			return o1.getName().compareTo(o2.getName());
		}

	}

	private class CopyUrlToClipboard extends AbstractAction {

		private final PopupupMouseListener popupupMouseListener;

		public CopyUrlToClipboard(PopupupMouseListener popupupMouseListener) {
			super("Copy URL to Clipboard");
			this.popupupMouseListener = popupupMouseListener;
		}

		public void actionPerformed(ActionEvent e) {
			popupupMouseListener.hidePopup();
			TreeNode li = popupupMouseListener.getSelected();
			Manager.setClipboard(li.getSvnDirEntry().getURL().toDecodedString());
		}
	}

	private class PopupupMouseListener extends MouseAdapter {

		private JPopupMenu ppVisible;
		private JPopupMenu ppAll;
		private TreeNode   selected;

		public PopupupMouseListener() {
			ppAll = new JPopupMenu();
			ppAll.add(new CopyUrlToClipboard(this));
		}

		public TreeNode getSelected() {
			return selected;
		}

		private void hidePopup() {
			if (ppVisible != null) {
				ppVisible.setVisible(false);
				ppVisible = null;
			}
		}

		private void showPopup(MouseEvent e) {
			hidePopup();

			int x = e.getX();
			int y = e.getY();
			JTree tree = (JTree) e.getSource();
			TreePath path = tree.getPathForLocation(x, y);
			if (path == null) {
				return;
			}

			tree.setSelectionPath(path);

			DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
			selected = (TreeNode) node.getUserObject();
			ppVisible = ppAll;
			ppVisible.setInvoker(tree);
			ppVisible.setLocation(e.getXOnScreen(), e.getYOnScreen());
			ppVisible.setVisible(true);
			e.consume();
		}

		public void mousePressed(MouseEvent e) {
			if (e.isPopupTrigger())
				showPopup(e);
		}

		public void mouseReleased(MouseEvent e) {
			if (e.isPopupTrigger())
				showPopup(e);
		}
	}

}
