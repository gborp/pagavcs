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

package com.mucommander.ui.action.impl;

import com.mucommander.file.AbstractFile;
import com.mucommander.ui.action.*;
import com.mucommander.ui.main.FolderPanel;
import com.mucommander.ui.main.MainFrame;

import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;
import java.util.Hashtable;

/**
 * This action changes the current folder of the currently active FolderPanel to the current folder's parent.
 * This action only gets enabled when the current folder has a parent.
 *
 * @author Maxence Bernard, Nicolas Rinaudo
 */
public class GoToParentAction extends ParentFolderAction {
    /**
     * Creates a new <code>GoToParentAction</code> with the specified parameters.
     * @param mainFrame  frame to which the action is attached.
     * @param properties action's properties.
     */
    public GoToParentAction(MainFrame mainFrame, Hashtable<String,Object> properties) {
        super(mainFrame, properties);
    }


    /**
     * Enables or disables this action based on the currently active folder's
     * has a parent, this action will be enabled, if not it will be disabled.
     */
    @Override
    protected void toggleEnabledState() {
        setEnabled(mainFrame.getActivePanel().getCurrentFolder().getParent()!=null);
    }



    ///////////////////////
    // Protected methods //
    ///////////////////////

    /**
     * Goes to <code>sourcePanel</code>'s parent in <code>destPanel</code>.
     * <p>
     * If <code>sourcePanel</code> doesn't have a parent, nothing will happen.
     * </p>
     * @param  sourcePanel panel whose parent should be used.
     * @param  destPanel   panel in which to change the location.
     * @return             <code>true</code> if <code>sourcePanel</code> has a parent, <code>false</code> otherwise.
     */
    protected boolean goToParent(FolderPanel sourcePanel, FolderPanel destPanel) {
        AbstractFile parent;

        if((parent = sourcePanel.getCurrentFolder().getParent()) != null) {
            destPanel.tryChangeCurrentFolder(parent, null, true);
            return true;
        }
        return false;
    }

    /**
     * Updates <code>panel</code>'s location to its parent.
     * <p>
     * This is a convenience method and is strictly equivalent to calling
     * <code>{@link #goToParent(FolderPanel,FolderPanel) goToParent(}panel, panel)</code>
     * </p>
     * @param  panel in which to change the location.
     * @return       <code>true</code> if <code>panel</code> has a parent, <code>false</code> otherwise.
     */
    protected boolean goToParent(FolderPanel panel) {
        return goToParent(panel, panel);
    }



    /////////////////////////////
    // MuAction implementation //
    /////////////////////////////
    /**
     * Goes to the current location's parent in the active panel.
     */
    @Override
    public void performAction() {
        // Changes the current folder to make it the current folder's parent.
        // Does nothing if the current folder doesn't have a parent.
        goToParent(mainFrame.getActivePanel());
    }
    
    public static class Factory implements ActionFactory {

		public MuAction createAction(MainFrame mainFrame, Hashtable<String,Object> properties) {
			return new GoToParentAction(mainFrame, properties);
		}
    }
    
    public static class Descriptor extends AbstractActionDescriptor {
    	public static final String ACTION_ID = "GoToParent";
    	
		public String getId() { return ACTION_ID; }

		public ActionCategory getCategory() { return ActionCategories.NAVIGATION; }

		public KeyStroke getDefaultAltKeyStroke() { return KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0); }

		public KeyStroke getDefaultKeyStroke() { return KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0); }
    }
}
