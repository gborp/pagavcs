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
import com.mucommander.ui.main.MainFrame;

import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;
import java.util.Hashtable;

/**
 * Changes the current directory to its parent and tries to do the same in the inactive panel.
 * <p>
 * When possible, this action will open the active panel's current folder's parent. Additionally,
 * if the inactive panel's current folder has a parent, it will open that one as well.
 * </p>
 * <p>
 * Note that this action's behaviour is strictly equivalent to that of {@link GoToParentAction} in the
 * active panel. Differences will only occur in the inactive panel, and then again only when possible.
 * </p>
 * <p>
 * This action opens both files synchronously: it will wait for the active panel location change confirmation
 * before performing the inactive one.
 * </p>
 * @author Nicolas Rinaudo
 */
public class GoToParentInBothPanelsAction extends GoToParentAction {
    // - Initialization ------------------------------------------------------------------
    // -----------------------------------------------------------------------------------
    /**
     * Creates a new <code>GoToParentInBothPanelsAction</code> instance with the specified parameters.
     * @param mainFrame  frame to which the action is attached.
     * @param properties action's properties.
     */
    public GoToParentInBothPanelsAction(MainFrame mainFrame, Hashtable<String,Object> properties) {
        super(mainFrame, properties);

        // Perform this action in a separate thread, to avoid locking the event thread
        setPerformActionInSeparateThread(true);
    }



    // - Action code ---------------------------------------------------------------------
    // -----------------------------------------------------------------------------------
    /**
     * Opens both the active and inactive folder panel's parent directories.
     */
    @Override
    public void performAction() {
        Thread       openThread;
        AbstractFile parent;

        // If the current panel has a parent file, navigate to it.
        if((parent = mainFrame.getActivePanel().getCurrentFolder().getParent()) != null) {
            openThread = mainFrame.getActivePanel().tryChangeCurrentFolder(parent);

            // If the inactive panel has a parent file, wait for the current panel change to be complete and navigate
            // to it.
            if((parent = mainFrame.getInactivePanel().getCurrentFolder().getParent()) != null) {
                if(openThread != null) {
                    while(openThread.isAlive()) {
                        try {openThread.join();}
                        catch(InterruptedException e) {}
                    }
                }
                mainFrame.getInactivePanel().tryChangeCurrentFolder(parent);
            }
        }
    }
    
    public static class Factory implements ActionFactory {

		public MuAction createAction(MainFrame mainFrame, Hashtable<String,Object> properties) {
			return new GoToParentInBothPanelsAction(mainFrame, properties);
		}
    }
    
    public static class Descriptor extends AbstractActionDescriptor {
    	public static final String ACTION_ID = "GoToParentInBothPanels";
    	
		public String getId() { return ACTION_ID; }

		public ActionCategory getCategory() { return ActionCategories.NAVIGATION; }

		public KeyStroke getDefaultAltKeyStroke() { return null; }

		public KeyStroke getDefaultKeyStroke() { return KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, KeyEvent.SHIFT_DOWN_MASK | KeyEvent.CTRL_DOWN_MASK); }
    }
}
