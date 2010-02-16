package hu.pagavcs.gui.platform;

import hu.pagavcs.bl.Manager;

import java.awt.Image;
import java.awt.Toolkit;

import javax.swing.JFrame;

public class Frame extends JFrame {

	private static Image defaultIcon;

	public Frame() {
		super();
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		if (defaultIcon == null) {
			defaultIcon = Toolkit.getDefaultToolkit().getImage(Manager.class.getResource("/hu/pagavcs/resources/icon.png"));
		}
		setIconImage(defaultIcon);
	}

	public void setTitlePrefix(String prefix) {
		setTitle(prefix + " - " + getTitle());
	}
}
