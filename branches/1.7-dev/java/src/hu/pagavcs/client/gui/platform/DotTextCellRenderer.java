package hu.pagavcs.client.gui.platform;

import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Insets;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

public class DotTextCellRenderer extends DefaultTableCellRenderer {

	private String origText;
	private String truncatePrefix;

	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

		int availableWidth = table.getColumnModel().getColumn(column).getWidth();
		availableWidth -= table.getIntercellSpacing().getWidth();
		Insets borderInsets = getBorder().getBorderInsets(this);
		availableWidth -= (borderInsets.left + borderInsets.right);
		String cellText = origText;
		FontMetrics fm = getFontMetrics(getFont());

		if (truncatePrefix != null && cellText.startsWith(truncatePrefix)) {
			cellText = "..." + cellText.substring(truncatePrefix.length());
		}

		if (fm.stringWidth(cellText) > availableWidth) {
			String dots = "...";
			int textWidth = fm.stringWidth(dots);
			int i = cellText.length() - 1;

			for (; i > 0; i--) {
				textWidth += fm.charWidth(cellText.charAt(i));

				if (textWidth > availableWidth) {
					break;
				}
			}

			super.setText(dots + cellText.substring(i + 1));
		} else {
			super.setText(cellText);
		}

		return this;
	}

	@Override
	public void setText(String text) {
		super.setText(text);
		origText = text;
	}

	public void setTruncatePrefix(String truncatePrefix) {
		this.truncatePrefix = truncatePrefix;
	}
}