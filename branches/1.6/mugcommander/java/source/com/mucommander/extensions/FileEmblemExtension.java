package com.mucommander.extensions;

import java.io.File;

import javax.swing.Icon;

import com.mucommander.file.AbstractFile;

public interface FileEmblemExtension {

	Icon getDecoratedAbstractFileIcon(Icon icon, AbstractFile file);

	Icon getDecoratedFileIcon(Icon icon, File file);
}
