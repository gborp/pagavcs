package hu.pagavcs.mug;

import java.io.IOException;
import java.io.InputStream;

import com.mucommander.file.AbstractFile;
import com.mucommander.file.AbstractROArchiveFile;
import com.mucommander.file.ArchiveEntry;
import com.mucommander.file.ArchiveEntryIterator;
import com.mucommander.file.UnsupportedFileOperationException;

public class FindFileArchiveFile extends AbstractROArchiveFile {

	private final String findId;

	public FindFileArchiveFile(String findId, AbstractFile file) {
		this(findId, new FakeFindFile(file));
	}

	private FindFileArchiveFile(String findId, FakeFindFile file) {
		super(file);
		this.findId = findId;

		long totalSize = 0;
		for (AbstractFile child : FindManager.getInstance().getResults(findId)) {
			if (!child.isDirectory()) {
				totalSize += child.getSize();
			}
		}
		file.setSize(totalSize);
	}

	// public String getAbsolutePath() {
	// return super.getAbsolutePath() + "/FIND";
	// }

	// //////////////////////////////////////
	// AbstractArchiveFile implementation //
	// //////////////////////////////////////

	@Override
	public ArchiveEntryIterator getEntryIterator() throws IOException, UnsupportedFileOperationException {
		return new FindFileArchiveEntryIterator(findId);
	}

	@Override
	public InputStream getEntryInputStream(ArchiveEntry entry, ArchiveEntryIterator entryIterator) throws IOException, UnsupportedFileOperationException {
		// Will throw an IOException if the file designated by the entry doesn't
		// exist
		return ((FindFileArchiveEntry) entry).getRealFile().getInputStream();
		// return FileFactory.getFile(entry.getPath(), true).getInputStream();
	}
}
