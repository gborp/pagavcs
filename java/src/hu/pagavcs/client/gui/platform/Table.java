package hu.pagavcs.client.gui.platform;

import hu.pagavcs.client.bl.Manager;
import hu.pagavcs.client.bl.OnSwing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.CellRendererPane;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

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
public class Table<L extends ListItem> extends JTable {

	private static final long serialVersionUID = -1;
	private Label lblMessage;
	private final TableModel<L> tableModel;
	private volatile Timer tmrResize;
	private volatile boolean resizeIsTimed;

	public Table(TableModel<L> tableModel) {
		super(tableModel);
		this.tableModel = tableModel;
		setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		setFillsViewportHeight(true);
		setShowGrid(false);
		tmrResize = new Timer("Delayed resize table", true);

		addAncestorListener(new AncestorListener() {

			public void ancestorRemoved(AncestorEvent event) {
				if (tmrResize != null) {
					Timer oldTimer = tmrResize;
					tmrResize = null;
					oldTimer.cancel();
					oldTimer.purge();
				}
			}

			public void ancestorMoved(AncestorEvent event) {
			}

			public void ancestorAdded(AncestorEvent event) {
			}
		});

		tableModel.addTableModelListener(new TableModelListener() {

			public void tableChanged(TableModelEvent e) {

				if (!resizeIsTimed) {
					resizeIsTimed = true;
					if (tmrResize != null) {
						tmrResize.schedule(new DoResizeTask(), Manager.TABLE_RESIZE_DELAY);
					}
				}
			}
		});

		addComponentListener(new ComponentAdapter() {

			public void componentResized(ComponentEvent e) {
				if (!resizeIsTimed) {
					resizeIsTimed = true;
					if (tmrResize != null) {
						tmrResize.schedule(new DoResizeTask(), Manager.TABLE_RESIZE_DELAY);
					}
				}
			}
		});

	}

	private class DoResizeTask extends TimerTask {

		public void run() {
			try {
				new OnSwing(true) {

					protected void process() throws Exception {
						resizeIsTimed = false;
						resizeColumns();
					}

				}.run();
			} catch (Exception e) {
				Manager.handle(e);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public TableModel<L> getModel() {
		return (TableModel<L>) super.getModel();
	}

	public void showMessage(String message, Icon icon) {
		hideMessage();
		lblMessage = new Label(message);
		lblMessage.setIcon(icon);
		lblMessage.setHorizontalAlignment(SwingConstants.CENTER);
		lblMessage.setVerticalAlignment(SwingConstants.CENTER);

		setLayout(new BorderLayout());
		add(lblMessage, BorderLayout.CENTER);
		revalidate();
		repaint();
	}

	public void hideMessage() {
		if (lblMessage != null) {
			remove(lblMessage);
			setLayout(null);
			lblMessage = null;
			revalidate();
			repaint();
		}
	}

	public void resizeColumns() {
		int columnCount = getColumnCount();

		DefaultTableColumnModel colModel = (DefaultTableColumnModel) getColumnModel();

		int rowCount = getRowCount();
		ArrayList<L> lstRows = new ArrayList<L>(rowCount);
		for (int i = 0; i < rowCount; i++) {
			lstRows.add(tableModel.getRow(convertRowIndexToModel(i)));
		}

		JViewport parent = (JViewport) getParent();
		int viewWidth = parent.getSize().width;

		boolean hasRows = !lstRows.isEmpty();
		int[] columnPreferredWidth = new int[columnCount];

		int headerTotalWidth = 0;
		int interCellSpacingHorizontal = getIntercellSpacing().width;
		for (int i = 0; i < columnCount; i++) {
			TableColumn col = colModel.getColumn(i);

			TableCellRenderer headerRenderer = col.getHeaderRenderer();
			if (headerRenderer == null) {
				headerRenderer = getTableHeader().getDefaultRenderer();
				if (headerRenderer == null) {
					headerRenderer = getDefaultRenderer(String.class);
				}
			}

			int width = headerRenderer.getTableCellRendererComponent(this, col.getHeaderValue(), false, false, -1, i).getPreferredSize().width
					+ interCellSpacingHorizontal;
			columnPreferredWidth[i] = width;

			if (!hasRows) {
				if (i != columnCount - 1) {
					col.setMaxWidth(width);
				} else {
					if ((headerTotalWidth + width) < viewWidth) {
						width = viewWidth - headerTotalWidth;
					}
				}

				// col.setMinWidth(width);
				col.setPreferredWidth(width);
				col.setWidth(width);
			}
			headerTotalWidth += width;
		}

		if (hasRows) {
			int dataTotalWidth = 0;

			for (int i = 0; i < columnCount; i++) {

				TableColumn col = colModel.getColumn(i);
				int width = 0;

				TableCellRenderer renderer = getCellRenderer(0, i);
				for (L li : lstRows) {
					Component comp = renderer.getTableCellRendererComponent(this, li.getValue(i), false, false, 0, i);
					width = Math.max(width, comp.getPreferredSize().width);
				}

				width += interCellSpacingHorizontal;
				width = Math.max(width, columnPreferredWidth[i]);

				if (i != columnCount - 1) {
					col.setMaxWidth(width);
				} else {
					if ((dataTotalWidth + width) < viewWidth) {
						width = viewWidth - dataTotalWidth;
					}
				}
				// col.setMinWidth(width);
				col.setPreferredWidth(width);
				col.setWidth(width);
				dataTotalWidth += width;
			}
			// setPreferredSize(new Dimension(Math.max(headerTotalWidth,
			// dataTotalWidth), 16));
		} else {
			// setPreferredSize(new Dimension(headerTotalWidth, 16));
		}
	}

	public void followScrollToNewItems() {
		revalidate();
		scrollRectToVisible(getCellRect(getRowCount() - 1, 0, true));
		repaint();
	}

	public String getToolTipText(MouseEvent e) {
		java.awt.Point p = e.getPoint();
		int rowIndex = rowAtPoint(p);
		int colIndex = columnAtPoint(p);
		if (rowIndex == -1 || colIndex == -1) {
			return null;
		}

		return tableModel.getCellToolTip(convertColumnIndexToModel(colIndex), convertRowIndexToModel(rowIndex));
	}

	protected JTableHeader createDefaultTableHeader() {
		return new JTableHeader(columnModel) {

			public String getToolTipText(MouseEvent e) {
				java.awt.Point p = e.getPoint();
				int index = columnModel.getColumnIndexAtX(p.x);
				if (index == -1) {
					return null;
				}
				int realIndex = columnModel.getColumn(index).getModelIndex();
				return tableModel.getColumnDescription(realIndex);
			}
		};
	}

	public void scrollRectToVisible(Rectangle aRect) {
		Container parent;
		int dx = getX(), dy = getY();

		for (parent = getParent(); !(parent == null) && !(parent instanceof JComponent) && !(parent instanceof CellRendererPane); parent = parent.getParent()) {
			Rectangle bounds = parent.getBounds();

			dx += bounds.x;
			dy += bounds.y;
		}

		if (!(parent == null) && !(parent instanceof CellRendererPane)) {
			aRect.x += dx;
			aRect.y += dy;

			JComponent jcompParent = ((JComponent) parent);

			Rectangle visibleRect = jcompParent.getVisibleRect();
			Rectangle newARect = new Rectangle(visibleRect.x, aRect.y, visibleRect.width, aRect.height);

			if (!jcompParent.getVisibleRect().intersects(newARect)) {
				aRect.x = newARect.x;
				aRect.y = newARect.y;
				aRect.width = newARect.width;
				aRect.height = newARect.height;

				jcompParent.scrollRectToVisible(aRect);
				aRect.x -= dx;
				aRect.y -= dy;
			}
		}
	}

	public List<L> getSelectedItems() {
		ArrayList<L> lstResult = new ArrayList<L>();
		for (int row : getSelectedRows()) {
			lstResult.add(tableModel.getRow(convertRowIndexToModel(row)));
		}
		return lstResult;
	}

	public L getSelectedItem() {
		return tableModel.getRow(convertRowIndexToModel(getSelectedRow()));
	}

}
