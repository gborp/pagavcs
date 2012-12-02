package hu.pagavcs.mug.gnome;

import com.mucommander.extensions.ContextMenuExtension;
import com.mucommander.extensions.ContextMenuExtensionFactory;

public class OpenWithExtensionFactory implements ContextMenuExtensionFactory {

	public ContextMenuExtension getContextMenuExtension() {
		return new OpenWithExtension();
	}

}
