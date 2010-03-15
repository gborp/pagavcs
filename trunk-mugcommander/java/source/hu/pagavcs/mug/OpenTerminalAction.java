package hu.pagavcs.mug;

import java.awt.event.KeyEvent;
import java.util.Hashtable;

import javax.swing.KeyStroke;

import com.mucommander.desktop.DesktopManager;
import com.mucommander.file.AbstractArchiveEntryFile;
import com.mucommander.file.AbstractFile;
import com.mucommander.file.FileProtocols;
import com.mucommander.ui.action.AbstractActionDescriptor;
import com.mucommander.ui.action.ActionCategories;
import com.mucommander.ui.action.ActionCategory;
import com.mucommander.ui.action.ActionFactory;
import com.mucommander.ui.action.MuAction;
import com.mucommander.ui.action.impl.ParentFolderAction;
import com.mucommander.ui.dialog.InformationDialog;
import com.mucommander.ui.main.MainFrame;


public class OpenTerminalAction extends ParentFolderAction {

    public OpenTerminalAction(MainFrame mainFrame, Hashtable<String,Object> properties) {
        super(mainFrame, properties);

        setEnabled(DesktopManager.canOpenInFileManager());
    }

    @Override
    protected void toggleEnabledState() {
        AbstractFile currentFolder = mainFrame.getActiveTable().getCurrentFolder();
        setEnabled(currentFolder.getURL().getScheme().equals(FileProtocols.FILE)
               && !currentFolder.isArchive()
               && !currentFolder.hasAncestor(AbstractArchiveEntryFile.class)
        );
    }

    @Override
    public void performAction() {
        try {
			// TODO check for gnome
			ProcessBuilder pb = new ProcessBuilder("gnome-terminal", "--working-directory", mainFrame.getActivePanel().getCurrentFolder().getAbsolutePath());
			pb.start();
        }
        catch(Exception e) {
            InformationDialog.showErrorDialog(mainFrame);
        }
    }
    
    public static class Factory implements ActionFactory {

		public MuAction createAction(MainFrame mainFrame, Hashtable<String,Object> properties) {
			return new OpenTerminalAction(mainFrame, properties);
		}
    }
    
	public static class Descriptor extends AbstractActionDescriptor {

		public static final String ACTION_ID = "OpenTerminal";

		public String getId() {
			return ACTION_ID;
		}

		public ActionCategory getCategory() {
			return ActionCategories.NAVIGATION;
		}

		public KeyStroke getDefaultAltKeyStroke() {
			return null;
		}

		public KeyStroke getDefaultKeyStroke() {
			return KeyStroke.getKeyStroke(KeyEvent.VK_T, KeyEvent.CTRL_DOWN_MASK);
		}

	}
}
