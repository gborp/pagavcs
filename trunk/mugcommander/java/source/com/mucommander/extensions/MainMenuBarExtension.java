package com.mucommander.extensions;

import javax.swing.JMenuItem;

import com.mucommander.ui.helper.MnemonicHelper;
import com.mucommander.ui.main.MainFrame;

public interface MainMenuBarExtension {

	JMenuItem getMainMenuBarItem(MnemonicHelper menuItemMnemonicHelper, MainFrame mainFrame);

}
