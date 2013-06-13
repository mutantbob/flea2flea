/*
 * Copyright 2002
 * Robert Forsman <thoth@purplefrog.com>
 */
package com.purplefrog.flea2flea;

import java.io.*;
import java.util.*;

public class Offering
    implements Map.Entry, Comparable
{

    /**
     * What HTTP path refers to this downloadable?
     */
    private String tag;

    /**
     * When they issue a GET on {@link #tag} what file do we serve up?
     */
    public File resource;



    public Offering(String tag, File resource)
    {
        if ('/' != tag.charAt(0))
            tag = '/'+tag;
        this.tag = tag;
        this.resource = resource;
    }

    public String getTag()
    {
	return tag;
    }

    public void setTag(String tag)
    {
	if ('/' != tag.charAt(0))
	    tag = '/'+tag;
    	this.tag = tag;
    }
    //

    public Object getKey()
    {
        return tag;
    }

    public Object getValue()
    {
        return this;
    }

    public Object setValue(Object value)
    {
        throw new UnsupportedOperationException("You MUST be on crack");
    }
    //

    public int compareTo(Object o)
    {
        return tag.compareTo(((Offering)o).tag);
    }

}
