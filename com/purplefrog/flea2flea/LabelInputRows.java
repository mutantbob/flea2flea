/*
 * Copyright 2002
 * Robert Forsman <thoth@purplefrog.com>
 */
package com.purplefrog.flea2flea;

import java.awt.*;
import javax.swing.*;

public class LabelInputRows
	extends JPanel
{
    public GridBagConstraints gbc;

    public LabelInputRows()
    {
	super(new GridBagLayout());

	gbc = new GridBagConstraints();
	gbc.gridy = 0;
	gbc.fill = GridBagConstraints.HORIZONTAL;
    }

    public JTextField addRow(String label)
    {
	JTextField rval = new JTextField(40);
	addRow(label, rval);
	return rval;
    }

    public void addRow(String label, Component rval)
    {
	add(new JLabel(label), gbc);
	add(rval, gbc);
	gbc.gridy++;
    }
}
