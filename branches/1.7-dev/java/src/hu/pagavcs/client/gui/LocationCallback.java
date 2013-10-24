package hu.pagavcs.client.gui;

public interface LocationCallback {

	/**
	 * 
	 * @return TRUE: file path; FALSE: repository location
	 */
	boolean isFilePath();

	String getLocation();
}
