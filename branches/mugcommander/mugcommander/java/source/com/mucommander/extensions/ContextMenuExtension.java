package com.mucommander.extensions;

import com.mucommander.file.AbstractFile;
import com.mucommander.file.util.FileSet;
import com.mucommander.ui.main.MainFrame;
import com.mucommander.ui.main.menu.TablePopupMenu;

public interface ContextMenuExtension {

	ContextMenuExtensionPositions getPosition();

	int getPriority();

	void addMenu(TablePopupMenu tablePopupMenu, MainFrame mainFrame, AbstractFile currentFolder, AbstractFile clickedFile, boolean parentFolderClicked,
	        FileSet markedFiles);
}
