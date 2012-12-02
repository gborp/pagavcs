package com.mucommander.extensions;

import javax.swing.JMenuItem;

import com.mucommander.ui.main.FolderPanel;
import com.mucommander.ui.main.MainFrame;

public interface DrivePopupMenuExtension {

	JMenuItem getDrivePopupMenuItem(FolderPanel folderPanel, MainFrame mainFrame);

}
