package hu.pagavcs.client.gui.platform;

import java.awt.Dimension;
import java.awt.Window;

import javax.swing.JDialog;
import javax.swing.SwingUtilities;

public class Dialog extends JDialog {

	public Dialog(Window owner) {
		super(owner);
	}

	public void packLater() {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				Dimension pref = getPreferredSize();
				Dimension current = getSize();
				double newWidth = current.getWidth();
				double newHeight = current.getHeight();
				if (pref.getWidth() > current.getWidth()) {
					newWidth = pref.getWidth();
				}
				if (pref.getHeight() > current.getHeight()) {
					newHeight = pref.getHeight();
				}
				Dimension newSize = new Dimension((int) newWidth,
						(int) newHeight);
				if (!current.equals(newSize)) {
					setSize(newSize);
				}
			}

		});
	}
}
