package hu.pagavcs.common;

import java.awt.Toolkit;
import java.net.URL;

import javax.swing.ImageIcon;

public class ResourceBundleAccessor {

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
