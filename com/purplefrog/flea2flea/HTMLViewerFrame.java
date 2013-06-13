/*
 * Copyright 2002
 * Robert Forsman <thoth@purplefrog.com>
 */
package com.purplefrog.flea2flea;

import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.io.*;
import javax.swing.*;
import javax.swing.event.*;

public class HTMLViewerFrame
	extends JFrame
{
    private JEditorPane editorPane;

    public HTMLViewerFrame(URL url)
    {
	super("Documentation");

	Container c = getContentPane();

	c.setLayout(new BorderLayout());

	editorPane = new JEditorPane();
	editorPane.setContentType("text/html");
	editorPane.addHyperlinkListener(new HyperlinkListener()
	{
	    public void hyperlinkUpdate(HyperlinkEvent e)
	    {
//		System.out.println(e);
		if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED))
		    setPage(e.getURL());
	    }
	});
	setPage(url);
	editorPane.setEditable(false);

	c.add(new JScrollPane(editorPane));

	JButton dismiss = new JButton("Dismiss");
	dismiss.addActionListener(new ActionListener()
	{
	    public void actionPerformed(ActionEvent e)
	    {
		setVisible(false);
	    }
	});
	c.add(dismiss, BorderLayout.SOUTH);

	pack();
    }

    public Dimension getPreferredSize()
    {
	return new Dimension(640, 480);
    }

    private void setPage(URL url)
    {
	if (null == url) {
	    editorPane.setText("null");
	    return;
	}

	try {
	    editorPane.setPage(url);
	} catch (IOException e) {
	    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
	}

    }

    public void selfDestruct(final int seconds)
    {
        Thread t = new Thread(new Runnable() {
            public void run()
            {
                try {
                    Thread.sleep(seconds*1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                setVisible(false);
            }
        }, "pop-down about");
        t.start();
    }

}
