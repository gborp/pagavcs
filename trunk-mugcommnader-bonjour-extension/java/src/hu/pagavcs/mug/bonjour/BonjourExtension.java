package hu.pagavcs.mug.bonjour;

import java.util.Hashtable;

import javax.swing.JMenuItem;

import com.mucommander.bonjour.BonjourMenu;
import com.mucommander.bonjour.BonjourService;
import com.mucommander.conf.impl.MuConfiguration;
import com.mucommander.extensions.DrivePopupMenuExtension;
import com.mucommander.extensions.MainMenuBarExtension;
import com.mucommander.extensions.ServiceExtension;
import com.mucommander.ui.action.MuAction;
import com.mucommander.ui.action.impl.OpenLocationAction;
import com.mucommander.ui.helper.MnemonicHelper;
import com.mucommander.ui.main.FolderPanel;
import com.mucommander.ui.main.MainFrame;
import com.mucommander.ui.main.SplashScreen;
import com.mucommander.ui.main.DrivePopupButton.CustomOpenLocationAction;

public class BonjourExtension implements DrivePopupMenuExtension, ServiceExtension, MainMenuBarExtension {

	public JMenuItem getDrivePopupMenuItem(final FolderPanel folderPanel, final MainFrame mainFrame) {
		return new BonjourMenu() {

			public MuAction getMenuItemAction(BonjourService bs) {
				return new CustomOpenLocationAction(folderPanel, mainFrame, new Hashtable<String, Object>(), bs.getURL(), bs.getNameWithProtocol());
			}
		};
	}

	public void startService(boolean useSplash, SplashScreen splashScreen) {
		if (useSplash) {
			splashScreen.setLoadingMessage("Starting Bonjour services discovery...");
		}
		com.mucommander.bonjour.BonjourDirectory.setActive(MuConfiguration.getVariable(MuConfiguration.ENABLE_BONJOUR_DISCOVERY,
		        MuConfiguration.DEFAULT_ENABLE_BONJOUR_DISCOVERY));
	}

	public JMenuItem getMainMenuBarItem(MnemonicHelper menuItemMnemonicHelper, final MainFrame mainFrame) {
		BonjourMenu bonjourMenu = new BonjourMenu() {

			@Override
			public MuAction getMenuItemAction(BonjourService bs) {
				return new OpenLocationAction(mainFrame, new Hashtable<String, Object>(), bs.getURL(), bs.getNameWithProtocol());
			}
		};
		char mnemonic = menuItemMnemonicHelper.getMnemonic(bonjourMenu.getName());
		if (mnemonic != 0)
			bonjourMenu.setMnemonic(mnemonic);
		bonjourMenu.setIcon(null);

		return bonjourMenu;
	}

}
