package com.mucommander.extensions;

import hu.pagavcs.mug.findfile.FindFileListContextMenu;
import hu.pagavcs.mug.openterminal.OpenTerminalExtensionFactory;
import hu.pagavcs.mug.pagavcs.PagaVcsContextMenuExtensionFactory;
import hu.pagavcs.mug.pagavcs.PagaVcsFileEmblemExtensionFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DynamicExtensionsManager {

	private static DynamicExtensionsManager                                    singleton;
	private boolean                                                            inited;

	private HashMap<ContextMenuExtensionPositions, List<ContextMenuExtension>> mapContextMenuExtensions;
	private List<FileEmblemExtension>                                          lstFileEmblemExtensions;

	public static DynamicExtensionsManager getInstance() {
		if (singleton == null) {
			singleton = new DynamicExtensionsManager();
		}
		return singleton;
	}

	private void addContextMenuExtension(ContextMenuExtension ext) {
		List<ContextMenuExtension> lst = mapContextMenuExtensions.get(ext.getPosition());
		if (lst == null) {
			lst = new ArrayList<ContextMenuExtension>();
			mapContextMenuExtensions.put(ext.getPosition(), lst);
		}
		lst.add(ext);
	}

	public void init() {
		if (!inited) {

			mapContextMenuExtensions = new HashMap<ContextMenuExtensionPositions, List<ContextMenuExtension>>();

			addContextMenuExtension(new PagaVcsContextMenuExtensionFactory().getInstance());
			addContextMenuExtension(new OpenTerminalExtensionFactory().getInstance());
			addContextMenuExtension(new FindFileListContextMenu());

			lstFileEmblemExtensions = new ArrayList<FileEmblemExtension>();
			lstFileEmblemExtensions.add(new PagaVcsFileEmblemExtensionFactory().getInstance());


			inited = true;
		}
	}

	public HashMap<ContextMenuExtensionPositions, List<ContextMenuExtension>> getMapContextMenuExtensions() {
		return this.mapContextMenuExtensions;
	}

	public List<FileEmblemExtension> getLstFileEmblemExtensions() {
		return this.lstFileEmblemExtensions;
	}

}
