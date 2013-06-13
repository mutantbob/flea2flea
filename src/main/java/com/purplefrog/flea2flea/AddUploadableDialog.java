/*
 * Copyright 2002
 * Robert Forsman <thoth@purplefrog.com>
 */
package com.purplefrog.flea2flea;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;

public class AddUploadableDialog
	extends JDialog
{
    public Donation donation=null;

    protected JTextField tag;
    protected FileBrowseInput dest;
    protected JTextField maxLength;

    public AddUploadableDialog(Frame owner)
    {
	super(owner, "Add Uploadable", true);
	
	Container p = getContentPane();
	p.setLayout(new BorderLayout());

	LabelInputRows middle = new LabelInputRows();
	p.add(middle);

	tag = middle.addRow("Tag: ");
	middle.addRow("Destination file:", dest = new FileBrowseInput());
	maxLength = middle.addRow("Maximum file size: ");

	JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
	p.add(bottom, BorderLayout.SOUTH);

	JButton ok = new JButton("OK");
	bottom.add(ok);
	ok.addActionListener(new OKCallback());

	JButton cancel = new JButton("Cancel");
	bottom.add(cancel);
	cancel.addActionListener(new ActionListener()
	{
	    public void actionPerformed(ActionEvent e)
	    {
		setVisible(false);
	    }
	});
    }

    private class OKCallback
	    implements ActionListener
    {
	public void actionPerformed(ActionEvent evt)
	{
	    File file = dest.getFile();
	    File parentFile = file.getParentFile();
	    if (parentFile.exists()) {
		if (!parentFile.isDirectory()) {
		    JOptionPane.showMessageDialog(AddUploadableDialog.this, parentFile+" is not a directory", "Oops", JOptionPane.ERROR_MESSAGE);
		    return;
		}
	    } else {
		if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(AddUploadableDialog.this, "Directory "+parentFile+" does not exist.  Create it?", "No Such Directory", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE)) {
		    if (!parentFile.mkdirs()) {
			// failed to create the directory
			JOptionPane.showMessageDialog(AddUploadableDialog.this, "Failed to create directory "+parentFile, "argh", JOptionPane.ERROR_MESSAGE);
			return;
		    }
		    // created the directory, keep going
		} else {
		    // decided not to create the directory
		    return;
		}
	    }
	    if (file.exists()) {
		String msg;
		if (file.isDirectory()) {
		    msg = file+" is a directory.  Your peers will be able to place new files in that directory with names of their choosing as long as this uploadable is active.";
		} else {
		    msg = file+" already exists.  If someone uploads, that file will be overwritten.";
		}
		if (JOptionPane.OK_OPTION !=
			JOptionPane.showConfirmDialog(AddUploadableDialog.this, msg,
				"Warning", JOptionPane.WARNING_MESSAGE, JOptionPane.OK_CANCEL_OPTION))
		    return;
	    }
	    

	    try {
		donation = new Donation(tag.getText(), file, Donation.parseFileSize(maxLength.getText()));
		setVisible(false);
	    } catch (NumberFormatException e1) {
		JOptionPane.showMessageDialog(AddUploadableDialog.this, "You must specify a maximum size.  \n" +
			"Examples: 1000000 , 4M, 1G.", "oops", JOptionPane.ERROR_MESSAGE);
	    }
	}

    }

    public static class FileBrowseInput
        extends JPanel
    {
        public JTextField text;
        public FileBrowseInput()
        {
            super(new BorderLayout());
            text = new JTextField(40);
            add(text);
            JButton button = new JButton("Browse");
            add(button, BorderLayout.EAST);
            button.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    JFileChooser dlg = new JFileChooser();
                    dlg.setSelectedFile(new File(text.getText()));
                    if (JFileChooser.APPROVE_OPTION == dlg.showDialog(FileBrowseInput.this, "OK")) {
                        text.setText(dlg.getSelectedFile().getPath());
                    }
                }
            });
        }

	public File getFile()
	{
	    return new File(text.getText());
	}
    }
}
