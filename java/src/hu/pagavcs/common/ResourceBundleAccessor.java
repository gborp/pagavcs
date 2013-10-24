package hu.pagavcs.common;

import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.net.URL;

import javax.swing.ImageIcon;

public class ResourceBundleAccessor {

	public static ImageIcon getSmallImage(String name) {
		ImageIcon image = getImage(name);
		BufferedImage result = new BufferedImage(image.getIconWidth(),
				image.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
		image.paintIcon(null, result.getGraphics(), 0, 0);

		return new ImageIcon(result.getScaledInstance(16, 16,
				BufferedImage.SCALE_SMOOTH));
	}

	public static ImageIcon getImage(String name) {
		URL url = ResourceBundleAccessor.class
				.getResource("/hu/pagavcs/client/resources/" + name);
		if (url != null) {
			ImageIcon ii = new ImageIcon(Toolkit.getDefaultToolkit().getImage(
					url));
			return ii;
		}
		return null;
	}
}
