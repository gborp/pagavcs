/*
 * This file is part of muCommander, http://www.mucommander.com
 * Copyright (C) 2002-2010 Maxence Bernard
 *
 * muCommander is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * muCommander is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mucommander.file.impl.local;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Pattern;

import com.mucommander.file.AbstractFile;
import com.mucommander.file.FileFactory;
import com.mucommander.file.FileLogger;
import com.mucommander.file.FileOperation;
import com.mucommander.file.FilePermissions;
import com.mucommander.file.FileProtocols;
import com.mucommander.file.FileURL;
import com.mucommander.file.GroupedPermissionBits;
import com.mucommander.file.IndividualPermissionBits;
import com.mucommander.file.PermissionBits;
import com.mucommander.file.ProtocolFile;
import com.mucommander.file.UnsupportedFileOperation;
import com.mucommander.file.UnsupportedFileOperationException;
import com.mucommander.file.filter.FilenameFilter;
import com.mucommander.file.util.PathUtils;
import com.mucommander.io.BufferPool;
import com.mucommander.io.FilteredOutputStream;
import com.mucommander.io.RandomAccessInputStream;
import com.mucommander.io.RandomAccessOutputStream;
import com.mucommander.runtime.OsFamilies;
import com.mucommander.runtime.OsFamily;

/**
 * LocalFile provides access to files located on a locally-mounted filesystem.
 * Note that despite the class' name, LocalFile instances may indifferently be
 * residing on a local hard drive, or on a remote server mounted locally by the
 * operating system.
 * 
 * <p>
 * The associated {@link FileURL} scheme is {@link FileProtocols#FILE}. The host
 * part should be {@link FileURL#LOCALHOST}, except for Windows UNC URLs (see
 * below). Native path separators ('/' or '\\' depending on the OS) can be used
 * in the path part.
 * 
 * <p>
 * Here are a few examples of valid local file URLs: <code>
 * file://localhost/C:\winnt\system32\<br>
 * file://localhost/usr/bin/gcc<br>
 * file://localhost/~<br>
 * file://home/maxence/..<br>
 * </code>
 * 
 * <p>
 * Windows UNC paths can be represented as FileURL instances, using the host
 * part of the URL. The URL format for those is the following:<br>
 * <code>file:\\server\share</code> .<br>
 * 
 * <p>
 * Under Windows, LocalFile will translate those URLs back into a UNC path. For
 * example, a LocalFile created with the <code>file://garfield/stuff</code>
 * FileURL will have the <code>getAbsolutePath()</code> method return
 * <code>\\garfield\stuff</code>. Note that this UNC path translation doesn't
 * happen on OSes other than Windows, which would not be able to handle the
 * path.
 * 
 * <p>
 * Access to local files is provided by the <code>java.io</code> API,
 * {@link #getUnderlyingFileObject()} allows to retrieve an
 * <code>java.io.File</code> instance corresponding to this LocalFile.
 * 
 * @author Maxence Bernard
 */
public class LocalFile extends ProtocolFile {

	protected File file;
	private FilePermissions permissions;

	/** Absolute file path, free of trailing separator */
	protected String absPath;

	/** Caches the parent folder, initially null until getParent() gets called */
	protected AbstractFile parent;
	/**
	 * Indicates whether the parent folder instance has been retrieved and
	 * cached or not (parent can be null)
	 */
	protected boolean parentValueSet;

	/**
	 * Underlying local filesystem's path separator: "/" under UNIX systems, "\"
	 * under Windows and OS/2
	 */
	public final static String SEPARATOR = File.separator;

	/** Pattern matching Windows-like drives' root, e.g. C:\ */
	final static Pattern driveRootPattern = Pattern
			.compile("^[a-zA-Z]{1}[:]{1}[\\\\]{1}");

	// Permissions can only be changed under Java 1.6 and up and are limited to
	// 'user' access.
	// Note: 'read' and 'execute' permissions have no meaning under Windows
	// (files are either read-only or
	// read-write) and as such can't be changed.

	/**
	 * Changeable permissions mask for Java 1.6 and up, on OSes other than
	 * Windows
	 */
	private static PermissionBits CHANGEABLE_PERMISSIONS_JAVA_1_6_NON_WINDOWS = new GroupedPermissionBits(
			448); // rwx------ (700 octal)

	/** Bit mask that indicates which permissions can be changed */
	private final static PermissionBits CHANGEABLE_PERMISSIONS = CHANGEABLE_PERMISSIONS_JAVA_1_6_NON_WINDOWS;

	/**
	 * List of known UNIX filesystems.
	 */
	public static final String[] KNOWN_UNIX_FS = { "adfs", "affs", "autofs",
			"cifs", "coda", "cramfs", "debugfs", "efs", "ext2", "ext3",
			"fuseblk", "hfs", "hfsplus", "hpfs", "iso9660", "jfs", "minix",
			"msdos", "ncpfs", "nfs", "nfs4", "ntfs", "qnx4", "reiserfs",
			"smbfs", "udf", "ufs", "usbfs", "vfat", "xfs" };

	/**
	 * Creates a new instance of LocalFile and a corresponding {@link File}
	 * instance.
	 */
	protected LocalFile(FileURL fileURL) throws IOException {
		this(fileURL, null);
	}

	/**
	 * Creates a new instance of LocalFile, using the given {@link File} if not
	 * <code>null</code>, creating a new {@link File} instance otherwise.
	 */
	protected LocalFile(FileURL fileURL, File file) throws IOException {
		super(fileURL);

		if (file == null) {
			String path;

			// If the URL denotes a Windows UNC file, translate the path back
			// into a Windows-style UNC path in the form
			// \\hostname\share\path .

			path = fileURL.getPath();

			// Create the java.io.File instance and throw an exception if the
			// path is not absolute.
			file = new File(path);
			if (!file.isAbsolute())
				throw new IOException();

			absPath = file.getAbsolutePath();

			// Remove the trailing separator if present
			if (absPath.endsWith(SEPARATOR))
				absPath = absPath.substring(0, absPath.length() - 1);
		} else {
			// the java.io.File instance was created by ls(), no need to
			// re-create
			// it or call the costly File#getAbsolutePath()
			this.absPath = fileURL.getPath();
		}

		this.file = file;
		this.permissions = new LocalFilePermissions(file);
	}

	// //////////////////////////////
	// LocalFile-specific methods //
	// //////////////////////////////

	/**
	 * Returns the user home folder. Most if not all OSes have one, but in the
	 * unlikely event that the OS doesn't have one or that the folder cannot be
	 * resolved, <code>null</code> will be returned.
	 * 
	 * @return the user home folder
	 */
	public static AbstractFile getUserHome() {
		String userHomePath = System.getProperty("user.home");
		if (userHomePath == null)
			return null;

		return FileFactory.getFile(userHomePath);
	}

	/**
	 * Returns the total and free space on the volume where this file resides.
	 * 
	 * <p>
	 * Using this method to retrieve both free space and volume space is more
	 * efficient than calling {@link #getFreeSpace()} and
	 * {@link #getTotalSpace()} separately -- the underlying method retrieving
	 * both attributes at the same time.
	 * </p>
	 * 
	 * @return a {totalSpace, freeSpace} long array, both values can be null if
	 *         the information could not be retrieved
	 * @throws IOException
	 *             if an I/O error occurred
	 */
	public long[] getVolumeInfo() throws IOException {
		return new long[] { getTotalSpace(), getFreeSpace() };
	}

	/**
	 * Uses platform dependant functions to retrieve the total and free space on
	 * the volume where this file resides.
	 * 
	 * @return a {totalSpace, freeSpace} long array, both values can be
	 *         <code>null</code> if the information could not be retrieved.
	 * @throws IOException
	 *             if an I/O error occurred
	 */
	protected long[] getNativeVolumeInfo() throws IOException {
		BufferedReader br = null;
		String absPath = getAbsolutePath();
		long dfInfo[] = new long[] { -1, -1 };

		try {
			if (OsFamily.getCurrent().isUnixBased()) {
				// Parses the output of 'df -P -k "filePath"' command on
				// UNIX-based systems to retrieve free and total space
				// information

				// 'df -P -k' returns totals in block of 1K = 1024 bytes, -P
				// uses the POSIX output format, ensures that line won't break
				Process process = Runtime.getRuntime().exec(
						new String[] { "df", "-P", "-k", absPath }, null, file);

				// Check that the process was correctly started
				if (process != null) {
					br = new BufferedReader(new InputStreamReader(
							process.getInputStream()));
					// Discard the first line
					// ("Filesystem   1K-blocks     Used    Avail Capacity  Mounted on");
					br.readLine();
					String line = br.readLine();

					// Sample lines:
					// /dev/disk0s2 116538416 109846712 6179704 95% /
					// automount -fstab [202] 0 0 0 100% /automount/Servers
					// /dev/disk2s2 2520 1548 972 61% /Volumes/muCommander 0.8

					// We're interested in the '1K-blocks' and 'Avail' fields
					// (only).
					// The 'Filesystem' and 'Mounted On' fields can contain
					// spaces (e.g. 'automount -fstab [202]' and
					// '/Volumes/muCommander 0.8' resp.) and therefore be made
					// of several tokens. A stable way to
					// determine the position of the fields we're interested in
					// is to look for the last token that
					// starts with a '/' character which should necessarily
					// correspond to the first token of the
					// 'Mounted on' field. The '1K-blocks' and 'Avail' fields
					// are 4 and 2 tokens away from it
					// respectively.

					// Start by tokenizing the whole line
					Vector<String> tokenV = new Vector<String>();
					if (line != null) {
						StringTokenizer st = new StringTokenizer(line);
						while (st.hasMoreTokens())
							tokenV.add(st.nextToken());
					}

					int nbTokens = tokenV.size();
					if (nbTokens < 6) {
						// This shouldn't normally happen
						FileLogger.warning("Failed to parse output of df -k "
								+ absPath + " line=" + line);
						return dfInfo;
					}

					// Find the last token starting with '/'
					int pos = nbTokens - 1;
					while (!tokenV.elementAt(pos).startsWith("/")) {
						if (pos == 0) {
							// This shouldn't normally happen
							FileLogger
									.warning("Failed to parse output of df -k "
											+ absPath + " line=" + line);
							return dfInfo;
						}

						--pos;
					}

					// '1-blocks' field (total space)
					dfInfo[0] = Long.parseLong(tokenV.elementAt(pos - 4)) * 1024;
					// 'Avail' field (free space)
					dfInfo[1] = Long.parseLong(tokenV.elementAt(pos - 2)) * 1024;
				}

				// // Retrieves the total and free space information using the
				// POSIX statvfs function
				// POSIX.STATVFSSTRUCT struct = new POSIX.STATVFSSTRUCT();
				// if(POSIX.INSTANCE.statvfs(absPath, struct)==0) {
				// dfInfo[0] = struct.f_blocks * (long)struct.f_frsize;
				// dfInfo[1] = struct.f_bfree * (long)struct.f_frsize;
				// }
			}
		} finally {
			if (br != null)
				try {
					br.close();
				} catch (IOException e) {
				}
		}

		return dfInfo;
	}

	/**
	 * Attemps to detect if this file is the root of a removable media drive
	 * (floppy, CD, DVD, USB drive...). This method produces accurate results
	 * only under Windows.
	 * 
	 * @return <code>true</code> if this file is the root of a removable media
	 *         drive (floppy, CD, DVD, USB drive...).
	 */
	public boolean guessRemovableDrive() {

		// For other OS that have root drives (OS/2), a weak way to characterize
		// removable drives is by checking if the
		// corresponding root folder is read-only.
		return hasRootDrives() && isRoot() && !file.canWrite();
	}

	/**
	 * Returns <code>true</code> if the underlying local filesystem uses drives
	 * assigned to letters (e.g. A:\, C:\, ...) instead of having a single root
	 * folder '/' under which mount points are attached. This is
	 * <code>true</code> for the following platforms:
	 * <ul>
	 * <li>Windows</li>
	 * <li>OS/2</li>
	 * <li>Any other platform that has '\' for a path separator</li>
	 * </ul>
	 * 
	 * @return <code>true</code> if the underlying local filesystem uses drives
	 *         assigned to letters
	 */
	public static boolean hasRootDrives() {
		return OsFamilies.OS_2.isCurrent() || "\\".equals(SEPARATOR);
	}

	/**
	 * Resolves and returns all local volumes:
	 * <ul>
	 * <li>On UNIX-based OSes, these are the mount points declared in
	 * <code>/etc/ftab</code>.</li>
	 * <li>On the Windows platform, these are the drives displayed in Explorer.
	 * Some of the returned volumes may correspond to removable drives and thus
	 * may not always be available -- if they aren't, {@link #exists()} will
	 * return <code>false</code>.</li>
	 * </ul>
	 * <p>
	 * The return list of volumes is purposively not cached so that new volumes
	 * will be returned as soon as they are mounted.
	 * </p>
	 * 
	 * @return all local volumes
	 */
	public static AbstractFile[] getVolumes() {
		Vector<AbstractFile> volumesV = new Vector<AbstractFile>();

		// Add Mac OS X's /Volumes subfolders and not file roots ('/') since
		// Volumes already contains a named link
		// (like 'Hard drive' or whatever silly name the user gave his primary
		// hard disk) to /
		if (OsFamilies.MAC_OS_X.isCurrent()) {
			addMacOSXVolumes(volumesV);
		} else {
			// Add java.io.File's root folders
			addJavaIoFileRoots(volumesV);

			// Add /proc/mounts folders under UNIX-based systems.
			if (OsFamily.getCurrent().isUnixBased())
				addMountEntries(volumesV);
		}

		// Add home folder, if it is not already present in the list
		AbstractFile homeFolder = getUserHome();
		if (!(homeFolder == null || volumesV.contains(homeFolder)))
			volumesV.add(homeFolder);

		AbstractFile volumes[] = new AbstractFile[volumesV.size()];
		volumesV.toArray(volumes);

		return volumes;
	}

	// //////////////////
	// Helper methods //
	// //////////////////

	/**
	 * Resolves the root folders returned by {@link File#listRoots()} and adds
	 * them to the given <code>Vector</code>.
	 * 
	 * @param v
	 *            the <code>Vector</code> to add root folders to
	 */
	private static void addJavaIoFileRoots(Vector<AbstractFile> v) {
		// Warning : No file operation should be performed on the resolved
		// folders as under Win32, this would cause a
		// dialog to appear for removable drives such as A:\ if no disk is
		// present.
		File fileRoots[] = File.listRoots();

		int nbFolders = fileRoots.length;
		for (int i = 0; i < nbFolders; i++)
			try {
				v.add(FileFactory.getFile(fileRoots[i].getAbsolutePath(), true));
			} catch (IOException e) {
			}
	}

	/**
	 * Parses <code>/proc/mounts</code> kernel virtual file, resolves all the
	 * mount points that look like regular filesystems it contains and adds them
	 * to the given <code>Vector</code>.
	 * 
	 * @param v
	 *            the <code>Vector</code> to add mount points to
	 */
	private static void addMountEntries(Vector<AbstractFile> v) {
		BufferedReader br;

		br = null;
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(
					"/proc/mounts")));
			StringTokenizer st;
			String line;
			AbstractFile file;
			String mountPoint, fsType;
			boolean knownFS;
			// read each line in file and parse it
			while ((line = br.readLine()) != null) {
				line = line.trim();
				// split line into tokens separated by " \t\n\r\f"
				// tokens are: device, mount_point, fs_type, attributes,
				// fs_freq, fs_passno
				st = new StringTokenizer(line);
				st.nextToken();
				mountPoint = st.nextToken().replace("\\040", " ");
				fsType = st.nextToken();
				knownFS = false;
				for (String fs : KNOWN_UNIX_FS) {
					if (fs.equals(fsType)) {
						// this is really known physical FS
						knownFS = true;
						break;
					}
				}

				if (knownFS) {
					file = FileFactory.getFile(mountPoint);
					if (file != null && !v.contains(file))
						v.add(file);
				}
			}
		} catch (Exception e) {
			FileLogger.warning("Error parsing /proc/mounts entries", e);
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
				}
			}
		}
	}

	/**
	 * Adds all <code>/Volumes</code> subfolders to the given
	 * <code>Vector</code>.
	 * 
	 * @param v
	 *            the <code>Vector</code> to add the volumes to
	 */
	private static void addMacOSXVolumes(Vector<AbstractFile> v) {
		// /Volumes not resolved for some reason, giving up
		AbstractFile volumesFolder = FileFactory.getFile("/Volumes");
		if (volumesFolder == null)
			return;

		// Adds subfolders
		try {
			AbstractFile volumesFiles[] = volumesFolder.ls();
			int nbFiles = volumesFiles.length;
			AbstractFile folder;
			for (int i = 0; i < nbFiles; i++)
				if ((folder = volumesFiles[i]).isDirectory()) {
					// The primary hard drive (the one corresponding to '/') is
					// listed under Volumes and should be
					// returned as the first volume
					if (folder.getCanonicalPath().equals("/"))
						v.insertElementAt(folder, 0);
					else
						v.add(folder);
				}
		} catch (IOException e) {
			FileLogger.warning("Can't get /Volumes subfolders", e);
		}
	}

	// ///////////////////////////////
	// AbstractFile implementation //
	// ///////////////////////////////

	/**
	 * Returns a <code>java.io.File</code> instance corresponding to this file.
	 */
	@Override
	public Object getUnderlyingFileObject() {
		return file;
	}

	@Override
	public boolean isSymlink() {

		// Note: this value must not be cached as its value can change over time
		// (canonical path can change)
		AbstractFile parent = getParent();
		String canonPath = getCanonicalPath(false);
		if (parent == null || canonPath == null)
			return false;
		else {
			String parentCanonPath = parent.getCanonicalPath(true);
			return !canonPath.equalsIgnoreCase(parentCanonPath + getName());
		}
	}

	@Override
	public long getDate() {
		return file.lastModified();
	}

	@Override
	public void changeDate(long lastModified) throws IOException {
		// java.io.File#setLastModified(long) throws an IllegalArgumentException
		// if time is negative.
		// If specified time is negative, set it to 0 (01/01/1970).
		if (lastModified < 0)
			lastModified = 0;

		if (!file.setLastModified(lastModified))
			throw new IOException();
	}

	@Override
	public long getSize() {
		return file.length();
	}

	@Override
	public AbstractFile getParent() {
		// Retrieve the parent AbstractFile instance and cache it
		if (!parentValueSet) {
			if (!isRoot()) {
				FileURL parentURL = getURL().getParent();
				if (parentURL != null) {
					parent = FileFactory.getFile(parentURL);
				}
			}
			parentValueSet = true;
		}
		return parent;
	}

	@Override
	public void setParent(AbstractFile parent) {
		this.parent = parent;
		this.parentValueSet = true;
	}

	@Override
	public boolean exists() {
		return file.exists();
	}

	@Override
	public FilePermissions getPermissions() {
		return permissions;
	}

	@Override
	public PermissionBits getChangeablePermissions() {
		return CHANGEABLE_PERMISSIONS;
	}

	@Override
	public void changePermission(int access, int permission, boolean enabled)
			throws IOException {

		boolean success = false;
		if (permission == READ_PERMISSION)
			success = file.setReadable(enabled);
		else if (permission == WRITE_PERMISSION)
			success = file.setWritable(enabled);
		else if (permission == EXECUTE_PERMISSION)
			success = file.setExecutable(enabled);

		if (!success)
			throw new IOException();
	}

	/**
	 * Always returns <code>null</code>, this information is not available
	 * unfortunately.
	 */
	@Override
	public String getOwner() {
		return null;
	}

	/**
	 * Always returns <code>false</code>, this information is not available
	 * unfortunately.
	 */
	@Override
	public boolean canGetOwner() {
		return false;
	}

	/**
	 * Always returns <code>null</code>, this information is not available
	 * unfortunately.
	 */
	@Override
	public String getGroup() {
		return null;
	}

	/**
	 * Always returns <code>false</code>, this information is not available
	 * unfortunately.
	 */
	@Override
	public boolean canGetGroup() {
		return false;
	}

	@Override
	public boolean isDirectory() {
		// This test is not necessary anymore now that 'No disk' error dialogs
		// are disabled entirely (using Kernel32
		// DLL's SetErrorMode function). Leaving this code commented for a while
		// in case the problem comes back.

		// // To avoid drive seeks and potential 'floppy drive not available'
		// dialog under Win32
		// // triggered by java.io.File.isDirectory()
		// if(IS_WINDOWS && guessFloppyDrive())
		// return true;

		return file.isDirectory();
	}

	/**
	 * Implementation notes: the returned <code>InputStream</code> uses a NIO
	 * {@link FileChannel} under the hood to benefit from
	 * <code>InterruptibleChannel</code> and allow a thread waiting for an I/O
	 * to be gracefully interrupted using <code>Thread#interrupt()</code>.
	 */
	@Override
	public InputStream getInputStream() throws IOException {
		return new LocalInputStream(new FileInputStream(file).getChannel());
	}

	/**
	 * Implementation notes: the returned <code>InputStream</code> uses a NIO
	 * {@link FileChannel} under the hood to benefit from
	 * <code>InterruptibleChannel</code> and allow a thread waiting for an I/O
	 * to be gracefully interrupted using <code>Thread#interrupt()</code>.
	 */
	@Override
	public OutputStream getOutputStream() throws IOException {
		return new LocalOutputStream(
				new FileOutputStream(absPath, false).getChannel());
	}

	/**
	 * Implementation notes: the returned <code>InputStream</code> uses a NIO
	 * {@link FileChannel} under the hood to benefit from
	 * <code>InterruptibleChannel</code> and allow a thread waiting for an I/O
	 * to be gracefully interrupted using <code>Thread#interrupt()</code>.
	 */
	@Override
	public OutputStream getAppendOutputStream() throws IOException {
		return new LocalOutputStream(
				new FileOutputStream(absPath, true).getChannel());
	}

	/**
	 * Implementation notes: the returned <code>InputStream</code> uses a NIO
	 * {@link FileChannel} under the hood to benefit from
	 * <code>InterruptibleChannel</code> and allow a thread waiting for an I/O
	 * to be gracefully interrupted using <code>Thread#interrupt()</code>.
	 */
	@Override
	public RandomAccessInputStream getRandomAccessInputStream()
			throws IOException {
		return new LocalRandomAccessInputStream(
				new RandomAccessFile(file, "r").getChannel());
	}

	/**
	 * Implementation notes: the returned <code>InputStream</code> uses a NIO
	 * {@link FileChannel} under the hood to benefit from
	 * <code>InterruptibleChannel</code> and allow a thread waiting for an I/O
	 * to be gracefully interrupted using <code>Thread#interrupt()</code>.
	 */
	@Override
	public RandomAccessOutputStream getRandomAccessOutputStream()
			throws IOException {
		return new LocalRandomAccessOutputStream(new RandomAccessFile(file,
				"rw").getChannel());
	}

	@Override
	public void delete() throws IOException {
		boolean ret = file.delete();

		if (!ret)
			throw new IOException();
	}

	@Override
	public AbstractFile[] ls() throws IOException {
		return ls((FilenameFilter) null);
	}

	@Override
	public void mkdir() throws IOException {
		if (!file.mkdir())
			throw new IOException();
	}

	@Override
	public void renameTo(AbstractFile destFile) throws IOException,
			UnsupportedFileOperationException {
		// Throw an exception if the file cannot be renamed to the specified
		// destination.
		// Fail in some situations where java.io.File#renameTo() doesn't.
		// Note that java.io.File#renameTo()'s implementation is
		// system-dependant, so it's always a good idea to
		// perform all those checks even if some are not necessary on this or
		// that platform.
		checkRenamePrerequisites(destFile, true, false);

		// The behavior of java.io.File#renameTo() when the destination file
		// already exists is not consistent
		// across platforms:
		// - Under UNIX, it succeeds and return true
		// - Under Windows, it fails and return false
		// This ticket goes in great details about the issue:
		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4017593
		//
		// => Since this method is required to succeed when the destination file
		// exists, the Windows platform needs
		// special treatment.

		destFile = destFile.getTopAncestor();
		File destJavaIoFile = ((LocalFile) destFile).file;

		if (!file.renameTo(destJavaIoFile))
			throw new IOException();
	}

	@Override
	public long getFreeSpace() throws IOException {
		return file.getUsableSpace();
	}

	@Override
	public long getTotalSpace() throws IOException {
		return file.getTotalSpace();
	}

	// Unsupported file operations

	/**
	 * Always throws {@link UnsupportedFileOperationException} when called.
	 * 
	 * @throws UnsupportedFileOperationException
	 *             , always
	 */
	@Override
	@UnsupportedFileOperation
	public void copyRemotelyTo(AbstractFile destFile)
			throws UnsupportedFileOperationException {
		throw new UnsupportedFileOperationException(FileOperation.COPY_REMOTELY);
	}

	// //////////////////////
	// Overridden methods //
	// //////////////////////

	@Override
	public String getName() {
		// If this file has no parent, return:
		// - the drive's name under OSes with root drives such as Windows, e.g.
		// "C:"
		// - "/" under Unix-based systems
		if (isRoot())
			return hasRootDrives() ? absPath : "/";

		return file.getName();
	}

	@Override
	public String getAbsolutePath() {
		// Append separator for root folders (C:\ , /) and for directories
		if (isRoot() || (isDirectory() && !absPath.endsWith(SEPARATOR)))
			return absPath + SEPARATOR;

		return absPath;
	}

	@Override
	public String getCanonicalPath() {
		// This test is not necessary anymore now that 'No disk' error dialogs
		// are disabled entirely (using Kernel32
		// DLL's SetErrorMode function). Leaving this code commented for a while
		// in case the problem comes back.

		// // To avoid drive seeks and potential 'floppy drive not available'
		// dialog under Win32
		// // triggered by java.io.File.getCanonicalPath()
		// if(IS_WINDOWS && guessFloppyDrive())
		// return absPath;

		// Note: canonical path must not be cached as its resolution can change
		// over time, for instance
		// if a file 'Test' is renamed to 'test' in the same folder, its
		// canonical path would still be 'Test'
		// if it was resolved prior to the renaming and thus be recognized as a
		// symbolic link
		try {
			String canonicalPath = file.getCanonicalPath();
			// Append separator for directories
			if (isDirectory() && !canonicalPath.endsWith(SEPARATOR))
				canonicalPath = canonicalPath + SEPARATOR;

			return canonicalPath;
		} catch (IOException e) {
			return absPath;
		}
	}

	@Override
	public String getSeparator() {
		return SEPARATOR;
	}

	@Override
	public AbstractFile[] ls(FilenameFilter filenameFilter) throws IOException {
		File files[] = file.listFiles(filenameFilter == null ? null
				: new LocalFilenameFilter(filenameFilter));

		if (files == null)
			throw new IOException();

		int nbFiles = files.length;
		AbstractFile children[] = new AbstractFile[nbFiles];
		FileURL childURL;

		for (int i = 0; i < nbFiles; i++) {

			// Clone the FileURL of this file and set the child's path, this is
			// more efficient than creating a new
			// FileURL instance from scratch.
			childURL = (FileURL) fileURL.clone();

			childURL.setPath(absPath + SEPARATOR + files[i].getName());

			// Retrieves an AbstractFile (LocalFile or AbstractArchiveFile)
			// instance that's potentially already in
			// the cache, reuse this file as the file's parent, and the
			// already-created java.io.File instance.
			children[i] = FileFactory.getFile(childURL, this, files[i]);
		}

		return children;
	}

	@Override
	public boolean isHidden() {
		return file.isHidden();
	}

	/**
	 * Overridden to return the local volum on which this file is located. The
	 * returned volume is one of the volumes returned by {@link #getVolumes()}.
	 */
	@Override
	public AbstractFile getVolume() {
		AbstractFile[] volumes = LocalFile.getVolumes();

		// Looks for the volume that best matches this file, i.e. the volume
		// that is the deepest parent of this file.
		// If this file is itself a volume, return it.
		int bestDepth = -1;
		int bestMatch = -1;
		int depth;
		AbstractFile volume;
		String volumePath;
		String thisPath = getAbsolutePath(true);

		for (int i = 0; i < volumes.length; i++) {
			volume = volumes[i];
			volumePath = volume.getAbsolutePath(true);

			if (thisPath.equals(volumePath)) {
				return this;
			} else if (thisPath.startsWith(volumePath)) {
				depth = PathUtils.getDepth(volumePath, volume.getSeparator());
				if (depth > bestDepth) {
					bestDepth = depth;
					bestMatch = i;
				}
			}
		}

		if (bestMatch != -1)
			return volumes[bestMatch];

		// If no volume matched this file (shouldn't normally happen), return
		// the root folder
		return getRoot();
	}

	// /////////////////
	// Inner classes //
	// /////////////////

	/**
	 * LocalRandomAccessInputStream extends RandomAccessInputStream to provide
	 * random read access to a LocalFile. This implementation uses a NIO
	 * <code>FileChannel</code> under the hood to benefit from
	 * <code>InterruptibleChannel</code> and allow a thread waiting for an I/O
	 * to be gracefully interrupted using <code>Thread#interrupt()</code>.
	 */
	public static class LocalRandomAccessInputStream extends
			RandomAccessInputStream {

		private final FileChannel channel;
		private final ByteBuffer bb;

		private LocalRandomAccessInputStream(FileChannel channel) {
			this.channel = channel;
			this.bb = BufferPool.getByteBuffer();
		}

		@Override
		public int read() throws IOException {
			synchronized (bb) {
				bb.position(0);
				bb.limit(1);

				int nbRead = channel.read(bb);
				if (nbRead <= 0)
					return nbRead;

				return 0xFF & bb.get(0);
			}
		}

		@Override
		public int read(byte b[], int off, int len) throws IOException {
			synchronized (bb) {
				bb.position(0);
				bb.limit(Math.min(bb.capacity(), len));

				int nbRead = channel.read(bb);
				if (nbRead <= 0)
					return nbRead;

				bb.position(0);
				bb.get(b, off, nbRead);

				return nbRead;
			}
		}

		@Override
		public void close() throws IOException {
			BufferPool.releaseByteBuffer(bb);
			channel.close();
		}

		public long getOffset() throws IOException {
			return channel.position();
		}

		public long getLength() throws IOException {
			return channel.size();
		}

		public void seek(long offset) throws IOException {
			channel.position(offset);
		}
	}

	/**
	 * A replacement for <code>java.io.FileInputStream</code> that uses a NIO
	 * {@link FileChannel} under the hood to benefit from
	 * <code>InterruptibleChannel</code> and allow a thread waiting for an I/O
	 * to be gracefully interrupted using <code>Thread#interrupt()</code>.
	 * 
	 * <p>
	 * This class simply delegates all its methods to a
	 * {@link com.mucommander.file.impl.local.LocalFile.LocalRandomAccessInputStream}
	 * instance. Therefore, this class does not derive from
	 * {@link com.mucommander.io.RandomAccessInputStream}, preventing
	 * random-access methods from being used.
	 * </p>
	 * 
	 */
	public static class LocalInputStream extends FilterInputStream {

		public LocalInputStream(FileChannel channel) {
			super(new LocalRandomAccessInputStream(channel));
		}
	}

	/**
	 * A replacement for <code>java.io.FileOutputStream</code> that uses a NIO
	 * {@link FileChannel} under the hood to benefit from
	 * <code>InterruptibleChannel</code> and allow a thread waiting for an I/O
	 * to be gracefully interrupted using <code>Thread#interrupt()</code>.
	 * 
	 * <p>
	 * This class simply delegates all its methods to a
	 * {@link com.mucommander.file.impl.local.LocalFile.LocalRandomAccessOutputStream}
	 * instance. Therefore, this class does not derive from
	 * {@link com.mucommander.io.RandomAccessOutputStream}, preventing
	 * random-access methods from being used.
	 * </p>
	 * 
	 */
	public static class LocalOutputStream extends FilteredOutputStream {

		public LocalOutputStream(FileChannel channel) {
			super(new LocalRandomAccessOutputStream(channel));
		}
	}

	/**
	 * LocalRandomAccessOutputStream extends RandomAccessOutputStream to provide
	 * random write access to a LocalFile. This implementation uses a NIO
	 * <code>FileChannel</code> under the hood to benefit from
	 * <code>InterruptibleChannel</code> and allow a thread waiting for an I/O
	 * to be gracefully interrupted using <code>Thread#interrupt()</code>.
	 */
	public static class LocalRandomAccessOutputStream extends
			RandomAccessOutputStream {

		private final FileChannel channel;
		private final ByteBuffer bb;

		public LocalRandomAccessOutputStream(FileChannel channel) {
			this.channel = channel;
			this.bb = BufferPool.getByteBuffer();
		}

		@Override
		public void write(int i) throws IOException {
			synchronized (bb) {
				bb.position(0);
				bb.limit(1);

				bb.put((byte) i);
				bb.position(0);

				channel.write(bb);
			}
		}

		@Override
		public void write(byte b[]) throws IOException {
			write(b, 0, b.length);
		}

		@Override
		public void write(byte b[], int off, int len) throws IOException {
			int nbToWrite;
			synchronized (bb) {
				do {
					bb.position(0);
					nbToWrite = Math.min(bb.capacity(), len);
					bb.limit(nbToWrite);

					bb.put(b, off, nbToWrite);
					bb.position(0);

					nbToWrite = channel.write(bb);

					len -= nbToWrite;
					off += nbToWrite;
				} while (len > 0);
			}
		}

		@Override
		public void setLength(long newLength) throws IOException {
			long currentLength = getLength();

			if (newLength == currentLength)
				return;

			long currentPos = channel.position();

			if (newLength < currentLength) {
				// Truncate the file and position the offset to the new EOF if
				// it was beyond
				channel.truncate(newLength);
				if (currentPos > newLength)
					channel.position(newLength);
			} else {
				// Expand the file by positionning the offset at the new EOF and
				// writing a byte, and reposition the
				// offset to where it was
				channel.position(newLength - 1); // Note: newLength cannot be 0
				write(0);
				channel.position(currentPos);
			}

		}

		@Override
		public void close() throws IOException {
			BufferPool.releaseByteBuffer(bb);
			channel.close();
		}

		public long getOffset() throws IOException {
			return channel.position();
		}

		public long getLength() throws IOException {
			return channel.size();
		}

		public void seek(long offset) throws IOException {
			channel.position(offset);
		}
	}

	/**
	 * A Permissions implementation for LocalFile.
	 */
	private static class LocalFilePermissions extends IndividualPermissionBits
			implements FilePermissions {

		private java.io.File file;

		// Permissions are limited to the user access type. Executable
		// permission flag is only available under Java 1.6
		// and up.
		// Note: 'read' and 'execute' permissions have no meaning under Windows
		// (files are either read-only or
		// read-write), but we return default values.

		/** Mask for supported permissions under Java 1.6 */
		private static PermissionBits JAVA_1_6_PERMISSIONS = new GroupedPermissionBits(
				448); // rwx------ (700 octal)

		private final static PermissionBits MASK = JAVA_1_6_PERMISSIONS;

		private LocalFilePermissions(java.io.File file) {
			this.file = file;
		}

		public boolean getBitValue(int access, int type) {
			// Only the 'user' permissions are supported
			if (access != USER_ACCESS)
				return false;

			if (type == READ_PERMISSION)
				return file.canRead();
			else if (type == WRITE_PERMISSION)
				return file.canWrite();
			else if (type == EXECUTE_PERMISSION)
				return file.canExecute();

			return false;
		}

		/**
		 * Overridden for peformance reasons.
		 */
		@Override
		public int getIntValue() {
			int userPerms = 0;

			if (getBitValue(USER_ACCESS, READ_PERMISSION))
				userPerms |= READ_PERMISSION;

			if (getBitValue(USER_ACCESS, WRITE_PERMISSION))
				userPerms |= WRITE_PERMISSION;

			if (getBitValue(USER_ACCESS, EXECUTE_PERMISSION))
				userPerms |= EXECUTE_PERMISSION;

			return userPerms << 6;
		}

		public PermissionBits getMask() {
			return MASK;
		}
	}

	/**
	 * Turns a {@link FilenameFilter} into a {@link java.io.FilenameFilter}.
	 */
	private static class LocalFilenameFilter implements java.io.FilenameFilter {

		private FilenameFilter filter;

		private LocalFilenameFilter(FilenameFilter filter) {
			this.filter = filter;
		}

		// /////////////////////////////////////////
		// java.io.FilenameFilter implementation //
		// /////////////////////////////////////////

		public boolean accept(File dir, String name) {
			return filter.accept(name);
		}
	}
}
