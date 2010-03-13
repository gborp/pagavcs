package hu.pagavcs.mug;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

import com.mucommander.file.AbstractFile;
import com.mucommander.file.UnsupportedFileOperationException;
import com.mucommander.text.Translator;
import com.mucommander.ui.action.ActionProperties;
import com.mucommander.ui.action.impl.AddBookmarkAction;
import com.mucommander.ui.dialog.DialogToolkit;
import com.mucommander.ui.dialog.FocusDialog;
import com.mucommander.ui.layout.XAlignedComponentPanel;
import com.mucommander.ui.layout.YBoxPanel;
import com.mucommander.ui.main.MainFrame;

/**
 * This dialog allows the user to add a bookmark and enter a name for it. User
 * can also choose to store login and password information in the bookmark's URL
 * if the bookmark contains login/password information.
 * 
 * @author Maxence Bernard
 */
public class FindFileDialog extends FocusDialog implements ActionListener {

	private JTextField             nameField;
	private JTextField             sfSearchText;
	private JTextField             sfSearchTextEncoding;
	private JCheckBox              cbCaseSensitive;
	private JCheckBox              cbSearchInArchive;

	private JButton                btnStartSearch;
	private JButton                btnCancel;

	// Dialog's width has to be at least 320
	private final static Dimension MINIMUM_DIALOG_DIMENSION = new Dimension(320, 0);

	// Dialog's width has to be at most 400
	private final static Dimension MAXIMUM_DIALOG_DIMENSION = new Dimension(400, 10000);
	private final MainFrame        mainFrame;

	public FindFileDialog(MainFrame mainFrame) {
		super(mainFrame, ActionProperties.getActionLabel(AddBookmarkAction.Descriptor.ACTION_ID), mainFrame);
		this.mainFrame = mainFrame;

		Container contentPane = getContentPane();
		YBoxPanel mainPanel = new YBoxPanel(5);

		final AbstractFile currentFolder = mainFrame.getActiveTable().getCurrentFolder();

		nameField = new JTextField("*");
		cbSearchInArchive = new JCheckBox("Search in archive");
		sfSearchText = new JTextField("");
		cbCaseSensitive = new JCheckBox("Case sensitive");
		sfSearchTextEncoding = new JTextField("UTF-8");

		XAlignedComponentPanel compPanel = new XAlignedComponentPanel();

		compPanel.addRow("Search in:", new JLabel(currentFolder.getPath()), 10);

		compPanel.addRow("Search for:", nameField, 10);
		compPanel.addRow(cbSearchInArchive, 10);
		compPanel.addRow("Find text:", sfSearchText, 10);
		compPanel.addRow(cbCaseSensitive, 10);
		compPanel.addRow("Text encoding:", sfSearchTextEncoding, 10);

		mainPanel.add(compPanel);

		contentPane.add(mainPanel, BorderLayout.NORTH);

		btnStartSearch = new JButton("Start search");
		btnStartSearch.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				startSearch(currentFolder);
			}

		});
		btnCancel = new JButton(Translator.get("cancel"));
		btnCancel.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		contentPane.add(DialogToolkit.createOKCancelPanel(btnStartSearch, btnCancel, getRootPane(), this), BorderLayout.SOUTH);

		// Select text in name field and transfer focus to it for immediate user
		// change
		nameField.selectAll();
		setInitialFocusComponent(nameField);

		// Packs dialog
		setMinimumSize(MINIMUM_DIALOG_DIMENSION);
		setMaximumSize(MAXIMUM_DIALOG_DIMENSION);

		showDialog();
	}

	private void startSearch(AbstractFile currentFolder) {
		try {
			String searchFileName = nameField.getText();
			String searchText = sfSearchText.getText();
			String searchTextEncoding = sfSearchTextEncoding.getText();
			boolean caseSensitive = cbCaseSensitive.isSelected();
			boolean searchInArchive = cbSearchInArchive.isSelected();
			Charset searchEncoding = null;

			searchFileName = searchFileName.trim();
			searchTextEncoding = searchTextEncoding.trim();
			if (searchFileName.isEmpty()) {
				searchFileName = "*";
			}

			searchFileName = searchFileName.replace(".", "\\.");
			searchFileName = searchFileName.replace("*", ".*");

			if (!searchText.isEmpty()) {

				if (!caseSensitive) {
					searchText = searchText.toUpperCase();
				}

				if (searchTextEncoding.isEmpty()) {
					searchTextEncoding = "ISO-8859-1";
				}
				searchEncoding = Charset.forName(searchTextEncoding);
			} else {
				searchText = null;
			}

			String findId = currentFolder + "|" + searchFileName + "|" + searchText + "|" + caseSensitive + "|" + searchInArchive + "|" + new Date();

			FindManager.getInstance().createResults(findId);

			search(findId, currentFolder, searchFileName, searchText, searchEncoding, caseSensitive, searchInArchive);
			dispose();

			mainFrame.getActivePanel().tryChangeCurrentFolder(new FindFileArchiveFile(findId, currentFolder));

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private void search(String findId, AbstractFile file, String searchFileName, String searchText, Charset searchEncoding, boolean caseSensitive,
	        boolean searchInArchive) {
		if (file.isDirectory() || (searchInArchive && file.isBrowsable())) {
			try {
				for (AbstractFile child : file.ls()) {
					search(findId, child, searchFileName, searchText, searchEncoding, caseSensitive, searchInArchive);
				}
			} catch (UnsupportedFileOperationException ex) {} catch (IOException ex) {}
		} else {
			if (file.getName().matches(searchFileName)) {
				if (searchText == null) {
					FindManager.getInstance().addResult(findId, file);
				} else {
					try {
						searchText(findId, file, searchText, searchEncoding, caseSensitive);
					} catch (UnsupportedFileOperationException ex) {} catch (IOException ex) {}
				}
			}
		}
	}

	private void searchText(String findId, AbstractFile file, String searchText, Charset searchEncoding, boolean caseSensitive)
	        throws UnsupportedFileOperationException, IOException {

		InputStreamReader in = new InputStreamReader(new BufferedInputStream(file.getInputStream(), 4 * 32768), searchEncoding);

		int j = 0;
		boolean found = false;

		int data;
		while ((data = in.read()) != -1) {

			char c = searchText.charAt(j);
			if (!caseSensitive) {
				data = Character.toUpperCase(data);
			}
			if (data == c) {
				j++;
				if (j == searchText.length()) {
					found = true;
					break;
				}
			} else {
				j = 0;
			}
		}

		in.close();

		if (found) {
			FindManager.getInstance().addResult(findId, file);
		}

	}

	public void actionPerformed(ActionEvent e) {}

}
