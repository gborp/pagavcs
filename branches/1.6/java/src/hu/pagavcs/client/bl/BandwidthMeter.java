package hu.pagavcs.client.bl;

import java.util.logging.Level;

import org.tmatesoft.svn.util.SVNDebugLogAdapter;
import org.tmatesoft.svn.util.SVNLogType;

public class BandwidthMeter extends SVNDebugLogAdapter {

	private int transmittedSinceLastRead;

	public BandwidthMeter() {}

	public void log(SVNLogType logType, Throwable th, Level logLevel) {}

	public void log(SVNLogType logType, String message, Level logLevel) {}

	public void log(SVNLogType logType, String message, byte[] data) {
		if (data != null) {
			transmittedSinceLastRead += data.length;
		}
	}

	public int getBandwidth() {
		int result = transmittedSinceLastRead;
		transmittedSinceLastRead = 0;
		return result;
	}

}
