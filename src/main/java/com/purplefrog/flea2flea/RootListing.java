/*
 * Copyright 2002
 * Robert Forsman <thoth@purplefrog.com>
 */
package com.purplefrog.flea2flea;

import java.io.*;
import java.util.*;
import java.text.*;

import org.mortbay.http.*;
import org.mortbay.http.handler.*;

public class RootListing
    extends AbstractHttpHandler
{
    protected Map offerings;

    public RootListing(Map offerings)
    {
        this.offerings = offerings;
    }

    public void handle(String pathInContext,
                       String pathParams,
                       HttpRequest request,
                       HttpResponse response)
        throws HttpException, IOException
    {
        if ( ! "/".equals(pathInContext))
            return;

        response.setStatus(200);
        response.setContentType("text/html");
        PrintWriter pw = new PrintWriter(response.getOutputStream());
        pw.println("<html>\n<head>\n<title>Flea 2 Flea public index</title>\n</head>");

        pw.println("<body>\n<h1>Flea 2 Flea public index</h1>");
        pw.println(offerings.size()+" advertised files available for download");
        if (0 < offerings.size()) {
	    String sortby = request.getParameter("sort");
	    pw.println(offeringsTable(sortby));
	}

        pw.println("\n<hr>");
        pw.println("<form action=\""+IncomingHandler.URI+"\" method=\"post\" enctype=\"multipart/form-data\">\n" +
            "<h2>upload</h2>\n"+
            "Tag: <input type=\"text\" name=\"tag\"><br>\n"+
            "local file to upload: <input type=\"file\" name=\"file\"> <br>\n" +
            "<input type=\"submit\" value=\"Send File\">\n"+
            "</form>");
        pw.println("</body>");
        pw.close();
    }

    StringBuffer offeringsTable(String sortby)
    {
        StringBuffer rval = new StringBuffer();

	DateFormat df = DateFormat.getDateTimeInstance();

        rval.append("<table>\n<tr> <th><a href=\"?sort=" +
		maybeReverse("-bypath", sortby)+"\">File</a></th> <th><a href=\"?sort=" +
		maybeReverse("bysize", sortby)+"\">size</a></th> <th><a href=\"?sort=" +
		maybeReverse("bytime", sortby) + "\">time</a></th> </tr>\n");


	Collection offerings = this.offerings.values();
	if ("bytime".equals(sortby)) {
	    ArrayList x = new ArrayList(offerings);
	    Collections.sort(x, new OfferingTimeComparator());
	    offerings = x;
	} else if ("-bytime".equals(sortby)) {
	    ArrayList x = new ArrayList(offerings);
	    Collections.sort(x, new ReverseOfferingTimeComparator());
	    offerings = x;
	} else if ("bypath".equals(sortby)) {
	    ArrayList x = new ArrayList(offerings);
	    Collections.sort(x);
	    offerings = x;
	} else if ("-bypath".equals(sortby)) {
	    ArrayList x = new ArrayList(offerings);
	    Collections.sort(x);
	    Collections.reverse(x);
	    offerings = x;
	} else if ("bysize".equals(sortby)) {
	    ArrayList x = new ArrayList(offerings);
	    Collections.sort(x, new OfferingSizeComparator());
	    offerings = x;
	} else if ("-bysize".equals(sortby)) {
	    ArrayList x = new ArrayList(offerings);
	    Collections.sort(x, new OfferingSizeComparator());
	    Collections.reverse(x);
	    offerings = x;
	}

	for (Iterator iter = offerings.iterator(); iter.hasNext();) {
            Offering of = (Offering) iter.next();
            rval.append("<tr><td>");
	    String htmlForTag = Main.htmlEncode(of.getTag());
	    String hrefForTag = Main.URLEncode(of.getTag());
	    rval.append("<a href=\""+hrefForTag+"\">"+htmlForTag+"</a>");
            rval.append("</td>\n");

            long length = of.resource.length();
            rval.append("<td align=right>"+length+"</td>");

	    long mtime = of.resource.lastModified();
	    rval.append("<td align=right>");

	    if (0 < mtime) {
		rval.append(substitute(df.format(new Date(mtime)), ' ', "&nbsp;"));
	    }
	    rval.append("</td>\n");

	    rval.append("</tr>\n");
        }
        rval.append("</table>\n" );

        return rval;
    }

    public static String maybeReverse(String targetKey, String current)
    {
	if (targetKey.equals(current)) {
	    if ('-' == targetKey.charAt(0))
		return targetKey.substring(1);
	    else
		return '-'+targetKey;
	} else {
	    return targetKey;
	}
    }

    private static String substitute(CharSequence str, char orig, String replacement)
    {
	StringBuffer rval = new StringBuffer();
	for (int i=0; i<str.length(); i++) {
	    char ch = str.charAt(i);
	    if (ch == orig)
		rval.append(replacement);
	    else
		rval.append(ch);
	}
	return rval.toString();
    }

    public static class ReverseOfferingTimeComparator
    extends OfferingTimeComparator
    {
	public int compare(Object o1, Object o2)
	{
	    return - super.compare(o1, o2);
	}
    }

    public static class OfferingTimeComparator
	    implements Comparator
    {
	public int compare(Object o1, Object o2)
	{
	    Offering a = (Offering) o1;
	    Offering b = (Offering) o2;
	    long mtime1 = a.resource.lastModified();
	    long mtime2 = b.resource.lastModified();
	    if (mtime1<mtime2)
		return 1;
	    else if (mtime1>mtime2)
		return -1;

	    return a.resource.compareTo(b.resource);
	}
    }

    public static class OfferingSizeComparator
	    implements Comparator
    {
	public int compare(Object o1, Object o2)
	{
	    Offering a = (Offering) o1;
	    Offering b = (Offering) o2;
	    long mtime1 = a.resource.length();
	    long mtime2 = b.resource.length();
	    if (mtime1<mtime2)
		return 1;
	    else if (mtime1>mtime2)
		return -1;

	    return a.resource.compareTo(b.resource);
	}
    }

}
