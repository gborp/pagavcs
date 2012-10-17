package hu.pagavcs.client.gui.repobrowser;

import hu.pagavcs.client.bl.Cancelable;
import hu.pagavcs.client.bl.Manager;
import hu.pagavcs.client.bl.OnSwing;
import hu.pagavcs.client.bl.OnSwingWait;
import hu.pagavcs.client.bl.PagaException;
import hu.pagavcs.client.bl.ThreadAction;
import hu.pagavcs.client.gui.LocationCallback;
import hu.pagavcs.client.gui.Working;
import hu.pagavcs.client.gui.action.ShowLogAction;
import hu.pagavcs.client.gui.platform.EditField;
import hu.pagavcs.client.gui.platform.Frame;
import hu.pagavcs.client.gui.platform.GuiHelper;
import hu.pagavcs.client.gui.platform.Label;
import hu.pagavcs.client.gui.platform.Tree;
import hu.pagavcs.client.operation.Checkout;
import hu.pagavcs.client.operation.RepoBrowser;
import hu.pagavcs.client.operation.RepoBrowser.RepoBrowserStatus;
import hu.pagavcs.common.ResourceBundleAccessor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
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
public class RepoBrowserGui implements Working, Cancelable,
		TreeWillExpandListener {

	private RepoBrowser repoBrowser;
	private Label lblWorkingCopy;
	private EditField sfUrl;
	private Label lblStatus;
	private JTree tree;
	private String rootUrl;
	private Frame frame;
	private Label lblTreeWorking;

	public RepoBrowserGui(RepoBrowser repoBrowser) {
		this.repoBrowser = repoBrowser;
	}

	public void display() throws SVNException {
		FormLayout layout = new FormLayout("right:p, 2dlu,p:g,p",
				"p,2dlu,p,2dlu,p:g,2dlu,p");
		JPanel pnlMain = new JPanel(layout);
		CellConstraints cc = new CellConstraints();
		lblWorkingCopy = new Label();
		sfUrl = new EditField();
		sfUrl.addFocusListener(new FocusListener() {

			public void focusLost(FocusEvent e) {
				try {
					urlChanged();
				} catch (Exception ex) {
					Manager.handle(ex);
				}
			}

			public void focusGained(FocusEvent e) {
			}
		});
		sfUrl.addKeyListener(new KeyAdapter() {

			public void keyTyped(KeyEvent e) {
				if (e.getKeyChar() == '\n') {
					try {
						urlChanged();
					} catch (Exception ex) {
						Manager.handle(ex);
					}
				}
			}
		});

		JButton btnRefresh = new JButton(new RefreshNodeAction());

		lblStatus = new Label();
		tree = new Tree();
		tree.addTreeWillExpandListener(this);
		tree.addMouseListener(new PopupupMouseListener());

		tree.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
				KeyStroke.getKeyStroke("F5"), "REFRESH_SVN_TREE");
		tree.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
				KeyStroke.getKeyStroke("control R"), "REFRESH_SVN_TREE");
		tree.getActionMap().put("REFRESH_SVN_TREE", new RefreshNodeAction());

		tree.setCellRenderer(new RepoTreeCellRender());
		treeWorking();

		pnlMain.add(new JLabel("URL:"), cc.xywh(1, 1, 1, 1));
		pnlMain.add(sfUrl, cc.xywh(3, 1, 1, 1));
		pnlMain.add(btnRefresh, cc.xywh(4, 1, 1, 1));

		pnlMain.add(new JLabel("Working copy:"), cc.xywh(1, 3, 1, 1));
		pnlMain.add(lblWorkingCopy, cc.xywh(3, 3, 1, 1));

		pnlMain.add(new JScrollPane(tree),
				cc.xywh(1, 5, 4, 1, CellConstraints.FILL, CellConstraints.FILL));

		pnlMain.add(lblStatus, cc.xywh(4, 7, 1, 1));

		frame = GuiHelper.createAndShowFrame(pnlMain, "Repository Browser");
	}

	private void treeWorking() {
		lblTreeWorking = new Label("Working...");
		lblTreeWorking.setIcon(Manager.getIconInformation());
		lblTreeWorking.setHorizontalAlignment(SwingConstants.CENTER);
		lblTreeWorking.setVerticalAlignment(SwingConstants.CENTER);

		tree.setLayout(new BorderLayout());
		tree.add(lblTreeWorking, BorderLayout.CENTER);
		tree.revalidate();
		tree.repaint();
	}

	private void treeFinished() {
		if (lblTreeWorking != null) {
			tree.remove(lblTreeWorking);
			lblTreeWorking = null;
		}
	}

	private RepoTreeNode getSelectedRepoTreeNode() {
		TreePath path = tree.getSelectionPath();

		DefaultMutableTreeNode node = (DefaultMutableTreeNode) path
				.getLastPathComponent();
		return (RepoTreeNode) node.getUserObject();
	}

	private class UrlChangedAction extends ThreadAction {

		public UrlChangedAction() {
			super("URL changed");
		}

		public void actionProcess(ActionEvent e) throws Exception {
			if ((rootUrl == null || !rootUrl.equals(sfUrl.getText()))
					&& !sfUrl.getText().trim().isEmpty()) {
				new OnSwing() {

					protected void process() throws Exception {
						workStarted();
					}
				}.run();
				rootUrl = sfUrl.getText();

				final List<SVNDirEntry> lstDirChain = repoBrowser
						.getDirEntryChain(rootUrl);
				final DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(
						"Root Node");
				new OnSwingWait<Object, Object>(null) {

					protected Object process() throws Exception {
						tree.setRootVisible(false);
						tree.setModel(new DefaultTreeModel(rootNode));
						tree.requestFocus();

						return null;
					}
				}.run();

				new OnSwing() {

					protected void process() throws Exception {
						treeFinished();
						addRootNode(rootNode, rootUrl, lstDirChain);
						workEnded();
					}
				}.run();
			}
		}
	}

	public void urlChanged() throws Exception {
		new UrlChangedAction().actionPerformed(null);
	}

	public void setStatus(RepoBrowserStatus status) {
		lblStatus.setText("Status: " + status.toString());
	}

	public void workStarted() {
		setStatus(RepoBrowserStatus.WORKING);
	}

	public void workEnded() {
		setStatus(RepoBrowserStatus.COMPLETED);
	}

	public boolean isCancel() {
		return repoBrowser.isCancel();
	}

	public void setCancel(boolean cancel) throws Exception {
		repoBrowser.setCancel(true);
	}

	public void setURL(final String url) throws Exception {
		sfUrl.setText(url);
		urlChanged();
	}

	private DefaultMutableTreeNode addNode(DefaultMutableTreeNode parentNode,
			SVNDirEntry dirEntry) {
		RepoTreeNode treeNode = new RepoTreeNode(dirEntry);
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

	private void addRootNode(DefaultMutableTreeNode parentNode, String url,
			List<SVNDirEntry> lstDirChain) throws Exception {

		final DefaultMutableTreeNode realParentNode = parentNode;
		final ArrayList<DefaultMutableTreeNode> lstPath = new ArrayList<DefaultMutableTreeNode>();
		lstPath.add(parentNode);

		for (SVNDirEntry li : lstDirChain) {
			parentNode = addNode(parentNode, li);
			lstPath.add(parentNode);
		}

		new OnSwing(true) {

			protected void process() throws Exception {
				ArrayList<DefaultMutableTreeNode> lstNewPath = new ArrayList<DefaultMutableTreeNode>();
				DefaultMutableTreeNode pNode = realParentNode;

				lstNewPath.add(pNode);
				tree.expandPath(new TreePath(lstNewPath.toArray()));

				for (int i = 1; i < lstPath.size(); i++) {

					DefaultMutableTreeNode path = lstPath.get(i);

					for (int c = 0; c < pNode.getChildCount(); c++) {
						DefaultMutableTreeNode child = (DefaultMutableTreeNode) pNode
								.getChildAt(c);
						RepoTreeNode userObject = (RepoTreeNode) child
								.getUserObject();
						if (path.getUserObject().equals(userObject)) {
							lstNewPath.add(child);
							pNode = child;
							break;
						}
					}
					tree.expandPath(new TreePath(lstNewPath.toArray()));
				}
				TreePath lastTreePath = new TreePath(lstNewPath.toArray());
				tree.makeVisible(lastTreePath);
				tree.scrollPathToVisible(lastTreePath);
				tree.setSelectionPath(lastTreePath);
			}
		}.run();

	}

	private void loadChildren(DefaultMutableTreeNode parentNode, SVNDirEntry url)
			throws SVNException, PagaException {

		if (url.getKind().equals(SVNNodeKind.DIR)) {
			List<SVNDirEntry> lstChildren = repoBrowser.getChilds(url.getURL()
					.toDecodedString());
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

	public void treeWillCollapse(TreeExpansionEvent event)
			throws ExpandVetoException {
	}

	public void treeWillExpand(TreeExpansionEvent event)
			throws ExpandVetoException {
		try {
			DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) event
					.getPath().getLastPathComponent();
			if (!(parentNode.getUserObject() instanceof RepoTreeNode)) {
				return;
			}

			RepoTreeNode parentTreeNode = (RepoTreeNode) parentNode
					.getUserObject();

			if (!parentTreeNode.isLoaded()) {
				parentNode.removeAllChildren();
				loadChildren(parentNode, parentTreeNode.getSvnDirEntry());
				parentTreeNode.setLoaded(true);
			}
		} catch (Exception e) {
			Manager.handle(e);
		}
	}

	private static class RepoTreeNode {

		private final SVNDirEntry svnDirEntry;
		private boolean loaded;

		public RepoTreeNode(SVNDirEntry svnDirEntry) {
			this.svnDirEntry = svnDirEntry;
		}

		public String toString() {
			if (loaded) {
				return svnDirEntry.getName();
			}
			return svnDirEntry.getName();
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

		public boolean equals(Object obj) {
			if (!(obj instanceof RepoTreeNode)) {
				return false;
			}
			return ((RepoTreeNode) obj).getSvnDirEntry().getRelativePath()
					.equals(svnDirEntry.getRelativePath());
		}

		public int hashCode() {
			return svnDirEntry.hashCode();
		}
	}

	private static class SvnDirEntryComparator implements
			Comparator<SVNDirEntry> {

		private static SvnDirEntryComparator singleton;

		public static SvnDirEntryComparator getInstance() {
			if (singleton == null) {
				singleton = new SvnDirEntryComparator();
			}
			return singleton;
		}

		public int compare(SVNDirEntry o1, SVNDirEntry o2) {
			if (o1.getKind().equals(SVNNodeKind.DIR)
					&& !o2.getKind().equals(SVNNodeKind.DIR)) {
				return -1;
			}
			if (!o1.getKind().equals(SVNNodeKind.DIR)
					&& o2.getKind().equals(SVNNodeKind.DIR)) {
				return 1;
			}

			return o1.getName().compareTo(o2.getName());
		}

	}

	private class RefreshNodeAction extends ThreadAction {

		public RefreshNodeAction() {
			super("Refresh", ResourceBundleAccessor
					.getSmallImage("actions/pagavcs-refresh.png"));
		}

		public void actionProcess(ActionEvent e) throws Exception {
			new OnSwing() {

				protected void process() throws Exception {
					TreePath path = tree.getSelectionPath();
					RepoTreeNode li = getSelectedRepoTreeNode();
					li.setLoaded(false);
					tree.collapsePath(path);
					tree.expandPath(path);

					((DefaultTreeModel) tree.getModel())
							.nodeStructureChanged((javax.swing.tree.TreeNode) path
									.getLastPathComponent());

				}
			}.run();

		}
	}

	private class CreateFolderAction extends ThreadAction {

		private final PopupupMouseListener popupupMouseListener;
		private boolean doCreate;
		private String folderName;
		private String logMessage;

		public CreateFolderAction(PopupupMouseListener popupupMouseListener) {
			super("Create folder", ResourceBundleAccessor
					.getSmallImage("actions/pagavcs-add.png"));
			this.popupupMouseListener = popupupMouseListener;
		}

		public void actionProcess(ActionEvent e) throws Exception {
			doCreate = false;
			new OnSwingWait<Object, Object>() {

				protected Object process() throws Exception {
					CreateFolderDialog dialog = new CreateFolderDialog();
					dialog.display(frame);
					if (dialog.isDoCreate()) {
						doCreate = true;
						folderName = dialog.getFolderName();
						logMessage = dialog.getLogMessage();
					}
					return null;
				}
			}.run();

			if (doCreate) {
				RepoTreeNode li = popupupMouseListener.getSelected();
				repoBrowser.createFolder(li.getSvnDirEntry(), folderName,
						logMessage);
				new RefreshNodeAction().actionProcess(null);
			}
		}
	}

	private class CheckoutAction extends ThreadAction {

		private final PopupupMouseListener popupupMouseListener;

		public CheckoutAction(PopupupMouseListener popupupMouseListener) {
			super("Checkout", ResourceBundleAccessor
					.getSmallImage("actions/pagavcs-checkout.png"));
			this.popupupMouseListener = popupupMouseListener;
		}

		public void actionProcess(ActionEvent e) throws Exception {
			RepoTreeNode li = popupupMouseListener.getSelected();

			Checkout checkout = new Checkout(repoBrowser.getPath(), li
					.getSvnDirEntry().getURL().toDecodedString());
			checkout.execute();
		}
	}

	private class CopyUrlToClipboard extends AbstractAction {

		private final PopupupMouseListener popupupMouseListener;

		public CopyUrlToClipboard(PopupupMouseListener popupupMouseListener) {
			super("Copy URL to Clipboard", ResourceBundleAccessor
					.getSmallImage("actions/pagavcs-dbus.png"));
			this.popupupMouseListener = popupupMouseListener;
		}

		public void actionPerformed(ActionEvent e) {
			RepoTreeNode li = popupupMouseListener.getSelected();
			Manager.setClipboard(li.getSvnDirEntry().getURL().toDecodedString());
		}
	}

	private class PopupupMouseListener extends MouseAdapter {

		private JPopupMenu ppAll;
		private RepoTreeNode selected;
		private TreePath path;

		public PopupupMouseListener() {
			ppAll = new JPopupMenu();
			ppAll.add(new RefreshNodeAction());
			ppAll.add(new CopyUrlToClipboard(this));
			ppAll.add(new CheckoutAction(this));
			ppAll.add(new ShowLogAction(new LocationCallback() {

				@Override
				public boolean isFilePath() {
					return false;
				}

				@Override
				public String getLocation() {
					return getSelectedRepoTreeNode().getSvnDirEntry().getURL()
							.toString();
				}
			}));
			ppAll.add(new CreateFolderAction(this));
		}

		public RepoTreeNode getSelected() {
			return selected;
		}

		private void showPopup(MouseEvent e) {

			int x = e.getX();
			int y = e.getY();
			JTree tree = (JTree) e.getSource();
			path = tree.getPathForLocation(x, y);
			if (path == null) {
				return;
			}

			tree.setSelectionPath(path);

			DefaultMutableTreeNode node = (DefaultMutableTreeNode) path
					.getLastPathComponent();
			selected = (RepoTreeNode) node.getUserObject();
			JPopupMenu ppVisible = ppAll;
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

	private static class RepoTreeCellRender extends DefaultTreeCellRenderer {

		private static final Color COLOR_NON_EXPANDED = new Color(27, 0, 116);
		private Font italicFont;
		private Font normalFont;

		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value,
				boolean selected, boolean expanded, boolean leaf, int row,
				boolean hasFocus) {
			Component ret = super.getTreeCellRendererComponent(tree, value,
					selected, expanded, leaf, row, hasFocus);

			JLabel label = (JLabel) ret;

			// Object userObj = ((DefaultMutableTreeNode)
			// value).getUserObject();

			if (!selected && !expanded && !leaf) {
				label.setForeground(COLOR_NON_EXPANDED);
			}

			if (!expanded && !leaf) {
				if (italicFont == null) {
					normalFont = label.getFont();
					italicFont = normalFont.deriveFont(Font.ITALIC);
				}
				label.setFont(italicFont);
			} else {
				if (normalFont != null) {
					label.setFont(normalFont);
				}
			}

			return ret;
		}

	}
}
