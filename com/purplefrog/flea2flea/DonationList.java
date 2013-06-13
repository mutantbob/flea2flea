/*
 * Copyright 2002
 * Robert Forsman <thoth@purplefrog.com>
 */
package com.purplefrog.flea2flea;

import java.util.*;
import javax.swing.table.*;

public class DonationList
{

    private List donations = new ArrayList();
    public DonationsTable.TModel tModel=null;

    public DonationList()
    {

    }

    public void add(Donation x)
    {
	donations.add(x);
	int row = donations.size();
	if (null != tModel)
	tModel.fireTableRowsInserted(row, row);
    }

    public Donation lookup(String tag)
    {
	for (Iterator iter = donations.iterator(); iter.hasNext();) {
	    Donation don = (Donation) iter.next();
	    if (!don.active) continue;
	    if (tag.equals(don.tag))
		return don;
	}
	return null;
    }

    public void deactivate(Donation don)
    {
	activate(don, false);

    }

    public void activate(Donation don, boolean active)
    {
	don.active = active;

	if (null == tModel)
	    return;

	int row =donations.indexOf(don);
	if (0 <= row)
	    tModel.fireTableRowsUpdated(row, row);
    }

    public TableModel getTableModel()
    {
	if (null == tModel)
	    tModel = new DonationsTable.TModel(donations);
	return tModel;
    }

    public void remove(int row)
    {
	donations.remove(row);
	tModel.fireTableRowsDeleted(row, row);
    }
}
