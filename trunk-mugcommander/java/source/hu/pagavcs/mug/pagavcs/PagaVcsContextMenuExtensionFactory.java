package hu.pagavcs.mug.pagavcs;

import com.mucommander.extensions.ContextMenuExtension;
import com.mucommander.extensions.ContextMenuExtensionFactory;

public class PagaVcsContextMenuExtensionFactory implements ContextMenuExtensionFactory {

	public ContextMenuExtension getInstance() {
		return new PagaVcsContextMenuExtension();
	}

}
