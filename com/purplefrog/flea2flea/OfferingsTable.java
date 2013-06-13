/*
 * Copyright 2002
 * Robert Forsman <thoth@purplefrog.com>
 */
package com.purplefrog.flea2flea;

import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;

public class OfferingsTable
    extends JTable
{
    public OfferingsTable(List offerings)
    {
        super(new TModel(offerings));
    }

    private static class TModel
        extends AbstractTableModel
    {
        private List offerings;

        private TModel(List offerings)
        {
            this.offerings = offerings;
        }

        public static final String[] names = {
            "URI",
            "file",
            //"listed",
        };
        public int getColumnCount()
        {
            return names.length;
        }

        public int getRowCount()
        {
            return offerings.size();
        }

        public String getColumnName(int column)
        {
            return names[column];
        }

        public boolean isCellEditable(int rowIndex, int columnIndex)
        {
            return true;
        }

        public void setValueAt(Object aValue, int rowIndex, int columnIndex)
        {
            Offering of = getRow(rowIndex);
            switch (columnIndex) {
                case 0:
                    of.setTag((String) aValue);
                    break;
                case 1:
                    of.resource = new File((String)aValue);
                    break;
                default:
                    throw new IllegalArgumentException("no such column "+columnIndex);
            }
        }

        public Object getValueAt(int rowIndex, int columnIndex)
        {
            Offering of = getRow(rowIndex);

            return getColumn(of, columnIndex);
        }

        private Offering getRow(int row)
        {
            return (Offering) offerings.get(row);
        }

        private static Object getColumn(Offering of, int col)
        {
            switch (col) {
                case 0: return of.getTag();
                case 1: return of.resource;
                default: throw new IllegalArgumentException("no such column "+col);
            }
        }

	public Class getColumnClass(int columnIndex)
	{
	    return String.class;
	}
    }
}
