package hu.pagavcs.mug;

import com.mucommander.file.AbstractFile;
import com.mucommander.file.util.FileSet;
import com.mucommander.ui.main.menu.TablePopupMenu;


public interface ContextMenuExtension {

	ContextMenuExtensionPositions getPosition();

	int getPriority();

	void addMenu(TablePopupMenu tablePopupMenu, AbstractFile currentFolder, AbstractFile clickedFile, boolean parentFolderClicked, FileSet markedFiles);
}
