package hu.pagavcs.mug;

import com.mucommander.file.AbstractFile;
import com.mucommander.file.ArchiveEntry;

public class FindFileArchiveEntry extends ArchiveEntry {

	protected AbstractFile realFile;

	public FindFileArchiveEntry(String path, boolean directory, long date, long size, AbstractFile realFile) {
		super(path, directory, date, size, true);
		this.realFile = realFile;
	}

	protected AbstractFile getRealFile() {
		return realFile;
	}
}
