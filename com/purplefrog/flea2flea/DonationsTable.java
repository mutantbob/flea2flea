/*
 * Copyright 2002
 * Robert Forsman <thoth@purplefrog.com>
 */
package com.purplefrog.flea2flea;

import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;

public class DonationsTable
        extends JTable
{

    public DonationsTable(DonationList donations)
    {
        super(donations.getTableModel());
    }

    public static class TModel
            extends AbstractTableModel

   {
        public static final String[] columns = {
            "tag", "file destination", "max size", "active"
        };

        private List donations;

        public TModel(List donations) {
            this.donations = Collections.unmodifiableList(donations);
        }

        public int getRowCount() {
            return donations.size();
        }

        public int getColumnCount() {
            return columns.length;  //To change body of implemented methods use File | Settings | File Templates.
        }

	public Class getColumnClass(int columnIndex)
	{
	    switch (columnIndex) {
		case 0:
		case 1:
		case 2:
		    return Object.class;
		case 3:
		    return Boolean.class;
		default:
		    throw new IllegalArgumentException("no such column "+columnIndex);
	    }
	}

        public Object getValueAt(int rowIndex, int columnIndex) {
            Donation d = get(rowIndex);

            switch (columnIndex) {
                case 0: return d.tag;
                case 1: return d.destination;
                case 2: return new Long(d.maxSize);
		case 3: return Boolean.valueOf(d.active);
                default: throw new IllegalArgumentException("no such column "+columnIndex);
            }
        }

        private Donation get(int rowIndex) {
            return (Donation) donations.get(rowIndex);
        }

        public String getColumnName(int column) {
            return columns[column];
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return true;
        }

        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            Donation d = get(rowIndex);
            switch (columnIndex) {
                case 0: d.tag = (String)aValue; break;
                case 1: d.destination = new File((String) aValue); break;
                case 2: d.maxSize = Donation.parseFileSize((String)aValue); break;
		case 3: d.active = ((Boolean)aValue).booleanValue(); break;
                default: throw new IllegalArgumentException("no such column "+columnIndex);
            }
        }

    }
}
