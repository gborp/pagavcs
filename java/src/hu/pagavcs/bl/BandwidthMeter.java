package hu.pagavcs.bl;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.tmatesoft.svn.util.SVNDebugLogAdapter;
import org.tmatesoft.svn.util.SVNLogType;

public class BandwidthMeter extends SVNDebugLogAdapter {

	private int bandwidthLastSecond;
	private int bandwidthActual;

	public BandwidthMeter() {
		ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
		executor.scheduleAtFixedRate(new Runnable() {

			public void run() {
				bandwidthLastSecond = bandwidthActual;
				bandwidthActual = 0;
			}

		}, 0, 1, TimeUnit.SECONDS);
	}

	public void log(SVNLogType logType, Throwable th, Level logLevel) {}

	public void log(SVNLogType logType, String message, Level logLevel) {}

	public void log(SVNLogType logType, String message, byte[] data) {
		if (data != null) {
			bandwidthActual += data.length;
		}
	}

	public int getBandwidth() {
		return bandwidthLastSecond;
	}

}
