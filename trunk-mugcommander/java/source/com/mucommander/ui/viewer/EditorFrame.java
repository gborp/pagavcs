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


package com.mucommander.ui.viewer;

import com.mucommander.AppLogger;
import com.mucommander.file.AbstractFile;
import com.mucommander.file.FileFactory;
import com.mucommander.file.FileProtocols;
import com.mucommander.job.FileCollisionChecker;
import com.mucommander.runtime.OsFamilies;
import com.mucommander.text.Translator;
import com.mucommander.ui.dialog.DialogToolkit;
import com.mucommander.ui.dialog.InformationDialog;
import com.mucommander.ui.dialog.QuestionDialog;
import com.mucommander.ui.dialog.file.FileCollisionDialog;
import com.mucommander.ui.helper.FocusRequester;
import com.mucommander.ui.helper.MenuToolkit;
import com.mucommander.ui.helper.MnemonicHelper;
import com.mucommander.ui.layout.AsyncPanel;
import com.mucommander.ui.main.MainFrame;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.IOException;


/**
 * A specialized <code>JFrame</code> that displays a {@link FileEditor} for a given file and provides some common
 * editing functionalities. The {@link FileEditor} instance is provided by {@link EditorRegistrar}.
 *
 * @author Maxence Bernard
 */
public class EditorFrame extends JFrame implements ActionListener {

    private JMenuItem saveItem;
    private JMenuItem saveAsItem;
    private JMenuItem closeItem;
	
    private MainFrame mainFrame;
    private AbstractFile file;
    private FileEditor editor;
	
    /** Serves to indicate if saving is needed before closing the window, value should only be modified using the setSaveNeeded() method */
    private boolean saveNeeded;
		
    private final static Dimension MIN_DIMENSION = new Dimension(480, 360);

    private final static int YES_ACTION = 0;
    private final static int NO_ACTION = 1;
    private final static int CANCEL_ACTION = 2;

    private final static String CUSTOM_DISPOSE_EVENT = "CUSTOM_DISPOSE_EVENT";

	
    /**
     * Creates a new EditorFrame to start viewing the given file.
     *
     * <p>This constructor has package access only, EditorFrame can to be created by
     * {@link EditorRegistrar#createEditorFrame(MainFrame,AbstractFile,Image)}.
     */
    EditorFrame(MainFrame mainFrame, AbstractFile file, Image icon) {
        super();

        setIconImage(icon);
        this.mainFrame = mainFrame;
        this.file = file;
		
        // Call #dispose() on close (default is hide)
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        setResizable(true);

        initContentPane();
    }

    /**
     * Creates a minimalist menu bar that allows to close the frame, and returns it.
     *
     * @return a minimalist menu bar that allows to close the frame
     */
    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        MnemonicHelper menuMnemonicHelper = new MnemonicHelper();
        MnemonicHelper menuItemMnemonicHelper = new MnemonicHelper();

        // File menu
        JMenu menu = MenuToolkit.addMenu(Translator.get("file_editor.file_menu"), menuMnemonicHelper, null);
        saveItem = MenuToolkit.addMenuItem(menu, Translator.get("file_editor.save"), menuItemMnemonicHelper, KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_MASK), this);
        saveAsItem = MenuToolkit.addMenuItem(menu, Translator.get("file_editor.save_as"), menuItemMnemonicHelper, null, this);
        menu.add(new JSeparator());
        closeItem = MenuToolkit.addMenuItem(menu, Translator.get("file_editor.close"), menuItemMnemonicHelper, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), this);
		menuBar.add(menu);

        return menuBar;
    }

    private void initContentPane() {
        AsyncPanel asyncPanel = new AsyncPanel() {
            @Override
            public JComponent getTargetComponent() {
                try {
                    editor = EditorRegistrar.createFileEditor(file);
                    if(editor==null)
                        throw new Exception("No suitable editor found");

                    // Set the editor's fields
                    editor.setFrame(EditorFrame.this);
                    JMenuBar menuBar = createMenuBar();
                    editor.setMenuBar(menuBar);
                    editor.setCurrentFile(file);

                    // Ask the editor to edit the file
                    editor.edit(file);

                    // Set the menu bar, only when it has been fully populated (see ticket #243)
                    EditorFrame.this.setJMenuBar(menuBar);
                }
                catch(Exception e) {
                    AppLogger.fine("Exception caught", e);

                    // May be a UserCancelledException if the user cancelled (refused to confirm the operation after a warning)
                    if(!(e instanceof UserCancelledException))
                        showGenericEditErrorDialog();

                    dispose();
                    return editor==null?new JPanel():editor;
                }

                setTitle(editor.getTitle());

                JScrollPane scrollPane = new JScrollPane(editor, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED) {
                        @Override
                        public Insets getInsets() {
                            return new Insets(0, 0, 0, 0);
                        }
                    };

                // Catch Apple+W keystrokes under Mac OS X to try and close the window
                if(OsFamilies.MAC_OS_X.isCurrent()) {
                    scrollPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_W, ActionEvent.META_MASK), CUSTOM_DISPOSE_EVENT);
                    scrollPane.getActionMap().put(CUSTOM_DISPOSE_EVENT, new AbstractAction() {
                            public void actionPerformed(ActionEvent e){
                                dispose();
                            }
                        });
                }

                return scrollPane;
            }

            @Override
            protected void updateLayout() {
                super.updateLayout();

                // Request focus on the viewer when it is visible
                FocusRequester.requestFocus(editor);
            }
        };

        // Add the AsyncPanel to the content pane
        JPanel contentPane = new JPanel(new BorderLayout());
        contentPane.add(asyncPanel, BorderLayout.CENTER);
        setContentPane(contentPane);

        // Sets panel to preferred size, without exceeding a maximum size and with a minumum size
        pack();
        setVisible(true);
    }

    public void showGenericEditErrorDialog() {
        InformationDialog.showErrorDialog(mainFrame, Translator.get("file_editor.edit_error_title"), Translator.get("file_editor.edit_error"));
    }


    public void setSaveNeeded(boolean saveNeeded) {
        if(this.saveNeeded!=saveNeeded) {
            this.saveNeeded = saveNeeded;
            // Marks/unmarks the window as dirty under Mac OS X (symbolized by a dot in the window closing icon)
            if(OsFamilies.MAC_OS_X.isCurrent())
                this.getRootPane().putClientProperty("windowModified", saveNeeded?Boolean.TRUE:Boolean.FALSE);
        }
		
    }

    public void trySaveAs() {
        JFileChooser fileChooser = new JFileChooser();
		
        // Sets selected file in JFileChooser to current file
        if(file.getURL().getScheme().equals(FileProtocols.FILE))
            fileChooser.setSelectedFile(new java.io.File(file.getAbsolutePath()));
        fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
        int ret = fileChooser.showSaveDialog(this);
		
        if (ret==JFileChooser.APPROVE_OPTION) {
            AbstractFile destFile;
            try {
                destFile = FileFactory.getFile(fileChooser.getSelectedFile().getAbsolutePath(), true);
            }
            catch(IOException e) {
                InformationDialog.showErrorDialog(this, Translator.get("write_error"), Translator.get("file_editor.cannot_write"));
                return;
            }

            // Check for file collisions, i.e. if the file already exists in the destination
            int collision = FileCollisionChecker.checkForCollision(null, destFile);
            if(collision!=FileCollisionChecker.NO_COLLOSION) {
                // File already exists in destination, ask the user what to do (cancel, overwrite,...) but
                // do not offer the multiple files mode options such as 'skip' and 'apply to all'.
                int action = new FileCollisionDialog(this, mainFrame, collision, null, destFile, false, false).getActionValue();

                // User chose to overwrite the file
                if (action== FileCollisionDialog.OVERWRITE_ACTION) {
                    // Do nothing, simply continue and file will be overwritten
                }
                // User chose to cancel or closed the dialog
                else {
                    return;
                }
            }

            if (trySave(destFile)) {
                this.file = destFile;
                editor.setCurrentFile(file);
                setTitle(editor.getTitle());
            }
        }
    }

    // Returns false if an error occurred while saving the file.
    public boolean trySave(AbstractFile destFile) {
        try {
            editor.saveAs(destFile);
            return true;
        }
        catch(IOException e) {
            InformationDialog.showErrorDialog(this, Translator.get("write_error"), Translator.get("file_editor.cannot_write"));
            return false;
        }
    }

    // Returns true if the file does not have any unsaved change or if the user refused to save the changes,
    // false if the user cancelled the dialog or the save failed.
    public boolean askSave() {
        if(!saveNeeded)
            return true;

        QuestionDialog dialog = new QuestionDialog(this, null, Translator.get("file_editor.save_warning"), this,
                                                   new String[] {Translator.get("save"), Translator.get("dont_save"), Translator.get("cancel")},
                                                   new int[]  {YES_ACTION, NO_ACTION, CANCEL_ACTION},
                                                   0);
        int ret = dialog.getActionValue();

        if((ret==YES_ACTION && trySave(file)) || ret==NO_ACTION) {
            setSaveNeeded(false);
            return true;
        }

        return false;       // User cancelled or the file couldn't be properly saved
    }




    ////////////////////////////
    // ActionListener methods //
    ////////////////////////////
	
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
		
        // File menu
        if (source==saveItem) {
            trySave(file);
        }		
        else if (source==saveAsItem) {
            trySaveAs();
        }		
        else if (source==closeItem) {
            dispose();
        }			
    }


    ////////////////////////
    // Overridden methods //
    ////////////////////////

    @Override
    public void pack() {
        super.pack();

        DialogToolkit.fitToScreen(this);
        DialogToolkit.fitToMinDimension(this, MIN_DIMENSION);

        DialogToolkit.centerOnWindow(this, mainFrame);
    }


    @Override
    public void dispose() {
        if(askSave())   /// Returns true if the file does not have any unsaved change or if the user refused to save the changes
            super.dispose();
    }
}
