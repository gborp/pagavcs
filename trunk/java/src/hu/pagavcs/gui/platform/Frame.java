package hu.pagavcs.gui.platform;

import hu.pagavcs.bl.Manager;

import java.awt.Toolkit;

import javax.swing.JFrame;

public class Frame extends JFrame {

	public Frame(String iconName) {
		super();
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setIconImage(Toolkit.getDefaultToolkit().getImage(Manager.class.getResource(iconName)));
	}

	public void setTitlePrefix(String prefix) {
		setTitle(prefix + " - " + getTitle());
	}
}
