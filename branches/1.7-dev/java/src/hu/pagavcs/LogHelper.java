package hu.pagavcs;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class LogHelper {

	public static Logger GENERAL = Logger.getLogger("com.braids.pagavcs.general");

	public static void init() {
		BasicConfigurator.configure();
		setDebugMode(false);
	}

	public static void setDebugMode(boolean debug) {

		GENERAL.setLevel(Level.INFO);
		GENERAL.info("Debug mode: " + debug);

		if (debug) {
			GENERAL.setLevel(Level.TRACE);
		} else {
			GENERAL.setLevel(Level.WARN);
		}
	}
}
