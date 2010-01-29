package hu.pagavcs.gui.platform;

import java.awt.Dimension;
import java.awt.Rectangle;

import javax.swing.JTree;
import javax.swing.plaf.TreeUI;

public class Tree extends JTree {

	public Tree() {
		super(new Object[] {});
		setVisibleRowCount(1);
		setShowsRootHandles(true);
		setExpandsSelectedPaths(true);
	}

	public Dimension getPreferredScrollableViewportSize() {
		int width = getPreferredSize().width;
		int visRows = getVisibleRowCount();
		int height = -1;

		if (isFixedRowHeight())
			height = visRows * getRowHeight();
		else {
			TreeUI ui = getUI();

			if (ui != null && visRows > 0) {
				int rc = ui.getRowCount(this);

				if (rc >= visRows) {
					Rectangle bounds = getRowBounds(visRows - 1);
					if (bounds != null) {
						height = bounds.y + bounds.height;
					}
				} else if (rc > 0) {
					Rectangle bounds = getRowBounds(0);
					if (bounds != null) {
						height = bounds.height * visRows;
					}
				}
			}
			if (height == -1) {
				height = 16 * visRows;
			}
		}
		return new Dimension(width, height);
	}

}
