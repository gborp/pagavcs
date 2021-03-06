package hu.pagavcs.client.operation;

import hu.pagavcs.client.bl.Manager;
import hu.pagavcs.client.gui.properties.PropertiesGui;
import hu.pagavcs.client.gui.properties.PropertiesListItem;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.ISVNPropertyHandler;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNPropertyData;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNWCClient;

/**
 * PagaVCS is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.<br>
 * <br>
 * PagaVCS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.<br>
 * <br>
 * You should have received a copy of the GNU General Public License along with
 * PagaVCS; If not, see http://www.gnu.org/licenses/.
 */
public class PropertiesOperation {

	private PropertiesGui gui;
	private File file;
	private List<PropertiesListItem> lstCurrentProperties;
	private List<PropertiesListItem> lstModifiedProperties;
	private List<PropertiesListItem> lstDeletedProperties;

	public PropertiesOperation(String fileName) throws Exception {
		file = new File(fileName);
	}

	public void execute() throws Exception {
		gui = new PropertiesGui(this);
		gui.display();

		gui.setWorkingCopy(file.getPath());
		gui.setRepo(Manager.getInfo(file).getURL().toDecodedString());

		lstCurrentProperties = getProperties();
		lstModifiedProperties = new ArrayList<PropertiesListItem>();
		lstDeletedProperties = new ArrayList<PropertiesListItem>();
		gui.setProperties(getProperties());
	}

	private List<PropertiesListItem> getProperties() throws SVNException {
		SVNClientManager mgrSvn = Manager
				.getSVNClientManagerForWorkingCopyOnly();
		try {
			SVNWCClient wcClient = mgrSvn.getWCClient();

			File dir = file.getAbsoluteFile();

			final List<SVNPropertyData> lstSvnProperties = new ArrayList<SVNPropertyData>();
			wcClient.doGetProperty(dir, null, SVNRevision.WORKING,
					SVNRevision.WORKING, SVNDepth.EMPTY,
					new ISVNPropertyHandler() {

						public void handleProperty(File path,
								SVNPropertyData property) throws SVNException {
							lstSvnProperties.add(property);
						}

						public void handleProperty(SVNURL url,
								SVNPropertyData property) throws SVNException {
						}

						public void handleProperty(long revision,
								SVNPropertyData property) throws SVNException {
						}

					}, null);

			List<PropertiesListItem> lstProperties = new ArrayList<PropertiesListItem>();
			for (SVNPropertyData svnPropertyData : lstSvnProperties) {
				PropertiesListItem property = new PropertiesListItem();

				if (svnPropertyData.getValue().isString()) {
					property.setKey(svnPropertyData.getName());
					property.setValue(svnPropertyData.getValue().getString());
					property.setDisplayValue(valueToDisplayValue(property
							.getValue()));
					lstProperties.add(property);
				} else if (svnPropertyData.getValue().getBytes().length == 0) {
					property.setKey(svnPropertyData.getName());
					property.setValue("");
					property.setDisplayValue("");
					lstProperties.add(property);
				} else {
					String value = null;
					try {
						try {
							value = new String(svnPropertyData.getValue()
									.getBytes(), "UTF-8");
						} catch (UnsupportedEncodingException e) {
							value = new String(svnPropertyData.getValue()
									.getBytes());
						}
					} catch (Throwable t) {
						// ignoring
					}
					if (value != null) {
						property.setKey(svnPropertyData.getName());
						property.setValue(value);
						property.setDisplayValue(valueToDisplayValue(value));
						lstProperties.add(property);
					}
				}
			}

			return lstProperties;
		} finally {
			mgrSvn.dispose();
		}
	}

	public String valueToDisplayValue(String value) {
		value = value.replace("\t", " ");
		value = value.replace("\n", " ");
		return value;
	}

	public void removeProperty(PropertiesListItem deleteLi) {
		for (PropertiesListItem li : lstModifiedProperties) {
			if (deleteLi.getKey().equals(li.getKey())) {
				lstModifiedProperties.remove(li);
				break;
			}
		}
		for (PropertiesListItem li : lstDeletedProperties) {
			if (deleteLi.getKey().equals(li.getKey())) {
				lstDeletedProperties.remove(li);
				break;
			}
		}
		lstDeletedProperties.add(deleteLi);

	}

	public boolean needsValueValidation(String key) {
		return key != null && key.startsWith("svn:");
	}

	public String validateProperty(String value) {
		if (value.isEmpty()) {
			value = "\n";
		} else {
			if (!value.endsWith("\n")) {
				value = value + "\n";
			}
		}
		return value;
	}

	public void modifyProperty(PropertiesListItem modifyLi) {
		if (modifyLi.getKey() == null) {
			return;
		}

		for (PropertiesListItem li : lstModifiedProperties) {
			if (modifyLi.getKey().equals(li.getKey())) {
				lstModifiedProperties.remove(li);
				break;
			}
		}
		for (PropertiesListItem li : lstDeletedProperties) {
			if (modifyLi.getKey().equals(li.getKey())) {
				lstDeletedProperties.remove(li);
				break;
			}
		}
		lstModifiedProperties.add(modifyLi);
	}

	public void commitChanges() throws SVNException {
		SVNClientManager mgrSvn = Manager
				.getSVNClientManagerForWorkingCopyOnly();
		try {
			SVNWCClient wcClient = mgrSvn.getWCClient();

			File dir = file.getAbsoluteFile();

			for (PropertiesListItem li : lstModifiedProperties) {
				wcClient.doSetProperty(dir, li.getKey(), SVNPropertyValue
						.create(li.getValue()), false,
						li.isRecoursively() ? SVNDepth.INFINITY
								: SVNDepth.EMPTY, null, null);
			}
			for (PropertiesListItem li : lstDeletedProperties) {
				wcClient.doSetProperty(dir, li.getKey(), SVNPropertyValue
						.create(li.getValue()), false,
						li.isRecoursively() ? SVNDepth.INFINITY
								: SVNDepth.EMPTY, null, null);
			}
			Manager.invalidateAllFiles();
		} finally {
			mgrSvn.dispose();
		}
	}

	public List<PropertiesListItem> getOriginalProperties() {
		return lstCurrentProperties;
	}

	public List<PropertiesListItem> getDeletedProperties() {
		return lstDeletedProperties;
	}

	public List<PropertiesListItem> getModifiedProperties() {
		return lstModifiedProperties;
	}

}
