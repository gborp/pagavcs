package hu.pagavcs.mug.openterminal;

import com.mucommander.extensions.ContextMenuExtension;
import com.mucommander.extensions.ContextMenuExtensionFactory;

public class OpenTerminalExtensionFactory implements ContextMenuExtensionFactory {

	public ContextMenuExtension getInstance() {
		return new OpenTerminalExtension();
	}

}
