package hu.pagavcs.mug.pagavcs;

import com.mucommander.extensions.FileEmblemExtension;
import com.mucommander.extensions.FileEmblemExtensionFactory;


public class PagaVcsFileEmblemExtensionFactory implements FileEmblemExtensionFactory {

	public FileEmblemExtension getInstance() {
		return PagaVcsFileEmblemExtension.getInstance();
	}

}
