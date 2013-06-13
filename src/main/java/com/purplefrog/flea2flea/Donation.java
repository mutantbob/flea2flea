/*
 * Copyright 2002
 * Robert Forsman <thoth@purplefrog.com>
 */
package com.purplefrog.flea2flea;

import java.io.*;
import java.util.*;

public class Donation
	implements Map.Entry, Comparable
{
    public String tag;
    public File destination;
    public long maxSize;
    public boolean active=true;

    public Donation(String tag, File destination, long maxSize) {
	this.tag = tag;
	this.destination = destination;
	this.maxSize = maxSize;
    }

    public Object getKey()
    {
	return active ? tag : null;
    }

    public Object getValue()
    {
	return this;
    }

    public Object setValue(Object value)
    {
	throw new UnsupportedOperationException("You MUST be on crack.");
    }

    public int compareTo(Object o)
    {
	return tag.compareTo(((Donation)o).tag);
    }

    public static long parseFileSize(String str) {
	long factor = 1;
	char last = Character.toLowerCase(str.charAt(str.length()-1));
	if ('g' == last) {
	    factor = 1L<<30;
	    str = str.substring(0, str.length()-1);
	} else if ('m' == last) {
	    factor = 1L<<20;
	    str = str.substring(0, str.length()-1);
	} else if ('k' == last) {
	    factor = 1L<<10;
	    str = str.substring(0, str.length()-1);
	}
	return factor* Long.parseLong(str);
    }
}
