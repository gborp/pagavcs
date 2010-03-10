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


package com.mucommander.ui.viewer.image;

import com.mucommander.file.AbstractFile;
import com.mucommander.text.Translator;
import com.mucommander.ui.helper.MenuToolkit;
import com.mucommander.ui.helper.MnemonicHelper;
import com.mucommander.ui.theme.*;
import com.mucommander.ui.viewer.FileViewer;
import com.mucommander.ui.viewer.ViewerFrame;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;


/**
 * A simple image viewer, capable of displaying <code>PNG</code>, <code>GIF</code> and <code>JPEG</code> images. 
 *
 * @author Maxence Bernard
 */
class ImageViewer extends FileViewer implements ActionListener, ThemeListener {
    private Image image;
    private Image scaledImage;
    private Color backgroundColor;
    private double zoomFactor;
	
    //	private JMenuItem prevImageItem;
    //	private JMenuItem nextImageItem;
    private JMenuItem zoomInItem;
    private JMenuItem zoomOutItem;
	
    public ImageViewer() {
        backgroundColor = ThemeManager.getCurrentColor(Theme.EDITOR_BACKGROUND_COLOR);
        ThemeManager.addCurrentThemeListener(this);
    }	

    private synchronized void loadImage(AbstractFile file) throws IOException {
        ViewerFrame frame = getFrame();
        frame.setCursor(new Cursor(Cursor.WAIT_CURSOR));
		
        int read;
        byte buffer[] = new byte[1024];
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        InputStream in = file.getInputStream();
        while ((read=in.read(buffer, 0, buffer.length))!=-1)
            bout.write(buffer, 0, read);

        byte imageBytes[] = bout.toByteArray();
        bout.close();
        in.close();

        this.scaledImage = null;
        this.image = getToolkit().createImage(imageBytes);

        waitForImage(image);

        int width = image.getWidth(null);
        int height = image.getHeight(null);
        this.zoomFactor = 1.0;
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();

        while(width>d.width || height>d.height) {
            width = width/2;
            height = height/2;
            zoomFactor = zoomFactor/2;
        }
		
        if(zoomFactor==1.0)
            this.scaledImage = image;
        else
            zoom(zoomFactor);
			
        checkZoom();
        frame.setCursor(Cursor.getDefaultCursor());
    }

	
    private void waitForImage(Image image) {
        //AppLogger.finest("Waiting for image to load "+image);
        MediaTracker tracker = new MediaTracker(this);
        tracker.addImage(image, 0);
        try { tracker.waitForID(0); }
        catch(InterruptedException e) {}
        tracker.removeImage(image);
        //AppLogger.finest("Image loaded "+image);
    }
	
	
    private synchronized void zoom(double factor) {
        ViewerFrame frame = getFrame();

        frame.setCursor(new Cursor(Cursor.WAIT_CURSOR));

        this.scaledImage = image.getScaledInstance((int)(image.getWidth(null)*factor), (int)(image.getHeight(null)*factor), Image.SCALE_DEFAULT);
        waitForImage(scaledImage);

        frame.setCursor(Cursor.getDefaultCursor());
    }

    private void updateFrame() {
        ViewerFrame frame = getFrame();

        // Revalidate, pack and repaint should be called in this order
        frame.setTitle(this.getTitle());
        revalidate();
        frame.pack();
        frame.getContentPane().repaint();
    }

    private void checkZoom() {
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		
        zoomInItem.setEnabled(zoomFactor<1.0 || (2*zoomFactor*image.getWidth(null) < d.width
                                                 && 2*zoomFactor*image.getHeight(null) < d.height));

        zoomOutItem.setEnabled(zoomFactor>1.0 || (zoomFactor/2*image.getWidth(null)>160
                                                  && zoomFactor/2*image.getHeight(null)>120));
    }

    /*
      private synchronized void goToImage(boolean next) {
      AbstractFile newFile;
      if(next)
      newFile = getNextFileInFolder(file, true);
      else
      newFile = getPreviousFileInFolder(file, true);

      if(newFile!=null) {
      try {
      loadImage(newFile);
      frame.setCurrentFile(newFile);
      updateFrame();
      }
      catch(IOException ex) {
      }
      }
      }
    */


    ///////////////////////////////
    // FileViewer implementation //
    ///////////////////////////////

    @Override
    public void view(AbstractFile file) throws IOException {

        ViewerFrame frame = getFrame();
        if(frame!=null) {
            MnemonicHelper menuMnemonicHelper = new MnemonicHelper();

            // Create Go menu
            JMenu controlsMenu = MenuToolkit.addMenu(Translator.get("image_viewer.controls_menu"), menuMnemonicHelper, null);
            //		nextImageItem = MenuToolkit.addMenuItem(controlsMenu, Translator.get("image_viewer.next_image"), menuItemMnemonicHelper, KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), this);
            //		prevImageItem = MenuToolkit.addMenuItem(controlsMenu, Translator.get("image_viewer.previous_image"), menuItemMnemonicHelper, KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), this);
            //		controlsMenu.add(new JSeparator());
            zoomInItem = MenuToolkit.addMenuItem(controlsMenu, Translator.get("image_viewer.zoom_in"), menuMnemonicHelper, KeyStroke.getKeyStroke(KeyEvent.VK_ADD, 0), this);
            zoomOutItem = MenuToolkit.addMenuItem(controlsMenu, Translator.get("image_viewer.zoom_out"), menuMnemonicHelper, KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, 0), this);

            getMenuBar().add(controlsMenu);
        }

        loadImage(file);
    }
	

    ////////////////////////
    // Overridden methods //
    ////////////////////////

    @Override
    public void paint(Graphics g) {
        int width = getWidth();
        int height = getHeight();

        g.setColor(backgroundColor);
        g.fillRect(0, 0, width, height);

        if(scaledImage!=null) {
            int imageWidth = scaledImage.getWidth(null);
            int imageHeight = scaledImage.getHeight(null);
            g.drawImage(scaledImage, Math.max(0, (width-imageWidth)/2), Math.max(0, (height-imageHeight)/2), null);
        }
    }


    ///////////////////////////////////
    // ActionListener implementation //
    ///////////////////////////////////

    @Override
    public String getTitle() {
        return super.getTitle()+" - "+image.getWidth(null)+"x"+image.getHeight(null)+" - "+((int)(zoomFactor*100))+"%";
    }

    @Override
    public synchronized Dimension getPreferredSize() {
        return new Dimension(scaledImage.getWidth(null), scaledImage.getHeight(null));
    }

    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
		
        //		if(source == prevImageItem)
        //			goToImage(false);
        //		else if(source == nextImageItem)
        //			goToImage(true);
        //		else {

        if(source==zoomInItem && zoomInItem.isEnabled()) {
            zoomFactor = zoomFactor*2;
            zoom(zoomFactor);
            updateFrame();
        }
        else if(source==zoomOutItem && zoomOutItem.isEnabled()) {
            zoomFactor = zoomFactor/2;
            zoom(zoomFactor);
            updateFrame();
        }
			
        checkZoom();
        //		}
    }


    //////////////////////////////////
    // ThemeListener implementation //
    //////////////////////////////////

    /**
     * Receives theme color changes notifications.
     */
    @Override
    public void colorChanged(ColorChangedEvent event) {
        if(event.getColorId() == Theme.EDITOR_BACKGROUND_COLOR) {
            backgroundColor = event.getColor();
            repaint();
        }
    }

    /**
     * Not used, implemented as a no-op.
     */
    @Override
    public void fontChanged(FontChangedEvent event) {}
}
