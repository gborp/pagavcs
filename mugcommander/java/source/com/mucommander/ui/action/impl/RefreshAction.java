/*
 * This file is part of muCommander, http://www.mucommander.com Copyright (C)
 * 2002-2010 Maxence Bernard muCommander is free software; you can redistribute
 * it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 3 of the License,
 * or (at your option) any later version. muCommander is distributed in the hope
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details. You should have received a copy of
 * the GNU General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 */

package com.mucommander.ui.action.impl;

import java.awt.event.KeyEvent;
import java.util.Hashtable;

import javax.swing.KeyStroke;

import com.mucommander.ui.action.AbstractActionDescriptor;
import com.mucommander.ui.action.ActionCategories;
import com.mucommander.ui.action.ActionCategory;
import com.mucommander.ui.action.ActionFactory;
import com.mucommander.ui.action.MuAction;
import com.mucommander.ui.main.MainFrame;

/**
 * This action refreshes the currently active FolderPanel (refreshes the content
 * of the folder).
 * 
 * @author Maxence Bernard
 */
public class RefreshAction extends MuAction {

	public RefreshAction(MainFrame mainFrame, Hashtable<String, Object> properties) {
		super(mainFrame, properties);
	}

	@Override
	public void performAction() {
		// Refresh current folder in a separate thread
		mainFrame.getActivePanel().tryRefreshCurrentFolder();
	}

	public static class Factory implements ActionFactory {

		public MuAction createAction(MainFrame mainFrame, Hashtable<String, Object> properties) {
			return new RefreshAction(mainFrame, properties);
		}
	}

	public static class Descriptor extends AbstractActionDescriptor {

		public static final String ACTION_ID = "Refresh";

		public String getId() {
			return ACTION_ID;
		}

		public ActionCategory getCategory() {
			return ActionCategories.VIEW;
		}

		public KeyStroke getDefaultAltKeyStroke() {
			return null;
		}

		public KeyStroke getDefaultKeyStroke() {
			return KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.CTRL_DOWN_MASK);
		}
	}
}
