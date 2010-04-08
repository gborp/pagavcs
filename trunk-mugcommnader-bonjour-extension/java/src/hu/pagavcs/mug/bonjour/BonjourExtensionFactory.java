package hu.pagavcs.mug.bonjour;

import com.mucommander.extensions.DrivePopupMenuExtension;
import com.mucommander.extensions.DrivePopupMenuExtensionFactory;
import com.mucommander.extensions.MainMenuBarExtension;
import com.mucommander.extensions.MainMenuBarExtensionFactory;
import com.mucommander.extensions.ServiceExtension;
import com.mucommander.extensions.ServiceExtensionFactory;

public class BonjourExtensionFactory implements DrivePopupMenuExtensionFactory, ServiceExtensionFactory, MainMenuBarExtensionFactory {

	public DrivePopupMenuExtension getDrivePopupMenuExtension() {
		return new BonjourExtension();
	}

	public ServiceExtension getServiceExtension() {
		return new BonjourExtension();
	}

	public MainMenuBarExtension getMainMenuBarExtension() {
		return new BonjourExtension();
	}

}
