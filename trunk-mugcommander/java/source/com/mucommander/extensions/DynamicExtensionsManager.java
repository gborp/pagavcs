package com.mucommander.extensions;

import hu.pagavcs.mug.findfile.FindFileListContextMenu;

import java.io.File;
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
			lstFileEmblemExtensions = new ArrayList<FileEmblemExtension>();

			String cfgDir = System.getProperty("extensions.cfg.dir");

			if (cfgDir == null) {
				// TODO throw exception or something
				System.out.println("extensions.cfg.dir variable is not set!");
			} else {

				for (String extClassName : new File(cfgDir, "extensions").list()) {
					try {
						Object instance = Class.forName(extClassName).newInstance();
						if (instance instanceof ContextMenuExtensionFactory) {
							addContextMenuExtension(((ContextMenuExtensionFactory) instance).getContextMenuExtension());
						}

						if (instance instanceof FileEmblemExtensionFactory) {
							lstFileEmblemExtensions.add(((FileEmblemExtensionFactory) instance).getFileEmblemExtension());
						}
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			}
			addContextMenuExtension(new FindFileListContextMenu());
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
